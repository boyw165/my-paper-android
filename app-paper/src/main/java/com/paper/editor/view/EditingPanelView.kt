// Copyright Feb 2018-present boyw165@gmail.com
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

package com.paper.editor.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.paper.R
import com.paper.shared.model.Rect
import io.reactivex.disposables.CompositeDisposable

class EditingPanelView : ConstraintLayout,
                         IEditingPanelView {

    private val mViewPortIndicatorView by lazy { findViewById<ViewPortIndicatorView>(R.id.view_port_indicator) }

    private val mDisposables = CompositeDisposable()

    constructor(context: Context) : this(context, null)
    constructor(context: Context,
                attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context, R.layout.view_editor_panel, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // TODO: Bind widget
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        mDisposables.clear()
    }

    override fun setCanvasAndViewPort(canvas: Rect,
                                      viewPort: Rect) {
        mViewPortIndicatorView.setCanvasAndViewPort(canvas, viewPort)
    }

    private fun ensureNoLeakingSubscriptions() {
        if (mDisposables.size() > 0) {
            throw IllegalStateException("Already bind to a widget")
        }
    }
}
