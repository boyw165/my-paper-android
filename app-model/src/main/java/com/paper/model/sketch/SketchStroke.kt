// Copyright (c) 2017-present boyw165
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

package com.paper.model.sketch

import com.paper.model.ModelConst
import com.paper.model.Point
import com.paper.model.Rect

/**
 * The sketch model. A sketch contains stroke(s), [SketchStroke]. Each
 * stroke contains tuple(s), [Point]. A tuple represents a node of
 * a path segment and contains at least one point, [Point]. These
 * points are endpoints or control-points for describing a bezier curve.
 */
data class SketchStroke(
    // The byte order is ARGB.
    val penColor: Int = 0,
    val penSize: Float = 0.toFloat(),
    val penType: PenType = PenType.PEN) {

    private var mIsHashDirty = true
    private var mHashCode = 0

    /**
     * A stroke essentially is a list of points.
     */
    private val mPointList = mutableListOf<Point>()
    /**
     * A stroke essentially is a list of points.
     */
    val pointList: List<Point> get() = mPointList.toList()

    /**
     * The upright rectangle just covering all the points.
     */
    private val mBound = Rect(java.lang.Float.MAX_VALUE,
                              java.lang.Float.MAX_VALUE,
                              java.lang.Float.MIN_VALUE,
                              java.lang.Float.MIN_VALUE)
    /**
     * The upright rectangle just covering all the points.
     */
    val bound get() = Rect(mBound.left, mBound.top, mBound.right, mBound.bottom)

    /**
     * The z-order, where the value should be greater than or equal to 0.
     * @see [ModelConst.INVALID_Z]
     */
    var z = ModelConst.INVALID_Z
        set(value) {
            field = value

            // Flag hash code dirty
            mIsHashDirty = true
        }

    fun addPath(p: Point): SketchStroke {
        // Calculate new boundary.
        calculateBound(p.x, p.y)

        mPointList.add(p)

        // Flag hash code dirty
        mIsHashDirty = true

        return this
    }

    fun addAllPath(PointList: List<Point>): SketchStroke {
        // Calculate new boundary.
        for (p in PointList) {
            calculateBound(p.x, p.y)
        }

        this.mPointList.addAll(PointList)

        // Flag hash code dirty
        mIsHashDirty = true

        return this
    }

    fun offset(offsetX: Float, offsetY: Float) {
        mPointList.forEach { p ->
            p.x += offsetX
            p.y += offsetY

            calculateBound(p.x, p.y)
        }

        // Flag hash code dirty
        mIsHashDirty = true
    }

    private fun calculateBound(x: Float, y: Float) {
        mBound.left = Math.min(mBound.left, x)
        mBound.top = Math.min(mBound.top, y)
        mBound.right = Math.max(mBound.right, x)
        mBound.bottom = Math.max(mBound.bottom, y)
    }

    // Equality & Hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SketchStroke

        if (penColor != other.penColor) return false
        if (penSize != other.penSize) return false
        if (penType != other.penType) return false
        if (mPointList != other.mPointList) return false

        return true
    }

    override fun hashCode(): Int {
        if (mIsHashDirty) {
            mHashCode = z.hashCode()
            mHashCode = 31 * mHashCode + penColor.hashCode()
            mHashCode = 31 * mHashCode + penSize.hashCode()
            mHashCode = 31 * mHashCode + penType.hashCode()
            mHashCode = 31 * mHashCode + mPointList.hashCode()

            mIsHashDirty = false
        }

        return mHashCode
    }

    override fun toString(): String {
        return "stroke{" +
               "  z=" + z +
               ", penColor=" + penColor +
               ", penSize=" + penSize +
               ", pointList=" + mPointList +
               '}'
    }
}
