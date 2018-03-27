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

package com.paper.shared.model

import com.paper.shared.model.sketch.SketchModel
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

open class ScrapModel(
    val uuid: UUID = UUID.randomUUID()) {

    var x: Float
        get() = mSetXSignal.value
        set(value) = mSetXSignal.onNext(value)
    var y: Float
        get() = mSetYSignal.value
        set(value) = mSetYSignal.onNext(value)
    private val mSetXSignal = BehaviorSubject.createDefault(0f)
    private val mSetYSignal = BehaviorSubject.createDefault(0f)
    /**
     *
     */
    fun onSetX(): Observable<Float> {
        return mSetXSignal
    }

    var scale: Float
        get() = mSetScaleSignal.value
        set(value) = mSetScaleSignal.onNext(value)
    var rotationInRadians: Float
        get() = mSetRotationSignal.value
        set(value) = mSetRotationSignal.onNext(value)
    private val mSetScaleSignal = BehaviorSubject.createDefault(1f)
    private val mSetRotationSignal = BehaviorSubject.createDefault(0f)

    var sketch: SketchModel
        get() = mSetSketchSignal.value
        set(value) = mSetSketchSignal.onNext(value)
    private val mSetSketchSignal = BehaviorSubject.createDefault(SketchModel())

    // TODO: Support image?
//    var image: Any
}
