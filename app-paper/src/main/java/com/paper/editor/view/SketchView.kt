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
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.cardinalblue.gesture.GestureDetector
import com.paper.R
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils

open class SketchView : FrameLayout,
                        IScrapView {

    private var mScrapId: Long = 0L

    // Gesture.
    protected val mTransformHelper: TransformUtils = TransformUtils()
    protected val mGestureDetector: GestureDetector by lazy {
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
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // TODO: Solve the hitting problem.

        // If the canvas don't handle the touch, bubble up the event.
//        return (mGestureDetector?.onTouchEvent(event, this, 0) ?: false) ||
//               super.onTouchEvent(event)
        return false
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

    override fun getScrapId(): Long {
        return mScrapId
    }

    override fun setScrapId(id: Long) {
        mScrapId = id
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

    override fun setTransformPivot(pivotX: Float,
                                   pivotY: Float) {
        TODO("not implemented")
    }

    override fun getTransformMatrix(): Matrix {
        TODO("not implemented")
    }

    override fun convertPointToParentWorld(point: FloatArray) {
        TODO("not implemented")
    }

    override fun getGestureDetector(): GestureDetector {
        TODO("not implemented")
//        return mGestureDetector
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gesture handling ///////////////////////////////////////////////////////
}

