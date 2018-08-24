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

import com.paper.model.repository.IPaperRepo
import java.net.URI
import java.util.*

open class BasePaper(private var id: Long = ModelConst.TEMP_ID,
                     private val uuid: UUID = UUID.randomUUID(),
                     private val createdAt: Long = 0L,
                     private var modifiedAt: Long = 0L,
                     private var width: Float = 512f,
                     private var height: Float = 512f,
                     private val viewPort: Rect = Rect(0f, 0f, 0f, 0f),
                     private var thumbnail: Triple<URI, Int, Int> = Triple(URI("file:///null"), 0, 0),
                     private var scraps: MutableList<IScrap> = mutableListOf())
    : IPaper,
      NoObfuscation {

    private val mLock = Any()

    // General ////////////////////////////////////////////////////////////////

    override fun getID(): Long {
        return id
    }

    fun setID(id: Long) {
        this.id = id
    }

    override fun getUUID(): UUID {
        return uuid
    }

    override fun getCreatedAt(): Long {
        return createdAt
    }

    override fun getModifiedAt(): Long {
        synchronized(mLock) {
            return modifiedAt
        }
    }

    override fun setModifiedAt(time: Long) {
        synchronized(mLock) {
            modifiedAt = time
        }
    }

    override fun getSize(): Pair<Float, Float> {
        synchronized(mLock) {
            return Pair(width, height)
        }
    }

    override fun setSize(size: Pair<Float, Float>) {
        synchronized(mLock) {
            val (width, height) = size
            this.width = width
            this.height = height
        }
    }

    override fun getViewPort(): Rect {
        synchronized(mLock) {
            return viewPort.copy()
        }
    }

    override fun setViewPort(rect: Rect) {
        synchronized(mLock) {
            viewPort.set(rect)
        }
    }

    // Caption & tags /////////////////////////////////////////////////////////

    override fun getCaption(): String {
        return ""
    }

    override fun getTags(): List<String> {
        return emptyList()
    }

    // Thumbnail //////////////////////////////////////////////////////////////

    override fun getThumbnail(): URI {
        return thumbnail.first
    }

    override fun setThumbnail(file: URI,
                              width: Int,
                              height: Int) {
        synchronized(mLock) {
            thumbnail = Triple(file, width, height)
        }
    }

    override fun getThumbnailSize(): Pair<Int, Int> {
        return Pair(thumbnail.second, thumbnail.third)
    }

    // Scraps /////////////////////////////////////////////////////////////////

    override fun getScraps(): List<IScrap> {
        synchronized(mLock) {
            // Must clone the list in case concurrent modification
            return scraps.toList()
        }
    }

    override fun addScrap(scrap: IScrap) {
        synchronized(mLock) {
            scraps.add(scrap)
        }
    }

    override fun removeScrap(scrap: IScrap) {
        synchronized(mLock) {
            scraps.remove(scrap)
        }
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun copy(): IPaper {
        return synchronized(mLock) {
            val copyScraps = mutableListOf<IScrap>()
            scraps.forEach { copyScraps.add(it.copy()) }

            BasePaper(id = id,
                      uuid = uuid,
                      createdAt = createdAt,
                      modifiedAt = modifiedAt,
                      width = width,
                      height = height,
                      viewPort = viewPort.copy(),
                      thumbnail = thumbnail.copy(),
                      scraps = copyScraps)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BasePaper

        if (getID() != other.getID()) return false
        if (getUUID() != other.getUUID()) return false
        if (getCreatedAt() != other.createdAt) return false
        if (getModifiedAt() != other.getModifiedAt()) return false
        if (getSize() != other.getSize()) return false
        if (getThumbnail() != other.getThumbnail()) return false
        if (getThumbnailSize() != other.getThumbnailSize()) return false
        if (getScraps() != other.getScraps()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = getID().hashCode()
        result = 31 * result + getUUID().hashCode()
        result = 31 * result + getCreatedAt().hashCode()
        result = 31 * result + getModifiedAt().hashCode()
        result = 31 * result + getSize().hashCode()
        getThumbnail()?.let {
            result = 31 * result + it.hashCode()
        }
        result = 31 * result + getThumbnailSize().hashCode()
        result = 31 * result + getScraps().hashCode()
        return result
    }
}
