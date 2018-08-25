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
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class SimpleEditorWidget(private val paperID: Long,
                         private val paperRepo: IPaperRepo,
                         private val caughtErrorSignal: Observer<Throwable>,
                         private val schedulers: ISchedulerProvider)
    : IWidget {

    protected val mCanvasWidget by lazy {
        CanvasWidget(schedulers = schedulers)
    }

    protected val mDisposables = CompositeDisposable()

    override fun start(): Completable {
        return autoStopCompletable {
            ensureNoLeakingSubscription()

            // Load paper and start the widget
            canvasWidgetSrc
                .subscribe { widget ->
                    // Signal out the widget is ready
                    mCanvasWidgetReadySignal.onNext(widget)
                }
                .addTo(mDisposables)
        }
    }

    override fun stop() {
        mDisposables.clear()
    }

    private fun ensureNoLeakingSubscription() {
        if (mDisposables.size() > 0) throw IllegalStateException(
            "Already start to a widget")
    }

    protected val paperSrc: Observable<IPaper> by lazy {
        paperRepo
            .getPaperById(paperID)
            .toObservable()
            .cache()
    }

    protected val canvasWidgetSrc by lazy {
        paperSrc
            .doOnSubscribe {
                mBusyFlag.markDirty(EditorDirtyFlag.READ_PAPER_FROM_REPO)
            }
            .flatMap { paper ->
                mBusyFlag.markNotDirty(EditorDirtyFlag.READ_PAPER_FROM_REPO)

                mCanvasWidget.inject(paper)

                mCanvasWidget.start()
                    .toSingleDefault(mCanvasWidget as ICanvasWidget)
                    .toObservable()
            }
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val mBusyFlag = EditorDirtyFlag(EditorDirtyFlag.READ_PAPER_FROM_REPO)

    /**
     * An overall busy state of the editor. The state is contributed by its sub-
     * components, e.g. canvas widget or history widget.
     */
    fun onBusy(): Observable<Boolean> {
        return Observables
            .combineLatest(
                mBusyFlag.onUpdate().map { it.flag != 0},
                mCanvasWidget.onBusy()) { editorBusy, canvasBusy ->
                println("${DomainConst.TAG}: " +
                        "editor busy=$editorBusy, " +
                        "canvas busy=$canvasBusy")
                editorBusy || canvasBusy
            }
    }

    // Canvas widget //////////////////////////////////////////////////////////

    private val mCanvasWidgetReadySignal = PublishSubject.create<ICanvasWidget>().toSerialized()

    fun onCanvasWidgetReady(): Observable<ICanvasWidget> {
        return mCanvasWidgetReadySignal
    }
}
