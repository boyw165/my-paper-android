// Copyright May 2018-present Paper
//
// Author: boyw165@gmail.com
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

import android.view.View
import com.airbnb.epoxy.EpoxyModel
import com.facebook.ads.NativeAd
import com.paper.model.IPaper
import io.reactivex.Observer

sealed class GalleryItem {
    abstract fun getEpoxyModel(): EpoxyModel<View>
}

data class CreatePaperItem(private val clickSignal: Observer<Any>) : GalleryItem() {

    override fun getEpoxyModel(): EpoxyModel<View> {
        return CreatePaperEpoxyModel()
            .onClick(clickSignal)
            .id("cta")
    }
}

data class PaperThumbItem(val paper: IPaper,
                          val clickSignal: Observer<Long>) : GalleryItem() {

    override fun getEpoxyModel(): EpoxyModel<View> {
        return PaperThumbnailEpoxyModel(mPaperId = paper.getId())
            .onClick(clickSignal)
            .setModifiedTime(paper.getModifiedAt())
            .setThumbnail(paper.getThumbnail(),
                          paper.getThumbnailWidth(),
                          paper.getThumbnailHeight())
            // Epoxy view-model ID.
            .id(paper.getId())
    }
}

data class NativeAdsItem(val ads: NativeAd,
                         val clickSignal: Observer<Any>) : GalleryItem() {

    override fun getEpoxyModel(): EpoxyModel<View> {
        return NativeAdsEpoxyModel(ads = ads)
            .onClick(clickSignal)
            .id("native ads")
    }
}
