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
import com.paper.model.ISchedulers
import com.paper.domain.ui_event.*
import com.paper.model.*
import com.paper.model.repository.IPaperRepo
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

open class WhiteboardWidget(protected val paperID: Long,
                            protected val paperRepo: IPaperRepo,
                            protected val caughtErrorSignal: Observer<Throwable>,
                            protected val schedulers: ISchedulers)
    : IWidget {

    protected val lock = Any()

    protected val penStyleSignal = BehaviorSubject.createDefault(VectorGraphics.DEFAULT_STYLE).toSerialized()

    protected val staticDisposableBag = CompositeDisposable()
    protected val dynamicDisposableBag = ConcurrentHashMap<BaseScrapWidget, CompositeDisposable>()

    // Focus scrap controller
    @Volatile
    protected var focusScrapWidget: BaseScrapWidget? = null

    // widgets
    protected val scrapWidgets = ConcurrentHashMap<UUID, BaseScrapWidget>()
    protected val highestZ = AtomicInteger(0)

    override fun start(): Observable<Boolean> {
        return autoStop {
                // First widget inflation
                paper.observeOn(schedulers.main())
                    .subscribe { paper ->
                        // Mark initializing
                        dirtyFlag.markDirty(EditorDirtyFlag.INITIALIZING)

                        paper.getScraps()
                            .forEach { scrap ->
                                createScrapWidget(scrap)
                            }

                        // Mark initialization done
                        dirtyFlag.markNotDirty(EditorDirtyFlag.INITIALIZING)
                    }
                    .addTo(staticDisposableBag)

                // Observe add scrap
                paper.observeOn(schedulers.main())
                    .flatMapObservable { paper ->
                        paper.observeAddScrap()
                    }
                    .subscribe { scrap ->
                        createScrapWidget(scrap)
                    }
                    .addTo(staticDisposableBag)
                // Observe remove scrap
                paper.observeOn(schedulers.main())
                    .flatMapObservable { paper ->
                        paper.observeRemoveScrap()
                    }
                    .subscribe { scrap ->
                        removeWidget(scrap.getID())
                    }
                    .addTo(staticDisposableBag)

            println("${DomainConst.TAG}: Start \"${javaClass.simpleName}\"")
        }
    }

    override fun stop() {
        staticDisposableBag.clear()
        println("${DomainConst.TAG}: Stop \"${javaClass.simpleName}\"")
    }

    protected val paper: Single<IPaper> by lazy {
        paperRepo
            .getPaperById(paperID)
            .doOnSubscribe {
                dirtyFlag.markDirty(EditorDirtyFlag.READ_PAPER_FROM_REPO)
            }
            .doOnSuccess {
                dirtyFlag.markNotDirty(EditorDirtyFlag.READ_PAPER_FROM_REPO)
            }
            .cache()
    }

    fun observeCanvasSize(): Single<Pair<Float, Float>> {
        return paper.map { it.getSize() }
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val childBusyCount = AtomicInteger(0)
    private val dirtyFlag = EditorDirtyFlag(EditorDirtyFlag.INITIALIZING)

    open fun observeBusy(): Observable<Boolean> {
        return dirtyFlag
            .onUpdate()
            .map { event ->
                // Detect any busy child
                when (event.changedType) {
                    EditorDirtyFlag.CHILD_IS_BUSY -> {
                        val childBusy = event.flag.and(EditorDirtyFlag.CHILD_IS_BUSY) == 1
                        if (childBusy) {
                            childBusyCount.incrementAndGet()
                        } else {
                            childBusyCount.decrementAndGet()
                        }
                    }
                }

                // Ready iff flag is not zero
                val busy = event.flag != 0 && childBusyCount.get() == 0

                println("${DomainConst.TAG}: editor busy=$busy")

                busy
            }
    }

    // Scrap manipulation /////////////////////////////////////////////////////

    private val updateScrapSignal = PublishSubject.create<UpdateScrapEvent>().toSerialized()

    fun observeScraps(): Observable<UpdateScrapEvent> {
        return updateScrapSignal
    }

    private fun createScrapWidget(scrap: BaseScrap) {
        val widget = when (scrap) {
            is SVGScrap -> {
                SVGScrapWidget(
                    scrap = scrap,
                    newSVGPenStyle = penStyleSignal,
                    schedulers = schedulers)
            }
            is ImageScrap -> {
                ImageScrapWidget(
                    scrap = scrap,
                    schedulers = schedulers)
            }
            is TextScrap -> {
                TextScrapWidget(
                    scrap = scrap,
                    schedulers = schedulers)
            }
            else -> TODO()
        }

        // Update z
        val z = widget.getFrame().z
        if (z > highestZ.get()) {
            highestZ.set(z)
        }

        addWidget(widget)
    }

    private fun addWidget(widget: BaseScrapWidget) {
        synchronized(lock) {
            scrapWidgets[widget.getID()] = widget

            val widgetDisposableBag = CompositeDisposable()

            // Observe the widget busy state
            widget.observeBusy()
                .subscribe { busy ->
                    if (busy) {
                        dirtyFlag.markDirty(EditorDirtyFlag.CHILD_IS_BUSY)
                    } else {
                        dirtyFlag.markNotDirty(EditorDirtyFlag.CHILD_IS_BUSY)
                    }
                }
                .addTo(widgetDisposableBag)
            // Start the widget
            widget.start()
                .subscribe()
                .addTo(widgetDisposableBag)
            dynamicDisposableBag[widget] = widgetDisposableBag

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
}
