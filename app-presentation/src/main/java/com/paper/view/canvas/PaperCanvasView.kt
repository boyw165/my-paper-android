// Copyright Feb 2018-present boyw165@gmail.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.view.canvas

import android.content.Context
import android.graphics.*
import android.os.Looper
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.IAllGesturesListener
import com.cardinalblue.gesture.MyMotionEvent
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.paper.AppConst
import com.paper.R
import com.paper.domain.DomainConst
import com.paper.domain.data.GestureRecord
import com.paper.domain.event.*
import com.paper.domain.util.TransformUtils
import com.paper.domain.widget.editor.IPaperCanvasWidget
import com.paper.domain.widget.editor.IScrapWidget
import com.paper.model.Point
import com.paper.model.Rect
import com.paper.model.repository.IBitmapRepo
import com.paper.model.sketch.PenType
import com.paper.view.IWidgetView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class PaperCanvasView : View,
                        IWidgetView<IPaperCanvasWidget>,
                        IPaperContext,
                        IParentView,
                        IAllGesturesListener {

    // Scraps.
    private val mScrapViews = mutableListOf<IScrapView>()
    private var mIfSharpenDrawing = true

    // Widget.
    private lateinit var mWidget: IPaperCanvasWidget
    private val mDisposables = CompositeDisposable()

    /**
     * A signal indicating the layout change.
     */
    private val mOnLayoutChangeSignal = BehaviorSubject.createDefault(false)

    // Temporary utils.
    private val mTmpPoint = FloatArray(2)
    private val mTmpBound = RectF()
    private val mTmpMatrix = Matrix()
    private val mTmpMatrixInverse = Matrix()
    private val mTmpMatrixStart = Matrix()

    /**
     * A util for getting translationX, translationY, scaleX, scaleY, and
     * rotationInDegrees from a [Matrix]
     */
    private val mTransformHelper = TransformUtils()

    private val mTextDetector by lazy { FirebaseVision.getInstance().visionTextDetector }
    private var mTextDetectorImage: FirebaseVisionImage? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
        : super(context, attrs, defStyleAttr) {
        mGridPaint.color = Color.LTGRAY
        mGridPaint.style = Paint.Style.STROKE
        mGridPaint.strokeWidth = 2f * mOneDp
    }

    override fun bindWidget(widget: IPaperCanvasWidget) {
        ensureMainThread()
        ensureNoLeakingSubscription()

        mWidget = widget

        // Canvas size change
        mDisposables.add(
            Observables.combineLatest(
                mOnLayoutChangeSignal,
                widget.onSetCanvasSize())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (changed, size) ->
                    if (changed &&
                        size.width > 0 &&
                        size.height > 0) {
                        println("${AppConst.TAG}: the layout is done, and canvas " +
                                "size is ${size.width} x ${size.height}")
                        onUpdateLayoutOrCanvas(size.width,
                                               size.height)
                    }
                })

        // Drawing
        mDisposables.add(
            mReadySignal
                .switchMap { ready ->
                    if (ready) {
                        widget.onDrawSVG(replayAll = true)
                            .startWith(ClearAllSketchEvent())
                    } else {
                        Observable.never()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    onDrawSVG(event)
                })
        mDisposables.add(
            mReadySignal
                .switchMap { ready ->
                    if (ready) {
                        onSaveBitmap()
                    } else {
                        Observable.never()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (bmpFile, bmpWidth, bmpHeight) ->
                    widget.setThumbnail(bmpFile, bmpWidth, bmpHeight)
                })

        // View port and canvas matrix change
        mDisposables.add(
            mReadySignal
                .switchMap { ready ->
                    if (ready) {
                        mViewPortSignal
                    } else {
                        Observable.never<RectF>()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vp ->
                    // Would trigger onDraw() call
                    markCanvasMatrixDirty()

                    // Notify any external observers
                    mDrawViewPortSignal.onNext(DrawViewPortEvent(
                        canvas = mMSize.value!!.copy(),
                        viewPort = Rect(vp.left,
                                        vp.top,
                                        vp.right,
                                        vp.bottom)))
                })

        // Add or remove scraps
        mDisposables.add(
            mReadySignal
                .switchMap { ready ->
                    if (ready) {
                        widget.onAddScrapWidget()
                    } else {
                        Observable.never()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scrapWidget ->
                    addScrap(scrapWidget)
                })
        mDisposables.add(
            mReadySignal
                .switchMap { ready ->
                    if (ready) {
                        widget.onRemoveScrapWidget()
                    } else {
                        Observable.never()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scrapWidget ->
                    removeScrap(scrapWidget)
                })

        // Debug
        mDisposables.add(
            widget.onPrintDebugMessage()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                })
    }

    override fun unbindWidget() {
        mDisposables.clear()

        mScrapViews.forEach { scrapView ->
            scrapView.unbindWidget()
        }
    }

    override fun onMeasure(widthSpec: Int,
                           heightSpec: Int) {
        println("${AppConst.TAG}: PaperCanvasView # onMeasure()")
        super.onMeasure(widthSpec, heightSpec)
    }

    override fun onLayout(changed: Boolean,
                          left: Int,
                          top: Int,
                          right: Int,
                          bottom: Int) {
        println("${AppConst.TAG}: PaperCanvasView # onLayout(changed=$changed)")
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            mOnLayoutChangeSignal.onNext(changed)
        }
    }

    // Add / Remove Scraps /////////////////////////////////////////////////////

    private fun addScrap(widget: IScrapWidget) {
        val scrapView = ScrapView(drawMode = mDrawMode,
                                  eraserMode = mEraserMode)

        scrapView.setPaperContext(this)
        scrapView.setParent(this)
        scrapView.bindWidget(widget)
        mScrapViews.add(scrapView)

        invalidate()
    }

    private fun removeScrap(widget: IScrapWidget) {
        val scrapView = mScrapViews.firstOrNull { it == widget }
                        ?: throw NoSuchElementException("Cannot find the widget")

        scrapView.unbindWidget()
        mScrapViews.remove(scrapView)

        invalidate()
    }

    // Drawing ////////////////////////////////////////////////////////////////

    /**
     * A signal indicating whether it's ready to interact with the user. see
     * [onUpdateLayoutOrCanvas].
     */
    private val mReadySignal = PublishSubject.create<Boolean>()

    /**
     * A signal of updating the canvas hash and Bitmap.
     */
    private val mUpdateBitmapSignal = PublishSubject.create<Bitmap>()

    /**
     * Model canvas size.
     */
    private val mMSize = BehaviorSubject.createDefault(Rect())
    /**
     * Scale factor from Model world to View world.
     */
    private val mScaleM2V = BehaviorSubject.createDefault(Float.NaN)

    /**
     * The matrix used for mapping a point from the canvas world to view port
     * world in the View perspective.
     *
     * @sample
     * .---------------.
     * |               | ----> View canvas
     * |       .---.   |
     * |       | V | --------> View port
     * |       | P |   |
     * |       '---'   |
     * '---------------'
     */
    private val mCanvasMatrix = Matrix()
    /**
     * The inverse matrix of [mCanvasMatrixInverse], which is used to mapping a
     * point from view port world to canvas world in the View perspective.
     */
    private val mCanvasMatrixInverse = Matrix()
    private var mCanvasMatrixDirty = false

    // Rendering resource.
    private val mOneDp by lazy { context.resources.getDimension(R.dimen.one_dp) }
    private val mMinStrokeWidth: Float by lazy { resources.getDimension(R.dimen.sketch_min_stroke_width) }
    private val mMaxStrokeWidth: Float by lazy { resources.getDimension(R.dimen.sketch_max_stroke_width) }
    private val mMatrixStack = Stack<Matrix>()

    /**
     * The Bitmap in which the sketch and the scraps are drawn to.
     */
    private var mBitmap: Bitmap? = null
    private val mBitmapPaint = Paint()
    private val mDrawMode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    private val mEraserMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    /**
     * The canvas used in the [dispatchDrawScraps] call.
     */
    private lateinit var mBitmapCanvas: Canvas

    // Background & grids
    private val mGridPaint = Paint()

    // Temporary strokes
    private val mStrokeDrawables = mutableListOf<SVGDrawable>()

    private fun onUpdateLayoutOrCanvas(canvasWidth: Float,
                                       canvasHeight: Float) {
        // Flag not ready for interacting with user.
        mReadySignal.onNext(false)

        // The maximum view port, a rectangle as the same width over
        // height ratio and it just fits in the canvas rectangle as
        // follow:
        // .-------.-----.-----.
        // |       |/////|     | <-- model canvas size
        // |       |/////| <-------- view port observed in model world
        // |       |/////|     |
        // |       |/////|     |
        // '-------'-----'-----'
        //
        // The minimum view port, a rectangle N times as small as the
        // max view port.
        // .-------------------.
        // |         .--.      | <-- model canvas size
        // |         '--'  <-------- min view port
        // |                   |
        // |                   |
        // '-------------------'
        val spaceWidth = this.width - ViewCompat.getPaddingStart(this) - ViewCompat.getPaddingEnd(this)
        val spaceHeight = this.height - paddingTop - paddingBottom
        val maxScale = Math.min(canvasWidth / spaceWidth,
                                canvasHeight / spaceHeight)
        val minScale = maxScale / DomainConst.VIEW_PORT_MIN_SCALE
        val scaleM2V = 1f / maxScale
        mViewPortMax.set(0f, 0f, maxScale * spaceWidth, maxScale * spaceHeight)
        mViewPortMin.set(0f, 0f, minScale * spaceWidth, minScale * spaceHeight)
        mViewPortBase.set(mViewPortMax)

        // Hold canvas size.
        mMSize.onNext(Rect(0f, 0f, canvasWidth, canvasHeight))

        // Initially the model-to-view scale is derived by the scale
        // from min view port boundary to the view boundary.
        // Check out the figure above :D
        mScaleM2V.onNext(scaleM2V)

        // Determine the default view-port (makes sense when view
        // layout is changed).
        resetViewPort(canvasWidth,
                      canvasHeight,
                      mViewPortMax.width(),
                      mViewPortMax.height())

        // Backed the canvas Bitmap.
        val mw = mMSize.value!!.width
        val mh = mMSize.value!!.height
        val vw = scaleM2V * mw
        val vh = scaleM2V * mh
        mBitmap?.recycle()
        mBitmap = Bitmap.createBitmap(vw.toInt(), vh.toInt(), Bitmap.Config.ARGB_8888)
        mBitmapCanvas = Canvas(mBitmap)

        invalidate()

        mReadySignal.onNext(true)
    }

    override fun requestSharpDrawing() {
        mIfSharpenDrawing = true
        invalidate()
    }

    private fun dispatchDrawScraps(canvas: Canvas,
                                   scrapViews: List<IScrapView>,
                                   ifSharpenDrawing: Boolean) {
        // Hold canvas matrix.
        mTmpMatrix.set(mCanvasMatrix)

        // Prepare the transform stack for later sharp rendering.
        mMatrixStack.clear()
        mMatrixStack.push(mCanvasMatrix)

        scrapViews.forEach { scrapView ->
            scrapView.dispatchDraw(canvas, mMatrixStack, ifSharpenDrawing)
        }

        // Ensure no scraps modify the canvas matrix.
        if (mTmpMatrix != mCanvasMatrix) {
            throw IllegalStateException("Canvas matrix is changed")
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isAllSet) return

        // Scale from model to view.
        val scaleM2V = mScaleM2V.value!!
        // Scale from view to model
        val scaleV2M = 1f / scaleM2V
        // Scale contributed by view port.
        val mw = mMSize.value!!.width
        val mh = mMSize.value!!.height
        val vw = scaleM2V * mw
        val vh = scaleM2V * mh

        val count = canvas.save()

        // Calculate the view port matrix.
        computeCanvasMatrix(scaleM2V)
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        // View might have padding, if so we need to shift canvas to show
        // padding on the screen.
        canvas.translate(ViewCompat.getPaddingStart(this).toFloat(), paddingTop.toFloat())

        // Extract the transform from the canvas matrix.
        mTransformHelper.getValues(mCanvasMatrix)
        val tx = mTransformHelper.translationX
        val ty = mTransformHelper.translationY
        val scaleVP = mTransformHelper.scaleX

        // Manually calculate position and size of the background cross/grids so
        // that they keep sharp!
        drawBackground(canvas, vw, vh, tx, ty, scaleVP)

        // Draw sketch and scraps
        var dirty = false
        if (mIfSharpenDrawing) {
            dispatchDrawScraps(mBitmapCanvas, mScrapViews, true)

            mStrokeDrawables.forEach { drawable ->
                drawable.onDraw(canvas, mCanvasMatrix)
            }

            dirty = true
        } else {
            // To view canvas world.
            canvas.concat(mCanvasMatrix)

            // TODO: Both scraps and sketch need to explicitly define the z-order
            // TODO: so that the paper knows how to render them in the correct
            // TODO: order.

            dispatchDrawScraps(mBitmapCanvas, mScrapViews, false)

            mStrokeDrawables.forEach { drawable ->
                dirty = drawable.onDraw(mBitmapCanvas) || dirty
            }
        }

        canvas.drawBitmap(mBitmap, 0f, 0f, mBitmapPaint)

        // Notify Bitmap update
        if (dirty) {
            mBitmap?.let { mUpdateBitmapSignal.onNext(it) }
        }

        // Turn off the sharpening draw because it's costly.
        mIfSharpenDrawing = false

        canvas.restoreToCount(count)
    }

    private fun onDrawSVG(event: DrawSVGEvent) {
        mIsHashDirty = true

        when (event) {
            is StartSketchEvent -> {
                val nx = event.point.x
                val ny = event.point.y
                val (x, y) = toViewWorld(nx, ny)

                val drawable = SVGDrawable(
                    context = this@PaperCanvasView,
                    penColor = event.penColor,
                    penSize = event.penSize,
                    porterDuffMode = if (event.penType == PenType.ERASER) mEraserMode else mDrawMode)
                drawable.moveTo(Point(x, y, event.point.time))

                mStrokeDrawables.add(drawable)
            }
            is OnSketchEvent -> {
                val nx = event.point.x
                val ny = event.point.y
                val (x, y) = toViewWorld(nx, ny)

                val drawable = mStrokeDrawables.last()
                drawable.lineTo(Point(x, y, event.point.time))
            }
            is StopSketchEvent -> {
                val drawable = mStrokeDrawables.last()
                drawable.close()
            }
            is ClearAllSketchEvent -> {
                mStrokeDrawables.clear()
            }
            else -> {
                // NOT SUPPORT
            }
        }

        invalidate()
    }

    private fun onSaveBitmap(): Observable<Triple<File, Int, Int>> {
        return mUpdateBitmapSignal
            .map { bmp -> Pair(hashCode(), bmp) }
            .filter { (hashCode, _) -> hashCode != AppConst.EMPTY_HASH }
            // Notify widget the thumbnail need to be update
            .doOnNext { mWidget.invalidateThumbnail() }
            // Debounce Bitmap writes
            .debounce(1000, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            // TEST: text recognition
            .observeOn(Schedulers.computation())
            .doOnNext { (_, bmp) ->
                println("${AppConst.TAG}: Ready to feed Bitmap to text detector")
                val image = FirebaseVisionImage.fromBitmap(bmp)
                mTextDetector.detectInImage(image)
                    .addOnSuccessListener { visionText ->
                        val builder = StringBuilder()
                        visionText.blocks.forEach { block ->
                            builder.append("[")
                            block.lines.forEachIndexed { i, line ->
                                line.elements.forEachIndexed { j, element ->
                                    builder.append(element.text)

                                    if (line.elements.size > 1 &&
                                        j < line.elements.lastIndex) {
                                        builder.append(" ")
                                    }
                                }

                                if (block.lines.size > 1 &&
                                    i < block.lines.lastIndex) {
                                    builder.append(",")
                                }
                            }
                            builder.append("]")
                        }
                        println("${AppConst.TAG}: successful => $builder")
                        Toast.makeText(context, builder.toString(), Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { err ->
                        println("${AppConst.TAG}: failed => $err")
                        Toast.makeText(context, err.toString(), Toast.LENGTH_SHORT).show()
                    }
            }
            .switchMap { (hashCode, bmp) ->
                mBitmapRepo
                    ?.putBitmap(hashCode, bmp)
                    ?.toObservable()
                    ?.map { bmpFile -> Triple(bmpFile, bmp.width, bmp.height) }
                ?: Observable.never<Triple<File, Int, Int>>()
            }
    }

    private var mBitmapRepo: IBitmapRepo? = null

    fun setBitmapRepo(repo: IBitmapRepo) {
        mBitmapRepo = repo
    }

    private fun markCanvasMatrixDirty() {
        mCanvasMatrixDirty = true

        invalidate()
    }

    // View port //////////////////////////////////////////////////////////////

    /**
     * The view-port boundary in the model world.
     */
    private val mViewPortSignal = BehaviorSubject.create<RectF>()
    /**
     * Minimum size of [mViewPortSignal].
     */
    private val mViewPortMin = RectF()
    /**
     * Maximum size of [mViewPortSignal].
     */
    private val mViewPortMax = RectF()
    /**
     * The initial view port size that is used to compute the scale factor from
     * Model to View, which is [mScaleM2V].
     */
    private val mViewPortBase = RectF()
    /**
     * The signal for external observers
     */
    private val mDrawViewPortSignal = BehaviorSubject.create<DrawViewPortEvent>()

    fun onDrawViewPort(): Observable<DrawViewPortEvent> {
        return mDrawViewPortSignal
    }

    fun setViewPortPosition(x: Float, y: Float) {
        val mw = mMSize.value!!.width
        val mh = mMSize.value!!.height

        mTmpBound.set(x, y,
                      x + mViewPortSignal.value!!.width(),
                      y + mViewPortSignal.value!!.height())

        // Constraint view port
        val minWidth = mViewPortMin.width()
        val minHeight = mViewPortMin.height()
        val maxWidth = mViewPortMax.width()
        val maxHeight = mViewPortMax.height()
        constraintViewPort(mTmpBound,
                           left = 0f,
                           top = 0f,
                           right = mw,
                           bottom = mh,
                           minWidth = minWidth,
                           minHeight = minHeight,
                           maxWidth = maxWidth,
                           maxHeight = maxHeight)

        mViewPortSignal.onNext(mTmpBound)
    }

    private fun resetViewPort(mw: Float,
                              mh: Float,
                              defaultW: Float,
                              defaultH: Float) {
//        // Place the view port center in the model world.
//        val viewPortX = (mw - defaultW) / 2
//        val viewPortY = (mh - defaultH) / 2

        // Place the view port left in the model world.
        val viewPortX = 0f
        val viewPortY = 0f
        mViewPortSignal.onNext(RectF(viewPortX, viewPortY,
                               viewPortX + defaultW,
                               viewPortY + defaultH))
    }

    /**
     * Compute the [mCanvasMatrix] given [mViewPortSignal].
     *
     * @param scaleM2V The scale from model to view.
     */
    private fun computeCanvasMatrix(scaleM2V: Float) {
        if (mCanvasMatrixDirty && mViewPortSignal.hasValue()) {
            // View port x
            val vx = mViewPortSignal.value!!.left
            // View port y
            val vy = mViewPortSignal.value!!.top
            // View port width
            val vw = mViewPortSignal.value!!.width()
            val scaleVP = mViewPortBase.width() / vw

            mCanvasMatrix.reset()
            //        canvas width
            // .-------------------------.
            // |h         .---.          |
            // |e         | V | --------------> view port
            // |i         | P |          |
            // |g         '---'          | ---> model
            // |h                        |
            // |t                        |
            // '-------------------------'
            mCanvasMatrix.postScale(scaleVP,
                                    scaleVP)
            mCanvasMatrix.postTranslate(-scaleVP * scaleM2V * vx,
                                        -scaleVP * scaleM2V * vy)
            mCanvasMatrix.invert(mCanvasMatrixInverse)

            mCanvasMatrixDirty = false
        }
    }

    private fun startUpdateViewport() {
        // Hold necessary starting states.
        mTmpMatrixStart.set(mCanvasMatrix)
    }

    // TODO: Make the view-port code a component.
    private fun onUpdateViewport(startPointers: Array<PointF>,
                                 stopPointers: Array<PointF>) {
        val mw = mMSize.value!!.width
        val mh = mMSize.value!!.height
        val scaleM2V = mScaleM2V.value!!

        // Compute new canvas matrix.
        val transform = TransformUtils.getTransformFromPointers(
            startPointers, stopPointers)
        val dx = transform[TransformUtils.DELTA_X]
        val dy = transform[TransformUtils.DELTA_Y]
        val dScale = transform[TransformUtils.DELTA_SCALE_X]
        val px = transform[TransformUtils.PIVOT_X]
        val py = transform[TransformUtils.PIVOT_Y]
        mTmpMatrix.set(mTmpMatrixStart)
        mTmpMatrix.postScale(dScale, dScale, px, py)
        mTmpMatrix.postTranslate(dx, dy)
        mTmpMatrix.invert(mTmpMatrixInverse)

        // Compute new view port bound in the view world:
        // .-------------------------.
        // |          .---.          |
        // | View     | V |          |
        // | Canvas   | P |          |
        // |          '---'          |
        // |                         |
        // '-------------------------'
        mTransformHelper.getValues(mTmpMatrix)
        val tx = mTransformHelper.translationX
        val ty = mTransformHelper.translationY
        val scaleVP = mTransformHelper.scaleX
        val vx = -tx / scaleVP / scaleM2V
        val vy = -ty / scaleVP / scaleM2V
        val vw = mViewPortMax.width() / scaleVP
        val vh = mViewPortMax.height() / scaleVP
        mTmpBound.set(vx, vy, vx + vw, vy + vh)

        // Constraint view port
        val minWidth = mViewPortMin.width()
        val minHeight = mViewPortMin.height()
        val maxWidth = mViewPortMax.width()
        val maxHeight = mViewPortMax.height()
        constraintViewPort(mTmpBound,
                           left = 0f,
                           top = 0f,
                           right = mw,
                           bottom = mh,
                           minWidth = minWidth,
                           minHeight = minHeight,
                           maxWidth = maxWidth,
                           maxHeight = maxHeight)

        // Apply final view port boundary
        mViewPortSignal.onNext(RectF(mTmpBound))
    }

    private fun stopUpdateViewport() {
        // TODO: Upsample?
    }

    private fun constraintViewPort(viewPort: RectF,
                                   left: Float,
                                   top: Float,
                                   right: Float,
                                   bottom: Float,
                                   minWidth: Float,
                                   minHeight: Float,
                                   maxWidth: Float,
                                   maxHeight: Float) {
        // In width...
        if (viewPort.width() < minWidth) {
            val cx = viewPort.centerX()
            viewPort.left = cx - minWidth / 2f
            viewPort.right = cx + minWidth / 2f
        } else if (viewPort.width() > maxWidth) {
            val cx = viewPort.centerX()
            viewPort.left = cx - maxWidth / 2f
            viewPort.right = cx + maxWidth / 2f
        }
        // In height...
        if (viewPort.height() < minHeight) {
            val cy = viewPort.centerY()
            viewPort.top = cy - minHeight / 2f
            viewPort.bottom = cy + minHeight / 2f
        } else if (viewPort.height() > maxHeight) {
            val cy = viewPort.centerY()
            viewPort.top = cy - maxHeight / 2f
            viewPort.bottom = cy + maxHeight / 2f
        }
        // In x...
        val viewPortWidth = viewPort.width()
        if (viewPort.left < left) {
            viewPort.left = left
            viewPort.right = viewPort.left + viewPortWidth
        } else if (viewPort.right > right) {
            viewPort.right = right
            viewPort.left = viewPort.right - viewPortWidth
        }
        // In y...
        val viewPortHeight = viewPort.height()
        if (viewPort.top < top) {
            viewPort.top = top
            viewPort.bottom = viewPort.top + viewPortHeight
        } else if (viewPort.bottom > bottom) {
            viewPort.bottom = bottom
            viewPort.top = viewPort.bottom - viewPortHeight
        }
    }

    /**
     * Convert the point from View (view port) world to Model world.
     */
    private fun toModelWorld(x: Float,
                             y: Float): FloatArray {
        ensureMainThread()

        // View might have padding, if so we need to subtract the padding to get
        // the position in the real view port.
        mTmpPoint[0] = x - ViewCompat.getPaddingStart(this)
        mTmpPoint[1] = y - this.paddingTop

        // Map the point from screen (view port) to the view canvas world.
        mCanvasMatrixInverse.mapPoints(mTmpPoint)

        // The point is still in the View world, we still need to map it to the
        // Model world.
        val scaleM2V = mScaleM2V.value!!
        mTmpPoint[0] = mTmpPoint[0] / scaleM2V
        mTmpPoint[1] = mTmpPoint[1] / scaleM2V

        return mTmpPoint
    }

    /**
     * Convert the point from Model world to View canvas world.
     */
    private fun toViewWorld(x: Float,
                            y: Float): FloatArray {
        ensureMainThread()

        val scaleM2V = mScaleM2V.value!!

        // Map the point from Model world to View world.
        mTmpPoint[0] = scaleM2V * x
        mTmpPoint[1] = scaleM2V * y

        return mTmpPoint
    }

    // Common Gesture /////////////////////////////////////////////////////////

    // Gesture.
    private val mTouchSlop by lazy { resources.getDimension(R.dimen.touch_slop) }
    private val mTapSlop by lazy { resources.getDimension(R.dimen.tap_slop) }
    private val mMinFlingVec by lazy { resources.getDimension(R.dimen.fling_min_vec) }
    private val mMaxFlingVec by lazy { resources.getDimension(R.dimen.fling_max_vec) }

    private val mGestureDetector by lazy {
        val detector = GestureDetector(Looper.getMainLooper(),
                                       ViewConfiguration.get(context),
                                       mTouchSlop,
                                       mTapSlop,
                                       mMinFlingVec,
                                       mMaxFlingVec)

        // Set mapper as the listener.
        detector.tapGestureListener = this@PaperCanvasView
        detector.dragGestureListener = this@PaperCanvasView
        detector.pinchGestureListener = this@PaperCanvasView

        return@lazy detector
    }
    private var mIfHandleAction = false
    private var mIfHandleDrag = false
    private val mGestureHistory = mutableListOf<GestureRecord>()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Interpret the event iff scale, canvas size is ready at ACTION_DOWN.
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            mIfHandleAction = isAllSet
        }

        return if (mIfHandleAction) {
            val handled = mGestureDetector.onTouchEvent(event, this, null)

            if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {
                mIfHandleAction = false
            }

            handled
        } else {
            false
        }
    }

    override fun onActionBegin(event: MyMotionEvent,
                               target: Any?,
                               context: Any?) {
        mGestureHistory.clear()

        // Prevent the following transform applied to the event from do the
        // sharp rendering.
        mIfSharpenDrawing = false

        mWidget.handleActionBegin()
    }

    override fun onActionEnd(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        mWidget.handleActionEnd()

//        // Prevent the following transform applied to the event from do the
//        // sharp rendering.
//        requestSharpDrawing()
    }

    // Tap Gesture ////////////////////////////////////////////////////////////

    override fun onSingleTap(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        mGestureHistory.add(GestureRecord.TAP)

        val (nx, ny) = toModelWorld(event.downFocusX, event.downFocusY)

        mWidget.handleTap(nx, ny)
    }

    override fun onDoubleTap(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        mGestureHistory.add(GestureRecord.TAP)
    }

    override fun onLongPress(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
    }

    override fun onLongTap(event: MyMotionEvent,
                           target: Any?,
                           context: Any?) {
    }

    override fun onMoreTap(event: MyMotionEvent,
                           target: Any?,
                           context: Any?,
                           tapCount: Int) {
    }

    // Drag Gesture ///////////////////////////////////////////////////////////

    override fun onDragBegin(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        mGestureHistory.add(GestureRecord.DRAG)

        mIfHandleDrag = mGestureHistory.indexOf(GestureRecord.PINCH) == -1
        // If there is NO PINCH in the history, do drag; Otherwise, do view
        // port transform.
        if (mIfHandleDrag) {
            val (nx, ny) = toModelWorld(event.downFocusX,
                                        event.downFocusY)
            mWidget.handleDragBegin(nx, ny)
        } else {
            startUpdateViewport()
        }
    }

    override fun onDrag(event: MyMotionEvent,
                        target: Any?,
                        context: Any?,
                        startPointer: PointF,
                        stopPointer: PointF) {
        // If there is NO PINCH in the history, do drag; Otherwise, do view
        // port transform.
        if (mIfHandleDrag) {
            val (nx, ny) = toModelWorld(event.downFocusX,
                                        event.downFocusY)
            mWidget.handleDrag(nx, ny)
        } else {
            onUpdateViewport(Array(2, { _ -> startPointer }),
                             Array(2, { _ -> stopPointer }))
        }
    }

    override fun onDragFling(event: MyMotionEvent,
                             target: Any?,
                             context: Any?,
                             startPointer: PointF,
                             stopPointer: PointF,
                             velocityX: Float,
                             velocityY: Float) {
    }

    override fun onDragEnd(event: MyMotionEvent,
                           target: Any?,
                           context: Any?,
                           startPointer: PointF,
                           stopPointer: PointF) {
        // If there is NO PINCH in the history, do drag; Otherwise, do view
        // port transform.
        if (mIfHandleDrag) {
            val (nx, ny) = toModelWorld(event.downFocusX,
                                        event.downFocusY)
            mWidget.handleDragEnd(nx, ny)
        } else {
            stopUpdateViewport()
        }
    }

    // Pinch Gesture //////////////////////////////////////////////////////////

    override fun onPinchBegin(event: MyMotionEvent,
                              target: Any?,
                              context: Any?,
                              startPointers: Array<PointF>) {
        mGestureHistory.add(GestureRecord.PINCH)

        startUpdateViewport()
    }

    override fun onPinch(event: MyMotionEvent,
                         target: Any?,
                         context: Any?,
                         startPointers: Array<PointF>,
                         stopPointers: Array<PointF>) {
        onUpdateViewport(startPointers, stopPointers)
    }

    override fun onPinchFling(event: MyMotionEvent,
                              target: Any?,
                              context: Any?) {
        // DO NOTHING.
    }

    override fun onPinchEnd(event: MyMotionEvent,
                            target: Any?,
                            context: Any?,
                            startPointers: Array<PointF>,
                            stopPointers: Array<PointF>) {
        stopUpdateViewport()
    }

    // Context ////////////////////////////////////////////////////////////////

    override fun getOneDp(): Float {
        return mOneDp
    }

    override fun getViewConfiguration(): ViewConfiguration {
        return ViewConfiguration.get(context)
    }

    override fun getMinStrokeWidth(): Float {
        return mMinStrokeWidth
    }

    override fun getMaxStrokeWidth(): Float {
        return mMaxStrokeWidth
    }

    override fun getTouchSlop(): Float {
        return mTouchSlop
    }

    override fun getTapSlop(): Float {
        return mTapSlop
    }

    override fun getMinFlingVec(): Float {
        return mMinFlingVec
    }

    override fun getMaxFlingVec(): Float {
        return mMaxFlingVec
    }

    override fun mapM2V(x: Float, y: Float): FloatArray {
        return toViewWorld(x, y)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun ensureMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalThreadStateException("Not in MAIN thread")
        }
    }

    private fun ensureNoLeakingSubscription() {
        if (mDisposables.size() > 0) throw IllegalStateException(
            "Already bind to a widget")
    }

    private val isAllSet
        get() = mScaleM2V.value != Float.NaN &&
                (mMSize.value!!.width > 0f &&
                 mMSize.value!!.height > 0f) &&
                mViewPortSignal.hasValue()

    private fun drawBackground(canvas: Canvas,
                               vw: Float,
                               vh: Float,
                               tx: Float,
                               ty: Float,
                               scaleVP: Float) {
        if (scaleVP <= 1f) return

        val scaledVW = scaleVP * vw
        val scaledVH = scaleVP * vh
        val cell = Math.max(scaledVW, scaledVH) / 16
        val crossHalfW = 7f * mOneDp

        // The closer to base view port, the smaller the alpha is;
        // So the convex set is:
        //
        // Base                 Min
        // |-------x-------------|
        // x = (1 - a) * Base + a * Min = scaleVP
        // thus...
        //       vpW - baseW
        // a = --------------
        //      minW - baseW
        val alpha = (mViewPortSignal.value!!.width() - mViewPortBase.width()) /
                    (mViewPortMin.width() - mViewPortBase.width())
        mGridPaint.alpha = (alpha * 0xFF).toInt()

        // Cross
        val left = tx - cell
        val right = tx + scaledVW + cell
        val top = ty - cell
        val bottom = ty + scaledVH + cell
        var y = top
        while (y <= bottom) {
            var x = left
            while (x <= right) {
                canvas.drawLine(x - crossHalfW, y, x + crossHalfW, y, mGridPaint)
                canvas.drawLine(x, y - crossHalfW, x, y + crossHalfW, mGridPaint)
                x += cell
            }
            y += cell
        }
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PaperCanvasView

        if (mScrapViews != other.mScrapViews) return false
        if (mStrokeDrawables != other.mStrokeDrawables) return false
        if (mViewPortMin != other.mViewPortMin) return false
        if (mViewPortMax != other.mViewPortMax) return false
        if (mViewPortBase != other.mViewPortBase) return false

        return true
    }

    private var mIsHashDirty = true
    private var mHashCode = AppConst.EMPTY_HASH

    override fun hashCode(): Int {
        return if (mScrapViews.isEmpty() && mStrokeDrawables.isEmpty()) {
            AppConst.EMPTY_HASH
        } else {
            // FIXME: Consider scraps hash too.

            if (mIsHashDirty) {
                mHashCode = AppConst.EMPTY_HASH
                mStrokeDrawables.forEach { d ->
                    mHashCode = 31 * mHashCode + d.hashCode()
                }

                mIsHashDirty = false
            }

            mHashCode
        }
    }
}
