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

package com.paper.shared.model.sketch

import android.graphics.PointF
import android.graphics.RectF

import java.util.ArrayList

/**
 * The sketch model. A sketch contains stroke(s), [SketchStrokeModel]. Each
 * stroke contains tuple(s), [PathTuple]. A tuple represents a node of
 * a path segment and contains at least one point, [PointF]. These
 * points are endpoints or control-points for describing a bezier curve.
 */
class SketchStrokeModel {

    // State.
    // The byte order is ARGB.
    private var mColor: Int = 0
    private var mWidth: Float = 0.toFloat()
    var isEraser: Boolean = false
        get() = field
        set(value) { field = value }
    private val mPathTupleList = ArrayList<PathTuple>()
    val bound = RectF(java.lang.Float.MAX_VALUE,
            java.lang.Float.MAX_VALUE,
            java.lang.Float.MIN_VALUE,
            java.lang.Float.MIN_VALUE)

    fun setWidth(width: Float): SketchStrokeModel {
        mWidth = width
        return this
    }

    fun getWidth(): Float = mWidth

    /**
     * Set color, the format is the same with [android.graphics.Color].
     */
    fun setColor(color: Int): SketchStrokeModel {
        mColor = color
        return this
    }

    fun getColor(): Int = mColor

    fun savePathTuple(tuple: PathTuple?): Boolean {
        if (tuple == null || tuple.pointSize == 0) return false

        // Calculate the boundary by the last point of the given tuple.
        val point = tuple.getPointAt(tuple.pointSize - 1)
        calculateBound(point.x, point.y)

        return mPathTupleList.add(tuple)
    }

    fun getPathTupleAt(position: Int): PathTuple = mPathTupleList[position]

    val firstPathTuple: PathTuple
        get() = mPathTupleList[0]

    val lastPathTuple: PathTuple
        get() = mPathTupleList[mPathTupleList.size - 1]

    fun pathTupleSize(): Int = mPathTupleList.size

    fun add(pathTuple: PathTuple) {
        val point = pathTuple.getPointAt(0)

        // Calculate new boundary.
        calculateBound(point.x, point.y)

        mPathTupleList.add(pathTuple)
    }

    fun addAll(pathTupleList: List<PathTuple>) {
        // Calculate new boundary.
        for (pathTuple in pathTupleList) {
            val point = pathTuple.getPointAt(0)
            calculateBound(point.x, point.y)
        }

        mPathTupleList.addAll(pathTupleList)
    }

    val allPathTuple: List<PathTuple>
        get() = mPathTupleList

    override fun toString(): String {
        return "stroke{" +
                ", color=" + mColor +
                ", width=" + mWidth +
                ", pathTupleList=" + mPathTupleList +
                '}'
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun calculateBound(x: Float, y: Float) {
        bound.left = Math.min(bound.left, x)
        bound.top = Math.min(bound.top, y)
        bound.right = Math.max(bound.right, x)
        bound.bottom = Math.max(bound.bottom, y)
    }
}
