package com.paper.gallery

import android.view.View
import com.airbnb.epoxy.EpoxyModel
import com.paper.R

class PaperThumbnailEpoxyModel(
    private var mPaperId: Long,
    private var mListener: OnClickPaperThumbnailListener? = null)
    : EpoxyModel<View>() {

    override fun getDefaultLayout(): Int {
        return R.layout.item_paper_thumbnail
    }

    override fun bind(view: View?) {
        super.bind(view)

        view?.setOnClickListener({
            mListener?.onClickPaperThumbnail(mPaperId)
        })
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    interface OnClickPaperThumbnailListener {

        fun onClickPaperThumbnail(id: Long)
    }
}
