// Copyright Apr 2018-present boyw165@gmail.com
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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.paper.shared.model.Point

class SVGDrawable(oneDp: Float) {

    private val mOneDp = oneDp

    private val mPath = Path()
    private val mStrokePoint = mutableListOf<Point>()
    private val mStrokePaint = Paint()

    init {
        mStrokePaint.strokeWidth = mOneDp
        mStrokePaint.color = Color.BLACK
        mStrokePaint.style = Paint.Style.STROKE
    }

    fun moveTo(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
    }

    fun lineTo(x: Float, y: Float) {
        mPath.lineTo(x, y)
    }

    fun close() {
        // DO NOTHING
    }

    fun onDraw(canvas: Canvas) {
        canvas.drawPath(mPath, mStrokePaint)
    }

//    private fun calculateCurveControlPoints(s1: TimedPoint, s2: TimedPoint, s3: TimedPoint): ControlTimedPoints {
//        val dx1 = s1.x - s2.x
//        val dy1 = s1.y - s2.y
//        val dx2 = s2.x - s3.x
//        val dy2 = s2.y - s3.y
//
//        val m1X = (s1.x + s2.x) / 2.0f
//        val m1Y = (s1.y + s2.y) / 2.0f
//        val m2X = (s2.x + s3.x) / 2.0f
//        val m2Y = (s2.y + s3.y) / 2.0f
//
//        val l1 = Math.sqrt((dx1 * dx1 + dy1 * dy1).toDouble()).toFloat()
//        val l2 = Math.sqrt((dx2 * dx2 + dy2 * dy2).toDouble()).toFloat()
//
//        val dxm = m1X - m2X
//        val dym = m1Y - m2Y
//        var k = l2 / (l1 + l2)
//        if (java.lang.Float.isNaN(k)) k = 0.0f
//        val cmX = m2X + dxm * k
//        val cmY = m2Y + dym * k
//
//        val tx = s2.x - cmX
//        val ty = s2.y - cmY
//
//        return mControlTimedPointsCached.set(getNewPoint(m1X + tx, m1Y + ty), getNewPoint(m2X + tx, m2Y + ty))
//    }
}
