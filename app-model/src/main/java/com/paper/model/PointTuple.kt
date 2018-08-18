// Copyright Aug 2018-present Paper
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

package com.paper.model

/**
 * A path tuple is the unit in a cubic bezier representing the curve, which
 * is also immutable.
 */
sealed class PointTuple {

    /**
     * Return a new tuple with shifted [x] and [y].
     */
    abstract fun offset(x: Float,
                        y: Float): PointTuple
}

/**
 * Linear tuple.
 */
data class LinearPointTuple(val x: Float,
                            val y: Float)
    : PointTuple(),
      NoObfuscation {

    private var mIsHashDirty = true
    private var mHashCode = 0

    override fun offset(x: Float,
                        y: Float): PointTuple {
        return LinearPointTuple(this.x + x,
                                this.y + y)
    }

    // Equality ///////////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LinearPointTuple

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        if (mIsHashDirty) {
            mHashCode = x.hashCode()
            mHashCode = 31 * mHashCode + y.hashCode()

            mIsHashDirty = false
        }
        return mHashCode
    }

    override fun toString(): String {
        return "LinearPointTuple{%.3f, %.3f}"
            .format(x, y)
    }
}

/**
 * Cubic bezier tuple.
 */
data class CubicPointTuple(val prevControl: Point,
                           val currentControl: Point,
                           val currentEnd: Point)
    : PointTuple(),
      NoObfuscation {

    private var mIsHashDirty = true
    private var mHashCode = 0

    /**
     * Return a new tuple with shifted [x] and [y].
     */
    override fun offset(x: Float,
                        y: Float): PointTuple {
        return CubicPointTuple(prevControl = Point(prevControl.x + x,
                                                   prevControl.y + y),
                               currentControl = Point(currentControl.x + x,
                                                      currentControl.y + y),
                               currentEnd = Point(currentEnd.x,
                                                  currentEnd.y))
    }

    // Equality ///////////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CubicPointTuple

        if (prevControl != other.prevControl) return false
        if (currentControl != other.currentControl) return false
        if (currentEnd != other.currentEnd) return false

        return true
    }

    override fun hashCode(): Int {
        if (mIsHashDirty) {
            mHashCode = prevControl.hashCode()
            mHashCode = 31 * mHashCode + currentControl.hashCode()
            mHashCode = 31 * mHashCode + currentEnd.hashCode()

            mIsHashDirty = false
        }
        return mHashCode
    }

    override fun toString(): String {
        return "CubicPointTuple{c=(%.3f, %.3f) c=(%.3f, %.3f) e=(%.3f, %.3f)}"
            .format(prevControl.x, prevControl.y,
                    currentControl.x, currentControl.y,
                    currentEnd.x, currentEnd.y)
    }
}
