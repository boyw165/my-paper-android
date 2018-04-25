// Copyright Apr 2018-present boyw165@gmail.com
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

package com.paper.view.editPanel

import android.graphics.drawable.ColorDrawable
import android.view.View
import com.airbnb.epoxy.EpoxyModel
import com.facebook.drawee.view.SimpleDraweeView
import com.paper.R
import com.paper.domain.widget.editor.PaperEditPanelWidget

class ColorTicketEpoxyViewModel(color: Int,
                                widget: PaperEditPanelWidget,
                                isUsing: Boolean = false)
    : EpoxyModel<View>() {

    private val mColor = color
    private val mWidget = widget
    private val mIsUsing = isUsing

    private lateinit var mColorView: SimpleDraweeView
    private lateinit var mHighlightedColorView: SimpleDraweeView

    override fun getDefaultLayout(): Int {
        return R.layout.item_color_ticket
    }

    override fun bind(view: View) {
        super.bind(view)

        mColorView = view.findViewById(R.id.image_view)
        mHighlightedColorView = view.findViewById(R.id.highlighted_image_view)

        mColorView.hierarchy.setPlaceholderImage(ColorDrawable(mColor))
        mColorView.alpha = if (mIsUsing) 0f else 1f

        mHighlightedColorView.hierarchy.setPlaceholderImage(ColorDrawable(mColor))
        mHighlightedColorView.alpha = if (mIsUsing) 1f else 0f

        view.setOnClickListener {
            mWidget.handleClickColor(mColor)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as ColorTicketEpoxyViewModel

        if (mColor != other.mColor) return false
        if (mIsUsing != other.mIsUsing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mColor
        result = 31 * result + mIsUsing.hashCode()
        return result
    }
}
