// Copyright Mar 2018-present boyw165@gmail.com
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
import com.paper.domain.data.DrawingMode
import com.paper.domain.ui_event.*
import com.paper.model.IImageScrap
import com.paper.model.IPaper
import com.paper.model.ISVGScrap
import com.paper.model.ITextScrap
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class EditorWidget(protected val schedulers: ISchedulerProvider)
    : IEditorWidget {

    protected val lock = Any()

    protected var paper: IPaper? = null

    protected val staticDisposableBag = CompositeDisposable()
    protected val dynamicDisposableBag = ConcurrentHashMap<BaseScrapWidget, Disposable>()

    // Focus scrap controller
    @Volatile
    protected var focusScrapWidget: IBaseScrapWidget? = null
    // Scrap controllers
    protected val scrapWidgets = ConcurrentHashMap<UUID, BaseScrapWidget>()

    // Debug
    protected val debugSignal = PublishSubject.create<String>()

    override fun start(): Observable<Boolean> {
        return autoStop {
            // Mark initializing
            dirtyFlag.markDirty(EditorDirtyFlag.INITIALIZING_CANVAS)

            val paper = paper!!

            // Model add scrap
            paper.observeAddScrap()
                .subscribe { scrap ->
                    val widget = when (scrap) {
                        is ISVGScrap -> SVGScrapWidget(scrap = scrap,
                                                       schedulers = schedulers)
                        is IImageScrap -> TODO()
                        is ITextScrap -> TODO()
                        else -> TODO()
                    }
                    addWidget(widget)
                }
                .addTo(staticDisposableBag)
            // Model remove scrap
            paper.observeRemoveScrap()
                .subscribe { scrap ->
                    removeWidget(scrap.getID())
                }
                .addTo(staticDisposableBag)

            inflateScrapWidgets(paper)

            // Mark initialization done
            dirtyFlag.markNotDirty(EditorDirtyFlag.INITIALIZING_CANVAS)
        }
    }

    private fun inflateScrapWidgets(paper: IPaper) {
        val scraps = paper.getScraps()
        scraps.forEach { scrap ->
            val widget = when (scrap) {
                is ISVGScrap -> {
                    SVGScrapWidget(
                        scrap = scrap,
                        schedulers = schedulers)
                }
                // TODO
                is IImageScrap,
                is ITextScrap -> BaseScrapWidget(scrap = scrap,
                                                 schedulers = schedulers)
                else -> TODO()
            }

            addWidget(widget)
        }
    }

    override fun stop() {
        staticDisposableBag.clear()
    }

    override fun inject(paper: IPaper) {
        synchronized(lock) {
            if (this.paper != null) throw IllegalAccessException("Already set model")

            this.paper = paper
        }
    }

    override fun toPaper(): IPaper {
        synchronized(lock) {
            return paper!!
        }
    }

    override fun getCanvasSize(): Single<Pair<Float, Float>> {
        val paper = paper!!
        return Single.just(paper.getSize())
    }

    // Debug //////////////////////////////////////////////////////////////////

    override fun observeDebugMessage(): Observable<String> {
        return debugSignal
    }

    // Number of on-going task ////////////////////////////////////////////////

    protected val dirtyFlag = EditorDirtyFlag(EditorDirtyFlag.INITIALIZING_CANVAS)

    open fun onBusy(): Observable<Boolean> {
        return dirtyFlag
            .onUpdate()
            .map { event ->
                // Ready iff flag is not zero
                val busy = event.flag != 0

                println("${DomainConst.TAG}: canvas busy=$busy")

                busy
            }
    }

    // Scrap manipulation /////////////////////////////////////////////////////

    private val updateScrapSignal = PublishSubject.create<UpdateScrapEvent>().toSerialized()

    override fun observeScraps(): Observable<UpdateScrapEvent> {
        return updateScrapSignal
    }

    private fun addWidget(widget: BaseScrapWidget) {
        synchronized(lock) {
            scrapWidgets[widget.getID()] = widget

            // Start the widget
            dynamicDisposableBag[widget] = widget
                .start()
                .subscribe()

            // Signal out
            updateScrapSignal.onNext(AddScrapEvent(widget))
        }
    }

    private fun removeWidget(id: UUID) {
        synchronized(lock) {
            scrapWidgets[id]?.let { widget ->
                scrapWidgets.remove(widget.getID())

                // Stop the scrap
                dynamicDisposableBag[widget]?.dispose()
                dynamicDisposableBag.remove(widget)

                // Clear focus
                if (focusScrapWidget == widget) {
                    focusScrapWidget = null
                }

                // Signal out
                updateScrapSignal.onNext(RemoveScrapEvent(widget))
            }
        }
    }

    private fun removeAllScraps() {
        synchronized(lock) {
            // Prepare remove events
            val events = mutableListOf<RemoveScrapEvent>()
            scrapWidgets.forEach { (_, widget) ->
                events.add(RemoveScrapEvent(widget))
            }

            // Remove all
            scrapWidgets.clear()

            // Signal out
            updateScrapSignal.onNext(GroupUpdateScrapEvent(events))
        }
    }

    protected fun focusScrapWidget(id: UUID): IBaseScrapWidget? {
        synchronized(lock) {
            val widget = scrapWidgets[id]!!
            focusScrapWidget = widget

            // Signal out
            updateScrapSignal.onNext(FocusScrapEvent(widget.getID()))

            return focusScrapWidget
        }
    }

    // Drawing ////////////////////////////////////////////////////////////////

//    protected fun startSketch(x: Float, y: Float) {
//        synchronized(lock) {
//            // Mark drawing
//            dirtyFlag.markDirty(EditorDirtyFlag.OPERATING_CANVAS)
//
//            val widget = focusScrapWidget!! as SVGScrapWidget
//
//            // TODO: Determine style
//            widget.moveTo(x, y)
//        }
//    }
//
//    protected fun doSketch(x: Float, y: Float) {
//        synchronized(lock) {
//            val widget = focusScrapWidget!! as SVGScrapWidget
//            val frame = widget.getFrame()
//            val nx = x - frame.x
//            val ny = y - frame.y
//
//            // TODO: Use cubic line?
//            widget.cubicTo(nx, ny,
//                           nx, ny,
//                           nx, ny)
//        }
//    }
//
//    protected fun stopSketch() {
//        synchronized(lock) {
//            val widget = focusScrapWidget!! as SVGScrapWidget
//
//            widget.close()
//
//            // Mark drawing
//            dirtyFlag.markNotDirty(EditorDirtyFlag.OPERATING_CANVAS)
//        }
//    }

    /**
     * The current stroke color.
     */
    protected var mPenColor = 0x2C2F3C
    /**
     * The current stroke width, where the value is from 0.0 to 1.0.
     */
    protected var mPenSize = 0.2f
    /**
     * The current view-port scale.
     */
    protected var mViewPortScale = Float.NaN

    override fun setDrawingMode(mode: DrawingMode) {
        synchronized(lock) {
            TODO()
        }
    }

    override fun setChosenPenColor(color: Int) {
        synchronized(lock) {
            mPenColor = color
        }
    }

    override fun setViewPortScale(scale: Float) {
        synchronized(lock) {
            mViewPortScale = scale
        }
    }

    override fun setPenSize(size: Float) {
        synchronized(lock) {
            mPenSize = size
        }
    }

    // Equality ///////////////////////////////////////////////////////////////

    override fun toString(): String {
        return paper?.let { paper ->
            "${javaClass.simpleName}{\n" +
            "id=${paper.getID()}, uuid=${paper.getUUID()}\n" +
            "scraps=${paper.getScraps().size}\n" +
            "}"
        } ?: "${javaClass.simpleName}{no model}"
    }
}
