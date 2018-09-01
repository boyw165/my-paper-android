// Copyright Apr 2017-present Paper
//
// Author: boyw165@gmail.com,
//         djken0106@gmail.com
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

import io.reactivex.Observable
import java.net.URI
import java.util.*

interface IPaper {

    // The SQLite ID.
    fun getID(): Long
    // The global ID.
    fun getUUID(): UUID

    fun getCreatedAt(): Long

    fun getModifiedAt(): Long
    fun setModifiedAt(time: Long)

    fun getSize(): Pair<Float, Float>
    fun setSize(size: Pair<Float, Float>)

    fun getViewPort(): Rect
    fun setViewPort(rect: Rect)

    fun getThumbnail(): URI
    fun setThumbnail(file: URI, width: Int, height: Int)

    fun getThumbnailSize(): Pair<Int, Int>

    fun getCaption(): String
    fun getTags(): List<String>

    fun copy(): IPaper

    // Scraps /////////////////////////////////////////////////////////////////

    fun getScraps(): List<IScrap>

    fun addScrap(scrap: IScrap)

    fun removeScrap(scrap: IScrap)

    fun observeAddScrap(): Observable<IScrap>

    fun observeRemoveScrap(): Observable<IScrap>
}
