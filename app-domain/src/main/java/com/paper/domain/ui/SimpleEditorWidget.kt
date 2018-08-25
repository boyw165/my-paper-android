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

package com.paper.domain.ui

import com.paper.domain.DomainConst
import com.paper.domain.ISchedulerProvider
import com.paper.domain.action.StartWidgetAutoStopObservable
import com.paper.model.IPaper
import com.paper.model.repository.IPaperRepo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class SimpleEditorWidget(private val paperID: Long,
                         private val paperRepo: IPaperRepo,
                         private val caughtErrorSignal: Observer<Throwable>,
                         private val schedulers: ISchedulerProvider)
    : IWidget {

    private val mCanvasWidget by lazy {
        CanvasWidget(schedulers = schedulers)
    }

    private val mDisposables = CompositeDisposable()

    override fun start() {
        ensureNoLeakingSubscription()

        // Load paper and establish the paper (canvas) and transform bindings.
        val paperSrc = paperRepo
            .getPaperById(paperID)
            .toObservable()
            .cache()
        val canvasWidgetReadySrc = paperSrc
            .flatMap { paper ->
                initCanvasWidget(paper)
            }
    }

    override fun stop() {
        mDisposables.clear()
    }

    private fun ensureNoLeakingSubscription() {
        if (mDisposables.size() > 0) throw IllegalStateException(
            "Already start to a widget")
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val mBusySignal = BehaviorSubject.createDefault(false)

    /**
     * An overall busy state of the editor. The state is contributed by its sub-
     * components, e.g. canvas widget or history widget.
     */
    private fun onBusy(): Observable<Boolean> {
        return Observables
            .combineLatest(
                mBusySignal,
                mCanvasWidget.onBusy()) { editorBusy, canvasBusy ->
                println("${DomainConst.TAG}: " +
                        "editor busy=$editorBusy, " +
                        "canvas busy=$canvasBusy")
                editorBusy || canvasBusy
            }
    }

    // Canvas widget & functions //////////////////////////////////////////////

    private fun initCanvasWidget(paper: IPaper): Observable<Boolean> {
        mCanvasWidget.setModel(paper)

        return StartWidgetAutoStopObservable(
            widget = mCanvasWidget,
            caughtErrorSignal = caughtErrorSignal)
            .subscribeOn(schedulers.main())
    }
}
