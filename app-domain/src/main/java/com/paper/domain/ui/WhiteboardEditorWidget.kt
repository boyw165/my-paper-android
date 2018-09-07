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

import com.cardinalblue.gesture.rx.DragBeginEvent
import com.cardinalblue.gesture.rx.DragDoingEvent
import com.cardinalblue.gesture.rx.DragEndEvent
import com.cardinalblue.gesture.rx.GestureObservable
import com.paper.model.ISchedulers
import com.paper.domain.ui.manipulator.SketchManipulator
import com.paper.domain.ui_event.AddScrapEvent
import com.paper.domain.ui_event.UndoAvailabilityEvent
import com.paper.model.Frame
import com.paper.model.Point
import com.paper.model.SVGScrap
import com.paper.model.event.IntProgressEvent
import com.paper.model.repository.ICommonPenPrefsRepo
import com.paper.model.repository.IWhiteboardRepository
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Maybes
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicReference

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class WhiteboardEditorWidget(paperID: Long,
                             paperRepo: IWhiteboardRepository,
                             private val undoWidget: UndoManager,
                             private val penPrefsRepo: ICommonPenPrefsRepo,
                             caughtErrorSignal: Observer<Throwable>,
                             schedulers: ISchedulers)
    : WhiteboardWidget(paperID = paperID,
                       paperRepo = paperRepo,
                       caughtErrorSignal = caughtErrorSignal,
                       schedulers = schedulers) {

    private val drawingDisposableBag = CompositeDisposable()

    override fun start(): Observable<Boolean> {
        return super.start()
            .flatMap { done ->
                if (done) {
                    paper.observeOn(schedulers.main())
                        .flatMapObservable {
                            undoWidget.start()
                        }
                        .subscribe()
                        .addTo(staticDisposableBag)

                    // Following are all about the edit panel outputs to paper widget:

                    // Choose what drawing tool
                    //        mEditPanelWidget
                    //            .onUpdateEditToolList()
                    //            .observeOn(uiScheduler)
                    //            .subscribe { event ->
                    //                val toolID = event.mode[event.usingIndex]
                    //
                    //                when (toolID) {
                    //                    EditorMode.SELECT_TO_DELETE -> {
                    //                        mCanvasWidget.setDrawingMode(DrawingMode.SELECT_TO_DELETE)
                    //                    }
                    //                    EditorMode.FREE_DRAWING -> {
                    //                        mCanvasWidget.setDrawingMode(DrawingMode.SKETCH)
                    //                    }
                    //                    else -> {
                    //                        println("${DomainConst.TAG}: Yet supported")
                    //                    }
                    //                }
                    //            }
                    //            .addTo(staticDisposableBag)

                    // Prepare initial tools and select the pen by default.

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

    override fun stop() {
        super.stop()
        drawingDisposableBag.clear()
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

    override fun observeBusy(): Observable<Boolean> {
        return Observables
            .combineLatest(super.observeBusy(),
                           undoWidget.observeBusy())
            .map { (selfBusy, undoBusy) ->
                selfBusy && undoBusy
            }
    }

    private fun createSVGScrap(id: UUID,
                               x: Float,
                               y: Float): SVGScrap {
        return SVGScrap(uuid = id,
                        frame = Frame(x = x,
                                      y = y,
                                      scaleX = 1f,
                                      scaleY = 1f,
                                      width = 1f,
                                      height = 1f,
                                      z = highestZ.get() + 1))
    }

    // Free drawing ///////////////////////////////////////////////////////////

    fun handleTouch(touchSrc: Observable<GestureObservable>) {
        Observables
            .combineLatest(editorModelSignal,
                           touchSrc)
            .observeOn(schedulers.main())
//            .subscribe { (mode, gesture) ->
//                // Mode determines what to do next
//                when (mode) {
//                    EditorMode.FREE_DRAWING -> {
//                        handleFreeDrawing(gesture)
//                    }
//                    else -> TODO()
//                }
//            }
            .flatMap { (mode, gesture) ->
                val manipulator = when (mode) {
                    EditorMode.FREE_DRAWING -> SketchManipulator(
                        editor = this@WhiteboardEditorWidget,
                        paper = this.paper,
                        highestZ = highestZ.get(),
                        schedulers = schedulers)
                    else -> TODO()
                }
                gesture.compose(manipulator)
            }
            .subscribe { operation ->
                undoWidget.putOperation(operation)
            }
            .addTo(staticDisposableBag)
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    fun handleUndo(undoSignal: Observable<Any>) {
        Observables
            .combineLatest(paper.toObservable(),
                           undoSignal)
            .flatMap { (paper, _) ->
                undoWidget.undo(paper)
                    .toObservable<Any>()
            }
            .subscribe()
            .addTo(staticDisposableBag)
    }

    fun handleRedo(redoSignal: Observable<Any>) {
        Observables
            .combineLatest(paper.toObservable(),
                           redoSignal)
            .flatMap { (paper, _) ->
                undoWidget.redo(paper)
                    .toObservable<Any>()
            }
            .subscribe()
            .addTo(staticDisposableBag)
    }

    fun observeUndoAvailability(): Observable<UndoAvailabilityEvent> {
        return Observables.combineLatest(
            observeBusy(),
            undoWidget.observeUndoCapacity())
            .map { (busy, event) ->
                if (busy) {
                    UndoAvailabilityEvent(canUndo = false,
                                          canRedo = false)
                } else {
                    event
                }
            }
    }

    // Editor mode ////////////////////////////////////////////////////////////

    private val editorModelSignal = BehaviorSubject.createDefault(EditorMode.FREE_DRAWING)

    fun handleChangeEditorMode(modeSignal: Observable<EditorMode>) {
        modeSignal
            .observeOn(schedulers.main())
            .subscribe { next ->
                val now = editorModelSignal.value

                if (next != now) {
                    editorModelSignal.onNext(next)
                }
            }
            .addTo(staticDisposableBag)
    }

    fun observeEditorMode(): Observable<EditorMode> {
        return editorModelSignal
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

    private val progressSignal = PublishSubject.create<IntProgressEvent>()

    fun onUpdateProgress(): Observable<IntProgressEvent> {
        return progressSignal
    }
}
