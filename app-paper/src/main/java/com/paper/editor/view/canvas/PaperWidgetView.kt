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

package com.paper.editor.view.canvas

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.IAllGesturesListener
import com.cardinalblue.gesture.MyMotionEvent
import com.jakewharton.rxbinding2.view.RxView
import com.paper.AppConst
import com.paper.R
import com.paper.editor.data.DrawSVGEvent
import com.paper.editor.data.DrawViewPortEvent
import com.paper.editor.data.GestureRecord
import com.paper.editor.widget.canvas.IPaperWidget
import com.paper.editor.widget.canvas.IScrapWidget
import com.paper.shared.model.Rect
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject

class PaperWidgetView : View,
                        IPaperWidgetView,
                        IPaperContext,
                        IParentWidgetView,
                        IAllGesturesListener {

    // Scraps.
    private val mScrapViews = mutableListOf<IScrapWidgetView>()

    // Widget.
    private lateinit var mWidget: IPaperWidget
    private val mWidgetDisposables = CompositeDisposable()

    /**
     * Model canvas size.
     */
    private val mMSize = BehaviorSubject.createDefault(Rect())
    /**
     * Scale factor from Model world to View world.
     */
    private val mScaleM2V = BehaviorSubject.createDefault(Float.NaN)
    private val mTransformM2V = BehaviorSubject.createDefault(TransformModel())
    /**
     * A util for getting translationX, translationY, scaleX, scaleY, and
     * rotationInDegrees from a [Matrix]
     */
    private val mTransformHelper = TransformUtils()

    // Temporary utils.
    private val mTmpPoint = FloatArray(2)
    private val mTmpBound = RectF()
    private val mTmpMatrix = Matrix()
    private val mTmpMatrixInverse = Matrix()
    private val mTmpMatrixStart = Matrix()

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
        detector.tapGestureListener = this@PaperWidgetView
        detector.dragGestureListener = this@PaperWidgetView
        detector.pinchGestureListener = this@PaperWidgetView

        return@lazy detector
    }
    private var mIfHandleAction = false
    private var mIfHandleDrag = false
    private val mGestureHistory = mutableListOf<GestureRecord>()

    // View port & canvas matrix
    /**
     * The view-port boundary in the model world.
     */
    private val mViewPort = BehaviorSubject.create<RectF>()
    /**
     * Minimum size of [mViewPort].
     */
    private val mViewPortMin = RectF()
    /**
     * Maximum size of [mViewPort].
     */
    private val mViewPortMax = RectF()
    /**
     * The initial view port size that is used to compute the scale factor from
     * Model to View, which is [mScaleM2V].
     */
    private val mViewPortBase = RectF()
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
    // View port paints
    private val mViewPortPaint = Paint()
    private val mCanvasBoundPaint = Paint()
    // Output signal related to view port
    private val mDrawViewPortSignal = BehaviorSubject.create<DrawViewPortEvent>()

    // Rendering resource.
    private val mUiHandler by lazy { Handler(Looper.getMainLooper()) }
    private val mOneDp by lazy { context.resources.getDimension(R.dimen.one_dp) }
    private val mMinStrokeWidth: Float by lazy { resources.getDimension(R.dimen.sketch_min_stroke_width) }
    private val mMaxStrokeWidth: Float by lazy { resources.getDimension(R.dimen.sketch_max_stroke_width) }
    private val mBackgroundPaint = Paint()
    private val mGridPaint = Paint()
    private val mStrokeDrawables = mutableListOf<SVGDrawable>()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
        : super(context, attrs, defStyleAttr) {
        mGridPaint.color = Color.LTGRAY
        mGridPaint.style = Paint.Style.STROKE
        mGridPaint.strokeWidth = 2f * mOneDp

        mBackgroundPaint.style = Paint.Style.FILL
        mBackgroundPaint.color = Color.WHITE

        // For showing the relative boundary of view-port and model.
        mCanvasBoundPaint.color = Color.RED
        mCanvasBoundPaint.style = Paint.Style.FILL
        mViewPortPaint.color = Color.GREEN
        mViewPortPaint.style = Paint.Style.STROKE
        mViewPortPaint.strokeWidth = 2f * mOneDp

//        // Giving a background would make onDraw() able to be called.
//        setBackgroundColor(Color.WHITE)
//        ViewCompat.setElevation(this, 12f * oneDp)
    }

    override fun onMeasure(widthSpec: Int,
                           heightSpec: Int) {
        Log.d(AppConst.TAG, "PaperWidgetView # onMeasure()")
        super.onMeasure(widthSpec, heightSpec)
    }

    override fun onLayout(changed: Boolean,
                          left: Int,
                          top: Int,
                          right: Int,
                          bottom: Int) {
        Log.d(AppConst.TAG, "PaperWidgetView # onLayout(changed=$changed)")
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun bindWidget(widget: IPaperWidget) {
        ensureMainThread()
        ensureNoLeakingSubscription()

        mWidget = widget

        // Add or remove scraps
        mWidgetDisposables.add(
            widget.onAddScrapWidget()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scrapWidget ->
                    addScrap(scrapWidget)
                })
        mWidgetDisposables.add(
            widget.onRemoveScrapWidget()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { scrapWidget ->
                    removeScrap(scrapWidget)
                })

        // Drawing
        mWidgetDisposables.add(
            widget.onDrawSVG()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    onDrawSVG(event)
                })

        // Canvas size change
        mWidgetDisposables.add(
            Observables.combineLatest(
                RxView.globalLayouts(this@PaperWidgetView),
                widget.onSetCanvasSize())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (_, size) ->
                    Log.d(AppConst.TAG, "the layout is done, and canvas size is ${size.width} x ${size.height}")

                    onUpdateLayoutOrCanvas(size.width,
                                           size.height)
                })
        // View port and canvas matrix change
        mWidgetDisposables.add(
            mViewPort
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vp ->
                    markCanvasMatrixDirty()
                    delayedInvalidate()

                    mDrawViewPortSignal.onNext(DrawViewPortEvent(
                        canvas = mMSize.value.copy(),
                        viewPort = Rect(vp.left,
                                        vp.top,
                                        vp.right,
                                        vp.bottom)))
                })

        // Debug
        mWidgetDisposables.add(
            mWidget
                .onPrintDebugMessage()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { message ->
                    Log.d(AppConst.TAG, message)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                })
    }

    override fun onDrawViewPort(): Observable<DrawViewPortEvent> {
        return mDrawViewPortSignal
    }

    override fun unbindWidget() {
        mWidgetDisposables.clear()

        mScrapViews.forEach { scrapView ->
            scrapView.unbindWidget()
        }
    }

    // Add / Remove Scraps /////////////////////////////////////////////////////

    private fun addScrap(widget: IScrapWidget) {
        val scrapView = ScrapWidgetView()

        scrapView.setPaperContext(this)
        scrapView.setParent(this)
        scrapView.bindWidget(widget)
        mScrapViews.add(scrapView)

        delayedInvalidate()
    }

    private fun removeScrap(widget: IScrapWidget) {
        val scrapView = mScrapViews.firstOrNull { it == widget }
                        ?: throw NoSuchElementException("Cannot find the widget")

        scrapView.unbindWidget()
        mScrapViews.remove(scrapView)

        delayedInvalidate()
    }

    // Drawing ////////////////////////////////////////////////////////////////

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
        val minScale = maxScale / AppConst.VIEW_PORT_MIN_SCALE
        mViewPortMax.set(0f, 0f, maxScale * spaceWidth, maxScale * spaceHeight)
        mViewPortMin.set(0f, 0f, minScale * spaceWidth, minScale * spaceHeight)
        mViewPortBase.set(mViewPortMax)

        // Hold canvas size.
        mMSize.onNext(Rect(0f, 0f, canvasWidth, canvasHeight))

        // Initially the model-to-view scale is derived by the scale
        // from min view port boundary to the view boundary.
        // Check out the figure above :D
        mScaleM2V.onNext(1f / maxScale)

        // Determine the default view-port (makes sense when view
        // layout is changed).
        resetViewPort(canvasWidth,
                      canvasHeight,
                      mViewPortMax.width(),
                      mViewPortMax.height())

        delayedInvalidate()
    }

    override fun delayedInvalidate() {
        mUiHandler.removeCallbacks(mInvalidateRunnable)
        mUiHandler.postDelayed(mInvalidateRunnable, 0)
    }

    private val mInvalidateRunnable = Runnable {
        invalidate()
    }

    private fun dispatchDrawScraps(canvas: Canvas,
                                   scrapViews: List<IScrapWidgetView>) {
        scrapViews.forEach { scrapView ->
            scrapView.dispatchDraw(canvas)
        }
    }

    private fun drawTempStrokes(canvas: Canvas,
                                drawables: List<SVGDrawable>) {
        drawables.forEach { drawable ->
            drawable.onDraw(canvas)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isAllSet) return

        // Scale from model to view.
        val scaleM2V = mScaleM2V.value
        // Scale from view to model
        val scaleV2M = 1f / scaleM2V
        // Scale contributed by view port.
        val scaleVP = mViewPortBase.width() / mViewPort.value.width()
        val mw = mMSize.value.width
        val mh = mMSize.value.height

        val count = canvas.save()

        // To view canvas world.
        computeCanvasMatrix(scaleM2V)
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        // View might have padding, if so we need to shift canvas to show
        // padding on the screen.
        canvas.translate(ViewCompat.getPaddingStart(this).toFloat(), paddingTop.toFloat())
        canvas.concat(mCanvasMatrix)

        // Background
        drawBackground(canvas, mw, mh, scaleM2V)

        // Draw scrap views.
        dispatchDrawScraps(canvas, mScrapViews)

        // Draw temporary sketch.
        drawTempStrokes(canvas, mStrokeDrawables)

        canvas.restoreToCount(count)

//        // Display the view-port relative boundary to the model.
//        drawMeter(canvas, mw, mh)
    }

    private fun onDrawSVG(event: DrawSVGEvent) {
        val nx = event.point.x
        val ny = event.point.y
        val (x, y) = toViewWorld(nx, ny)

        when (event.action) {
            DrawSVGEvent.Action.MOVE -> {
                val drawable = SVGDrawable(oneDp = mOneDp)
                drawable.moveTo(x, y)

                mStrokeDrawables.add(drawable)
            }
            DrawSVGEvent.Action.LINE_TO -> {
                val drawable = mStrokeDrawables.last()
                drawable.lineTo(x, y)
            }
            DrawSVGEvent.Action.CLOSE -> {
                val drawable = mStrokeDrawables.last()
                drawable.close()
            }
            DrawSVGEvent.Action.CLEAR_ALL -> {
                mStrokeDrawables.clear()
            }
            else -> {
                // NOT SUPPORT
            }
        }

        delayedInvalidate()
    }

    override fun takeSnapshot(): Bitmap {
        TODO()
//        val bmp = Bitmap.createBitmap((mModelToViewScale * mModelWidth).toInt(),
//                                      (mModelToViewScale * mModelHeight).toInt(),
//                                      Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bmp)
//
//        canvas.drawRect(0f, 0f,
//                        bmp.width.toFloat(), bmp.height.toFloat(),
//                        mBackgroundPaint)
//        mRootContainer.draw(canvas)
//
//        return bmp
    }

    private fun markCanvasMatrixDirty() {
        mCanvasMatrixDirty = true
    }

    /**
     * Compute the [mCanvasMatrix] given [mViewPort].
     *
     * @param scaleM2V The scale from model to view.
     */
    private fun computeCanvasMatrix(scaleM2V: Float) {
        if (mCanvasMatrixDirty && mViewPort.hasValue()) {
            // View port x
            val vx = mViewPort.value.left
            // View port y
            val vy = mViewPort.value.top
            // View port width
            val vw = mViewPort.value.width()
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

//            Log.d(AppConst.TAG, "compute matrix: m=$mCanvasMatrix")

            mCanvasMatrixDirty = false
        }
    }

    // Common Gesture /////////////////////////////////////////////////////////

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

        mWidget.handleActionBegin()
    }

    override fun onActionEnd(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        mWidget.handleActionEnd()
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

    // View port //////////////////////////////////////////////////////////////

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
        mViewPort.onNext(RectF(viewPortX, viewPortY,
                               viewPortX + defaultW,
                               viewPortY + defaultH))
    }

    private fun startUpdateViewport() {
        // Hold necessary starting states.
        mTmpMatrixStart.set(mCanvasMatrix)
    }

    // TODO: Make the view-port code a component.
    private fun onUpdateViewport(startPointers: Array<PointF>,
                                 stopPointers: Array<PointF>) {
        val mw = mMSize.value.width
        val mh = mMSize.value.height
        val scaleM2V = mScaleM2V.value

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
        mViewPort.onNext(RectF(mTmpBound))
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
        // View might have padding, if so we need to subtract the padding to get
        // the position in the real view port.
        mTmpPoint[0] = x - ViewCompat.getPaddingStart(this)
        mTmpPoint[1] = y - this.paddingTop

        // Map the point from screen (view port) to the view canvas world.
        mCanvasMatrixInverse.mapPoints(mTmpPoint)

        // The point is still in the View world, we still need to map it to the
        // Model world.
        val scaleVP = mViewPortBase.width() / mViewPort.value.width()
        val scaleM2V = mScaleM2V.value
        mTmpPoint[0] = mTmpPoint[0] / scaleVP / scaleM2V
        mTmpPoint[1] = mTmpPoint[1] / scaleVP / scaleM2V

        return mTmpPoint
    }

    /**
     * Convert the point from Model world to View canvas world.
     */
    private fun toViewWorld(x: Float,
                            y: Float): FloatArray {
        val scaleVP = mViewPortBase.width() / mViewPort.value.width()
        val scaleM2V = mScaleM2V.value

        // Map the point from Model world to View world.
        mTmpPoint[0] = scaleVP * scaleM2V * x
        mTmpPoint[1] = scaleVP * scaleM2V * y

        return mTmpPoint
    }

    override fun getViewConfiguration(): ViewConfiguration {
        return ViewConfiguration.get(context)
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

    // Context ////////////////////////////////////////////////////////////////

    override fun getOneDp(): Float {
        return mOneDp
    }

    override fun getMinStrokeWidth(): Float {
        return mMinStrokeWidth
    }

    override fun getMaxStrokeWidth(): Float {
        return mMaxStrokeWidth
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
        if (mWidgetDisposables.size() > 0) throw IllegalStateException(
            "Already bind to a widget")
    }

    private val isAllSet get() = mScaleM2V.value != Float.NaN &&
                                 (mMSize.value.width > 0f &&
                                  mMSize.value.height > 0f) &&
                                 mViewPort.hasValue()

    private fun drawBackground(canvas: Canvas,
                               mw: Float,
                               mh: Float,
                               scaleM2V: Float) {
        // FIX: Granularity seems wrong.
        val scaledCanvasWidth = scaleM2V * mw
        val scaledCanvasHeight = scaleM2V * mh
        val cell = Math.min(scaledCanvasWidth, scaledCanvasHeight) / 20

        // Boundary.
        canvas.drawLine(0f, 0f, scaledCanvasWidth, 0f, mGridPaint)
        canvas.drawLine(scaledCanvasWidth, 0f, scaledCanvasWidth, scaledCanvasHeight, mGridPaint)
        canvas.drawLine(scaledCanvasWidth, scaledCanvasHeight, 0f, scaledCanvasHeight, mGridPaint)
        canvas.drawLine(0f, scaledCanvasHeight, 0f, 0f, mGridPaint)

        // Grid.
        var x = 0f
        while (x < scaledCanvasWidth) {
            canvas.drawLine(x, 0f, x, scaledCanvasHeight, mGridPaint)
            x += cell
        }
        var y = 0f
        while (y < scaledCanvasHeight) {
            canvas.drawLine(0f, y, scaledCanvasWidth, y, mGridPaint)
            y += cell
        }
    }

    private fun drawMeter(canvas: Canvas,
                          canvasWidth: Float,
                          canvasHeight: Float) {
        if (!mViewPort.hasValue()) return

        val count = canvas.save()

        val ratio = canvasWidth / canvasHeight
        val bgWidth = Math.min(width, height) / 5f
        val bgHeight = bgWidth / ratio
        val scale = bgWidth / canvasWidth

        canvas.clipRect(0f, 0f, bgWidth, bgHeight)
        canvas.drawRect(0f, 0f, bgWidth, bgHeight, mCanvasBoundPaint)
        canvas.drawRect(scale * mViewPort.value.left,
                        scale * mViewPort.value.top,
                        scale * mViewPort.value.right,
                        scale * mViewPort.value.bottom,
                        mViewPortPaint)

        canvas.restoreToCount(count)
    }
}
