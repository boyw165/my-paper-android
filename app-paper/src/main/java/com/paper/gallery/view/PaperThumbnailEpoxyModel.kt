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

package com.paper.gallery.view

import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.view.View
import android.widget.ImageView
import com.airbnb.epoxy.EpoxyModel
import com.bumptech.glide.RequestManager
import com.paper.R

class PaperThumbnailEpoxyModel(
    private var mPaperId: Long)
    : EpoxyModel<View>() {

    private var mListener: IOnClickPaperThumbnailListener? = null

    private var mGlide: RequestManager? = null
    private var mThumbPath: String? = null
    private var mThumbWidth = 0
    private var mThumbHeight = 0

    // View
    private var mThumbView: ImageView? = null

    override fun getDefaultLayout(): Int {
        return R.layout.item_paper_thumbnail
    }

    override fun bind(view: View?) {
        super.bind(view)

        if (mThumbView == null) {
            mThumbView = view?.findViewById(R.id.image_view)
        }

        // Click
        view?.setOnClickListener({
            mListener?.onClickPaperThumbnail(mPaperId)
        })

        // Thumb
        mGlide?.let { imgLoader ->
            imgLoader
                .load(mThumbPath)
                .into(mThumbView)
        }
    }

    override fun unbind(view: View?) {
        super.unbind(view)

        // Click
        view?.setOnClickListener(null)
    }

    fun setThumbnail(glide: RequestManager?,
                     path: String,
                     width: Int,
                     height: Int): PaperThumbnailEpoxyModel {
        mGlide = glide
        mThumbPath = path
        mThumbWidth = width
        mThumbHeight = height
        return this
    }

    fun setClickListener(listener: IOnClickPaperThumbnailListener?): PaperThumbnailEpoxyModel {
        mListener = listener
        return this
    }
}
