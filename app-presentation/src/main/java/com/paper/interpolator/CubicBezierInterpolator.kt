// Copyright Jun 2018-present Paper
//
// Author: boyw165@gmail.com
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

package com.paper.interpolator

import android.graphics.Path
import com.paper.model.Point

/**
 * A cubic bezier function is a polynomial function in which the variable x has
 * degree at most one.
 *
 * See wiki page, https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Cubic_B%C3%A9zier_curves
 */
class CubicBezierInterpolator(val start: Point,
                              val startControl: Point,
                              val end: Point,
                              val endControl: Point)
    : ISplineInterpolator {

    /**
     * f(x) with cubic bezier interpolation.
     *
     * @param t [0.0 .. 1.0]
     */
    override fun f(t: Double): Point {
        if (t < 0.0 || t > 1.0) {
            throw IllegalArgumentException("Given t is out of boundary")
        }

        // Four basis functions:
        // b1 = (1 - t)^3
        // b2 = 3t(1 - t)^2
        // b3 = 3(1 - t)t^2
        // b4 = t^3
        val oneComplement = 1.0 - t
        val b1 = Math.pow(oneComplement, 3.0)
        val b2 = 3.0 * t * Math.pow(oneComplement, 2.0)
        val b3 = 3.0 * oneComplement * Math.pow(t, 2.0)
        val b4 = Math.pow(t, 3.0)

        // The formula is: b1*P0 + b2*P1 + b3*P2 + b4*P3
        return Point(x = (b1 * start.x + b2 * startControl.x + b3 * endControl.x + b4 * end.x).toFloat(),
                     y = (b1 * start.y + b2 * startControl.y + b3 * endControl.y + b4 * end.y).toFloat(),
                     time = 0)
    }

    override fun constructPath(path: Path) {
        path.reset()
        path.moveTo(start.x, start.y)
        path.cubicTo(startControl.x, startControl.y,
                     endControl.x, endControl.y,
                     end.x, end.y)
    }
}
