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
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.GesturePolicy
import com.paper.R
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils
import java.util.*

open class ScrapView : FrameLayout,
                       IScrapView {

    private lateinit var mModel: ScrapModel

    // Rendering properties.
    private var mIsCacheDirty: Boolean = true
    private val mScrapBound = RectF()
    private val mSketchPath = Path()
    private val mSketchPaint = Paint()
    private val mSketchMinWidth: Float by lazy { resources.getDimension(R.dimen.sketch_min_stroke_width) }
    private val mSketchMaxWidth: Float by lazy { resources.getDimension(R.dimen.sketch_max_stroke_width) }

    // Gesture.
    private val mTransformHelper: TransformUtils = TransformUtils()
    private val mGestureDetector: GestureDetector by lazy {
        GestureDetector(context,
                        getTouchSlop(),
                        getTapSlop(),
                        getMinFlingVec(),
                        getMaxFlingVec())
    }

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?,
                attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Changing the pivot to left-top at the beginning.
        // Note: Changing the pivot will update the rendering matrix, where it
        // is like making the parent see the child in a different angles.
        pivotX = 0f
        pivotY = 0f

        // Giving a background would make onDraw() able to be called.
        setBackgroundColor(Color.TRANSPARENT)

        invalidateRenderingCache()

        // TEST: Set DRAG_ONLY policy.
        mGestureDetector.setPolicy(GesturePolicy.ALL)
    }

    override fun onDraw(canvas: Canvas) {
        mSketchPaint.style = Paint.Style.STROKE
        mSketchPaint.color = Color.BLUE
        mSketchPaint.strokeWidth = mSketchMinWidth * 3

        validateRenderingCache()

        // Boundary.
        canvas.drawRect(mScrapBound, mSketchPaint)
        // Sketch.
        canvas.drawPath(mSketchPath, mSketchPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> Log.d("xyz", "ACTION_DOWN, count=%d".format(event.pointerCount))
            MotionEvent.ACTION_MOVE -> Log.d("xyz", "ACTION_MOVE, count=%d".format(event.pointerCount))
            MotionEvent.ACTION_UP -> Log.d("xyz", "ACTION_UP, count=%d".format(event.pointerCount))
            MotionEvent.ACTION_CANCEL -> Log.d("xyz", "ACTION_CANCEL, count=%d".format(event.pointerCount))
            MotionEvent.ACTION_OUTSIDE -> Log.d("xyz", "ACTION_OUTSIDE, count=%d".format(event.pointerCount))
        }

        // Validate the hitting boundary.
        validateRenderingCache()

        val x = event.getX(0)
        val y = event.getY(0)

        // If the canvas doesn't handle the touch, bubble up the event.
        return if (mScrapBound.contains(x, y)) {
            mGestureDetector.onTouchEvent(event, this, null)
        } else {
            false
        }
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
    // IScrapView /////////////////////////////////////////////////////////////

    override fun setModel(model: ScrapModel) {
        mModel = model

        invalidateRenderingCache()
    }

    override fun getScrapId(): UUID {
        return mModel.id
    }

    override fun getCanvasWidth(): Int {
        return width
    }

    override fun getCanvasHeight(): Int {
        return height
    }

    override fun getTransform(): TransformModel {
        return TransformModel(
            translationX = this.translationX,
            translationY = this.translationY,
            scaleX = this.scaleX,
            scaleY = this.scaleY,
            rotationInRadians = Math.toRadians(this.rotation.toDouble()).toFloat())
    }

    override fun setTransform(transform: TransformModel) {
        scaleX = transform.scaleX
        scaleY = transform.scaleY
        rotation = Math.toDegrees(transform.rotationInRadians.toDouble()).toFloat()
        translationX = transform.translationX
        translationY = transform.translationY
    }

    override fun getTransformMatrix(): Matrix {
        return matrix
    }

    override fun setTransformPivot(px: Float,
                                   py: Float) {
        pivotX = px
        pivotY = py
    }

    override fun convertPointToParentWorld(point: FloatArray) {
        matrix.mapPoints(point)
    }

    override fun setGestureListener(listener: SimpleGestureListener?) {
        mGestureDetector.tapGestureListener = listener
        mGestureDetector.dragGestureListener = listener
        mGestureDetector.pinchGestureListener = listener
    }

    override fun invalidateRenderingCache() {
        // Mark the rendering cache is dirty and later validateRenderingCache()
        // would update the necessary properties for rendering and touching.
        mIsCacheDirty = true
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun validateRenderingCache() {
        if (!mIsCacheDirty) return

        // Translation.
        translationX = mModel.x * width
        translationY = mModel.y * height

        // Boundary.
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = Float.MIN_VALUE
        var bottom = Float.MIN_VALUE
        mModel.sketch?.allStrokes?.forEach { stroke ->
            stroke.allPathTuple.forEach { tuple ->
                left = Math.min(left, tuple.firstPoint.x)
                top = Math.min(top, tuple.firstPoint.y)
                right = Math.max(right, tuple.firstPoint.x)
                bottom = Math.max(bottom, tuple.firstPoint.y)
            }
        }
        mScrapBound.set(left * width,
                        top * height,
                        right * width,
                        bottom * height)

        // Path for sketch.
        rebuildSketchPath()

        mIsCacheDirty = false
    }

    private fun rebuildSketchPath() {
        mModel.sketch?.let { sketch ->
            mSketchPath.reset()

            sketch.allStrokes.forEach { stroke ->
                stroke.allPathTuple.forEachIndexed { i, tuple ->
                    val x = tuple.firstPoint.x * width
                    val y = tuple.firstPoint.y * height

                    when (i) {
                        0 -> {
                            mSketchPath.moveTo(x, y)
                        }
                        else -> {
                            mSketchPath.lineTo(x, y)
                        }
                    }
                }
            }
        }
    }
}

