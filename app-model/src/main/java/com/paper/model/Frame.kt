// Copyright Mar 2018-present boyw165@gmail.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.model

data class Frame(val x: Float = 0f,
                 val y: Float = 0f,
                 val width: Float = 0f,
                 val height: Float = 0f,
                 val scaleX: Float = 0f,
                 val scaleY: Float = 0f,
                 val rotationInDegrees: Float = 0f,
                 val z: Int = 0)
    : NoObfuscation {

    fun add(other: Frame): Frame {
        return Frame(x = x + other.x,
                     y = y + other.y,
                     width = width + other.width,
                     height = height + other.height,
                     scaleX = scaleX + other.scaleX,
                     scaleY = scaleY + other.scaleY,
                     rotationInDegrees = rotationInDegrees + other.rotationInDegrees,
                     z = z + other.z)
    }

    fun sub(other: Frame): Frame {
        return Frame(x = x - other.x,
                     y = y - other.y,
                     width = width - other.width,
                     height = height - other.height,
                     scaleX = scaleX - other.scaleX,
                     scaleY = scaleY - other.scaleY,
                     rotationInDegrees = rotationInDegrees - other.rotationInDegrees,
                     z = z - other.z)
    }
}
