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
 * In numerical analysis, a cubic Hermite spline or cubic Hermite interpolator
 * is a spline where each piece is a third-degree polynomial specified in
 * Hermite form; that is, by its values and first derivatives at the end
 * points of the corresponding domain interval.
 *
 * See wiki page, https://en.wikipedia.org/wiki/Cubic_Hermite_spline;
 * or try this interactive website, http://demofox.org/cubichermite2d.html.
 */
class HermiteCubicSplineInterpolator(private val start: Point,
                                     private val startSlope: Point,
                                     private val end: Point,
                                     private val endSlope: Point)
    : ISplineInterpolator {

    /**
     * f(x) with Hermite cubic interpolation.
     *
     * @param t [0.0 .. 1.0]
     */
    override fun f(t: Double): Point {
        if (t < 0.0 || t > 1.0) {
            throw IllegalArgumentException("Given t is out of boundary")
        }

        val h00 = funcHermite00(t)
        val h10 = funcHermite10(t)
        val h01 = funcHermite01(t)
        val h11 = funcHermite11(t)

        return Point(x = (h00 * start.x + h10 * startSlope.x + h01 * end.x + h11 * endSlope.x).toFloat(),
                     y = (h00 * start.y + h10 * startSlope.y + h01 * end.y + h11 * endSlope.y).toFloat(),
                     time = 0)
    }

    /**
     * The first of the four Hermite basis functions, which is
     * f(x) = (1 + 2t)(1 - t)^2. Check wiki page,
     * https://en.wikipedia.org/wiki/Cubic_Hermite_spline, for more details.
     */
    private fun funcHermite00(t: Double): Double {
        return (1.0 + 2.0 * t) * Math.pow(1.0 - t, 2.0)
    }

    /**
     * The third of the four Hermite basis functions, which is
     * f(x) = t^2(3 - 2t)^2. Check wiki page,
     * https://en.wikipedia.org/wiki/Cubic_Hermite_spline, for more details.
     */
    private fun funcHermite01(t: Double): Double {
        return Math.pow(t, 2.0) * (3.0 - 2.0 * t)
    }

    /**
     * The second of the four Hermite basis functions, which is
     * f(x) = t(1 - t)^2. Check wiki page,
     * https://en.wikipedia.org/wiki/Cubic_Hermite_spline, for more details.
     */
    private fun funcHermite10(t: Double): Double {
        return t * Math.pow(1.0 - t, 2.0)
    }

    /**
     * The fourth of the four Hermite basis functions, which is
     * f(x) = t^2(t - 1). Check wiki page,
     * https://en.wikipedia.org/wiki/Cubic_Hermite_spline, for more details.
     */
    private fun funcHermite11(t: Double): Double {
        return Math.pow(t, 2.0) * (t - 1.0)
    }
}
