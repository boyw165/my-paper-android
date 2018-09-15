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

import com.paper.model.sketch.VectorGraphics
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.*

open class SketchScrap(uuid: UUID = UUID.randomUUID(),
                       frame: Frame = Frame(),
                       private var svg: VectorGraphics)
    : Scrap(uuid = uuid,
            frame = frame) {

    private val updateSVGSignal = PublishSubject.create<VectorGraphics>().toSerialized()

    fun setSVG(other: VectorGraphics) {
        synchronized(lock) {
            svg = other

            // Signal out
            updateSVGSignal.onNext(svg)
        }
    }

    fun getSVG(): VectorGraphics {
        synchronized(lock) {
            return svg
        }
    }

    fun observeSVG(): Observable<VectorGraphics> {
        return updateSVGSignal
    }

    // Equality & Hash ////////////////////////////////////////////////////////

    override fun copy(): Scrap {
        return SketchScrap(uuid = UUID.randomUUID(),
                           frame = getFrame(),
                           svg = getSVG())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SketchScrap

        if (!super.equals(other)) return false

        val svg = getSVG()
        val otherSVGs = other.getSVG()

        if (svg.hashCode() != otherSVGs.hashCode()) return false

        return true
    }

    override fun hashCode(): Int {
        val isHashDirty = synchronized(lock) { isHashDirty }
        if (isHashDirty) {
            var hashCode = super.hashCode()
            // FIXME: There is a very short moment in between super.hashCode()
            // FIXME: and the following code such that isHashDirty is false
            hashCode = 31 * hashCode + getSVG().hashCode()

            synchronized(lock) {
                this.isHashDirty = false
                cacheHashCode = hashCode
            }
        }

        return cacheHashCode
    }
}
