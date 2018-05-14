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

import com.airbnb.epoxy.TypedEpoxyController
import com.bumptech.glide.RequestManager
import com.paper.R
import com.paper.domain.data.ToolType
import com.paper.domain.event.UpdateEditToolsEvent
import com.paper.domain.widget.editor.PaperEditPanelWidget

class ToolListEpoxyController(imageLoader: RequestManager)
    : TypedEpoxyController<UpdateEditToolsEvent>() {

    private val mImgLoader = imageLoader

    override fun buildModels(data: UpdateEditToolsEvent) {
        data.toolIDs.forEachIndexed { i, toolType ->
            ToolEpoxyViewModel(
                toolType = toolType,
                imgLoader = mImgLoader,
                resourceId = getResourceId(toolType),
                isUsing = data.usingIndex == i,
                widget = mWidget)
                .id(toolType.ordinal)
                .addTo(this)
        }
    }

    private fun getResourceId(toolId: ToolType): Int {
        return when (toolId) {
            ToolType.ERASER -> R.drawable.sel_img_e_eraser
            ToolType.PEN -> R.drawable.sel_img_e_pen
            ToolType.LASSO -> R.drawable.sel_img_e_scissor
            else -> throw IllegalArgumentException("Unsupported tool ID")
        }
    }

    private var mWidget: PaperEditPanelWidget? = null

    fun setWidget(widget: PaperEditPanelWidget?) {
        mWidget = widget
    }
}
