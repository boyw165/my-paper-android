// Copyright (c) 2017-present CardinalBlue
//
// Author: boy@cardinalblue.com
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.paper.editor

import android.graphics.Matrix

/**
 * A convenient class for getting tx, ty, sx, sy and rotation given a
 * [Matrix].
 * <br></br>
 * Usage:
 * <pre>
 * // Usage 1:
 * final TwoDTransformUtils util = new TwoDTransformUtils(matrix);
 * util.getTranslationX();
 *
 * // Usage 2:
 * TwoDTransformUtils.getTranslationX(matrix);
 * </pre>
 */
class TwoDTransformUtils {

    // Given...
    private val mValues: FloatArray

    val translationX: Float
        get() = mValues[Matrix.MTRANS_X]

    val translationY: Float
        get() = mValues[Matrix.MTRANS_Y]

    // TODO: Has to take the negative scale into account.
    // [a, b, tx]   [ sx*cos  -sy*sin  ? ]
    // [c, d, ty] = [ sx*sin   sy*cos  ? ]
    // [0, 0,  1]   [    0        0    1 ]
    //  ^  ^   ^
    //  i  j   k hat (axis vector)
    val scaleX: Float
        get() {
            val a = mValues[Matrix.MSCALE_X]
            val b = mValues[Matrix.MSKEW_X]

            return Math.hypot(a.toDouble(), b.toDouble()).toFloat()
        }

    // TODO: Has to take the negative scale into account.
    // [a, b, tx]   [ sx*cos  -sy*sin  ? ]
    // [c, d, ty] = [ sy*sin   sy*cos  ? ]
    // [0, 0,  1]   [    0        0    1 ]
    //  ^  ^   ^
    //  i  j   k hat (axis vector)
    val scaleY: Float
        get() {
            val c = mValues[Matrix.MSKEW_Y]
            val d = mValues[Matrix.MSCALE_Y]

            return Math.hypot(c.toDouble(), d.toDouble()).toFloat()
        }

    // TODO: Has to take the negative scale into account.
    // [a, b, tx]   [ sx*cos  -sy*sin  ? ]
    // [c, d, ty] = [ sx*sin   sy*cos  ? ]
    // [0, 0,  1]   [    0        0    1 ]
    //  ^  ^   ^
    //  i  j   k hat (axis vector)
    // From -pi to +pi.
    val rotationInRadians: Float
        get() {
            val a = mValues[Matrix.MSCALE_X]
            val c = mValues[Matrix.MSKEW_Y]
            return Math.atan2(c.toDouble(), a.toDouble()).toFloat()
        }

    val rotationInDegrees: Float
        get() = Math.toDegrees(rotationInRadians.toDouble()).toFloat()

    constructor() {
        mValues = FloatArray(9)
    }

    constructor(matrix: Matrix) {
        mValues = FloatArray(9)

        // Get the values from the matrix.
        matrix.getValues(mValues)
    }

    fun getValues(matrix: Matrix) {
        matrix.getValues(mValues)
    }

    companion object {

        ///////////////////////////////////////////////////////////////////////////
        // Public Static Methods //////////////////////////////////////////////////

        fun getTranslationX(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TwoDTransformUtils(matrix).translationX
            }
        }

        fun getTranslationY(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TwoDTransformUtils(matrix).translationY
            }
        }

        /**
         * Get the scaleX from an affine transform matrix.
         *
         * @param matrix The affine transform matrix.
         */
        fun getScaleX(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TwoDTransformUtils(matrix).scaleX
            }
        }

        /**
         * Get the scaleY from an affine transform matrix.
         *
         * @param matrix The affine transform matrix.
         */
        fun getScaleY(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TwoDTransformUtils(matrix).scaleY
            }
        }

        fun getRotationInRadians(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TwoDTransformUtils(matrix).rotationInRadians
            }
        }

        fun getRotationInDegrees(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TwoDTransformUtils(matrix).rotationInDegrees
            }
        }
    }
}
