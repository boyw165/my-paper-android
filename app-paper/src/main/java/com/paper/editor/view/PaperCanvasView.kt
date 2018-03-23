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

package com.paper.editor.view

import android.content.Context
import android.graphics.*
import android.os.Looper
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.cardinalblue.gesture.GestureDetector
import com.paper.AppConsts
import com.paper.R
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.sketch.PathTuple
import com.paper.shared.model.sketch.SketchStroke
import com.paper.util.TransformUtils
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.lang.UnsupportedOperationException
import java.util.*

class PaperCanvasView : FrameLayout,
                        ICanvasView {

    // Scraps.
    private val mViewLookupTable = hashMapOf<UUID, View>()

    // Container.
    private val mRootContainer by lazy { FrameLayout(context, null) }
    private val mRootContainerBound = RectF()

    // View-Models.
    private var mModelWidth: Float = 0f
    private var mModelHeight: Float = 0f
    private var mModelToViewScale: Float = 0f

    // Listeners.
    private var mListener: IScrapLifecycleListener? = null

    // Gesture.
    private val mGestureDetector by lazy {
        GestureDetector(Looper.getMainLooper(),
                        ViewConfiguration.get(context),
                        resources.getDimension(R.dimen.touch_slop),
                        resources.getDimension(R.dimen.tap_slop),
                        resources.getDimension(R.dimen.fling_min_vec),
                        resources.getDimension(R.dimen.fling_max_vec))
    }
    private val mPointerMap = floatArrayOf(0f, 0f)
    private val mTmpMatrix = Matrix()
    private val mTransformHelper = TransformUtils()
    private val mEventNormalizationHelper by lazy {
        GestureEventNormalizationHelper()
    }

    // Common transform.
    private val mMatrixFromViewToModel = Matrix()

    // View port.
    /**
     * The view-port boundary relative to model boundary.
     */
    private val mViewPort = RectF()
    private val mViewPortStart = RectF()
    private val mViewPortPaint = Paint()
    private val mModelBoundPaint = Paint()
    private val mStartMatrixFromContainerToThis = Matrix()

    // Rendering resource.
    private val mGridPaint = Paint()
    private val mStrokePaint = Paint()
    private val mStrokePath = Path()
    private val mStrokePathTupleList = mutableListOf<PathTuple>()

    // Layout signal
    private val mLayoutFinishedSignal = BehaviorSubject.create<ICanvasView>()

    constructor(context: Context) : this(context, null)
    constructor(context: Context,
                attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // Map the events to the canvas world and apply normalization function.
        mGestureDetector.tapGestureListener = mEventNormalizationHelper
        mGestureDetector.dragGestureListener = mEventNormalizationHelper
        mGestureDetector.pinchGestureListener = mEventNormalizationHelper

        val oneDp = context.resources.getDimension(R.dimen.one_dp)

        mGridPaint.color = Color.LTGRAY
        mGridPaint.style = Paint.Style.STROKE
        mGridPaint.strokeWidth = oneDp

        mStrokePaint.strokeWidth = oneDp
        mStrokePaint.color = Color.BLACK
        mStrokePaint.style = Paint.Style.STROKE

        // For showing the relative boundary of view-port and model.
        mModelBoundPaint.color = Color.RED
        mModelBoundPaint.style = Paint.Style.FILL
        mViewPortPaint.color = Color.GREEN
        mViewPortPaint.style = Paint.Style.STROKE
        mViewPortPaint.strokeWidth = 2f * oneDp

        // Giving a background would make onDraw() able to be called.
        setBackgroundColor(Color.WHITE)
        ViewCompat.setElevation(this, 12f * oneDp)

        // Changing the pivot to left-top at the beginning.
        // Note: Changing the pivot will update the rendering matrix, where it
        // is like making the parent see the child in a different angles.
        mRootContainer.pivotX = 0f
        mRootContainer.pivotY = 0f
        addView(mRootContainer)
    }

    override fun onMeasure(widthSpec: Int,
                           heightSpec: Int) {
        Log.d("xyz", "PaperCanvasView # onMeasure()")

        if (mModelWidth == 0f || mModelHeight == 0f) {
            super.onMeasure(widthSpec, heightSpec)
        } else {
            val measureWidth = MeasureSpec.getSize(widthSpec)
            val measureHeight = MeasureSpec.getSize(heightSpec)

            val widthScale = measureWidth / mModelWidth
            val heightScale = measureHeight / mModelHeight
            val minScale = Math.min(widthScale, heightScale)
            val viewWidth = minScale * mModelWidth
            val viewHeight = minScale * mModelHeight

            super.onMeasure(MeasureSpec.makeMeasureSpec(viewWidth.toInt(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(viewHeight.toInt(), MeasureSpec.EXACTLY))
        }
    }

    override fun onLayout(changed: Boolean,
                          left: Int,
                          top: Int,
                          right: Int,
                          bottom: Int) {
        Log.d(AppConsts.TAG, "PaperCanvasView # onLayout(changed=$changed)")

        super.onLayout(changed, left, top, right, bottom)

        if (changed &&
            mModelWidth > 0f && mModelHeight > 0f) {
            val viewWidth = right - left
            val viewHeight = bottom - top

            // Hold the scale factor.
            mModelToViewScale = Math.min(viewWidth / mModelWidth,
                                             viewHeight / mModelHeight)

            // Also update the event normalization helper.
            mEventNormalizationHelper.setNormalizationFactors(
                1f / mModelToViewScale, 1f / mModelToViewScale)

            // Reset container's transform.
            mRootContainer.scaleX = 1f
            mRootContainer.scaleY = 1f
            mRootContainer.translationX = 0f
            mRootContainer.translationY = 0f

            // View port (in the model coordinate).
            mViewPort.set(0f, 0f,
                          viewWidth / mModelToViewScale,
                          viewHeight / mModelToViewScale)

            Log.d(AppConsts.TAG, "Layout with model size and view-port configuration updated.")

            // Notify layout finished.
            mLayoutFinishedSignal.onNext(this)
//            mLayoutFinishedSignal.onComplete()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (mModelWidth == 0f || mModelHeight == 0f) return

        // Grid.
        drawBoundAndGrid(canvas)

        // SketchModel.
        canvas.drawPath(mStrokePath, mStrokePaint)

        // Display the view-port relative boundary to the model.
        drawMeter(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event, null, null)
    }

    override fun invalidate() {
        super.invalidate()

        // Invalidate internal matrix.
        mMatrixFromViewToModel.reset()
    }

    ///////////////////////////////////////////////////////////////////////////
    // ICanvasView ////////////////////////////////////////////////////////////

    override fun onLayoutFinished(): Observable<ICanvasView> {
        return mLayoutFinishedSignal
    }

    override fun setCanvasSize(width: Float,
                               height: Float) {
        mModelWidth = width
        mModelHeight = height

        // Trigger onDraw() and onMeasure()
        invalidate()
        requestLayout()
    }

    override fun setScrapLifecycleListener(listener: IScrapLifecycleListener?) {
        mListener = listener
    }

    // TODO: Separate the application domain and business domain model.
    override fun addScrapView(scrap: ScrapModel) {
        ensureMainThread()

        val id = scrap.uuid
        when {
            scrap.sketch != null -> {
                val scrapView = ScrapView(context)
                scrapView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)

                scrapView.setViewToModelScale(1f / mModelToViewScale)
                // TODO: Initialize the view by model?
                // TODO: Separate the application domain and business domain model.
                scrapView.setModel(scrap)

                // Add view.
                Log.d(AppConsts.TAG, "PaperCanvasView # addView()")
                mRootContainer.addView(scrapView)
                // child view's mParent is assigned by here.

                // Add to lookup table.
                mViewLookupTable[id] = scrapView

                // Dispatch the adding event so that the scrap-controller is
                // aware of the scrap-view.
                onAttachToCanvas(scrapView)
            }
            else -> {
                // TODO: Support more types of scrap
                throw UnsupportedOperationException("Unrecognized scrap model")
            }
        }
    }

    override fun removeScrapView(id: UUID) {
        ensureMainThread()

        val view = (mViewLookupTable[id] ?: throw NoSuchElementException(
            "No view with ID=%d".format(id)))
        val scrapView = view as ScrapView

        // Remove view.
        mRootContainer.removeView(view)

        // Remove view from the lookup table.
        mViewLookupTable.remove(view.getScrapId())

        // Dispatch the removing event so that the scrap-controller becomes
        // unaware of the scrap-view.
        onDetachFromCanvas(scrapView)
    }

    override fun removeAllViews() {
        ensureMainThread()

        mViewLookupTable.values.forEach {
            val scrapView = it as IScrapView
            removeScrapView(scrapView.getScrapId())
        }
    }

    override fun onAttachToCanvas(view: IScrapView) {
        mListener?.onAttachToCanvas(view)

        Toast.makeText(
            context, "scrap added, now there are %d scraps"
            .format(mViewLookupTable.size),
            Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDetachFromCanvas(view: IScrapView) {
        mListener?.onDetachFromCanvas(view)

        Toast.makeText(
            context, "scrap removed, now there are %d scraps"
            .format(mViewLookupTable.size),
            Toast.LENGTH_SHORT)
            .show()
    }

    // Transform //////////////////////////////////////////////////////////////

    override fun setGestureListener(listener: SimpleGestureListener?) {
        mEventNormalizationHelper.setGestureListener(listener)
    }

    override fun startUpdateViewport() {
        // Hold container matrix starting value.
        mStartMatrixFromContainerToThis.set(mRootContainer.matrix)

        // Hold view port starting state.
        mViewPortStart.set(mViewPort)
    }

    override fun onUpdateViewport(startPointers: Array<PointF>,
                                  stopPointers: Array<PointF>) {
        // TODO: Make the view-port code a component.

        // Calculate the container matrix.
        val transform = TransformUtils.getTransformFromPointers(
            startPointers, stopPointers)
        val dx = mEventNormalizationHelper.inverseNormalizationToX(transform[TransformUtils.DELTA_X])
        val dy = mEventNormalizationHelper.inverseNormalizationToY(transform[TransformUtils.DELTA_Y])
        val dScale = transform[TransformUtils.DELTA_SCALE_X]
        val pivotX = transform[TransformUtils.PIVOT_X]
        val pivotY = transform[TransformUtils.PIVOT_Y]
        mTmpMatrix.set(mStartMatrixFromContainerToThis)
        mTmpMatrix.postScale(dScale, dScale, pivotX, pivotY)
        mTmpMatrix.postTranslate(dx, dy)

        // Use the matrix to get the container boundary.
        mRootContainerBound.set(0f, 0f, width.toFloat(), height.toFloat())
        mTmpMatrix.mapRect(mRootContainerBound)

        // Infer the view-port by above container boundary.
        mViewPort.set(0f, 0f, mModelWidth, mModelHeight)
        val viewPortScale = width.toFloat() / mRootContainerBound.width()
        val viewPortTx = -mRootContainerBound.left / mModelToViewScale
        val viewPortTy = -mRootContainerBound.top / mModelToViewScale
        mTmpMatrix.reset()
        mTmpMatrix.postTranslate(viewPortTx, viewPortTy)
        mTmpMatrix.postScale(viewPortScale, viewPortScale, 0f, 0f)
        mTmpMatrix.mapRect(mViewPort)

        // Constraint view port.
        val maxWidth = mModelWidth
        val maxHeight = mModelHeight
        val minWidth = maxWidth / 4
        val minHeight = maxHeight / 4
        constraintViewPort(mViewPort, minWidth, minHeight, maxWidth, maxHeight)

        // TODO: Make the following code a function.
        // Convert view-port to container transform.
        val finalScale = mModelWidth / mViewPort.width()
        val finalTx = -mViewPort.left
        val finalTy = -mViewPort.top
        mRootContainer.scaleX = finalScale
        mRootContainer.scaleY = finalScale
        mRootContainer.translationX = mRootContainer.scaleX * mModelToViewScale * finalTx
        mRootContainer.translationY = mRootContainer.scaleX * mModelToViewScale * finalTy

        invalidate()
    }

    override fun stopUpdateViewport() {
        // DO NOTHING.
    }

    // Drawing ////////////////////////////////////////////////////////////////

    override fun startDrawStroke(x: Float, y: Float) {
        mStrokePath.reset()
        mStrokePath.moveTo(mEventNormalizationHelper.inverseNormalizationToX(x),
                           mEventNormalizationHelper.inverseNormalizationToY(y))
        mStrokePath.lineTo(mEventNormalizationHelper.inverseNormalizationToX(x),
                           mEventNormalizationHelper.inverseNormalizationToY(y))

        mStrokePathTupleList.clear()
        mStrokePathTupleList.add(PathTuple(x, y))

        invalidate()
    }

    override fun onDrawStroke(x: Float, y: Float) {
        mStrokePath.lineTo(mEventNormalizationHelper.inverseNormalizationToX(x),
                           mEventNormalizationHelper.inverseNormalizationToY(y))

        mStrokePathTupleList.add(PathTuple(x, y))

        invalidate()
    }

    override fun stopDrawStroke(): SketchStroke {
        mStrokePath.reset()

        val sketchStroke = SketchStroke()
        // Map tuple list to model world by also considering view-port transform.
        val offsetX = mViewPort.left
        val offsetY = mViewPort.top
        val scale = Math.max(mViewPort.width() / mModelWidth,
                             mViewPort.height() / mModelHeight)
        mStrokePathTupleList.forEach { tuple ->
            tuple.scale(scale)
            tuple.translate(offsetX, offsetY)

            sketchStroke.addPathTuple(tuple)
        }
        mStrokePathTupleList.clear()

        invalidate()

        return sketchStroke
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun ensureMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalThreadStateException("Not in MAIN thread")
        }
    }

    private fun validateViewPortMatrix() {
        if (mMatrixFromViewToModel.isIdentity) {
            // Calculate the canvas transform in terms of the view-port
            val scaleFromViewToModel = 1f / mModelToViewScale
            val viewPortScale = mViewPort.width() / mModelWidth
            val viewPortTx = mViewPort.left
            val viewPortTy = mViewPort.top
            mMatrixFromViewToModel.postScale(scaleFromViewToModel,
                                             scaleFromViewToModel)
            mMatrixFromViewToModel.postScale(viewPortScale, viewPortScale)
            mMatrixFromViewToModel.postTranslate(viewPortTx, viewPortTy)
        }
    }

    private fun mapPoint(matrix: Matrix, point: PointF): PointF {
        mPointerMap[0] = point.x
        mPointerMap[1] = point.y

        matrix.mapPoints(mPointerMap)

        return PointF(mPointerMap[0], mPointerMap[1])
    }

    private fun constraintViewPort(viewPort: RectF,
                                   minWidth: Float,
                                   minHeight: Float,
                                   maxWidth: Float,
                                   maxHeight: Float) {
        // In width...
        if (viewPort.width() < minWidth) {
            viewPort.right = viewPort.left + minWidth
        } else if (viewPort.width() > maxWidth) {
            viewPort.right = viewPort.left + maxWidth
        }
        // In height...
        if (viewPort.height() < minHeight) {
            viewPort.bottom = viewPort.top + minHeight
        } else if (viewPort.height() > maxHeight) {
            viewPort.bottom = viewPort.top + maxHeight
        }
        // In x...
        val viewPortWidth = viewPort.width()
        if (viewPort.left < 0f) {
            viewPort.left = 0f
            viewPort.right = viewPort.left + viewPortWidth
        } else if (viewPort.right > maxWidth) {
            viewPort.right = maxWidth
            viewPort.left = viewPort.right - viewPortWidth
        }
        // In y...
        val viewPortHeight = viewPort.height()
        if (viewPort.top < 0f) {
            viewPort.top = 0f
            viewPort.bottom = viewPort.top + viewPortHeight
        } else if (viewPort.bottom > maxHeight) {
            viewPort.bottom = maxHeight
            viewPort.top = viewPort.bottom - viewPortHeight
        }
    }

    private fun drawBoundAndGrid(canvas: Canvas) {
        val count = canvas.save()

        // From model to view coordinate.
        mTmpMatrix.reset()
        mTmpMatrix.postScale(mModelToViewScale,
                             mModelToViewScale)

        // Transform contributed by view-port.
        val viewPortScale = mViewPort.width() / mModelWidth
        val viewPortTx = mViewPort.left
        val viewPortTy = mViewPort.top
        mTmpMatrix.postScale(1f / viewPortScale,
                             1f / viewPortScale)
        mTmpMatrix.postTranslate(1f / viewPortScale * mModelToViewScale * -viewPortTx,
                                 1f / viewPortScale * mModelToViewScale * -viewPortTy)
        canvas.concat(mTmpMatrix)

        val cell = Math.min(mModelWidth, mModelHeight) / 20

        // Boundary.
        canvas.drawLine(0f, 0f, mModelWidth, 0f, mGridPaint)
        canvas.drawLine(mModelWidth, 0f, mModelWidth, mModelHeight, mGridPaint)
        canvas.drawLine(mModelWidth, mModelHeight, 0f, mModelHeight, mGridPaint)
        canvas.drawLine(0f, mModelHeight, 0f, 0f, mGridPaint)

        // Grid.
        var x = 0f
        while (x < mModelWidth) {
            canvas.drawLine(x, 0f, x, mModelHeight, mGridPaint)
            x += cell
        }
        var y = 0f
        while (y < mModelHeight) {
            canvas.drawLine(0f, y, mModelWidth, y, mGridPaint)
            y += cell
        }

        canvas.restoreToCount(count)
    }

    private fun drawMeter(canvas: Canvas) {
        val count = canvas.save()

        val scale = 1f / 6
        mTmpMatrix.reset()
        mTmpMatrix.postScale(scale, scale)
        mTmpMatrix.postScale(mModelToViewScale, mModelToViewScale)
        canvas.concat(mTmpMatrix)

        canvas.drawRect(0f, 0f, mModelWidth, mModelHeight, mModelBoundPaint)
        canvas.drawRect(mViewPort.left,
                        mViewPort.top,
                        mViewPort.right,
                        mViewPort.bottom,
                        mViewPortPaint)

        canvas.restoreToCount(count)
    }
}
