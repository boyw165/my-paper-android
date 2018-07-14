// Copyright Apr 2018-present Paper
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

package com.paper.view

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

class HorizontalCentricItemDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect,
                                view: View,
                                parent: RecyclerView,
                                state: RecyclerView.State) {
        val viewWidth = getValidViewWidth(view)
        val viewHeight = getValidViewHeight(view)

        outRect.top = (parent.height - viewHeight) / 2

        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = (parent.width - viewWidth) / 2
        } else if (parent.getChildAdapterPosition(view) == parent.adapter.itemCount - 1) {
            outRect.right = (parent.width - viewWidth) / 2
        }
    }

    private fun getValidViewWidth(view: View): Int {
        return if (view.width == 0) {
            view.layoutParams.width
        } else {
            view.width
        }
    }

    private fun getValidViewHeight(view: View): Int {
        return if (view.width == 0) {
            view.layoutParams.height
        } else {
            view.height
        }
    }
}
