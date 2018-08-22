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

package com.paper.presenter

import com.paper.domain.DomainConst
import com.paper.domain.ISchedulerProvider
import com.paper.domain.data.DrawingMode
import com.paper.domain.data.ToolType
import com.paper.domain.event.UndoRedoEvent
import com.paper.domain.event.UpdateColorTicketsEvent
import com.paper.domain.event.UpdateEditToolsEvent
import com.paper.domain.useCase.StartWidgetAutoStopObservable
import com.paper.domain.vm.IPaperCanvasWidget
import com.paper.domain.vm.PaperCanvasWidget
import com.paper.domain.vm.PaperHistoryWidget
import com.paper.domain.vm.PaperMenuWidget
import com.paper.model.IPaper
import com.paper.model.ICanvasOperationRepo
import com.paper.model.event.EventLifecycle
import com.paper.model.event.IntProgressEvent
import com.paper.model.repository.ICommonPenPrefsRepo
import com.paper.model.repository.IPaperRepo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class PaperEditorPresenter(private val paperID: Long,
                           private val paperRepo: IPaperRepo,
                           private val paperTransformRepo: ICanvasOperationRepo,
                           private val penPrefsRepo: ICommonPenPrefsRepo,
                           private val caughtErrorSignal: Observer<Throwable>,
                           private val schedulers: ISchedulerProvider)
    : IPresenter {

    private val mCanvasWidget by lazy {
        PaperCanvasWidget(schedulers = schedulers)
    }
    private val mHistoryWidget by lazy {
        PaperHistoryWidget(historyRepo = paperTransformRepo,
                           schedulers = schedulers)
    }

    private val mDisposables = CompositeDisposable()

    override fun bindView() {
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
        val historyWidgetReadySrc = paperSrc
            .flatMap {
                initHistoryWidget()
            }
        Observables
            .zip(canvasWidgetReadySrc,
                 historyWidgetReadySrc)
            .subscribe { (canvasWidgetReady, historyWidgetReady) ->
                if (canvasWidgetReady && historyWidgetReady) {
                    mOnCanvasWidgetReadySignal.onNext(mCanvasWidget)
                } else {
                    if (!canvasWidgetReady && historyWidgetReady) {
                        throw IllegalStateException("Fail to start paper model with paper widget")
                    } else if (canvasWidgetReady && !historyWidgetReady) {
                        throw IllegalStateException("Fail to start paper model with history widget")
                    } else {
                        throw IllegalStateException("Fail to both")
                    }
                }
            }
            .addTo(mDisposables)

        // Following are all about the edit panel outputs to paper widget:

        // Choose what drawing tool
//        mEditPanelWidget
//            .onUpdateEditToolList()
//            .observeOn(uiScheduler)
//            .subscribe { event ->
//                val toolID = event.toolIDs[event.usingIndex]
//
//                when (toolID) {
//                    ToolType.ERASER -> {
//                        mCanvasWidget.setDrawingMode(DrawingMode.ERASER)
//                    }
//                    ToolType.PEN -> {
//                        mCanvasWidget.setDrawingMode(DrawingMode.SKETCH)
//                    }
//                    else -> {
//                        println("${DomainConst.TAG}: Yet supported")
//                    }
//                }
//            }
//            .addTo(mDisposables)

        // Prepare initial tools and select the pen by default.
        val tools = getEditToolIDs()
        mToolIndex = tools.indexOf(ToolType.PEN)
        mEditingTools.onNext(UpdateEditToolsEvent(
            toolIDs = tools,
            usingIndex = mToolIndex))

        // Prepare initial color tickets
        Observables
            .combineLatest(
                penPrefsRepo.getPenColors(),
                penPrefsRepo.getChosenPenColor())
            .observeOn(schedulers.main())
            .subscribe { (colors, chosenColor) ->
                val index = colors.indexOf(chosenColor)

                // Export the signal as onUpdatePenColorList()
                mColorTicketsSignal.onNext(UpdateColorTicketsEvent(
                    colorTickets = colors,
                    usingIndex = index))
            }
            .addTo(mDisposables)

        // Prepare initial pen size
        penPrefsRepo.getPenSize()
            .observeOn(schedulers.main())
            .subscribe { penSize ->
                mPenSizeSignal.onNext(penSize)
            }
            .addTo(mDisposables)
    }

    override fun unBindView() {
        mDisposables.clear()
    }

    /**
     * Request to stop; It is granted to stop when receiving a true; vice versa.
     */
    fun requestStop(bmpFile: File, bmpWidth: Int, bmpHeight: Int): Observable<Boolean> {
        return try {
            mCanvasWidget.setThumbnail(bmpFile, bmpWidth, bmpHeight)
            Observable.just(true)
        } catch (err: Throwable) {
            Observable.just(false)
        }
    }

    private fun ensureNoLeakingSubscription() {
        if (mDisposables.size() > 0) throw IllegalStateException(
            "Already start to a widget")
    }

    // Initialization /////////////////////////////////////////////////////////

    private val mInitSignal = PublishSubject.create<InitEvent>().toSerialized()

    sealed class InitEvent {

        object OnStart

        /**
         * The set the signals returned by [start], where most of them provide the
         * widget which is ready for binding with view.
         */
        data class OnFinished(val canvasWidget: IPaperCanvasWidget,
                              val menuWidget: PaperMenuWidget) : InitEvent()
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

    private fun initCanvasWidget(paper: IPaper): Observable<Boolean> {
        mCanvasWidget.model = paper

        return StartWidgetAutoStopObservable(
            widget = mCanvasWidget,
            caughtErrorSignal = caughtErrorSignal)
            .subscribeOn(uiScheduler)
    }

    private val mOnCanvasWidgetReadySignal = BehaviorSubject.create<IPaperCanvasWidget>().toSerialized()

    fun onCanvasWidgetReady(): Observable<IPaperCanvasWidget> {
        return mOnCanvasWidgetReadySignal
    }

    fun eraseCanvas(): Observable<Boolean> {
        mHistoryWidget.eraseAll()
        mCanvasWidget.eraseCanvas()
        return Observable.just(true)
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    private fun initHistoryWidget(): Observable<Boolean> {
        return StartWidgetAutoStopObservable(
            widget = mHistoryWidget,
            caughtErrorSignal = caughtErrorSignal)
            .subscribeOn(uiScheduler)
    }

    fun handleOnClickUndoButton(undoSignal: Observable<Any>) {
        undoSignal
            .flatMap {
                mHistoryWidget
                    .undo()
                    .toObservable()
            }
            .subscribe()
            .addTo(mDisposables)
    }

    fun handleOnClickRedoButton(undoSignal: Observable<Any>) {
        undoSignal
            .flatMap {
                mHistoryWidget
                    .redo()
                    .toObservable()
            }
            .subscribe()
            .addTo(mDisposables)
    }

    fun onUpdateUndoRedoCapacity(): Observable<UndoRedoEvent> {
        return Observables.combineLatest(
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
    }

    // Edit tool //////////////////////////////////////////////////////////////

    private var mToolIndex = -1
    private val mEditingTools = BehaviorSubject.create<UpdateEditToolsEvent>()

    private val mUnsupportedToolMsg = PublishSubject.create<Any>()

    // TODO: Since the tool list (data) and UI click both lead to the
    // TODO: UI view-model change, merge this two upstream in the new
    // TODO: design!
    fun handleClickTool(toolID: ToolType) {
        val toolIDs = getEditToolIDs()
        val usingIndex = when (toolID) {
            ToolType.PEN,
            ToolType.ERASER -> {
                toolIDs.indexOf(toolID)
            }
            else -> {
                mUnsupportedToolMsg.onNext(0)
                toolIDs.indexOf(ToolType.PEN)
            }
        }

        mEditingTools.onNext(UpdateEditToolsEvent(
            toolIDs = toolIDs,
            usingIndex = usingIndex))
    }

    fun onUpdateEditToolList(): Observable<UpdateEditToolsEvent> {
        return mEditingTools
    }

    fun onChooseUnsupportedEditTool(): Observable<Any> {
        return mUnsupportedToolMsg
    }

    private fun getEditToolIDs(): List<ToolType> {
        return listOf(
            ToolType.ERASER,
            ToolType.PEN,
            ToolType.LASSO)
    }

    // Pen Color & size //////////////////////////////////////////////////////

    private val mColorTicketsSignal = BehaviorSubject.create<UpdateColorTicketsEvent>()

    override fun setPenColor(color: Int) {
        mCancelSignal.onNext(0)

        mDisposables.add(
            penPrefsRepo
                .putChosenPenColor(color)
                .toObservable()
                .takeUntil(mCancelSignal)
                .subscribe())
    }

    override fun onUpdatePenColorList(): Observable<UpdateColorTicketsEvent> {
        return mColorTicketsSignal
    }

    override fun setPenSize(size: Float) {
        return penSizeSrc
            .flatMap { event ->
                println("${DomainConst.TAG}: change pen size=${event.size}")

                when (event.lifecycle) {
                    EventLifecycle.START,
                    EventLifecycle.STOP -> {
                        Observable.never<Boolean>()
                    }
                    EventLifecycle.DOING -> {
                        penPrefsRepo
                            .putPenSize(event.size)
                            .toObservable()
                    }
                }
            }
            .subscribe()
            .addTo(mDisposables)
    }

    private val mPenSizeSignal = BehaviorSubject.create<Float>()

    /**
     * Update of pen size ranging from 0.0 to 1.0
     *
     * @return An observable of pen size ranging from 0.0 to 1.0
     */
    override fun onUpdatePenSize(): Observable<Float> {
        return mPenSizeSignal
    }

    // Progress & error & Editor status ///////////////////////////////////////

    private val mUpdateProgressSignal = PublishSubject.create<IntProgressEvent>()

    fun onUpdateProgress(): Observable<IntProgressEvent> {
        return mUpdateProgressSignal
    }
}
