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

package com.paper.domain.widget

import com.paper.domain.widget.canvas.IPaperWidget
import com.paper.model.repository.protocol.IPaperModelRepo
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler

class LoadPaperFromStore(paperID: Long,
                         paperWidget: IPaperWidget,
                         paperRepo: IPaperModelRepo,
                         uiScheduler: Scheduler)
    : ObservableTransformer<Any, IPaperWidget> {

    private val mPaperID = paperID

    private val mPaperWidget = paperWidget
    private val mPaperRepo = paperRepo

    private val mUiScheduler = uiScheduler

    override fun apply(upstream: Observable<Any>): ObservableSource<IPaperWidget> {
        return upstream.switchMap { applyImpl() }
    }

    private fun applyImpl(): Observable<IPaperWidget> {
        return mPaperRepo
            .getPaperById(mPaperID)
            .observeOn(mUiScheduler)
            .map { paper ->
                // Bind widget with data.
                mPaperWidget.bindModel(paper)
                return@map mPaperWidget
            }
            .doOnDispose {
                // Unbind widget from data.
                mPaperWidget.unbindModel()
            }
            .toObservable()
    }
}
