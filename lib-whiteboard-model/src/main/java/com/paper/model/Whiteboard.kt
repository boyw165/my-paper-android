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

import io.useful.delegate.rx.RxMutableSet
import io.useful.delegate.rx.RxValue
import java.net.URI
import java.util.*

open class Whiteboard(id: Long = ModelConst.TEMP_ID,
                      val uuid: UUID = UUID.randomUUID(),
                      val createdAt: Long = 0L,
                      modifiedAt: Long = 0L,
                      width: Float = 512f,
                      height: Float = 512f,
                      viewPort: Rect = Rect(0f, 0f, 0f, 0f),
                      thumbnail: Triple<URI, Int, Int> = Triple(ModelConst.NULL_FILE, 0, 0),
                      scraps: Set<Scrap> = mutableSetOf())
    : NoObfuscation {

    var id: Long by RxValue(id)
    var modifiedAt: Long by RxValue(modifiedAt)
    var thumbnail: Triple<URI, Int, Int> by RxValue(thumbnail)

    var size: Pair<Float, Float> by RxValue(Pair(width, height))
    var viewPort: Rect by RxValue(viewPort)

    val scraps: MutableSet<Scrap> by RxMutableSet(scraps.toMutableSet())

    // Caption & tags /////////////////////////////////////////////////////////

    fun getCaption(): String {
        return ""
    }

    fun getTags(): List<String> {
        return emptyList()
    }

    // Scraps /////////////////////////////////////////////////////////////////

    fun getScrapByID(id: UUID): Scrap {
        return scraps.first { it.id == id }
    }

    fun addScrap(scrap: Scrap) {
        scraps.add(scrap)
    }

    fun removeScrap(scrap: Scrap) {
        scraps.remove(scrap)
    }

    // Equality & hash ////////////////////////////////////////////////////////

    fun copy(): Whiteboard {
        val copyScraps = scraps.toSet()

        return Whiteboard(id = id,
                          uuid = uuid,
                          createdAt = createdAt,
                          modifiedAt = modifiedAt,
                          width = size.first,
                          height = size.second,
                          viewPort = viewPort.copy(),
                          thumbnail = thumbnail.copy(),
                          scraps = copyScraps)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Whiteboard

        if (id != other.id) return false
        if (uuid != other.uuid) return false
        if (createdAt != other.createdAt) return false
        if (modifiedAt != other.modifiedAt) return false
        if (size != other.size) return false
        if (thumbnail != other.thumbnail) return false
        if (scraps != other.scraps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + modifiedAt.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + thumbnail.hashCode()
        result = 31 * result + scraps.hashCode()
        return result
    }
}
