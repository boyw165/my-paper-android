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

open class Scrap(
    val uuid: UUID = UUID.randomUUID())
    : NoObfuscation {

    private var mIsHashDirty = true
    private var mHashCode = 0

    private val mSetXSignal = BehaviorSubject.createDefault(0f)
    private val mSetYSignal = BehaviorSubject.createDefault(0f)
    /**
     * The center x.
     */
    var x: Float
        get() = mSetXSignal.value!!
        set(value) {
            mSetXSignal.onNext(value)

            // Flag hash code dirty
            mIsHashDirty = true
        }
    /**
     * Observe x update.
     */
    fun onSetX(): Observable<Float> {
        return mSetXSignal
    }
    /**
     * The center y.
     */
    var y: Float
        get() = mSetYSignal.value!!
        set(value) {
            mSetYSignal.onNext(value)

            // Flag hash code dirty
            mIsHashDirty = true
        }
    /**
     * Observe y update.
     */
    fun onSetY(): Observable<Float> {
        return mSetYSignal
    }

    /**
     * The z-order, where the value should be greater than or equal to 0.
     * @see [ModelConst.INVALID_Z]
     */
    var z: Long
        get() = mSetZSignal.value!!
        set(value) {
            mSetZSignal.onNext(value)

            // Flag hash code dirty
            mIsHashDirty = true
        }
    private val mSetZSignal = BehaviorSubject.createDefault(ModelConst.INVALID_Z)
    /**
     * Observe z order update.
     */
    fun onSetZ(): Observable<Long> {
        return mSetZSignal
    }

    var scale: Float
        get() = mSetScaleSignal.value!!
        set(value) {
            mSetScaleSignal.onNext(value)

            // Flag hash code dirty
            mIsHashDirty = true
        }
    var rotationInRadians: Float
        get() = mSetRotationSignal.value!!
        set(value) {
            mSetRotationSignal.onNext(value)

            // Flag hash code dirty
            mIsHashDirty = true
        }
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

    // Equality & Hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Scrap

        if (uuid != other.uuid) return false
        if (mSetXSignal.value!! != other.mSetXSignal.value!!) return false
        if (mSetYSignal.value!! != other.mSetYSignal.value!!) return false
        if (mSetZSignal.value!! != other.mSetZSignal.value!!) return false
        if (mSetScaleSignal.value!! != other.mSetScaleSignal.value!!) return false
        if (mSetRotationSignal.value!! != other.mSetRotationSignal.value!!) return false
        if (mSketch != other.mSketch) return false

        return true
    }

    override fun hashCode(): Int {
        if (mIsHashDirty) {
            mHashCode = uuid.hashCode()
            mHashCode = 31 * mHashCode + mSetXSignal.value!!.hashCode()
            mHashCode = 31 * mHashCode + mSetYSignal.value!!.hashCode()
            mHashCode = 31 * mHashCode + mSetZSignal.value!!.hashCode()
            mHashCode = 31 * mHashCode + mSetScaleSignal.value!!.hashCode()
            mHashCode = 31 * mHashCode + mSetRotationSignal.value!!.hashCode()
            mHashCode = 31 * mHashCode + mSketch.hashCode()

            mIsHashDirty = false
        }

        return mHashCode
    }
}
