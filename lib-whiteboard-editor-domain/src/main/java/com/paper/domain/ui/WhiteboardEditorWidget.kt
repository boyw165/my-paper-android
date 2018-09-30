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

import com.paper.model.repository.IWhiteboardStore
import com.paper.domain.ui.manipulator.EditorManipulator
import com.paper.domain.ui.manipulator.ScrapManipulator
import com.paper.model.IBundle
import com.paper.model.ISchedulers
import com.paper.model.event.IntProgressEvent
import com.paper.model.repository.ICommonPenPrefsRepo
import com.paper.model.repository.IUndoRepository
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import io.useful.delegate.rx.RxMutableSet
import io.useful.delegate.rx.RxValue
import io.useful.itemAdded
import io.useful.itemRemoved
import io.useful.rx.GestureEvent
import java.io.File

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

class WhiteboardEditorWidget(override val whiteboardWidget: IWhiteboardWidget,
                             override val undoRepo: IUndoRepository,
                             private val penPrefsRepo: ICommonPenPrefsRepo,
                             private val schedulers: ISchedulers)
    : IWhiteboardEditorWidget {

    private val lock = Any()

    private val staticDisposableBag = CompositeDisposable()

    override fun start() {
        setupScrapManipulatorInjection()
        setupUserTouch()
        setupUndoRedo()

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

        // Start widgets
        whiteboardWidget.start()
    }

    override fun stop() {
        whiteboardWidget.stop()

        staticDisposableBag.clear()
    }

    override fun saveStates(bundle: IBundle) {
        // DO NOTHING
    }

    override fun restoreStates(bundle: IBundle) {
        // DO NOTHING
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

    private fun setupScrapManipulatorInjection() {
        whiteboardWidget::scrapWidgets
            .itemAdded()
            .observeOn(schedulers.main())
            .subscribe { widget ->
                widget.userTouchManipulator = ScrapManipulator(
                    scrapWidget = widget,
                    editorWidget = this@WhiteboardEditorWidget)
            }
            .addTo(staticDisposableBag)
        whiteboardWidget::scrapWidgets
            .itemRemoved()
            .observeOn(schedulers.main())
            .subscribe { widget ->
                widget.userTouchManipulator = null
            }
            .addTo(staticDisposableBag)
    }

    private fun setupUserTouch() {
        userTouchInbox
            .observeOn(schedulers.main())
            .switchMapCompletable { gestureSequence ->
                EditorManipulator(editorWidget = this@WhiteboardEditorWidget)
                    .apply(gestureSequence)
            }
            .subscribe()
            .addTo(staticDisposableBag)
    }

    private fun setupUndoRedo() {
        undoInbox
            .observeOn(schedulers.main())
            .switchMap { undoDemand ->
                undoDemand.flatMap {
                    undoRepo.undo()
                        .toObservable()
                }
            }
            .observeOn(schedulers.main())
            .subscribe { command ->
                whiteboardWidget.whiteboardStore.offerCommandUndo(command)
            }
            .addTo(staticDisposableBag)
        redoInbox
            .observeOn(schedulers.main())
            .switchMap { redoDemand ->
                redoDemand.flatMap {
                    undoRepo.redo()
                        .toObservable()
                }
            }
            .observeOn(schedulers.main())
            .subscribe { command ->
                whiteboardWidget.whiteboardStore.offerCommandDoo(command)
            }
            .addTo(staticDisposableBag)
    }

    // Picker, magic-dot widgets /////////////////////////////////////////////

    override val pickerWidgets by RxMutableSet(mutableSetOf<IWidget>())

    override var transformWidget: IWidget by RxValue(Empty.MAGIC_DOT_WIDGET)

    // Touch /////////////////////////////////////////////////////////////////

    override val userTouchInbox: Subject<Observable<Observable<GestureEvent>>> by lazy {
        BehaviorSubject
            .create<Observable<Observable<GestureEvent>>>()
            .toSerialized()
    }

    // Busy ///////////////////////////////////////////////////////////////////

    override val busy: Observable<Boolean>
        get() {
            return Observable
                .combineLatest(listOf(whiteboardWidget.busy)) { busyArray: Array<in Boolean> ->
                    var overallBusy = false
                    busyArray.forEach { overallBusy = overallBusy || it as Boolean }
                    overallBusy
                }
                .observeOn(schedulers.main())
        }

    // Undo & undo ////////////////////////////////////////////////////////////

    override val undoInbox: Subject<Observable<Any>> by lazy {
        PublishSubject
            .create<Observable<Any>>()
            .toSerialized()
    }

    override val redoInbox: Subject<Observable<Any>> by lazy {
        PublishSubject
            .create<Observable<Any>>()
            .toSerialized()
    }

    override val canUndo: Observable<Boolean>
        get() = Observables.combineLatest(
            busy,
            undoRepo.canUndo)
            .map { (busy, canUndo) -> if (busy) false else canUndo }

    override val canRedo: Observable<Boolean>
        get() = Observables.combineLatest(
            busy,
            undoRepo.canRedo)
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
