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

import java.util.ArrayList

/**
 * A path tuple represents a path node. It may contains more than one x-y
 * pair in order to draw Bezier curve. A x-y pair is called TuplePoint.
 */
class PathTuple {

    private val mPoints = ArrayList<PointF>()

    constructor() : this(0f, 0f)
    constructor(x: Float,
                y: Float) {
        addPoint(x, y)
    }
    constructor(other: PathTuple) {
        mPoints.clear()
        other.allPoints.forEach { pt ->
            mPoints.add(PointF(pt.x, pt.y))
        }
    }

    fun addPoint(x: Float, y: Float) {
        mPoints.add(PointF(x, y))
    }

    // TODO: Copy and return.
    fun getPointAt(position: Int): PointF = mPoints[position]

    val firstPoint: PointF
        // TODO: Copy and return.
        get() = mPoints[0]

    val lastPoint: PointF
        get() = mPoints[pointSize - 1]

    val pointSize: Int
        get() = mPoints.size

    val allPoints: List<PointF>
        // TODO: Copy and return.
        get() = mPoints

    fun scale(scale: Float) {
        mPoints.forEach { pt ->
            pt.x = scale * pt.x
            pt.y = scale * pt.y
        }
    }

    fun translate(tx: Float, ty: Float) {
        mPoints.forEach { pt ->
            pt.x += tx
            pt.y += ty
        }
    }

    override fun toString(): String {
        return "PathTuple[" +
                mPoints +
                ']'
    }
}
