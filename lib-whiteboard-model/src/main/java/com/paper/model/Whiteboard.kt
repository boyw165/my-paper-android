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

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.net.URI
import java.util.*

open class Whiteboard(private var id: Long = ModelConst.TEMP_ID,
                      private val uuid: UUID = UUID.randomUUID(),
                      private val createdAt: Long = 0L,
                      private var modifiedAt: Long = 0L,
                      private var width: Float = 512f,
                      private var height: Float = 512f,
                      private val viewPort: Rect = Rect(0f, 0f, 0f, 0f),
                      private var thumbnail: Triple<URI, Int, Int> = Triple(ModelConst.NULL_FILE, 0, 0),
                      private var scraps: MutableList<Scrap> = mutableListOf())
    : NoObfuscation {

    private val lock = Any()

    // General ////////////////////////////////////////////////////////////////

    fun getID(): Long {
        return id
    }

    fun setID(id: Long) {
        this.id = id
    }

    fun getUUID(): UUID {
        return uuid
    }

    fun getCreatedAt(): Long {
        return createdAt
    }

    fun getModifiedAt(): Long {
        synchronized(lock) {
            return modifiedAt
        }
    }

    fun setModifiedAt(time: Long) {
        synchronized(lock) {
            modifiedAt = time
        }
    }

    fun getSize(): Pair<Float, Float> {
        synchronized(lock) {
            return Pair(width, height)
        }
    }

    fun setSize(size: Pair<Float, Float>) {
        synchronized(lock) {
            val (width, height) = size
            this.width = width
            this.height = height
        }
    }

    fun getViewPort(): Rect {
        synchronized(lock) {
            return viewPort.copy()
        }
    }

    fun setViewPort(rect: Rect) {
        synchronized(lock) {
            viewPort.set(rect)
        }
    }

    // Caption & tags /////////////////////////////////////////////////////////

    fun getCaption(): String {
        return ""
    }

    fun getTags(): List<String> {
        return emptyList()
    }

    // Thumbnail //////////////////////////////////////////////////////////////

    fun getThumbnail(): URI {
        return thumbnail.first
    }

    fun setThumbnail(file: URI,
                              width: Int,
                              height: Int) {
        synchronized(lock) {
            thumbnail = Triple(file, width, height)
        }
    }

    fun getThumbnailSize(): Pair<Int, Int> {
        return Pair(thumbnail.second, thumbnail.third)
    }

    // Scraps /////////////////////////////////////////////////////////////////

    fun getScraps(): List<Scrap> {
        synchronized(lock) {
            // Must clone the list in case concurrent modification
            return scraps.toList()
        }
    }

    fun getScrapByID(id: UUID): Scrap {
        return synchronized(lock) {
            scraps.first { it.getID() == id }
        }
    }

    fun addScrap(scrap: Scrap) {
        synchronized(lock) {
            if (scraps.add(scrap)) {
                addScrapSignal.onNext(scrap)
            }
        }
    }

    fun removeScrap(scrap: Scrap) {
        synchronized(lock) {
            if (scraps.remove(scrap)) {
                removeScrapSignal.onNext(scrap)
            }
        }
    }

    private val addScrapSignal = PublishSubject.create<Scrap>().toSerialized()
    private val removeScrapSignal = PublishSubject.create<Scrap>().toSerialized()

    fun scrapAdded(): Observable<Scrap> {
        return addScrapSignal
    }

    fun scrapRemoved(): Observable<Scrap> {
        return removeScrapSignal
    }

    // Equality & hash ////////////////////////////////////////////////////////

    fun copy(): Whiteboard {
        return synchronized(lock) {
            val copyScraps = mutableListOf<Scrap>()
            scraps.forEach { copyScraps.add(it.copy()) }

            Whiteboard(id = id,
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

        other as Whiteboard

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
        result = 31 * result + getThumbnail().hashCode()
        result = 31 * result + getThumbnailSize().hashCode()
        result = 31 * result + getScraps().hashCode()
        return result
    }
}
