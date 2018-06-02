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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class DrawableView : View {

    private val mDots: MutableList<PointF> = mutableListOf()

    // Paint.
    private val mDotPaint: Paint by lazy {
        val p = Paint()
        p.style = Paint.Style.STROKE
        p.color = Color.BLACK
        p.strokeWidth = 15f
        p
    }

    constructor(context: Context)
        : this(context, null)

    constructor(context: Context?,
                attrs: AttributeSet?)
        : this(context, attrs, 0)

    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int)
        : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val r = mDotPaint.strokeWidth / 2f;
        for (p in mDots) {
            canvas.drawCircle(p.x, p.y, r, mDotPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int,
                           heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                             MeasureSpec.getSize(heightMeasureSpec))
    }

    fun addDot(p: PointF) {
        mDots.add(p)
        postInvalidateDelayed(150)
    }

    fun removeDot(p: PointF) {
        mDots.remove(p)
        postInvalidateDelayed(150)
    }

    fun clearAllDots() {
       mDots.clear()
        postInvalidateDelayed(150)
    }
}
