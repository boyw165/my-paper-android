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
 * A linear function is a polynomial function in which the variable x has
 * degree at most one.
 *
 * See wiki page, https://en.wikipedia.org/wiki/Linear_function_(calculus)
 */
class LinearInterpolator(private val start: Point,
                         private val end: Point)
    : ISplineInterpolator {

    /**
     * f(x) with linear interpolation.
     *
     * @param t [0.0 .. 1.0]
     */
    override fun f(t: Double): Point {
        if (t < 0.0 || t > 1.0) {
            throw IllegalArgumentException("Given t is out of boundary")
        }

        return Point(x = ((1.0 - t) * start.x + t * end.x).toFloat(),
                     y = ((1.0 - t) * start.y + t * end.y).toFloat(),
                     time = 0)
    }

    override fun constructPath(path: Path) {
        path.reset()
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)
    }
}
