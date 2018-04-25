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
import com.paper.domain.event.UpdateEditingToolsEvent
import com.paper.domain.widget.editor.PaperEditPanelWidget
import com.paper.domain.widget.editor.EditingToolFactory

class ToolListEpoxyController(
    private val mWidget: PaperEditPanelWidget,
    private val mImgLoader: RequestManager)
    : TypedEpoxyController<UpdateEditingToolsEvent>() {

    override fun buildModels(data: UpdateEditingToolsEvent) {
        data.toolIDs.forEachIndexed { i, toolId ->
            ToolEpoxyViewModel(
                toolId,
                mWidget,
                mImgLoader = mImgLoader,
                mResourceId = getResourceId(toolId),
                mFadeResourceId = getFadeResourceId(toolId),
                mIsUsing = data.usingIndex == i)
                .id(toolId)
                .addTo(this)
        }
    }

    private fun getResourceId(toolId: Int): Int {
        return when (toolId) {
            EditingToolFactory.TOOL_ERASER -> R.drawable.sel_img_e_eraser
            EditingToolFactory.TOOL_PEN -> R.drawable.sel_img_e_pen
            EditingToolFactory.TOOL_SCISSOR -> R.drawable.sel_img_e_scissor
            else -> throw IllegalArgumentException("Unsupported tool ID")
        }
    }

    private fun getFadeResourceId(toolId: Int): Int {
        return when (toolId) {
            EditingToolFactory.TOOL_ERASER -> R.drawable.img_e_tool_eraser_unselected
            EditingToolFactory.TOOL_PEN -> R.drawable.img_e_tool_pen_unselected
            EditingToolFactory.TOOL_SCISSOR -> R.drawable.img_e_tool_scissor_unselected
            else -> throw IllegalArgumentException("Unsupported tool ID")
        }
    }
}
