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

package com.paper.domain.interpolator

import com.paper.model.Point

/**
 * A linear function is a polynomial function in which the variable x has
 * degree at most one.
 *
 * See wiki page, https://en.wikipedia.org/wiki/Linear_function_(calculus)
 */
class LinearInterpolator(override val start: Point,
                         override val end: Point)
    : ISplineInterpolator {

    private var mCacheIn = Double.NaN
    private var mCacheOut = Double.NaN

    override fun f(x: Double): Double {
        if (mCacheIn != x) {
            //                             (y1 - y0)
            // f(x) = m * (x - x0) + y0 = ----------- * (x - x0) + y0
            //                             (x1 - x0)
            val m = (end.y - start.y) / (end.x - start.x)
            mCacheOut = m * (x - start.x) + start.y
            mCacheIn = x
        }

        return mCacheOut
    }
}
