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

import android.view.View
import android.view.ViewGroup
import com.airbnb.epoxy.EpoxyModel
import com.paper.R
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class TapToCreateEpoxyModel : EpoxyModel<View>() {

    override fun getDefaultLayout(): Int {
        return R.layout.item_paper_tap_to_create
    }

    override fun buildView(parent: ViewGroup): View {
        val layout = super.buildView(parent)

        layout.setOnClickListener {
            mOnClickSignal?.onNext(0)
        }

        return layout
    }

    // Click //////////////////////////////////////////////////////////////////

    private var mOnClickSignal: Subject<Any>? = null

    fun onClick(signal: PublishSubject<Any>): TapToCreateEpoxyModel {
        mOnClickSignal = signal
        return this
    }
}
