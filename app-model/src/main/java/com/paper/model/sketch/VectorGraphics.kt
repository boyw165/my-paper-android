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

import com.paper.model.*
import java.util.*

/**
 * The sketch model. A sketch contains stroke(s), [VectorGraphics]. Each
 * stroke contains tuple(s), [PointTuple]. A tuple represents a node of
 * a path segment and contains at least one point, [Point]. These
 * points are endpoints or control-points for describing a bezier curve.
 */
data class VectorGraphics(
    val id: UUID = UUID.randomUUID(),
    val style: Set<SVGStyle> = setOf(SVGStyle.Stroke(color = Color.RED,
                                                     size = 0.1f,
                                                     closed = false)),
    private val tupleList: MutableList<PointTuple> = mutableListOf()) {

    private var mIsHashDirty = true
    private var mHashCode = 0

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
     * A stroke essentially is a list of points.
     */
    fun getTupleList(): List<PointTuple> {
        return tupleList.toList()
    }

    fun getTupleAt(position: Int): PointTuple {
        val src = tupleList[position]

        return when (src) {
            is LinearPointTuple -> src.copy()
            is CubicPointTuple -> src.copy()
        }
    }

    fun addTuple(p: PointTuple): VectorGraphics {
        // Calculate new boundary.
        calculateBound(p)

        tupleList.add(p)

        // Flag hash code dirty
        mIsHashDirty = true

        return this
    }

    fun setTupleList(PointList: List<PointTuple>): VectorGraphics {
        tupleList.clear()
        tupleList.addAll(PointList)

        // Calculate new boundary.
        tupleList.forEach { calculateBound(it) }

        // Flag hash code dirty
        mIsHashDirty = true

        return this
    }

    fun offset(offsetX: Float,
               offsetY: Float) {
        val src = tupleList.toList()

        tupleList.clear()
        src.forEach {
            tupleList.add(it.offset(offsetX, offsetY))
            calculateBound(it)
        }

        // Flag hash code dirty
        mIsHashDirty = true
    }

    private fun calculateBound(p: PointTuple) {
        // TODO: Bezier curve has different way to calculate boundary
//        mBound.left = Math.min(mBound.left, p.currentEnd.x)
//        mBound.top = Math.min(mBound.top, p.currentEnd.y)
//        mBound.right = Math.max(mBound.right, p.currentEnd.x)
//        mBound.bottom = Math.max(mBound.bottom, p.currentEnd.y)
    }

    // Equality & Hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VectorGraphics

        if (style != other.style) return false
        if (tupleList != other.tupleList) return false

        return true
    }

    override fun hashCode(): Int {
        if (mIsHashDirty) {
            mHashCode = tupleList.hashCode()
            mHashCode = 31 * mHashCode + style.hashCode()

            mIsHashDirty = false
        }

        return mHashCode
    }

    override fun toString(): String {
        return "stroke{" +
               ", style=" + style +
               ", getTupleList=" + tupleList +
               '}'
    }
}
