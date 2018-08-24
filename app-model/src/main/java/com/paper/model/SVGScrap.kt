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

import com.paper.model.sketch.SVGStyle
import com.paper.model.sketch.VectorGraphics
import java.util.*
import java.util.concurrent.locks.ReentrantLock

open class SVGScrap(override val uuid: UUID = UUID.randomUUID(),
                    override var mutableFrame: Frame = Frame(),
                    private val graphicsList: MutableList<VectorGraphics> = mutableListOf())
    : BaseScrap(uuid, mutableFrame),
      ISVGScrap {

    override fun setSVGs(other: List<VectorGraphics>) {
        synchronized(mLock) {
            graphicsList.clear()
            graphicsList.addAll(other)
        }
    }

    override fun getSVGs(): List<VectorGraphics> {
        synchronized(mLock) {
            return graphicsList.toList()
        }
    }

    @Volatile
    private var mTmpVectorGraphics: VectorGraphics? = null
//    private val mTmpVectorGraphicsLock = ReentrantLock()

    override fun moveTo(x: Float,
                        y: Float,
                        style: Set<SVGStyle>) {
        synchronized(mLock) {
            // TODO: Fix the session lock
//            mTmpVectorGraphicsLock.lock()

            mTmpVectorGraphics = VectorGraphics(
                style = style,
                tupleList = mutableListOf(LinearPointTuple(x, y)))
        }
    }

    override fun lineTo(x: Float, y: Float) {
        synchronized(mLock) {
            val v = mTmpVectorGraphics!!
            v.addTuple(LinearPointTuple(x, y))
        }
    }

    override fun cubicTo(previousControlX: Float,
                         previousControlY: Float,
                         currentControlX: Float,
                         currentControlY: Float,
                         currentEndX: Float,
                         currentEndY: Float) {
        synchronized(mLock) {
            val v = mTmpVectorGraphics!!
            v.addTuple(CubicPointTuple(previousControlX, previousControlY,
                                       currentControlX, currentControlY,
                                       currentEndX, currentEndY))
        }
    }

    override fun close() {
        synchronized(mLock) {
            val v = mTmpVectorGraphics!!
            mTmpVectorGraphics = null

            graphicsList.add(v)

//            mTmpVectorGraphicsLock.unlock()
        }
    }

    // Equality & Hash ////////////////////////////////////////////////////////

    override fun copy(): IScrap {
        return synchronized(mLock) {
            SVGScrap(uuid = uuid,
                     mutableFrame = mutableFrame.copy(),
                     graphicsList = graphicsList.toMutableList())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SVGScrap

        if (!super.equals(other)) return false

        val svgs = synchronized(mLock) { graphicsList.toList() }
        val otherSVGs = other.getSVGs()

        if (svgs.hashCode() != otherSVGs.hashCode()) return false

        return true
    }

    override fun hashCode(): Int {
        val isHashDirty = synchronized(mLock) { mIsHashDirty }
        if (isHashDirty) {
            var hashCode = super.hashCode()
            // FIXME: There is a very short moment in between super.hashCode()
            // FIXME: and the following code such that mIsHashDirty is false
            hashCode = 31 * hashCode + getSVGs().hashCode()

            synchronized(mLock) {
                mIsHashDirty = false
                mHashCode = hashCode
            }
        }

        return mHashCode
    }
}
