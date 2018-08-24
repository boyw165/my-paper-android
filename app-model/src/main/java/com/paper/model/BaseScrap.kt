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

import java.util.*

open class BaseScrap(open val uuid: UUID = UUID.randomUUID(),
                     protected open var mutableFrame: Frame = Frame())
    : IScrap,
      NoObfuscation {

    protected val mLock = Any()

    protected var mIsHashDirty = true
    protected var mHashCode = 0

    override fun getId(): UUID {
        return uuid
    }

    override fun setFrame(frame: Frame) {
        synchronized(mLock) {
            mutableFrame = frame
        }
    }

    override fun getFrame(): Frame {
        synchronized(mLock) {
            return mutableFrame.copy()
        }
    }

    // Equality & Hash ////////////////////////////////////////////////////////

    override fun copy(): IScrap {
        return BaseScrap(uuid = uuid,
                         mutableFrame = mutableFrame.copy())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseScrap

        val frame = synchronized(mLock) { mutableFrame }
        val otherFrame = other.getFrame()

        if (uuid != other.uuid) return false
        if (frame.x != otherFrame.x) return false
        if (frame.y != otherFrame.y) return false
        if (frame.z != otherFrame.z) return false
        if (frame.scaleX != otherFrame.scaleX) return false
        if (frame.scaleY != otherFrame.scaleY) return false
        if (frame.rotationInDegrees != otherFrame.rotationInDegrees) return false

        return true
    }

    override fun hashCode(): Int {
        val isHashDirty = synchronized(mLock) { mIsHashDirty }
        if (isHashDirty) {
            val frame = synchronized(mLock) { mutableFrame }
            var hashCode = uuid.hashCode()
            hashCode = 31 * hashCode + frame.x.hashCode()
            hashCode = 31 * hashCode + frame.y.hashCode()
            hashCode = 31 * hashCode + frame.z.hashCode()
            hashCode = 31 * hashCode + frame.scaleX.hashCode()
            hashCode = 31 * hashCode + frame.scaleY.hashCode()
            hashCode = 31 * hashCode + frame.rotationInDegrees.hashCode()

            synchronized(mLock) {
                mIsHashDirty = false
                mHashCode = hashCode
            }
        }

        synchronized(mLock) {
            return mHashCode
        }
    }
}
