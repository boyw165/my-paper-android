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

open class TextScrap(uuid: UUID = UUID.randomUUID(),
                     frame: Frame = Frame(),
                     text: String)
    : Scrap(id = uuid,
            frame = frame) {

    var text: String by RxValue(text)

    // Equality & Hash ////////////////////////////////////////////////////////

    override fun copy(): Scrap {
        return TextScrap(uuid = UUID.randomUUID(),
                         frame = frame,
                         text = text)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextScrap

        if (!super.equals(other)) return false

        return true
    }

    override fun hashCode(): Int {
        val isHashDirty = this.isHashDirty
        if (isHashDirty) {
            val hashCode = super.hashCode()
            // FIXME: There is a very short moment in between super.hashCode()
            // FIXME: and the following code such that isHashDirty is false

            this.isHashDirty = false
            this.cacheHashCode = hashCode
        }

        return this.cacheHashCode
    }
}
