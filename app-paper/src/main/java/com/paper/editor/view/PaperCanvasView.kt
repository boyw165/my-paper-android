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
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.cardinalblue.gesture.GestureDetector
import com.paper.R
import com.paper.shared.model.ScrapModel
import com.paper.util.TransformUtils
import java.lang.UnsupportedOperationException
import java.util.*

class PaperCanvasView : FrameLayout,
                        ICanvasView {

    // Views.
    private val mViewLookupTable = hashMapOf<UUID, View>()
    private val mRootContainer by lazy { FrameLayout(context, null) }

    // View-Models.
    private var mModelWidth: Float = 0f
    private var mModelHeight: Float = 0f
    private var mScaleFromModelToView: Float = 0f

    // Listeners.
    private var mListener: IScrapLifecycleListener? = null

    // Gesture.
    private val mGestureDetector by lazy {
        GestureDetector(context,
                        resources.getDimension(R.dimen.touch_slop),
                        resources.getDimension(R.dimen.tap_slop),
                        resources.getDimension(R.dimen.fling_min_vec),
                        resources.getDimension(R.dimen.fling_max_vec))
    }
    private val mPointerMap = floatArrayOf(0f, 0f)
    private val mTmpMatrix = Matrix()
    private val mTransformHelper = TransformUtils()

    // View port.
    private val mViewPort = RectF()
    private val mViewPortStart = RectF()
    private val mViewPortPaint = Paint()
    private val mModelBoundPaint = Paint()

    // Rendering resource.
    private val mGridPaint = Paint()
    private val mSketchPaint = Paint()
    private val mSketchPath = Path()

    constructor(context: Context) : this(context, null)
    constructor(context: Context,
                attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val oneDp = context.resources.getDimension(R.dimen.one_dp)

        mGridPaint.color = Color.LTGRAY
        mGridPaint.style = Paint.Style.STROKE
        mGridPaint.strokeWidth = oneDp

        mSketchPaint.strokeWidth = oneDp
        mSketchPaint.color = Color.BLACK
        mSketchPaint.style = Paint.Style.STROKE

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

            // Hold the scale factor.
            mScaleFromModelToView = minScale

            // View port (in the model coordinate).
            mViewPort.set(0f, 0f,
                          viewWidth / mScaleFromModelToView,
                          viewHeight / mScaleFromModelToView)

            super.onMeasure(MeasureSpec.makeMeasureSpec(viewWidth.toInt(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(viewHeight.toInt(), MeasureSpec.EXACTLY))
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (mModelWidth == 0f || mModelHeight == 0f) return

        drawBoundAndGrid(canvas)

        // Sketch.
        canvas.drawPath(mSketchPath, mSketchPaint)

        // Display the view-port relative boundary to the model.
        drawMeter(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event, null, null)
    }

    ///////////////////////////////////////////////////////////////////////////
    // ICanvasView ////////////////////////////////////////////////////////////

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
    override fun addViewBy(scrap: ScrapModel) {
        val id = scrap.id
        when {
            scrap.sketch != null -> {
                val scrapView = ScrapView(context)
                scrapView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)

                // TODO: Initialize the view by model?
                // TODO: Separate the application domain and business domain model.
                scrapView.setModel(scrap)

                // Add view.
                mRootContainer.addView(scrapView)

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

    override fun removeViewBy(id: UUID) {
        val view = (mViewLookupTable[id] ?: throw NoSuchElementException(
            "No view with ID=%d".format(id)))
        val scrapView = view as IScrapView

        // Remove view.
        mRootContainer.removeView(view)

        // Remove view from the lookup table.
        mViewLookupTable.remove(view.getScrapId())

        // Dispatch the removing event so that the scrap-controller becomes
        // unaware of the scrap-view.
        onDetachFromCanvas(scrapView)
    }

    override fun removeAllViews() {
        mViewLookupTable.values.forEach {
            val scrapView = it as IScrapView
            removeViewBy(scrapView.getScrapId())
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
        mGestureDetector.tapGestureListener = listener
        mGestureDetector.dragGestureListener = listener
        mGestureDetector.pinchGestureListener = listener
    }

    override fun startTransformViewport() {
        // Hold view port starting state.
        mViewPortStart.set(mViewPort)
    }

    override fun onTransformViewport(startPointers: Array<PointF>,
                                     stopPointers: Array<PointF>) {
        // Calculate the view port according to the pointers change.
        val transform = TransformUtils.getTransformFromPointers(
            startPointers, stopPointers)
        val dx = transform[TransformUtils.DELTA_X]
        val dy = transform[TransformUtils.DELTA_Y]
        val dScale = transform[TransformUtils.DELTA_SCALE_X]
        val pivotX = transform[TransformUtils.PIVOT_X] / width * mViewPortStart.width()
        val pivotY = transform[TransformUtils.PIVOT_Y] / height * mViewPortStart.height()
        mViewPort.set(mViewPortStart)
        mTmpMatrix.reset()
        mTmpMatrix.postScale(1f / dScale, 1f / dScale, pivotX, pivotY)
        mTmpMatrix.postTranslate(-dx, -dy)
        mTmpMatrix.mapRect(mViewPort)

        // Constraint view port.
        val maxWidth = mModelWidth
        val maxHeight = mModelHeight
        val minWidth = maxWidth / 4
        val minHeight = maxHeight / 4
        // In width...
        if (mViewPort.width() < minWidth) {
            mViewPort.right = mViewPort.left + minWidth
        } else if (mViewPort.width() > maxWidth) {
            mViewPort.right = mViewPort.left + maxWidth
        }
        // In height...
        if (mViewPort.height() < minHeight) {
            mViewPort.bottom = mViewPort.top + minHeight
        } else if (mViewPort.height() > maxHeight) {
            mViewPort.bottom = mViewPort.top + maxHeight
        }
        // In x...
        val viewPortWidth = mViewPort.width()
        if (mViewPort.left < 0f) {
            mViewPort.left = 0f
            mViewPort.right = mViewPort.left + viewPortWidth
        } else if (mViewPort.right > maxWidth) {
            mViewPort.right = maxWidth
            mViewPort.left = mViewPort.right - viewPortWidth
        }
        // In y...
        val viewPortHeight = mViewPort.height()
        if (mViewPort.top < 0f) {
            mViewPort.top = 0f
            mViewPort.bottom = mViewPort.top + viewPortHeight
        } else if (mViewPort.bottom > maxHeight) {
            mViewPort.bottom = maxHeight
            mViewPort.top = mViewPort.bottom - viewPortHeight
        }

        // Calculate the canvas transform in terms of the view-port
        val viewPortScale = mViewPort.width() / mModelWidth
        val viewPortTx = mViewPort.left
        val viewPortTy = mViewPort.top
        mRootContainer.scaleX = 1f / viewPortScale
        mRootContainer.scaleY = 1f / viewPortScale
        mRootContainer.translationX = mScaleFromModelToView * -viewPortTx
        mRootContainer.translationY = mScaleFromModelToView * -viewPortTy

        invalidate()
    }

    override fun stopTransformViewport() {
        // DO NOTHING.
    }

    override fun normalizePointer(p: PointF): PointF {
        return PointF(p.x / width, p.y / height)
    }

    // Drawing ////////////////////////////////////////////////////////////////

    private fun drawBoundAndGrid(canvas: Canvas) {
        canvas.save()
        canvas.concat(mRootContainer.matrix)

        val cell = Math.min(width.toFloat(), height.toFloat()) / 10

        // Boundary.
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, mGridPaint)
        canvas.drawLine(width.toFloat(), 0f, width.toFloat(), height.toFloat(), mGridPaint)
        canvas.drawLine(width.toFloat(), height.toFloat(), 0f, height.toFloat(), mGridPaint)
        canvas.drawLine(0f, height.toFloat(), 0f, 0f, mGridPaint)

        // Grid.
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), mGridPaint)
            x += cell
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, mGridPaint)
            y += cell
        }

        canvas.restore()
    }

    private fun drawMeter(canvas: Canvas) {
        val count = canvas.saveCount

        val scale = 1f / 6
        mTmpMatrix.reset()
        mTmpMatrix.postScale(scale, scale)
        mTmpMatrix.postScale(mScaleFromModelToView, mScaleFromModelToView)
        canvas.concat(mTmpMatrix)

        canvas.drawRect(0f, 0f, mModelWidth, mModelHeight, mModelBoundPaint)
        canvas.drawRect(mViewPort.left,
                        mViewPort.top,
                        mViewPort.right,
                        mViewPort.bottom,
                        mViewPortPaint)

        canvas.restoreToCount(count)
    }

    override fun startDrawSketch(x: Float, y: Float) {
        mSketchPath.reset()
        mSketchPath.moveTo(x, y)
        mSketchPath.lineTo(x, y)

//        mRootContainer.invalidate()
        invalidate()
    }

    override fun onDrawSketch(x: Float, y: Float) {
        mSketchPath.lineTo(x, y)

//        mRootContainer.invalidate()
        invalidate()
    }

    override fun stopDrawSketch() {
        mSketchPath.reset()

//        mRootContainer.invalidate()
        invalidate()
    }

    private fun convertPointToParentWorld(point: PointF): PointF {
        mPointerMap[0] = point.x
        mPointerMap[1] = point.y
        mRootContainer.matrix.mapPoints(mPointerMap)

        return PointF(mPointerMap[0], mPointerMap[1])
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun scaleFromModelToView(): Float {
        return width.toFloat() / mModelWidth
    }
}
