package com.paper.gallery

import android.view.View
import com.airbnb.epoxy.EpoxyModel
import com.paper.R

class PaperThumbnailEpoxyModel : EpoxyModel<View>() {

    override fun getDefaultLayout(): Int {
        return R.layout.item_paper_thumbnail
    }
}
