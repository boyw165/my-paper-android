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

package com.paper

import android.graphics.Matrix
import android.graphics.PointF

/**
 * A convenient class for getting tx, ty, sx, sy and rotation given a
 * [Matrix].
 * <br></br>
 * Usage:
 * <pre>
 * // Usage 1:
 * final TransformUtils util = new TransformUtils(matrix);
 * util.getX();
 *
 * // Usage 2:
 * TransformUtils.getX(matrix);
 * </pre>
 */
class TransformUtils {

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

    ///////////////////////////////////////////////////////////////////////////
    // Public Static Methods //////////////////////////////////////////////////

    companion object {

        fun getTranslationX(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TransformUtils(matrix).translationX
            }
        }

        fun getTranslationY(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TransformUtils(matrix).translationY
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
                TransformUtils(matrix).scaleX
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
                TransformUtils(matrix).scaleY
            }
        }

        fun getRotationInRadians(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TransformUtils(matrix).rotationInRadians
            }
        }

        fun getRotationInDegrees(matrix: Matrix?): Float {
            return if (matrix == null) {
                0f
            } else {
                TransformUtils(matrix).rotationInDegrees
            }
        }

        ///////////////////////////////////////////////////////////////////////

        val DELTA_X = 0
        val DELTA_Y = 1
        val DELTA_SCALE_X = 2
        val DELTA_SCALE_Y = 3
        val DELTA_RADIANS = 4
        val PIVOT_X = 5
        val PIVOT_Y = 6

        /**
         * Get an array of [tx, ty, sx, sy, rotation] sequence representing
         * the transformation from the given event.
         */
        fun getTransformFromPointers(startPointers: Array<PointF>,
                                     stopPointers: Array<PointF>): FloatArray {
            if (startPointers.size < 2 || stopPointers.size < 2) {
                throw IllegalStateException(
                    "The event must have at least two down pointers.")
            }

            val transform = floatArrayOf(
                // delta x
                0f,
                // delta y
                0f,
                // delta scale x
                1f,
                // delta scale y
                1f,
                // delta rotation in radians
                0f,
                // pivot x.
                0f,
                // pivot y.
                0f)

            // Start pointer 1.
            val startX1 = startPointers[0].x
            val startY1 = startPointers[0].y
            // Start pointer 2.
            val startX2 = startPointers[1].x
            val startY2 = startPointers[1].y

            // Stop pointer 1.
            val stopX1 = stopPointers[0].x
            val stopY1 = stopPointers[0].y
            // Stop pointer 2.
            val stopX2 = stopPointers[1].x
            val stopY2 = stopPointers[1].y

            // Start vector.
            val startVecX = startX2 - startX1
            val startVecY = startY2 - startY1
            // Stop vector.
            val stopVecX = stopX2 - stopX1
            val stopVecY = stopY2 - stopY1

            // Start pivot.
            val startPivotX = (startX1 + startX2) / 2f
            val startPivotY = (startY1 + startY2) / 2f
            // Stop pivot.
            val stopPivotX = (stopX1 + stopX2) / 2f
            val stopPivotY = (stopY1 + stopY2) / 2f

            // Calculate the translation.
            transform[DELTA_X] = stopPivotX - startPivotX
            transform[DELTA_Y] = stopPivotY - startPivotY
            // Calculate the rotation degree.
            transform[DELTA_RADIANS] = if (startVecX == 0f && startVecY == 0f &&
                                                                              stopVecX == 0f && stopVecY == 0f) {
                0f
            } else {
                (Math.atan2(stopVecY.toDouble(), stopVecX.toDouble()) -
                 Math.atan2(startVecY.toDouble(), startVecX.toDouble())).toFloat()
            }
            // Calculate the scale change.
            val dScale = if (startVecX == 0f && startVecY == 0f &&
                             stopVecX == 0f && stopVecY == 0f) {
                1f
            } else {
                (Math.hypot(stopVecX.toDouble(),
                            stopVecY.toDouble()) /
                 Math.hypot(startVecX.toDouble(),
                            startVecY.toDouble())).toFloat()
            }

            transform[DELTA_SCALE_X] = dScale
            transform[DELTA_SCALE_Y] = dScale
            transform[PIVOT_X] = startPivotX
            transform[PIVOT_Y] = startPivotY

            return transform
        }

        ///////////////////////////////////////////////////////////////////////

        const val ORIENTATION_CW = -1
        const val ORIENTATION_COLLINEAR = 0
        const val ORIENTATION_CCW = 1

        fun getOrientation(p1: PointF, p2: PointF, p3: PointF): Int {
            // Vector u: from p1 to p2.
            val ux = p2.x - p1.x
            val uy = p2.y - p1.y

            // Vector v: from p1 to p3.
            val vx = p3.x - p1.x
            val vy = p3.y - p1.y

            //      [ ux vx]
            // det( [ uy vy] ) = ux * vy - vx * uy
            val det = ux * vy - vx * uy
            return when {
                det > 0 -> ORIENTATION_CCW
                det < 0 -> ORIENTATION_CW
                else -> ORIENTATION_COLLINEAR
            }
        }
    }
}
