// Copyright Apr 2018-present djken0106@gmail.com
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

package com.paper.domain.data

import com.paper.model.Point

class Bezier {
    lateinit  var startPoint: Point
    lateinit var control1: Point
    lateinit var control2: Point
    lateinit var endPoint: Point

    operator fun set(startPoint: Point,
                     control1: Point,
                     control2: Point,
                     endPoint: Point): Bezier {
        this.startPoint = startPoint
        this.control1 = control1
        this.control2 = control2
        this.endPoint = endPoint
        return this
    }

    fun length(): Float {
        val steps = 10
        var length = 0f
        var cx: Double
        var cy: Double
        var px = 0.0
        var py = 0.0
        var xDiff: Double
        var yDiff: Double

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            cx = point(t, this.startPoint.x, this.control1.x,
                    this.control2.x, this.endPoint.x)
            cy = point(t, this.startPoint.y, this.control1.y,
                    this.control2.y, this.endPoint.y)
            if (i > 0) {
                xDiff = cx - px
                yDiff = cy - py
                length += Math.sqrt(xDiff * xDiff + yDiff * yDiff).toFloat()
            }
            px = cx
            py = cy
        }
        return length

    }

    fun point(t: Float, start: Float, c1: Float, c2: Float, end: Float): Double {
        return (start.toDouble() * (1.0 - t) * (1.0 - t) * (1.0 - t)
                + 3.0 * c1.toDouble() * (1.0 - t) * (1.0 - t) * t.toDouble()
                + 3.0 * c2.toDouble() * (1.0 - t) * t.toDouble() * t.toDouble()
                + (end * t * t * t).toDouble())
    }

}
