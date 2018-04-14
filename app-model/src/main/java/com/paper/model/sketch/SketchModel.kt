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
class SketchModel {

    private val mMutex = Any()

    private val mStrokes: MutableList<SketchStroke> = ArrayList()
    private var mStrokesBoundDirty = true
    private var mStrokesBound = Rect()

    constructor() : this(emptyList())
    constructor(other: SketchModel) : this(other.allStrokes) {
//        mStrokesBound = RectF(other.strokesBoundaryWithinCanvas)
        mStrokesBoundDirty = false
    }
    constructor(strokes: List<SketchStroke> = emptyList()) {
        if (!strokes.isEmpty()) {
            mStrokes.addAll(strokes)
        }
    }

    val strokeSize: Int
        get() {
            synchronized(mMutex) {
                return mStrokes.size
            }
        }

    fun getStrokeAt(position: Int): SketchStroke {
        synchronized(mMutex) {
            return mStrokes[position]
        }
    }

    fun addStroke(stroke: SketchStroke) {
        synchronized(mMutex) {
            mStrokes.add(stroke)
            mStrokesBoundDirty = true
        }
    }

    fun addAllStroke(stroke: List<SketchStroke>) {
        synchronized(mMutex) {
            mStrokes.addAll(stroke)
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
                // TODO: Recursively copy
                return ArrayList(mStrokes)
            }
        }

    fun clearStrokes() {
        synchronized(mMutex) {
            mStrokes.clear()
            mStrokesBoundDirty = true
        }
    }

    fun clone(): SketchModel {
        return SketchModel(this)
    }

    override fun toString(): String {
        return "SketchModel{" +
               ", strokes=[" + mStrokes + "]" +
               '}'
    }
}
