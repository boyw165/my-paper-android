package com.paper.gallery

import com.airbnb.epoxy.TypedEpoxyController
import com.paper.shared.model.PaperModel

class PaperThumbnailEpoxyController : TypedEpoxyController<List<PaperModel>>() {

    override fun buildModels(data: List<PaperModel>) {
        data.forEachIndexed { i, _ ->
            PaperThumbnailEpoxyModel()
                .id(i)
                .addTo(this)
        }
    }
}
