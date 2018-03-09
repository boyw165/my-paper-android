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
    private var mCanvasWidth: Float = 0f
    private var mCanvasHeight: Float = 0f

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
    private val mStartMatrix = Matrix()
    private val mStopMatrix = Matrix()
    private val mTmpMatrix = Matrix()
    private val mTransformHelper = TransformUtils()

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
        if (mCanvasWidth == 0f || mCanvasHeight == 0f) {
            super.onMeasure(widthSpec, heightSpec)
        } else {
            val measureWidth = MeasureSpec.getSize(widthSpec)
            val measureHeight = MeasureSpec.getSize(heightSpec)

            val widthScale = measureWidth / mCanvasWidth
            val heightScale = measureHeight / mCanvasHeight
            val minScale = Math.min(widthScale, heightScale)
            val width = minScale * mCanvasWidth
            val height = minScale * mCanvasHeight

            super.onMeasure(MeasureSpec.makeMeasureSpec(width.toInt(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(height.toInt(), MeasureSpec.EXACTLY))
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (mCanvasWidth == 0f || mCanvasHeight == 0f) return

        drawBoundAndGrid(canvas)

        // Sketch.
        canvas.drawPath(mSketchPath, mSketchPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event, null, null)
    }

    ///////////////////////////////////////////////////////////////////////////
    // ICanvasView ////////////////////////////////////////////////////////////

    override fun setCanvasSize(width: Float,
                               height: Float) {
        mCanvasWidth = width
        mCanvasHeight = height

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
        // Hold start transform
        mStartMatrix.reset()
        mStartMatrix.set(mRootContainer.matrix)
        mStopMatrix.reset()
        mStopMatrix.set(mStartMatrix)
    }

    override fun onTransformViewport(startPointers: Array<PointF>,
                                     stopPointers: Array<PointF>) {
        // Get the matrix that converts coordinates from this to the container.
        mRootContainer.matrix.invert(mTmpMatrix)

        // Map the coordinates from this world to the container world.
        val startPointersInContainer = Array(startPointers.size, { i ->
            mPointerMap[0] = startPointers[i].x
            mPointerMap[1] = startPointers[i].y
            mTmpMatrix.mapPoints(mPointerMap)

            return@Array PointF(mPointerMap[0], mPointerMap[1])
        })
        val stopPointersInContainer = Array(stopPointers.size, { i ->
            mPointerMap[0] = stopPointers[i].x
            mPointerMap[1] = stopPointers[i].y
            mTmpMatrix.mapPoints(mPointerMap)

            return@Array PointF(mPointerMap[0], mPointerMap[1])
        })

        // Calculate the transformation.
        val transform = TransformUtils.getTransformFromPointers(
            startPointersInContainer, stopPointersInContainer)

        val dx = transform[TransformUtils.DELTA_X]
        val dy = transform[TransformUtils.DELTA_Y]
        val dScale = transform[TransformUtils.DELTA_SCALE_X]
        val pivotX = transform[TransformUtils.PIVOT_X]
        val pivotY = transform[TransformUtils.PIVOT_Y]

        // Update the RAW transform (without any modification).
        mStopMatrix.set(mStartMatrix)
        mStopMatrix.postScale(dScale, dScale, pivotX, pivotY)
        mStopMatrix.postTranslate(dx, dy)

        // Prepare the transform for the view (might be modified).
        mTransformHelper.getValues(mStopMatrix)

        mRootContainer.scaleX = mTransformHelper.scaleX
        mRootContainer.scaleY = mTransformHelper.scaleY
        mRootContainer.translationX = mTransformHelper.translationX
        mRootContainer.translationY = mTransformHelper.translationY

        invalidate()
    }

    override fun stopTransformViewport() {
        // DO NOTHING.
    }

    private fun convertPointToParentWorld(point: PointF): PointF {
        mPointerMap[0] = point.x
        mPointerMap[1] = point.y
        mRootContainer.matrix.mapPoints(mPointerMap)

        return PointF(mPointerMap[0], mPointerMap[1])
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

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun scaleFromModelToView(): Float {
        return width.toFloat() / mCanvasWidth
    }
}
