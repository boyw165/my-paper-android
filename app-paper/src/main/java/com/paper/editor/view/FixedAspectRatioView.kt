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
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.paper.protocol.ICanvasDelegate
import com.paper.protocol.ITouchDelegate

open class FixedAspectRatioView : FrameLayout {

    // Delegate.
    private var mCanvasDelegate: ICanvasDelegate? = null
    private var mTouchDelegate: ITouchDelegate? = null

    private var mWidthOverHeight: Float = 0f

    constructor(context: Context) : this(context, null)

    constructor(context: Context,
                attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mWidthOverHeight == 0f) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val measureWidth = MeasureSpec.getSize(widthMeasureSpec)
            val measureHeight = MeasureSpec.getSize(heightMeasureSpec)

            val width = Math.min(measureWidth, measureHeight).toFloat()
            val height = width / mWidthOverHeight

            super.onMeasure(MeasureSpec.makeMeasureSpec(width.toInt(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(height.toInt(), MeasureSpec.EXACTLY))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mCanvasDelegate?.onDelegateDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (mTouchDelegate == null) {
            super.onTouchEvent(event)
        } else {
            mTouchDelegate!!.onDelegateTouchEvent(event)
        }
    }

    fun setWidthOverHeightRatio(ratio: Float) {
        mWidthOverHeight = ratio

        invalidate()
        requestLayout()
    }

    fun setCanvasDelegate(delegate: ICanvasDelegate) {
        mCanvasDelegate = delegate
    }

    fun setTouchEventDelegate(delegate: ITouchDelegate) {
        mTouchDelegate = delegate
    }
}
