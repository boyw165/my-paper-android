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

import io.useful.delegate.rx.RxValue
import java.util.*

open class Scrap(val id: UUID = UUID.randomUUID(),
                 frame: Frame = Frame())
    : NoObfuscation {

    var frame: Frame by RxValue(frame)

    protected var isHashDirty = true
    protected var cacheHashCode = 0

    // Equality & Hash ////////////////////////////////////////////////////////

    open fun copy(): Scrap {
        return Scrap(id = UUID.randomUUID(),
                     frame = frame.copy())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Scrap

        val frame = frame
        val otherFrame = other.frame

        if (id != other.id) return false
        if (frame.x != otherFrame.x) return false
        if (frame.y != otherFrame.y) return false
        if (frame.z != otherFrame.z) return false
        if (frame.scaleX != otherFrame.scaleX) return false
        if (frame.scaleY != otherFrame.scaleY) return false
        if (frame.rotationInDegrees != otherFrame.rotationInDegrees) return false

        return true
    }

    override fun hashCode(): Int {
        val isHashDirty = this.isHashDirty
        if (isHashDirty) {
            val frame = frame
            var hashCode = id.hashCode()
            hashCode = 31 * hashCode + frame.x.hashCode()
            hashCode = 31 * hashCode + frame.y.hashCode()
            hashCode = 31 * hashCode + frame.z.hashCode()
            hashCode = 31 * hashCode + frame.scaleX.hashCode()
            hashCode = 31 * hashCode + frame.scaleY.hashCode()
            hashCode = 31 * hashCode + frame.rotationInDegrees.hashCode()

            this.isHashDirty = false
            cacheHashCode = hashCode
        }

        return cacheHashCode
    }
}
