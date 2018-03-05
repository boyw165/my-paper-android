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
import com.paper.editor.ITouchConfig
import com.paper.protocol.ICanvasDelegate
import com.paper.protocol.ITouchDelegate
import com.paper.shared.model.ScrapModel
import com.paper.util.TransformUtils
import java.lang.UnsupportedOperationException
import java.util.*

class PaperCanvasView : FrameLayout,
                        ITouchConfig,
                        ICanvasView,
                        ICanvasDelegate,
                        ITouchDelegate,
                        View.OnLayoutChangeListener {

    // Views.
    private val mScrapContainer by lazy { FixedAspectRatioView(context, null) }
    private val mViewLookupTable = hashMapOf<UUID, View>()

    // Listeners.
    private var mListener: IScrapLifecycleListener? = null

    // Gesture.
    private val mNormalizationTransform = Matrix()
    private var mNormalizedEvent: MotionEvent? = null
    private val mTransformHelper: TransformUtils = TransformUtils()
    private val mGestureDetector: GestureDetector by lazy {
        GestureDetector(context,
                        getTouchSlop(),
                        getTapSlop(),
                        getMinFlingVec(),
                        getMaxFlingVec())
    }

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

        // Changing the pivot to left-top at the beginning.
        // Note: Changing the pivot will update the rendering matrix, where it
        // is like making the parent see the child in a different angles.
        mScrapContainer.pivotX = 0f
        mScrapContainer.pivotY = 0f
        // Giving a background would make onDraw() able to be called.
        mScrapContainer.setBackgroundColor(Color.WHITE)
        // For drawing grid background.
        mScrapContainer.setCanvasDelegate(this)
        // For padding/margin insensitive touch.
        mScrapContainer.setTouchEventDelegate(this)
        // TEST
        //        mScrapContainer.scaleX = 0.9f
        //        mScrapContainer.scaleY = 0.9f
        //        mScrapContainer.translationX = 16 * oneDp
        //        mScrapContainer.translationY = 16 * oneDp
        ViewCompat.setElevation(mScrapContainer, 20 * oneDp)
        mScrapContainer.addOnLayoutChangeListener(this@PaperCanvasView)
        addView(mScrapContainer)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        mScrapContainer.removeOnLayoutChangeListener(this@PaperCanvasView)

        mNormalizedEvent?.recycle()
    }

    override fun onLayoutChange(v: View,
                                left: Int,
                                top: Int,
                                right: Int,
                                bottom: Int,
                                oldLeft: Int,
                                oldTop: Int,
                                oldRight: Int,
                                oldBottom: Int) {
        mNormalizationTransform.postScale(1f / mScrapContainer.width,
                                          1f / mScrapContainer.height)
    }

    override fun onDelegateDraw(canvas: Canvas) {
        val canvasWidth = mScrapContainer.width.toFloat()
        val canvasHeight = mScrapContainer.height.toFloat()
        val cell = Math.min(canvasWidth, canvasHeight) / 10

        // Boundary.
        canvas.drawLine(0f, 0f, canvasWidth, 0f, mGridPaint)
        canvas.drawLine(canvasWidth, 0f, canvasWidth, canvasHeight, mGridPaint)
        canvas.drawLine(canvasWidth, canvasHeight, 0f, canvasHeight, mGridPaint)
        canvas.drawLine(0f, canvasHeight, 0f, 0f, mGridPaint)

        // Grid.
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, canvasHeight, mGridPaint)
            x += cell
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, canvasWidth, y, mGridPaint)
            y += cell
        }

        // Sketch.
        canvas.drawPath(mSketchPath, mSketchPaint)
    }

    override fun onDelegateTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event, null, null)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Touch config ///////////////////////////////////////////////////////////

    override fun getTouchSlop(): Float {
        return resources.getDimension(R.dimen.touch_slop)
    }

    override fun getTapSlop(): Float {
        return resources.getDimension(R.dimen.tap_slop)
    }

    override fun getMinFlingVec(): Float {
        return resources.getDimension(R.dimen.fling_min_vec)
    }

    override fun getMaxFlingVec(): Float {
        return resources.getDimension(R.dimen.fling_max_vec)
    }

    ///////////////////////////////////////////////////////////////////////////
    // ICanvasView ////////////////////////////////////////////////////////////

    override fun setCanvasWidthOverHeightRatio(ratio: Float) {
        mScrapContainer.setWidthOverHeightRatio(ratio)
    }

    override fun setScrapLifecycleListener(listener: IScrapLifecycleListener?) {
        mListener = listener
    }

    override fun setGestureListener(listener: SimpleGestureListener?) {
        mGestureDetector.tapGestureListener = listener
        mGestureDetector.dragGestureListener = listener
        mGestureDetector.pinchGestureListener = listener
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
                mScrapContainer.addView(scrapView)

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
        mScrapContainer.removeView(view)

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

    ///////////////////////////////////////////////////////////////////////////
    // Drawing ////////////////////////////////////////////////////////////////

    override fun normalizePointer(p: PointF): PointF {
        return PointF(p.x / mScrapContainer.width,
                      p.y / mScrapContainer.height)
    }

    override fun startDrawSketch(x: Float, y: Float) {
        mSketchPath.reset()
        mSketchPath.moveTo(x, y)
        mSketchPath.lineTo(x, y)

        mScrapContainer.invalidate()
    }

    override fun onDrawSketch(x: Float, y: Float) {
        mSketchPath.lineTo(x, y)

        mScrapContainer.invalidate()
    }

    override fun stopDrawSketch() {
        mSketchPath.reset()

        mScrapContainer.invalidate()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun getCanvasWidth(): Float {
        return mScrapContainer.width.toFloat()
    }

    private fun getCanvasHeight(): Float {
        return mScrapContainer.height.toFloat()
    }
}
