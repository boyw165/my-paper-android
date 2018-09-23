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
import io.useful.delegate.rx.RxValue
import java.util.*

open class SketchScrap(uuid: UUID = UUID.randomUUID(),
                       frame: Frame = Frame(),
                       svg: VectorGraphics)
    : Scrap(id = uuid,
            frame = frame) {

    var svg: VectorGraphics by RxValue(svg)

    // Equality & Hash ////////////////////////////////////////////////////////

    override fun copy(): Scrap {
        return SketchScrap(uuid = UUID.randomUUID(),
                           frame = frame,
                           svg = svg)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SketchScrap

        if (!super.equals(other)) return false

        val svg = this.svg
        val otherSVGs = other.svg

        if (svg.hashCode() != otherSVGs.hashCode()) return false

        return true
    }

    override fun hashCode(): Int {
        val isHashDirty = this.isHashDirty
        if (isHashDirty) {
            var hashCode = super.hashCode()
            // FIXME: There is a very short moment in between super.hashCode()
            // FIXME: and the following code such that isHashDirty is false
            hashCode = 31 * hashCode + svg.hashCode()

            this.isHashDirty = false
            this.cacheHashCode = hashCode
        }

        return this.cacheHashCode
    }
}
