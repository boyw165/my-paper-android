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

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.facebook.drawee.view.SimpleDraweeView
import com.paper.R
import io.reactivex.Observer
import java.io.File

class PaperThumbnailEpoxyModel(private var mPaperId: Long) : EpoxyModelWithHolder<EpoxyHolder>() {

    private var mThumbFile: File? = null
    private var mThumbWidth = 0
    private var mThumbHeight = 0
    private var mModifiedAt = 0L

    override fun getDefaultLayout(): Int {
        return R.layout.gallery_item_of_paper_thumbnail
    }

    override fun createNewHolder(): EpoxyHolder {
        return PaperThumbnailEpoxyModel.Holder()
    }

    override fun bind(holder: EpoxyHolder) {
        super.bind(holder)

        // Smart casting
        holder as PaperThumbnailEpoxyModel.Holder

        // Update thumbnail size by fixing width and changing the height.
        if (mThumbWidth > 0 && mThumbHeight > 0) {
            val scale = Math.min(holder.defaultThumbWidth / mThumbWidth,
                                 holder.defaultThumbHeight / mThumbHeight)
            val layoutParams = holder.thumbViewContainer.layoutParams
            layoutParams.width = (mThumbWidth * scale).toInt()
            layoutParams.height = (mThumbHeight * scale).toInt()
            holder.thumbViewContainer.layoutParams = layoutParams
        }

        // Click
        holder.itemView.setOnClickListener {
            mOnClickPaperSignal?.onNext(mPaperId)
        }

        // Thumb
        mThumbFile?.let { file ->
            holder.thumbView.setImageURI(Uri.fromFile(file).toString())
        }
    }

    override fun unbind(holder: EpoxyHolder) {
        super.unbind(holder)

        // Smart casting
        holder as PaperThumbnailEpoxyModel.Holder

        // Click
        holder.itemView.setOnClickListener(null)
    }

    fun setModifiedTime(modifiedAt: Long): PaperThumbnailEpoxyModel {
        mModifiedAt = modifiedAt
        return this
    }

    fun setThumbnail(file: File?,
                     width: Int,
                     height: Int): PaperThumbnailEpoxyModel {
        mThumbFile = file
        mThumbWidth = width
        mThumbHeight = height
        return this
    }

    // Click //////////////////////////////////////////////////////////////////

    private var mOnClickPaperSignal: Observer<Long>? = null

    fun onClick(signal: Observer<Long>): PaperThumbnailEpoxyModel {
        mOnClickPaperSignal = signal
        return this
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PaperThumbnailEpoxyModel

        if (mPaperId != other.mPaperId) return false
        if (mThumbFile != other.mThumbFile) return false
        if (mThumbWidth != other.mThumbWidth) return false
        if (mThumbHeight != other.mThumbHeight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mPaperId.hashCode()
        result = 31 * result + (mThumbFile?.hashCode() ?: 0)
        result = 31 * result + mThumbWidth
        result = 31 * result + mThumbHeight
        return result
    }

    // Clazz //////////////////////////////////////////////////////////////////

    class Holder : EpoxyHolder() {

        lateinit var itemView: View

        lateinit var thumbView: SimpleDraweeView
        lateinit var thumbViewContainer: ViewGroup
        var defaultThumbWidth = 0f
        var defaultThumbHeight = 0f

        override fun bindView(itemView: View) {
            this.itemView = itemView

            if (defaultThumbWidth == 0f) {
                defaultThumbWidth = itemView.layoutParams.width.toFloat() -
                    itemView.paddingStart - itemView.paddingEnd
            }
            if (defaultThumbHeight == 0f) {
                defaultThumbHeight = itemView.layoutParams.height.toFloat() -
                    itemView.paddingTop - itemView.paddingBottom
            }

            thumbView = itemView.findViewById(R.id.image_view)
            thumbViewContainer = itemView.findViewById(R.id.image_view_container)
        }
    }
}
