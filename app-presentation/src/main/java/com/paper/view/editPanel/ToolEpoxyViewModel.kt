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

import android.view.View
import android.widget.ImageView
import com.airbnb.epoxy.EpoxyModel
import com.bumptech.glide.RequestManager
import com.paper.R
import com.paper.domain.data.ToolType
import com.paper.domain.widget.editor.PaperEditPanelWidget

class ToolEpoxyViewModel(
    toolType: ToolType,
    resourceId: Int,
    imgLoader: RequestManager,
    isUsing: Boolean = false,
    widget: PaperEditPanelWidget? = null)
    : EpoxyModel<View>() {

    private val mToolID = toolType
    private val mImgLoader = imgLoader
    private val mResourceId = resourceId
    private val mIsUsing = isUsing

    private val mWidget = widget

    private var mImgView: ImageView? = null

    override fun getDefaultLayout(): Int {
        return R.layout.item_editing_tool
    }

    override fun bind(view: View) {
        if (mImgView == null) {
            mImgView = view.findViewById(R.id.image_view)
        }

        view.setOnClickListener {
            // TODO: Since the tool list (data) and UI click both lead to the
            // TODO: UI view-model change, merge this two upstream in the new
            // TODO: design!
            mWidget?.handleClickTool(mToolID)
        }

        mImgView?.setImageResource(mResourceId)
        mImgView?.isSelected = mIsUsing
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as ToolEpoxyViewModel

        if (mToolID != other.mToolID) return false
        if (mIsUsing != other.mIsUsing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mToolID.hashCode()
        result = 31 * result + mIsUsing.hashCode()
        return result
    }
}
