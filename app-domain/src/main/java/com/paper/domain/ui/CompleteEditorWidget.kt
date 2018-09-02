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

import com.paper.domain.ISchedulerProvider
import com.paper.domain.data.ToolType
import com.paper.domain.ui_event.UndoRedoAvailabilityEvent
import com.paper.domain.ui_event.UpdateEditToolsEvent
import com.paper.model.event.IntProgressEvent
import com.paper.model.repository.ICommonPenPrefsRepo
import com.paper.model.repository.IPaperRepo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class CompleteEditorWidget(paperID: Long,
                           paperRepo: IPaperRepo,
                           private val lruCacheDir: File,
                           private val penPrefsRepo: ICommonPenPrefsRepo,
                           caughtErrorSignal: Observer<Throwable>,
                           schedulers: ISchedulerProvider)
    : SimpleEditorWidget(paperID = paperID,
                         paperRepo = paperRepo,
                         caughtErrorSignal = caughtErrorSignal,
                         schedulers = schedulers) {

    private val undoWidget by lazy {
        UndoRepository(fileDir = lruCacheDir,
                       schedulers = schedulers)
    }

    override fun start(): Observable<Boolean> {
        return super.start()
            .flatMap { done ->
                if (done) {
                    initUndoWidget()
                        .subscribe()
                        .addTo(staticDisposableBag)

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
                    //            .addTo(staticDisposableBag)

                    // Prepare initial tools and select the pen by default.
                    val tools = getEditToolIDs()
                    mToolIndex = tools.indexOf(ToolType.PEN)
                    mEditingTools.onNext(UpdateEditToolsEvent(
                        toolIDs = tools,
                        usingIndex = mToolIndex))

                    // Prepare initial color tickets
                    //        Observables
                    //            .combineLatest(
                    //                penPrefsRepo.getPenColors(),
                    //                penPrefsRepo.getChosenPenColor())
                    //            .observeOn(schedulers.main())
                    //            .subscribe { (colors, chosenColor) ->
                    //                val index = colors.indexOf(chosenColor)
                    //
                    //                // Export the signal as onUpdatePenColorList()
                    //                mColorTicketsSignal.onNext(UpdateColorTicketsEvent(
                    //                    colorTickets = colors,
                    //                    usingIndex = index))
                    //            }
                    //            .addTo(staticDisposableBag)

                    // Prepare initial pen size
                    //        penPrefsRepo.getPenSize()
                    //            .observeOn(schedulers.main())
                    //            .subscribe { penSize ->
                    //                mPenSizeSignal.onNext(penSize)
                    //            }
                    //            .addTo(staticDisposableBag)

                    Observable.just(true)
                } else {
                    Observable.empty<Boolean>()
                }
            }
    }

    /**
     * Request to stop; It is granted to stop when receiving a true; vice versa.
     */
    fun requestStop(bmpFile: File,
                    bmpWidth: Int,
                    bmpHeight: Int): Observable<Boolean> {
        return try {
            //            mCanvasWidget.setThumbnail(bmpFile, bmpWidth, bmpHeight)
            Observable.just(true)
        } catch (err: Throwable) {
            Observable.just(false)
        }
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    private fun initUndoWidget(): Observable<Boolean> {
        return paper
            .flatMapObservable { paper ->
                undoWidget.inject(paper)
                undoWidget.start()
            }
    }

    fun handleOnClickUndoButton(undoSignal: Observable<Any>) {
        Observables
            .combineLatest(paper.toObservable(),
                           undoSignal)
            .flatMap { (paper, _) ->
                undoWidget
                    .undo(paper)
                    .toObservable()
            }
            .subscribe()
            .addTo(staticDisposableBag)
    }

    fun handleOnClickRedoButton(undoSignal: Observable<Any>) {
        Observables
            .combineLatest(paper.toObservable(),
                           undoSignal)
            .flatMap { (paper, _) ->
                undoWidget
                    .redo(paper)
                    .toObservable()
            }
            .subscribe()
            .addTo(staticDisposableBag)
    }

    fun onUpdateUndoRedoCapacity(): Observable<UndoRedoAvailabilityEvent> {
        return Observables.combineLatest(
            onBusy(),
            undoWidget.onUpdateUndoRedoCapacity())
            .map { (busy, event) ->
                if (busy) {
                    UndoRedoAvailabilityEvent(canUndo = false,
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

    /**
     * The current stroke color.
     */
    protected var penColor = 0x2C2F3C
    /**
     * The current stroke width, where the value is from 0.0 to 1.0.
     */
    protected var mPenSize = 0.2f
    /**
     * The current view-port scale.
     */
    protected var mViewPortScale = Float.NaN

    fun setChosenPenColor(color: Int) {
        synchronized(lock) {
            penColor = color
        }
    }

    fun setViewPortScale(scale: Float) {
        synchronized(lock) {
            mViewPortScale = scale
        }
    }

    fun setPenSize(size: Float) {
        synchronized(lock) {
            mPenSize = size
        }
    }

    //    private val mColorTicketsSignal = BehaviorSubject.create<UpdateColorTicketsEvent>()
    //
    //    override fun setPenColor(color: Int) {
    //        cancelSignal.onNext(0)
    //
    //        staticDisposableBag.add(
    //            penPrefsRepo
    //                .putChosenPenColor(color)
    //                .toObservable()
    //                .takeUntil(cancelSignal)
    //                .subscribe())
    //    }
    //
    //    override fun onUpdatePenColorList(): Observable<UpdateColorTicketsEvent> {
    //        return mColorTicketsSignal
    //    }
    //
    //    override fun setPenSize(size: Float) {
    //        return penSizeSrc
    //            .flatMap { event ->
    //                println("${DomainConst.TAG}: change pen size=${event.size}")
    //
    //                when (event.lifecycle) {
    //                    EventLifecycle.START,
    //                    EventLifecycle.STOP -> {
    //                        Observable.never<Boolean>()
    //                    }
    //                    EventLifecycle.DOING -> {
    //                        penPrefsRepo
    //                            .putPenSize(event.size)
    //                            .toObservable()
    //                    }
    //                }
    //            }
    //            .subscribe()
    //            .addTo(staticDisposableBag)
    //    }
    //
    //    private val mPenSizeSignal = BehaviorSubject.create<Float>()
    //
    //    /**
    //     * Update of pen size ranging from 0.0 to 1.0
    //     *
    //     * @return An observable of pen size ranging from 0.0 to 1.0
    //     */
    //    override fun onUpdatePenSize(): Observable<Float> {
    //        return mPenSizeSignal
    //    }

    // Progress & error & Editor status ///////////////////////////////////////

    private val mUpdateProgressSignal = PublishSubject.create<IntProgressEvent>()

    fun onUpdateProgress(): Observable<IntProgressEvent> {
        return mUpdateProgressSignal
    }
}
