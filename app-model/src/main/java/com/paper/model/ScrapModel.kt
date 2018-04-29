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

package com.paper.model

import com.paper.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

open class ScrapModel(
    val uuid: UUID = UUID.randomUUID()) {

    // X and y
    private val mSetXSignal = BehaviorSubject.createDefault(0f)
    private val mSetYSignal = BehaviorSubject.createDefault(0f)
    var x: Float
        get() = mSetXSignal.value!!
        set(value) = mSetXSignal.onNext(value)
    var y: Float
        get() = mSetYSignal.value!!
        set(value) = mSetYSignal.onNext(value)
    /**
     * Observe x update.
     */
    fun onSetX(): Observable<Float> {
        return mSetXSignal
    }
    /**
     * Observe y update.
     */
    fun onSetY(): Observable<Float> {
        return mSetYSignal
    }

    var z: Long
        get() = mSetZSignal.value!!
        set(value) = mSetZSignal.onNext(value)
    private val mSetZSignal = BehaviorSubject.createDefault(0L)
    /**
     * Observe z order update.
     */
    fun onSetZ(): Observable<Long> {
        return mSetZSignal
    }

    var scale: Float
        get() = mSetScaleSignal.value!!
        set(value) = mSetScaleSignal.onNext(value)
    var rotationInRadians: Float
        get() = mSetRotationSignal.value!!
        set(value) = mSetRotationSignal.onNext(value)
    private val mSetScaleSignal = BehaviorSubject.createDefault(1f)
    private val mSetRotationSignal = BehaviorSubject.createDefault(0f)

    // Sketch /////////////////////////////////////////////////////////////////

    /**
     * Sketch is a set of strokes.
     */
    private val mSketch = mutableListOf<SketchStroke>()
    val sketch: List<SketchStroke>
        get() = mSketch.toList()

    fun addStrokeToSketch(stroke: SketchStroke) {
        mSketch.add(stroke)
    }

    fun removeStrokeToSketch(stroke: SketchStroke) {
        mSketch.remove(stroke)
    }

    // Image //////////////////////////////////////////////////////////////////

    // TODO: Support image?
}
