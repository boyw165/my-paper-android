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

package com.paper.view.editor

import com.paper.domain.event.ProgressEvent
import com.paper.domain.useCase.LoadPaperAndBindModel
import com.paper.domain.widget.editor.IPaperWidget
import com.paper.domain.widget.editor.PaperWidget
import com.paper.model.repository.IPaperRepo
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class PaperEditorWidget(paperRepo: IPaperRepo,
                        uiScheduler: Scheduler,
                        ioScheduler: Scheduler) {

    private val mPaperRepo = paperRepo

    private val mUiScheduler = uiScheduler
    private val mIoScheduler = ioScheduler

    // Progress
    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()

    private val mPaperWidget by lazy {
        PaperWidget(mUiScheduler,
                    mIoScheduler)
    }

    private val mDisposables = CompositeDisposable()

    fun start(paperID: Long) {
        mDisposables.add(
            LoadPaperAndBindModel(
                paperID = paperID,
                paperWidget = mPaperWidget,
                paperRepo = mPaperRepo,
                updateProgressSignal = mUpdateProgressSignal,
                uiScheduler = mUiScheduler)
                .subscribe { done ->
                    if (done) {
                        mOnPaperWidgetReadySignal.onNext(mPaperWidget)
                    }
                })
    }

    fun stop() {
        mDisposables.dispose()
    }

    private val mOnPaperWidgetReadySignal = PublishSubject.create<IPaperWidget>()

    fun onPaperWidgetReady(): Observable<IPaperWidget> {
        return mOnPaperWidgetReadySignal
    }
}
