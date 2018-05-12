// Copyright Apr 2018-present boyw165@gmail.com
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

data class Point(var x: Float = 0f,
                 var y: Float = 0f,
                 var time: Long = System.currentTimeMillis()) {

    private var mIsHashDirty = true
    private var mHashCode = 0

    fun offset(tx: Float,
               ty: Float) {
        x += tx
        y += ty

        mIsHashDirty = false
    }

    fun velocityFrom(start: Point): Float {
        val vec = distanceTo(start) / (this.time - start.time)
        return if (vec.isNaN()) 0f else vec
    }

    fun distanceTo(other: Point): Float {
        val dx = other.x - this.x
        val dy = other.y - this.y
        return Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        if (mIsHashDirty) {
            mHashCode = if (x != +0.0f) java.lang.Float.floatToIntBits(x) else 0
            mHashCode = 31 * mHashCode + if (y != +0.0f) java.lang.Float.floatToIntBits(y) else 0

            mIsHashDirty = false
        }
        return mHashCode
    }

    override fun toString(): String {
        return "Point(%.3f, %.3f)".format(this.x, this.y)
    }
}
