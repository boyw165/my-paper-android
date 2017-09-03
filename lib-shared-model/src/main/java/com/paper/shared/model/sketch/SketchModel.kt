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

import android.graphics.RectF
import kotlin.collections.ArrayList

/**
 * A sketch scrap could contains multiple strokes. Every stroke has an array of
 * tuple. PathTuple is the data describing the node in a path segment.
 * <br></br>
 * A tuple could contains multiple x-y pairs. The design is for drawing either
 * straight line or Bezier curve.
 * <br></br>
 * If it's a single element tuple, the line is straight.
 * <br></br>
 * If it's a two elements tuple, the line is a Bezier curve.
 * <br></br>
 * If it's a three elements tuple, the line is a Bezier curve with smoother
 * visualization.

 * <pre>
 * A sketch stroke of a sketch scrap.
 * (+) is the tuple.
 * (-) is the straight/bezier line connects two tuple.
 * .-------------------.
 * |                   |
 * | +-+         +--+  |
 * |    \        |     |
 * |    +-+    +-+     |
 * |      |   /        |
 * |      +--+         |
 * |                   |
 * '-------------------'
 * </pre>
 */
class SketchModel constructor(id: Long) {

    private val mMutex = Any()

    val id: Long = id
    var width: Int = 0
    var height: Int = 0
    val mStrokes: MutableList<SketchStroke> = ArrayList()
    var mStrokesBoundDirty = true
    var mStrokesBound = RectF()

    constructor()
        : this(0, 0, 0, emptyList())

    constructor(width: Int,
                height: Int)
        : this(0, width, height, emptyList())

    constructor(id: Long,
                width: Int,
                height: Int,
                strokes: List<SketchStroke> = emptyList())
        : this(id) {
        this.width = width
        this.height = height

        if (!strokes.isEmpty()) {
            mStrokes.addAll(strokes)
        }
    }

    constructor(other: SketchModel)
        : this(other.id,
               other.width,
               other.height,
               other.allStrokes) {
        mStrokesBound = RectF(other.strokesBoundaryWithinCanvas)
        mStrokesBoundDirty = false
    }

    fun setSize(width: Int, height: Int) {
        synchronized(mMutex) {
            this.width = width
            this.height = height

            mStrokesBoundDirty = true
        }
    }

    val strokeSize: Int
        get() {
            synchronized(mMutex) {
                return mStrokes!!.size
            }
        }

    fun getStrokeAt(position: Int): SketchStroke {
        synchronized(mMutex) {
            return mStrokes!![position]
        }
    }

    fun addStroke(stroke: SketchStroke) {
        synchronized(mMutex) {
            mStrokes!!.add(stroke)
            mStrokesBoundDirty = true
        }
    }

    val firstStroke: SketchStroke
        get() {
            synchronized(mMutex) {
                return mStrokes[0]
            }
        }

    val lastStroke: SketchStroke
        get() {
            synchronized(mMutex) {
                return mStrokes[mStrokes.size - 1]
            }
        }

    val allStrokes: List<SketchStroke>
        get() {
            synchronized(mMutex) {
                return ArrayList(mStrokes)
            }
        }

    fun clearStrokes() {
        synchronized(mMutex) {
            mStrokes!!.clear()
        }
    }

    // Also consider the stroke width.
    // Constraint the boundary inside the canvas.
    val strokesBoundaryWithinCanvas: RectF
        get() {
            if (mStrokesBoundDirty) {
                synchronized(mMutex) {
                    mStrokesBound.set(
                        java.lang.Float.MAX_VALUE,
                        java.lang.Float.MAX_VALUE,
                        java.lang.Float.MIN_VALUE,
                        java.lang.Float.MIN_VALUE
                                     )

                    val aspectRatio = width.toFloat() / height
                    for (stroke in mStrokes!!) {
                        val strokeBound = stroke.bound
                        val halfStrokeWidth = stroke.getWidth() / 2

                        mStrokesBound.left = Math.min(mStrokesBound.left, strokeBound.left - halfStrokeWidth)
                        mStrokesBound.top = Math.min(mStrokesBound.top, strokeBound.top - halfStrokeWidth * aspectRatio)
                        mStrokesBound.right = Math.max(mStrokesBound.right, strokeBound.right + halfStrokeWidth)
                        mStrokesBound.bottom = Math.max(mStrokesBound.bottom, strokeBound.bottom + halfStrokeWidth * aspectRatio)
                    }

                    // Clamp the bound within a 1x1 square.
                    mStrokesBound.left = Math.max(mStrokesBound.left, 0f)
                    mStrokesBound.top = Math.max(mStrokesBound.top, 0f)
                    mStrokesBound.right = Math.min(mStrokesBound.right, 1f)
                    mStrokesBound.bottom = Math.min(mStrokesBound.bottom, 1f)

                    mStrokesBoundDirty = false
                }
            }

            return mStrokesBound
        }

    fun setStrokes(strokes: List<SketchStroke>?) {
        synchronized(mMutex) {
            mStrokes!!.clear()

            if (strokes != null) {
                mStrokes!!.addAll(strokes)
            }

            mStrokesBoundDirty = true
        }
    }

    fun clone(): SketchModel {
        return SketchModel(this)
    }

    override fun toString(): String {
        return "SketchModel{" +
               ", width=" + width +
               ", height=" + height +
               ", strokes=[" + mStrokes + "]" +
               '}'
    }
}
