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

import com.paper.model.Rect

/**
 * The sketch model. A sketch contains stroke(s), [SketchStroke]. Each
 * stroke contains tuple(s), [PathTuple]. A tuple represents a node of
 * a path segment and contains at least one point, [Point]. These
 * points are endpoints or control-points for describing a bezier curve.
 */
data class SketchStroke(
    // The byte order is ARGB.
    var color: Int = 0,
    var width: Float = 0.toFloat(),
    var isEraser: Boolean = false) {

    private val mPathTupleList = mutableListOf<PathTuple>()

    val pathTupleList: List<PathTuple> get() = mPathTupleList

    private val mBound = Rect(java.lang.Float.MAX_VALUE,
                              java.lang.Float.MAX_VALUE,
                              java.lang.Float.MIN_VALUE,
                              java.lang.Float.MIN_VALUE)
    val bound get() = Rect(mBound.left, mBound.top, mBound.right, mBound.bottom)

    val firstPathTuple: PathTuple
        get() = mPathTupleList.first()

    val lastPathTuple: PathTuple
        get() = mPathTupleList.last()

    fun pathTupleSize(): Int = mPathTupleList.size

    fun clearAllPathTuple() {
        mPathTupleList.clear()
    }

    fun addPathTuple(pathTuple: PathTuple) {
        val point = pathTuple.getPointAt(0)

        // Calculate new boundary.
        calculateBound(point.x, point.y)

        mPathTupleList.add(pathTuple)
    }

    fun addAllPathTuple(pathTupleList: List<PathTuple>): SketchStroke {
        // Calculate new boundary.
        for (pathTuple in pathTupleList) {
            val point = pathTuple.getPointAt(0)
            calculateBound(point.x, point.y)
        }

        this.mPathTupleList.addAll(pathTupleList)

        return this
    }

    fun offset(offsetX: Float, offsetY: Float) {
        mPathTupleList.forEach { tuple ->
            tuple.allPoints.forEach { pt ->
                pt.offset(offsetX, offsetY)
            }
        }
    }

    override fun toString(): String {
        return "stroke{" +
               ", color=" + color +
               ", width=" + width +
               ", mPathTupleList=" + mPathTupleList +
               '}'
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun calculateBound(x: Float, y: Float) {
        mBound.left = Math.min(mBound.left, x)
        mBound.top = Math.min(mBound.top, y)
        mBound.right = Math.max(mBound.right, x)
        mBound.bottom = Math.max(mBound.bottom, y)
    }
}
