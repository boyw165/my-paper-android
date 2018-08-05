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
import com.paper.domain.event.UndoRedoEvent
import com.paper.domain.useCase.BindWidgetWithModel
import com.paper.model.IPaperTransformRepo
import com.paper.model.event.ProgressEvent
import com.paper.model.repository.ICommonPenPrefsRepo
import com.paper.model.repository.IPaperRepo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class PaperEditorWidget(paperRepo: IPaperRepo,
                        paperTransformRepo: IPaperTransformRepo,
                        penPrefs: ICommonPenPrefsRepo,
                        caughtErrorSignal: Observer<Throwable>,
                        uiScheduler: Scheduler,
                        ioScheduler: Scheduler)
    : IPaperEditorWidget {

    private val mPaperRepo = paperRepo
    private val mPenPrefs = penPrefs
    private val mCaughtErrorSignal = caughtErrorSignal

    private val mUiScheduler = uiScheduler
    private val mIoScheduler = ioScheduler

    private val mDisposables = CompositeDisposable()

    override fun start(paperID: Long): Observable<IPaperEditorWidget.OnStart> {
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
        val onCanvasWidgetReadySignal = Observables
            .zip(paperBindingSrc,
                 historyBindingSrc)
            .map { (paperBindingDone, historyBindingDone) ->
                if (paperBindingDone && historyBindingDone) {
                    mCanvasWidget as IPaperCanvasWidget
                } else {
                    if (!paperBindingDone && historyBindingDone) {
                        throw IllegalStateException("Fail to bind paper model with paper widget")
                    } else if (paperBindingDone && !historyBindingDone) {
                        throw IllegalStateException("Fail to bind paper model with history widget")
                    } else {
                        throw IllegalStateException("Fail to both")
                    }
                }
            }
        val onMenuWidgetReadySignal = BindWidgetWithModel(
            widget = mEditPanelWidget,
            model = mPenPrefs)
            .map { done ->
                if (done) {
                    mEditPanelWidget
                } else {
                    throw IllegalStateException("Fail to bind pen preferences with widget")
                }
            }
        val onGetUndoRedoEventSignal = Observables.combineLatest(
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

        return Observable.just(IPaperEditorWidget.OnStart(
            onCanvasWidgetReady = onCanvasWidgetReadySignal,
            onMenuWidgetReady = onMenuWidgetReadySignal,
            onGetUndoRedoEvent = onGetUndoRedoEventSignal))
    }

    override fun stop() {
        mDisposables.clear()
    }

    /**
     * Request to stop; It is granted to stop when receiving a true; vice versa.
     */
    override fun requestStop(bmpFile: File, bmpWidth: Int, bmpHeight: Int): Observable<Boolean> {
        return try {
            mCanvasWidget.setThumbnail(bmpFile, bmpWidth, bmpHeight)
            Observable.just(true)
        } catch (err: Throwable) {
            Observable.just(false)
        }
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

    private val mOnCanvasWidgetReadySignal = PublishSubject.create<IPaperCanvasWidget>().toSerialized()

    override fun eraseCanvas(): Observable<Boolean> {
        mHistoryWidget.eraseAll()
        mCanvasWidget.eraseCanvas()
        return Observable.just(true)
    }

    // Edit panel widget //////////////////////////////////////////////////////

    private val mEditPanelWidget by lazy {
        PaperEditPanelWidget(
            uiScheduler = mUiScheduler,
            workerScheduler = mIoScheduler)
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    private val mHistoryWidget by lazy {
        PaperTransformWidget(historyRepo = paperTransformRepo,
                             uiScheduler = mUiScheduler,
                             ioScheduler = mIoScheduler)
    }

    private val mUndoSignals = mutableListOf<Observable<Any>>()

    override fun addUndoSignal(source: Observable<Any>) {
        mUndoSignals.add(source)
    }

    private val mRedoSignals = mutableListOf<Observable<Any>>()

    override fun addRedoSignal(source: Observable<Any>) {
        mRedoSignals.add(source)
    }

    // Progress & error & Editor status ///////////////////////////////////////

    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()

    override fun onUpdateProgress(): Observable<ProgressEvent> {
        return mUpdateProgressSignal
    }
}
