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
import android.view.MotionEvent
import android.widget.FrameLayout
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.GesturePolicy
import com.paper.R
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils

open class ScrapView : FrameLayout,
                       IScrapView {

    private lateinit var mModel: ScrapModel

    // Rendering properties.
    private var mIsCacheDirty: Boolean = false
    private val mScrapBound = RectF()

    // Gesture.
    private val mTransformHelper: TransformUtils = TransformUtils()
    private val mGestureDetector: GestureDetector by lazy {
        GestureDetector(context,
                        getTouchSlop(),
                        getTapSlop(),
                        getMinFlingVec(),
                        getMaxFlingVec())
    }

    constructor(context: Context?) : super(context)

    constructor(context: Context?,
                attrs: AttributeSet?) : super(context, attrs)

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

        // TEST: Set DRAG_ONLY policy.
        mGestureDetector.setPolicy(GesturePolicy.DRAG_ONLY)
    }

    private val mTestPaint = Paint()

    override fun onDraw(canvas: Canvas) {
        mTestPaint.style = Paint.Style.FILL
        mTestPaint.color = Color.BLUE

        validateRenderingCache()

        canvas.drawRect(mScrapBound, mTestPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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

        // Mark the rendering cache is dirty and later validateRenderingCache()
        // would update the necessary properties for rendering and touching.
        mIsCacheDirty = true
    }

    override fun getScrapId(): Long {
        return mModel.id
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

    override fun getGestureDetector(): GestureDetector {
        return mGestureDetector
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gesture handling ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun validateRenderingCache() {
        if (mIsCacheDirty) {
            translationX = mModel.x * width
            translationY = mModel.y * height

            mScrapBound.set(0f, 0f,
                            mModel.width * width,
                            mModel.height * height)

            mIsCacheDirty = false
        }
    }
}

