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
class HermiteCubicSplineInterpolator(override val start: Point,
                                     override val startSlope: Double,
                                     override val end: Point,
                                     override val endSlope: Double)
    : ISplineInterpolator {

    private var mDx = end.x - start.x
    private var mCacheIn = Double.NaN
    private var mCacheOut = Double.NaN

    private val mFunAffine = HermiteAffineFunction(startX = start.x.toDouble(),
                                                   endX = end.x.toDouble())
    private val mFunH00 = Hermite00Function(mFunAffine)
    private val mFunH10 = Hermite10Function(mFunAffine)
    private val mFunH01 = Hermite01Function(mFunAffine)
    private val mFunH11 = Hermite11Function(mFunAffine)

    override fun f(x: Double): Double {
        if (mCacheIn != x) {
            mCacheOut = mFunH00.f(x) * start.y +
                mFunH10.f(x) * mDx * startSlope +
                mFunH01.f(x) * end.y +
                mFunH11.f(x) * mDx * endSlope
            mCacheIn = x
        }

        return mCacheOut
    }

    /**
     * The Hermite affine function, f(x) = (x - start) / (end - start).
     * See wiki page, https://en.wikipedia.org/wiki/Cubic_Hermite_spline, for
     * more details.
     */
    private class HermiteAffineFunction(val startX: Double,
                                        val endX: Double) : IMathFunctionOf {

        var cacheIn = Double.NaN
        var cacheOut = Double.NaN

        override fun f(x: Double): Double {
            if (cacheIn != x) {
                cacheOut = (x - startX) / (endX - startX)
                cacheIn = x
            }

            return cacheOut
        }
    }
}
