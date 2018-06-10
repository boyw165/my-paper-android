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
import com.cardinalblue.gesture.rx.*
import com.paper.AppConst
import com.paper.R
import com.paper.domain.DomainConst
import com.paper.domain.data.GestureRecord
import com.paper.domain.event.*
import com.paper.domain.util.ProfilerUtils
import com.paper.domain.util.TransformUtils
import com.paper.domain.widget.editor.IPaperCanvasWidget
import com.paper.domain.widget.editor.IScrapWidget
import com.paper.model.Point
import com.paper.model.Rect
import com.paper.model.repository.IBitmapRepo
import com.paper.model.sketch.PenType
import com.paper.view.IWidgetView
import com.paper.view.with
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.lang.NullPointerException
import java.util.*
import java.util.concurrent.TimeUnit

class PaperCanvasView : View,
                        IWidgetView<IPaperCanvasWidget>,
                        IPaperContext,
                        IParentView {

    // Dirty flags
    private var mIsNew = true

    // Scraps.
    private val mScrapViews = mutableListOf<IScrapView>()

    // Widget.
    private lateinit var mWidget: IPaperCanvasWidget
    private val mDisposables = CompositeDisposable()

    /**
     * A signal indicating the layout change.
     */
    private val mOnLayoutChangeSignal = BehaviorSubject.createDefault(false)

    // Temporary utils.
    private val mTmpPoint = FloatArray(2)
    private val mTmpMatrix = Matrix()
    private val mTmpMatrixInverse = Matrix()
    private val mTmpMatrixStart = Matrix()

    /**
     * A util for getting translationX, translationY, scaleX, scaleY, and
     * rotationInDegrees from a [Matrix]
     */
    private val mTransformHelper = TransformUtils()

//    private val mTextDetector by lazy { FirebaseVision.getInstance().visionTextDetector }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
        : super(context, attrs, defStyleAttr) {
        mGridPaint.color = Color.LTGRAY
        mGridPaint.style = Paint.Style.STROKE
        mGridPaint.strokeWidth = 2f * mOneDp

        mBitmapPaint.isAntiAlias = true

        mEraserPaint.style = Paint.Style.FILL
        mEraserPaint.color = Color.WHITE
        mEraserPaint.xfermode = mEraserMode
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
                    if (changed && !size.isAreaEmpty) {
                        println("${AppConst.TAG}: the layout is done, and canvas " +
                                "size is ${size.width} x ${size.height}")

                        // Mark it not ready for interacting with user.
                        mDrawReadySignal.onNext(false)

                        onUpdateLayoutOrCanvas(size.width, size.height)

                        // Mark it ready!
                        mDrawReadySignal.onNext(true)
                    }
                })

        // Drawing
        mDisposables.add(
            mDrawReadySignal
                .switchMap { ready ->
                    if (ready) {
                        widget.onDrawSVG(replayAll = true)
                            .compose(handleDrawSVGEvent())
                    } else {
                        Observable.never()
                    }
                }
                .subscribe())
        mDisposables.add(
            mDrawReadySignal
                .switchMap { ready ->
                    if (ready) {
                        onSaveBitmap()
                    } else {
                        Observable.never()
                    }
                }
                .subscribe())

        // Anti-aliasing drawing
        mDisposables.add(
            mDrawReadySignal
                .switchMap { ready ->
                    if (ready) {
                        onAntiAliasingDraw()
                    } else {
                        Observable.never()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scene ->
                    mSceneBuffer.setCurrentScene(scene)

                    invalidate()
                })

        // Add or remove scraps
        mDisposables.add(
            mDrawReadySignal
                .switchMap { ready ->
                    if (ready) {
                        widget.onAddScrapWidget()
                            .subscribeOn(AndroidSchedulers.mainThread())
                    } else {
                        Observable.never()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scrapWidget ->
                    addScrap(scrapWidget)
                })
        mDisposables.add(
            mDrawReadySignal
                .switchMap { ready ->
                    if (ready) {
                        widget.onRemoveScrapWidget()
                            .subscribeOn(AndroidSchedulers.mainThread())
                    } else {
                        Observable.never()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scrapWidget ->
                    removeScrap(scrapWidget)
                })

        // Touch
        mDisposables.add(
            Observables
                .combineLatest(
                    mDrawReadySignal,
                    mInteractionReadySignal)
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap { (drawReady, interactionReady) ->
                    if (drawReady && interactionReady) {
                        Observable.merge(
                            GestureEventObservable(mGestureDetector)
                                // Consume the [GestureEvent] and produce [CanvasEvent]
                                .compose(handleTouchEvent()),
                            // Other canvas event sources
                            Observable.merge(mCanvasEventSources))
                            // Consume the [CanvasEvent]
                            .compose(handleCanvasEvent())
                    } else {
                        Observable.never()
                    }
                }
                .subscribe())

        // Debug
        mDisposables.add(
            widget.onPrintDebugMessage()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                })
        mDisposables.add(
            mDrawReadySignal
                .subscribe { drawReady ->
                    println("${AppConst.TAG}: Draw ready=$drawReady")
                })
        mDisposables.add(
            mInteractionReadySignal
                .subscribe { interactionReady ->
                    println("${AppConst.TAG}: Interaction ready=$interactionReady")
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

    private val mRenderingScheduler = SingleScheduler()

    /**
     * A signal indicating whether it's ready to render. see [onUpdateLayoutOrCanvas].
     */
    private val mDrawReadySignal = BehaviorSubject.createDefault(false)

    /**
     * A signal indicating whether it's ready to interact with the user. see
     * [handleDrawSVGEvent].
     */
    private val mInteractionReadySignal = BehaviorSubject.createDefault(false)

    /**
     * A signal of updating the canvas hash and Bitmap.
     */
    private val mUpdateBitmapSignal = PublishSubject.create<Pair<Int, Bitmap>>()

    /**
     * A signal of requesting the anti-aliasing drawing.
     */
    private val mAntiAliasingSignal = PublishSubject.create<Boolean>()

    /**
     * Model canvas size.
     */
    private var mMSize = Rect()
        set(value) {
            field.set(value)
        }
    /**
     * Scale factor from Model size to View size.
     */
    private var mScaleM2V = Float.NaN
    /**
     * Scale factor from thumbnail size to View size.
     */
    private var mScaleThumb = Float.NaN

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
     * The Bitmap in which the sketch and the scraps are drawn to, yet thumbnail
     * resolution.
     */
    private var mThumbBitmap: Bitmap? = null
    /**
     * The canvas hiding [mThumbBitmap].
     */
    private lateinit var mThumbCanvas: Canvas

    /**
     * The scene buffer in which the sketch and the scraps are drawn to, which
     * is the best resolution but cut to the rectangle as big as the view's
     * visible area.
     */
    private lateinit var mSceneBuffer: SceneBuffer

    /**
     * The Bitmap that all the layers merge to, of size of the rectangle as big
     * as the view's visible area.
     */
    private var mMergedBitmap: Bitmap? = null
    /**
     * The canvas hiding [mMergedBitmap].
     */
    private lateinit var mMergedCanvas: Canvas

    private val mBitmapPaint = Paint()
    private val mEraserPaint = Paint()
    private val mDrawMode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    private val mEraserMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    // Background & grids
    private val mGridPaint = Paint()

    // Temporary strokes
    private val mStrokeDrawables = mutableListOf<SVGDrawable>()

    private fun onUpdateLayoutOrCanvas(canvasWidth: Float,
                                       canvasHeight: Float) {
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
        mMSize = Rect(0f, 0f, canvasWidth, canvasHeight)

        // Initially the model-to-view scale is derived by the scale
        // from min view port boundary to the view boundary.
        // Check out the figure above :D
        mScaleM2V = scaleM2V

        // Determine the default view-port (makes sense when view
        // layout is changed).
        resetViewPort()

        // Bitmap layers:

        // The thumbnail layer
        val mw = mMSize.width
        val mh = mMSize.height
        val vw = scaleM2V * mw
        val vh = scaleM2V * mh
        mScaleThumb = Math.max(vw / DomainConst.BASE_THUMBNAIL_WIDTH,
                               vh / DomainConst.BASE_THUMBNAIL_HEIGHT)
        val bw = vw / mScaleThumb
        val bh = vh / mScaleThumb
        mThumbBitmap?.recycle()
        mThumbBitmap = Bitmap.createBitmap(bw.toInt(), bh.toInt(), Bitmap.Config.ARGB_8888)
        mThumbCanvas = Canvas(mThumbBitmap)

        // The view-port layer
        mSceneBuffer = SceneBuffer(bufferSize = 2,
                                   canvasWidth = spaceWidth,
                                   canvasHeight = spaceHeight,
                                   bitmapPaint = mBitmapPaint,
                                   eraserPaint = mEraserPaint)

        // The merged layer
        mMergedBitmap?.recycle()
        mMergedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mMergedCanvas = Canvas(mMergedBitmap)
    }

    private fun getPaintMode(penType: PenType): PorterDuffXfermode {
        return if (penType == PenType.ERASER) mEraserMode else mDrawMode
    }

    /**
     * Apply the padding transform to the canvas before processing the given
     * lambda.
     */
    private inline fun<T> Canvas.withPadding(lambda: (canvas: Canvas) -> T):T {
        return with {
            // View might have padding, if so we need to shift canvas to show
            // padding on the screen.
            translate(ViewCompat.getPaddingStart(this@PaperCanvasView).toFloat(),
                      paddingTop.toFloat())
            lambda(this)
        }
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
        if (!mDrawReadySignal.value!!) return

        // Scale from model to view.
        val scaleM2V = mScaleM2V
        // Scale contributed by view port.
        val mw = mMSize.width
        val mh = mMSize.height
        val vw = scaleM2V * mw
        val vh = scaleM2V * mh

        // Calculate the view port matrix.
        computeCanvasMatrix(scaleM2V)

        // Layers rendering ///////////////////////////////////////////////////

        // Background layer
        canvas.withPadding { c ->
            // Extract the transform from the canvas matrix.
            mTransformHelper.getValues(mCanvasMatrix)
            val tx = mTransformHelper.translationX
            val ty = mTransformHelper.translationY
            val scaleVP = mTransformHelper.scaleX

            // Manually calculate position and size of the background cross/grids
            // so that they keep sharp!
            drawBackground(c, vw, vh, tx, ty, scaleVP)
        }

        // Layers blending ////////////////////////////////////////////////////

        mMergedBitmap?.eraseColor(Color.TRANSPARENT)

        // Print the thumbnail Bitmap to the merged layer
        mMergedCanvas.withPadding { c ->
            c.concat(mCanvasMatrix)
            c.scale(mScaleThumb, mScaleThumb)
            c.drawBitmap(mThumbBitmap, 0f, 0f, mBitmapPaint)
        }

        // Print the anti-aliasing Bitmap to the merged layer
        mMergedCanvas.withPadding { c ->
            mSceneBuffer.getCurrentScene().print(c)
        }

        // Print the merged layer to view canvas
        canvas.with { c ->
            c.drawBitmap(mMergedBitmap, 0f, 0f, mBitmapPaint)
        }
    }

    private fun handleDrawSVGEvent(): ObservableTransformer<CanvasEvent, Unit> {
        return ObservableTransformer { upstream ->
            upstream
                .observeOn(mRenderingScheduler)
                // To drawable (must do) and invalidation event
                .flatMap { event ->
                    when (event) {
                        is StartSketchEvent -> {
                            val nx = event.point.x
                            val ny = event.point.y
                            val (x, y) = toViewWorld(nx, ny)

                            val drawable = SVGDrawable(id = event.strokeID,
                                                       context = this@PaperCanvasView,
                                                       penColor = event.penColor,
                                                       penSize = event.penSize,
                                                       porterDuffMode = getPaintMode(event.penType))
                            drawable.moveTo(Point(x, y, event.point.time))

                            mStrokeDrawables.add(drawable)

                            mIsHashDirty = true

                            Observable.just(InvalidationEvent())
                        }
                        is OnSketchEvent -> {
                            val nx = event.point.x
                            val ny = event.point.y
                            val (x, y) = toViewWorld(nx, ny)

                            val drawable = mStrokeDrawables.last()
                            drawable.lineTo(Point(x, y, event.point.time))

                            mIsHashDirty = true

                            Observable.just(InvalidationEvent())
                        }
                        is StopSketchEvent -> {
                            val drawable = mStrokeDrawables.last()
                            drawable.close()

                            Observable.just(InvalidationEvent())
                        }
                        is AddSketchStrokeEvent -> {
                            if (-1 == mStrokeDrawables.indexOfFirst { d -> d.id == event.strokeID }) {
                                val drawable = SVGDrawable(id = event.strokeID,
                                                           points = event.points.map { p ->
                                                               val (x, y) = toViewWorld(p.x, p.y)
                                                               Point(x, y)
                                                           },
                                                           context = this@PaperCanvasView,
                                                           penColor = event.penColor,
                                                           penSize = event.penSize,
                                                           porterDuffMode = getPaintMode(event.penType))
                                mStrokeDrawables.add(drawable)

                                mIsHashDirty = true

                                Observable.just(InvalidationEvent())
                            } else {
                                Observable.just(NullCanvasEvent())
                            }
                        }
                        is RemoveSketchStrokeEvent -> {
                            val i = mStrokeDrawables.indexOfFirst { d ->
                                d.id == event.strokeID
                            }
                            if (i >= 0) {
                                mStrokeDrawables.removeAt(i)

                                // Invalidate drawables
                                mStrokeDrawables.forEach { d -> d.markUndrew() }

                                mIsHashDirty = true

                                Observable.just(
                                    EraseCanvasEvent(),
                                    InvalidationEvent())
                            } else {
                                Observable.just(NullCanvasEvent())
                            }
                        }
                        else -> Observable.just(event)
                    }
                }
                .observeOn(mRenderingScheduler)
                // State reducing
                .scan { prev: CanvasEvent, now: CanvasEvent ->
                    if (prev is InitializationBeginEvent ||
                        (prev is InitializationDoingEvent &&
                         now !is InitializationEndEvent)) {
                        // Transform any events in between init-begin and init-end
                        // to init-doing events.
                        // This is only happening in the initialization process
                        // e.g.
                        // Given  [init begin, _whatever_, ..., init end]
                        // Return [init begin, init doing, ..., init end]
                        //
                        // It's important to use the same observable pattern to
                        // initialize the stroke without slowing down by the
                        // frequent rendering requests.
                        InitializationDoingEvent()
                    } else {
                        if (now !is InitializationEndEvent) {
                            mIsNew = false
                        }

                        // Given  [..., init end, foo]
                        // Return [..., init end, foo]
                        now
                    }
                }
                .observeOn(mRenderingScheduler)
                .map { event ->
                    when (event) {
                        is InitializationBeginEvent -> {
                            // Mark interaction disabled
                            mInteractionReadySignal.onNext(false)
                        }
                        is InitializationEndEvent,
                        is InvalidationEvent -> {
                            // First try to load Bitmap from file;
                            // secondly try to redraw
                            try {
                                // Load file
                                val hash = hashCode()
                                ProfilerUtils.with("load thumbnail", {
                                    val bmp = mBitmapRepo?.getBitmap(hash)?.blockingGet()
                                              ?: throw NullPointerException()
                                    mThumbCanvas.with { c ->
                                        c.drawBitmap(bmp, 0f, 0f, mBitmapPaint)
                                    }
                                    bmp.recycle()

                                    // Notify Bitmap update
                                    postWriteBitmapFile()
                                })
                            } catch (err: Throwable) {
                                // Redraw
                                ProfilerUtils.with("draw thumbnail", {
                                    // Draw sketch and scraps on thumbnail Bitmap
                                    // TODO: Both scraps and sketch need to explicitly define the z-order
                                    // TODO: so that the paper knows how to render them in the correct
                                    // TODO: order.
                                    mThumbCanvas.with { c ->
                                        c.scale(1f / mScaleThumb, 1f / mScaleThumb)

                                        var dirty = false
                                        //dispatchDrawScraps(canvas = c,
                                        //                   scrapViews = mScrapViews,
                                        //                   ifSharpenDrawing = false)
                                        // Draw the strokes on the thumbnail canvas
                                        mStrokeDrawables.forEach { drawable ->
                                            dirty = dirty || drawable.isSomethingToDraw()
                                            drawable.onDraw(canvas = c)
                                        }

                                        // Notify Bitmap update
                                        if (dirty) {
                                            postWriteBitmapFile()
                                        }
                                    }
                                })
                            }

                            // Draw sketch on view-port Bitmap
                            ProfilerUtils.with("draw view-port", {
                                mSceneBuffer.getCurrentScene().draw { c ->
                                    computeCanvasMatrix(mScaleM2V)
                                    c.concat(mCanvasMatrix)

                                    mStrokeDrawables.forEach { d ->
                                        d.onDraw(canvas = c)
                                    }
                                }
                            })

                            // By marking drawables not dirty, the drawable's
                            // cache is renewed.
                            mStrokeDrawables.forEach { d ->
                                d.markAllDrew()
                            }

                            postInvalidate()

                            // Mark interaction enabled
                            if (event is InitializationEndEvent) {
                                mInteractionReadySignal.onNext(true)
                            }
                        }
                        is EraseCanvasEvent -> {
                            mThumbCanvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR)
                            // Notify Bitmap update
                            postWriteBitmapFile()

                            mSceneBuffer.getCurrentScene().draw { c ->
                                c.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR)
                            }

                            postInvalidate()
                        }
                    }
                }
        }
    }

    private fun onAntiAliasingDraw(): Observable<Scene> {
        return mAntiAliasingSignal
            .debounce(75, TimeUnit.MILLISECONDS, mRenderingScheduler)
            // FIXME: The debounce is buggy when the interval is large
            //.debounce(1750, TimeUnit.MILLISECONDS, mRenderingScheduler)
            .switchMap { doIt ->
                if (doIt) {
                    Observable
                        .fromCallable {
                            // View-port drawing
                            val scene = mSceneBuffer.getEmptyScene()

                            ProfilerUtils.with("draw view-port", {
                                scene.resetTransform()
                                scene.eraseDraw { c ->
                                    c.concat(mCanvasMatrix)

                                    mStrokeDrawables.forEach { d ->
                                        d.onDraw(canvas = c, startOver = true)
                                    }
                                }
                            })

                            return@fromCallable scene
                        }
                        .subscribeOn(mRenderingScheduler)
                } else {
                    Observable.never()
                }
            }
    }

    private fun postWriteBitmapFile() {
        if (mIsNew) return

        mThumbBitmap?.let { bmp ->
            mUpdateBitmapSignal.onNext(Pair(hashCode(), bmp))
        }
    }

    private fun onSaveBitmap(): Observable<Any> {
        return mUpdateBitmapSignal
            // Notify widget the thumbnail need to be update
            .doOnNext {
                if (mIsNew) return@doOnNext
                mWidget.invalidateThumbnail()
            }
            // Debounce Bitmap writes
            .debounce(1000, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
//            // TEST: text recognition
//            .observeOn(Schedulers.computation())
//            .doOnNext { (_, bmp) ->
//                val image = FirebaseVisionImage.fromBitmap(bmp)
//                mTextDetector.detectInImage(image)
//                    .addOnSuccessListener { visionText ->
//                        val builder = StringBuilder()
//                        visionText.blocks.forEach { block ->
//                            builder.append("[")
//                            block.lines.forEachIndexed { i, line ->
//                                line.elements.forEachIndexed { j, element ->
//                                    builder.append(element.text)
//
//                                    if (line.elements.size > 1 &&
//                                        j < line.elements.lastIndex) {
//                                        builder.append(" ")
//                                    }
//                                }
//
//                                if (block.lines.size > 1 &&
//                                    i < block.lines.lastIndex) {
//                                    builder.append(",")
//                                }
//                            }
//                            builder.append("]")
//                        }
//                        Toast.makeText(context, builder.toString(), Toast.LENGTH_SHORT).show()
//                    }
//                    .addOnFailureListener { err ->
//                        Toast.makeText(context, err.toString(), Toast.LENGTH_SHORT).show()
//                    }
//            }
            .switchMap { (hashCode, bmp) ->
                mBitmapRepo
                    ?.putBitmap(hashCode, bmp)
                    ?.toObservable()
                    // Feed the saved Bitmap file to the widget
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.map { bmpFile ->
                        mWidget.setThumbnail(bmpFile, bmp.width, bmp.height)
                    }
                ?: Observable.never<Triple<File, Int, Int>>()
            }
    }

    private var mBitmapRepo: IBitmapRepo? = null

    fun setBitmapRepo(repo: IBitmapRepo) {
        mBitmapRepo = repo
    }

    private fun cancelAntiAliasingDrawing() {
        mAntiAliasingSignal.onNext(false)
    }

    private fun requestAntiAliasingDrawing() {
        mAntiAliasingSignal.onNext(true)
    }

    // View port //////////////////////////////////////////////////////////////

    /**
     * The view-port boundary in the model world.
     */
    private var mViewPort = Rect()
        set(value) {
            field.set(value)

            mCanvasMatrixDirty = true

            // Notify any external observers
            mDrawViewPortSignal.onNext(DrawViewPortEvent(
                canvas = mMSize.copy(),
                viewPort = Rect(value.left,
                                value.top,
                                value.right,
                                value.bottom)))

            invalidate()
        }

    private val mViewPortStart = Rect()
    /**
     * Minimum size of [mViewPort].
     */
    private val mViewPortMin = Rect()
    /**
     * Maximum size of [mViewPort].
     */
    private val mViewPortMax = Rect()
    /**
     * The initial view port size that is used to compute the scale factor from
     * Model to View, which is [mScaleM2V].
     */
    private val mViewPortBase = Rect()
    /**
     * The signal for external observers.
     */
    private val mDrawViewPortSignal = BehaviorSubject.create<DrawViewPortEvent>()

    fun onDrawViewPort(): Observable<DrawViewPortEvent> {
        return mDrawViewPortSignal
    }

    private fun resetViewPort() {
        val defaultW = mViewPortMax.width
        val defaultH = mViewPortMax.height

        // Place the view port left in the model world.
        val viewPortX = 0f
        val viewPortY = 0f
        mViewPort = Rect(viewPortX, viewPortY,
                         viewPortX + defaultW,
                         viewPortY + defaultH)
    }

    /**
     * Compute the [mCanvasMatrix] given [mViewPort].
     *
     * @param scaleM2V The scale from model to view.
     */
    private fun computeCanvasMatrix(scaleM2V: Float) {
        if (mCanvasMatrixDirty) {
            // View port x
            val vx = mViewPort.left
            // View port y
            val vy = mViewPort.top
            // View port width
            val vw = mViewPort.width
            val scaleVP = mViewPortBase.width / vw

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

    private fun handleViewPortEvent(event: ViewPortEvent) {
        when (event) {
            is ViewPortBeginUpdateEvent -> {
                // Hold necessary starting states.
                mTmpMatrixStart.set(mCanvasMatrix)
                mViewPortStart.set(mViewPort)

                cancelAntiAliasingDrawing()
            }
            is ViewPortOnUpdateEvent -> {
                val bound = constraintViewPort(
                    event.bound,
                    left = 0f,
                    top = 0f,
                    right = mMSize.width,
                    bottom = mMSize.height,
                    minWidth = mViewPortMin.width,
                    minHeight = mViewPortMin.height,
                    maxWidth = mViewPortMax.width,
                    maxHeight = mViewPortMax.height)

                // After applying the constraint, calculate the matrix for anti-aliasing
                // Bitmap
                val scaleVp = mViewPortBase.width / bound.width
                val vpDs = mViewPortStart.width / bound.width
                val vpDx = (mViewPortStart.left - bound.left) * mScaleM2V * scaleVp
                val vpDy = (mViewPortStart.top - bound.top) * mScaleM2V * scaleVp
                mTmpMatrix.reset()
                mTmpMatrix.postScale(vpDs, vpDs)
                mTmpMatrix.postTranslate(vpDx, vpDy)
                mSceneBuffer.getCurrentScene().setNewTransform(mTmpMatrix)

                // Apply final view port boundary
                mViewPort = bound

                // Calculate the canvas matrix contributed by view-port boundary.
                computeCanvasMatrix(mScaleM2V)

                cancelAntiAliasingDrawing()
            }
            is ViewPortStopUpdateEvent -> {
                mTmpMatrixStart.reset()
                mTmpMatrix.reset()
                mTmpMatrixInverse.reset()

                mSceneBuffer.getCurrentScene().commitNewTransform()

                requestAntiAliasingDrawing()
            }
        }
    }

    // TODO: Make the view-port code a component.
    private fun calculateViewPortBound(startPointers: Array<PointF>,
                                       stopPointers: Array<PointF>): Rect {
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
        val s = mTransformHelper.scaleX
        val vx = -tx / s / mScaleM2V
        val vy = -ty / s / mScaleM2V
        val vw = mViewPortMax.width / s
        val vh = mViewPortMax.height / s

        return Rect(vx, vy, vx + vw, vy + vh)
    }

    private fun constraintViewPort(viewPort: Rect,
                                   left: Float,
                                   top: Float,
                                   right: Float,
                                   bottom: Float,
                                   minWidth: Float,
                                   minHeight: Float,
                                   maxWidth: Float,
                                   maxHeight: Float): Rect {
        val bound = Rect(left = viewPort.left,
                         top = viewPort.top,
                         right = viewPort.right,
                         bottom = viewPort.bottom)

        // In width...
        if (bound.width < minWidth) {
            val cx = bound.centerX
            bound.left = cx - minWidth / 2f
            bound.right = cx + minWidth / 2f
        } else if (bound.width > maxWidth) {
            val cx = bound.centerX
            bound.left = cx - maxWidth / 2f
            bound.right = cx + maxWidth / 2f
        }
        // In height...
        if (bound.height < minHeight) {
            val cy = bound.centerY
            bound.top = cy - minHeight / 2f
            bound.bottom = cy + minHeight / 2f
        } else if (bound.height > maxHeight) {
            val cy = bound.centerY
            bound.top = cy - maxHeight / 2f
            bound.bottom = cy + maxHeight / 2f
        }
        // In x...
        val viewPortWidth = bound.width
        if (bound.left < left) {
            bound.left = left
            bound.right = bound.left + viewPortWidth
        } else if (bound.right > right) {
            bound.right = right
            bound.left = bound.right - viewPortWidth
        }
        // In y...
        val viewPortHeight = bound.height
        if (bound.top < top) {
            bound.top = top
            bound.bottom = bound.top + viewPortHeight
        } else if (bound.bottom > bottom) {
            bound.bottom = bottom
            bound.top = bound.bottom - viewPortHeight
        }

        return bound
    }

    /**
     * Convert the point from View (view port) world to Model world.
     */
    private fun toModelWorld(x: Float,
                             y: Float): FloatArray {
        // View might have padding, if so we need to subtract the padding to get
        // the position in the real view port.
        mTmpPoint[0] = x - ViewCompat.getPaddingStart(this)
        mTmpPoint[1] = y - this.paddingTop

        // Map the point from screen (view port) to the view canvas world.
        mCanvasMatrixInverse.mapPoints(mTmpPoint)

        // The point is still in the View world, we still need to map it to the
        // Model world.
        val scaleM2V = mScaleM2V
        mTmpPoint[0] = mTmpPoint[0] / scaleM2V
        mTmpPoint[1] = mTmpPoint[1] / scaleM2V

        return mTmpPoint
    }

    /**
     * Convert the point from Model world to View canvas world.
     */
    private fun toViewWorld(x: Float,
                            y: Float): FloatArray {
        val scaleM2V = mScaleM2V

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
        GestureDetector(Looper.getMainLooper(),
                        ViewConfiguration.get(context),
                        mTouchSlop,
                        mTapSlop,
                        mMinFlingVec,
                        mMaxFlingVec)
    }
    private var mIfHandleTouch = false
    private var mIfHandleDrag = false
    private val mGestureHistory = mutableListOf<GestureRecord>()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event, this, null)
    }

    /**
     * Convert the [GestureEvent] to [CanvasEvent].
     */
    private fun handleTouchEvent(): ObservableTransformer<GestureEvent, CanvasEvent> {
        return ObservableTransformer { upstream ->
            upstream
                .map { event ->
                    when (event) {
                        is TouchBeginEvent,
                        is TouchEndEvent -> handleTouchLifecycleEvent(event)

                        is TapEvent -> handleTapEvent(event)

                        is DragBeginEvent,
                        is OnDragEvent,
                        is DragEndEvent -> handleDragEvent(event)

                        is PinchBeginEvent,
                        is OnPinchEvent,
                        is PinchEndEvent -> handlePinchEvent(event)

                        else -> NullCanvasEvent()
                    }
                }
        }
    }

    private val mCanvasEventSources = mutableListOf<Observable<CanvasEvent>>()

    /**
     * For [CanvasEvent] external sources.
     */
    fun addCanvasEventSource(source: Observable<CanvasEvent>) {
        mCanvasEventSources.add(source)
    }

    /**
     * Consume the [CanvasEvent].
     */
    private fun handleCanvasEvent(): ObservableTransformer<in CanvasEvent, out Unit> {
        return ObservableTransformer { upstream ->
            upstream
                .map { event ->
                    when (event) {
                        is ViewPortEvent -> handleViewPortEvent(event)
                        else -> Unit
                    }
                }
        }
    }

    private fun handleTouchLifecycleEvent(event: GestureEvent): CanvasEvent {
        when (event) {
            is TouchBeginEvent -> {
                mGestureHistory.clear()
                mWidget.handleTouchBegin()
            }
            is TouchEndEvent -> {
                mWidget.handleTouchEnd()
            }
        }

        // Null action
        return NullCanvasEvent()
    }

    private fun handleTapEvent(event: TapEvent): CanvasEvent {
        mGestureHistory.add(GestureRecord.TAP)

        val (nx, ny) = toModelWorld(event.downX,
                                    event.downY)
        mWidget.handleTap(nx, ny)

        // Null action
        return NullCanvasEvent()
    }

    private fun handleDragEvent(event: GestureEvent): CanvasEvent {
        if (event is DragBeginEvent) {
            mGestureHistory.add(GestureRecord.DRAG)

            // If there is NO PINCH in the history, do drag;
            // Otherwise, do view port transform.
            mIfHandleDrag = mGestureHistory.indexOf(GestureRecord.PINCH) == -1
        }

        return if (mIfHandleDrag) {
            when (event) {
                is DragBeginEvent -> {
                    val (nx, ny) = toModelWorld(event.startPointer.x,
                                                event.startPointer.y)
                    mWidget.handleDragBegin(nx, ny)
                }
                is OnDragEvent -> {
                    val (nx, ny) = toModelWorld(event.stopPointer.x,
                                                event.stopPointer.y)
                    mWidget.handleDrag(nx, ny)
                }
                is DragEndEvent -> {
                    val (nx, ny) = toModelWorld(event.stopPointer.x,
                                                event.stopPointer.y)
                    mWidget.handleDragEnd(nx, ny)
                }
            }

            // Null action
            NullCanvasEvent()
        } else {
            when (event) {
                is DragBeginEvent -> {
                    ViewPortBeginUpdateEvent()
                }
                is OnDragEvent -> {
                    ViewPortOnUpdateEvent(
                        calculateViewPortBound(
                            startPointers = Array(2, { _ -> event.startPointer }),
                            stopPointers = Array(2, { _ -> event.stopPointer })))
                }
                is DragEndEvent -> {
                    ViewPortStopUpdateEvent()
                }
                else -> {
                    // Null action
                    NullCanvasEvent()
                }
            }
        }
    }

    private fun handlePinchEvent(event: GestureEvent): CanvasEvent {
        return when (event) {
            is PinchBeginEvent -> {
                mGestureHistory.add(GestureRecord.PINCH)
                ViewPortBeginUpdateEvent()
            }
            is OnPinchEvent -> {
                ViewPortOnUpdateEvent(
                    calculateViewPortBound(
                        startPointers = event.startPointers,
                        stopPointers = event.stopPointers))
            }
            is PinchEndEvent -> {
                ViewPortStopUpdateEvent()
            }
            else -> {
                // Null action
                NullCanvasEvent()
            }
        }
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
        val alpha = (mViewPort.width - mViewPortBase.width) /
                    (mViewPortMin.width - mViewPortBase.width)
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

    // Equality & Hash ////////////////////////////////////////////////////////

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
