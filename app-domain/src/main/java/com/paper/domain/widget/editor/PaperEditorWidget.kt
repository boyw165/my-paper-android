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

package com.paper.domain.widget.editor

import com.paper.domain.DomainConst
import com.paper.domain.data.DrawingMode
import com.paper.domain.data.ToolType
import com.paper.domain.event.ProgressEvent
import com.paper.domain.event.UndoRedoEvent
import com.paper.domain.useCase.BindWidgetWithModel
import com.paper.model.IPaperTransformRepo
import com.paper.model.repository.ICommonPenPrefsRepo
import com.paper.model.repository.IPaperRepo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class PaperEditorWidget(paperRepo: IPaperRepo,
                        paperTransformRepo: IPaperTransformRepo,
                        penPrefs: ICommonPenPrefsRepo,
                        caughtErrorSignal: Observer<Throwable>,
                        uiScheduler: Scheduler,
                        ioScheduler: Scheduler) {

    private val mPaperRepo = paperRepo
    private val mPenPrefs = penPrefs
    private val mCaughtErrorSignal = caughtErrorSignal

    private val mUiScheduler = uiScheduler
    private val mIoScheduler = ioScheduler

    private val mDisposables = CompositeDisposable()

    fun start(paperID: Long) {
        ensureNoLeakingSubscription()

        // Load paper and establish the paper (canvas) and transform bindings.
        val paperSrc = mPaperRepo
            .getPaperById(paperID)
            .toObservable()
            .cache()
        val paperBindingSrc = paperSrc
            .flatMap { paper ->
                BindWidgetWithModel(
                    widget = mCanvasWidget,
                    model = paper,
                    caughtErrorSignal = mCaughtErrorSignal)
                    .subscribeOn(mUiScheduler)
            }
        val historyBindingSrc = paperSrc
            .flatMap { paper ->
                BindWidgetWithModel(
                    widget = mHistoryWidget,
                    model = paper,
                    caughtErrorSignal = mCaughtErrorSignal)
                    .subscribeOn(mUiScheduler)
            }
        mDisposables.add(
            Observables
                .zip(paperBindingSrc,
                     historyBindingSrc)
                .map { (result1, result2) -> result1 && result2 }
                .observeOn(mUiScheduler)
                .subscribe { done ->
                    if (done) {
                        mOnCanvasWidgetReadySignal.onNext(mCanvasWidget)
                    }
                })

        // Bind the edit panel widget with the pen preference model
        mDisposables.add(
            BindWidgetWithModel(
                widget = mEditPanelWidget,
                model = mPenPrefs)
                .observeOn(mUiScheduler)
                .subscribe { done ->
                    if (done) {
                        mOnEditPanelWidgetReadySignal.onNext(mEditPanelWidget)
                    }
                })

        // Following are all about the edit panel outputs to paper widget:

        // Choose what drawing tool
        mDisposables.add(
            mEditPanelWidget
                .onUpdateEditToolList()
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    val toolID = event.toolIDs[event.usingIndex]

                    when (toolID) {
                        ToolType.ERASER -> {
                            mCanvasWidget.setDrawingMode(DrawingMode.ERASER)
                        }
                        ToolType.PEN -> {
                            mCanvasWidget.setDrawingMode(DrawingMode.SKETCH)
                        }
                        else -> {
                            println("${DomainConst.TAG}: Yet supported")
                        }
                    }
                })

        // Pen colors
        mDisposables.add(
            mEditPanelWidget
                .onUpdatePenColorList()
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    val color = event.colorTickets[event.usingIndex]
                    mCanvasWidget.setChosenPenColor(color)
                })
        // Pen size
        mDisposables.add(
            mEditPanelWidget
                .onUpdatePenSize()
                .observeOn(mUiScheduler)
                .subscribe { penSize ->
                    mCanvasWidget.setPenSize(penSize)
                })

        // Undo and redo
        // TODO: Before really undo, check editor state. If it's free, do it
        // TODO: immediately. While doing, please update the editor state to
        // TODO: busy. Once finished, update it to free.
        mDisposables.add(
            Observable
                .merge(mUndoSignals)
                .flatMap {
                    mHistoryWidget
                        .undo()
                        .toObservable()
                }
                .subscribe())
        mDisposables.add(
            Observable
                .merge(mRedoSignals)
                .flatMap {
                    mHistoryWidget
                        .redo()
                        .toObservable()
                }
                .subscribe())
        mDisposables.add(
            Observables.combineLatest(
                onBusy(),
                mHistoryWidget.onUpdateUndoRedoCapacity())
                .map { (busy, event) ->
                    if (busy) {
                        UndoRedoEvent(canUndo = false,
                                      canRedo = false)
                    } else {
                        event
                    }
                }
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    mUndoRedoEventSignal.onNext(event)
                })
    }

    fun stop() {
        mDisposables.clear()
    }

    /**
     * Request to stop; It is granted to stop when receiving a true; vice versa.
     */
    fun requestStop(): Observable<Boolean> {
        return onBusy()
            .doOnNext { busy ->
                if (busy) {
                    mUpdateProgressSignal.onNext(ProgressEvent.start(0))
                } else {
                    mUpdateProgressSignal.onNext(ProgressEvent.stop(100))
                }
            }
            .map { !it }
    }

    private fun ensureNoLeakingSubscription() {
        if (mDisposables.size() > 0) throw IllegalStateException(
            "Already bind to a widget")
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val mBusySignal = BehaviorSubject.createDefault(false)

    /**
     * A overall busy state of the editor. The state is contributed by its sub-
     * components, e.g. canvas widget or history widget.
     */
    private fun onBusy(): Observable<Boolean> {
        return Observables
            .combineLatest(
                mBusySignal,
                mCanvasWidget.onBusy(),
                mHistoryWidget.onBusy(),
                mEditPanelWidget.onBusy()) { editorBusy, canvasBusy, historyBusy, panelBusy ->
                println("${DomainConst.TAG}: " +
                        "editor busy=$editorBusy, " +
                        "canvas busy=$canvasBusy, " +
                        "history busy=$historyBusy, " +
                        "panel busy=$panelBusy")
                editorBusy || canvasBusy || historyBusy || panelBusy
            }
    }

    // Canvas widget & functions //////////////////////////////////////////////

    private val mCanvasWidget by lazy {
        PaperCanvasWidget(mUiScheduler,
                          mIoScheduler)
    }

    private val mOnCanvasWidgetReadySignal = PublishSubject.create<IPaperCanvasWidget>()

    // TODO: The interface is probably redundant
    fun onCanvasWidgetReady(): Observable<IPaperCanvasWidget> {
        return mOnCanvasWidgetReadySignal
    }

    fun eraseCanvas(): Observable<Boolean> {
        mHistoryWidget.eraseAll()
        mCanvasWidget.eraseCanvas()
        return Observable.just(true)
    }

    // Edit panel widget //////////////////////////////////////////////////////

    private val mOnEditPanelWidgetReadySignal = PublishSubject.create<PaperEditPanelWidget>()

    private val mEditPanelWidget by lazy {
        PaperEditPanelWidget(
            uiScheduler = mUiScheduler,
            workerScheduler = mIoScheduler)
    }

    fun onEditPanelWidgetReady(): Observable<PaperEditPanelWidget> {
        return mOnEditPanelWidgetReadySignal
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    private val mHistoryWidget by lazy {
        PaperTransformWidget(historyRepo = paperTransformRepo,
                             uiScheduler = mUiScheduler,
                             ioScheduler = mIoScheduler)
    }

    private val mUndoSignals = mutableListOf<Observable<Any>>()

    fun addUndoSignal(source: Observable<Any>) {
        mUndoSignals.add(source)
    }

    private val mRedoSignals = mutableListOf<Observable<Any>>()

    fun addRedoSignal(source: Observable<Any>) {
        mRedoSignals.add(source)
    }

    private val mUndoRedoEventSignal = BehaviorSubject.createDefault(
        UndoRedoEvent(canUndo = false,
                      canRedo = false))

    fun onGetUndoRedoEvent(): Observable<UndoRedoEvent> {
        return mUndoRedoEventSignal
    }

    // Progress & error & Editor status ///////////////////////////////////////

    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()

    fun onUpdateProgress(): Observable<ProgressEvent> {
        return mUpdateProgressSignal
    }

    fun onGetStatus(): Observable<Any> {
        TODO()
    }
}
