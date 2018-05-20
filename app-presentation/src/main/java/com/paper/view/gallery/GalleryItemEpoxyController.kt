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

package com.paper.view.gallery

import com.airbnb.epoxy.TypedEpoxyController
import com.paper.model.IPaper

class GalleryItemEpoxyController : TypedEpoxyController<List<GalleryItem>>() {

    override fun buildModels(data: List<GalleryItem>) {
        data.forEach { item ->
            item.getEpoxyModel()
                .addTo(this)
        }
    }

    fun getPaperFromAdapterPosition(position: Int): IPaper? {
        val actualData = currentData ?: return null
        val actualItem = actualData[position]

        return if (actualItem is PaperThumbItem) {
            actualItem.paper
        } else {
            null
        }
    }

    fun getAdapterPositionByPaperID(paperID: Long): Int {
        val actualData = currentData ?: return -1
        return actualData.indexOfFirst { item ->
            if (item is PaperThumbItem) {
                item.paper.getId() == paperID
            } else {
                false
            }
        }
    }
}
