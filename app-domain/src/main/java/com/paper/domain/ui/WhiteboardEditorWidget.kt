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

import com.cardinalblue.gesture.rx.GestureObservable
import com.paper.model.ISchedulers
import com.paper.model.event.IntProgressEvent
import com.paper.model.repository.ICommonPenPrefsRepo
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class WhiteboardEditorWidget(private val whiteboardWidget: IWhiteboardWidget,
                             private val undoWidget: IUndoWidget,
                             private val penPrefsRepo: ICommonPenPrefsRepo,
                             private val schedulers: ISchedulers)
    : IWhiteboardEditorWidget {

    private val lock = Any()

    private val drawingDisposableBag = CompositeDisposable()
    private val staticDisposableBag = CompositeDisposable()

    override fun start() {
        whiteboardWidget.start()
        undoWidget.start()

        // Following are all about the edit panel outputs to whiteboard widget:

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
    }

    override fun stop() {
        whiteboardWidget.stop()
        undoWidget.stop()

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

    override val busy: Observable<Boolean> get() {
        return Observables
            .combineLatest(whiteboardWidget.busy,
                           undoWidget.busy)
            .map { (selfBusy, undoBusy) ->
                selfBusy && undoBusy
            }
    }

    // Free drawing ///////////////////////////////////////////////////////////

    override fun handleTouch(gestureSequence: Observable<GestureObservable>) {
//        Observables
//            .combineLatest(editorModelSignal,
//                           gestureSequence)
//            .observeOn(schedulers.main())
//            .flatMapCompletable { (mode, touchSequence) ->
//                when (mode) {
//                    EditorMode.FREE_DRAWING -> {
//                        SketchManipulator(editor = this@WhiteboardEditorWidget,
//                                          highestZ = highestZ.get(),
//                                          whiteboardStore = whiteboardStore,
//                                          undoWidget = undoWidget,
//                                          schedulers = schedulers)
//                            .apply(touchSequence)
//                    }
//                    else -> TODO()
//                }
//            }
//            .subscribe()
//            .addTo(staticDisposableBag)
    }

    // Undo & undo ////////////////////////////////////////////////////////////

    override fun handleUndo(undoSignal: Observable<Any>) {
        undoSignal
            .flatMap {
                undoWidget.undo()
                    .toObservable()
            }
            .subscribe { command ->
                whiteboardWidget
                    .whiteboardStore
                    .offerCommandUndo(command)
            }
            .addTo(staticDisposableBag)
    }

    override fun handleRedo(redoSignal: Observable<Any>) {
        redoSignal
            .flatMap {
                undoWidget.redo()
                    .toObservable()
            }
            .subscribe { command ->
                whiteboardWidget
                    .whiteboardStore
                    .offerCommandDoo(command)
            }
            .addTo(staticDisposableBag)
    }

    override val canUndo: Observable<Boolean>
        get() = Observables.combineLatest(
            busy,
            undoWidget.canUndo)
            .map { (busy, canUndo) -> if (busy) false else canUndo }

    override val canRedo: Observable<Boolean>
        get() = Observables.combineLatest(
            busy,
            undoWidget.canRedo)
            .map { (busy, canUndo) -> if (busy) false else canUndo }

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
