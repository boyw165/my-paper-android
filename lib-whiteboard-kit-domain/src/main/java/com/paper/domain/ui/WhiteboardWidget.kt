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

import co.sodalabs.delegate.rx.RxMutableSet
import co.sodalabs.delegate.rx.itemAdded
import co.sodalabs.delegate.rx.itemRemoved
import com.paper.domain.DomainConst
import com.paper.domain.store.IWhiteboardStore
import com.paper.model.IBundle
import com.paper.model.ISchedulers
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

open class WhiteboardWidget(override val whiteboardStore: IWhiteboardStore,
                            protected val schedulers: ISchedulers)
    : IWhiteboardWidget {

    private val lock = Any()

    private val staticDisposableBag = CompositeDisposable()
    private val dynamicDisposableBag = ConcurrentHashMap<UUID, CompositeDisposable>()

    // Focus scrap controller
    @Volatile
    protected var focusScrapWidget: ScrapWidget? = null

    // widgets
    override val scrapWidgets: MutableSet<ScrapWidget> by RxMutableSet(mutableSetOf())
    override var highestZ: Int = 0

    override fun start() {
        // Watch scrap widget added and removed, then do start and stop accordingly
        this::scrapWidgets
            .itemAdded()
            .subscribeOn(schedulers.main())
            .subscribe { scrapWidget ->
                startScrapWidget(scrapWidget)
            }
        this::scrapWidgets
            .itemRemoved()
            .subscribeOn(schedulers.main())
            .subscribe { scrapWidget ->
                stopScrapWidget(scrapWidget)
            }

        // First widget inflation
        whiteboardStore
            .whiteboard
            .observeOn(schedulers.main())
            .subscribe { whiteboard ->
                // Mark initializing
                dirtyFlag.markDirty(WhiteboardDirtyFlag.INITIALIZING)

                whiteboard.getScraps()
                    .forEach { scrap ->
                        val widget = ScrapWidgetFactory.createScrapWidget(
                            scrap,
                            schedulers)

                        // Update z
                        val z = widget.getFrame().z
                        if (z > highestZ) {
                            highestZ = z
                        }

                        scrapWidgets.add(widget)
                    }

                // Mark initialization done
                dirtyFlag.markNotDirty(WhiteboardDirtyFlag.INITIALIZING)
            }
            .addTo(staticDisposableBag)

        // Observe add scrap
        whiteboardStore
            .whiteboard
            .observeOn(schedulers.main())
            .flatMapObservable { document ->
                document.scrapAdded()
            }
            .subscribe { scrap ->
                val found = scrapWidgets.firstOrNull { it.getID() == scrap.getID() }
                if (found != null) return@subscribe

                val widget = ScrapWidgetFactory.createScrapWidget(
                    scrap,
                    schedulers)
                scrapWidgets.add(widget)
            }
            .addTo(staticDisposableBag)
        // Observe remove scrap
        whiteboardStore
            .whiteboard
            .observeOn(schedulers.main())
            .flatMapObservable { document ->
                document.scrapRemoved()
            }
            .subscribe { scrap ->
                val widget = scrapWidgets.firstOrNull { it.getID() == scrap.getID() }
                widget?.let { scrapWidgets.remove(it) }
            }
            .addTo(staticDisposableBag)

        whiteboardStore.start()

        println("${DomainConst.TAG}: Start \"${javaClass.simpleName}\"")
    }

    override fun stop() {
        whiteboardStore.stop()

        staticDisposableBag.clear()

        println("${DomainConst.TAG}: Stop \"${javaClass.simpleName}\"")
    }

    override fun saveStates(bundle: IBundle) {
        // DO NOTHING
    }

    override fun restoreStates(bundle: IBundle) {
        // DO NOTHING
    }

    val canvasSize: Single<Pair<Float, Float>> get() {
        return whiteboardStore
            .whiteboard
            .map { it.getSize() }
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val childBusyCount = AtomicInteger(0)
    private val dirtyFlag = WhiteboardDirtyFlag(WhiteboardDirtyFlag.INITIALIZING)

    override val busy: Observable<Boolean> get() {
        return Observable
            .combineLatest(listOf(selfBusy,
                                  whiteboardStore.busy)
                          ) { busyArray: Array<in Boolean> ->
                var overallBusy = false
                busyArray.forEach { overallBusy = overallBusy || it as Boolean }
                overallBusy
            }
            .observeOn(schedulers.ui())
    }

    private val selfBusy: Observable<Boolean> get() {
        return dirtyFlag
            .onUpdate()
            .observeOn(schedulers.main())
            .map { event ->
                // Detect any busy child
                when (event.changedType) {
                    WhiteboardDirtyFlag.CHILD_IS_BUSY -> {
                        val childBusy = event.flag.and(WhiteboardDirtyFlag.CHILD_IS_BUSY) == 1
                        if (childBusy) {
                            childBusyCount.incrementAndGet()
                        } else {
                            // Most of the child is initially NOT busy, therefore
                            // we need to constraint the count low to zero.
                            if (childBusyCount.decrementAndGet() < 0) {
                                childBusyCount.set(0)
                            }
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

    private fun startScrapWidget(scrapWidget: ScrapWidget) {
        val widgetDisposableBag = CompositeDisposable()

        // Observe the widget busy state
        scrapWidget.busy
            .subscribe { busy ->
                if (busy) {
                    dirtyFlag.markDirty(WhiteboardDirtyFlag.CHILD_IS_BUSY)
                } else {
                    dirtyFlag.markNotDirty(WhiteboardDirtyFlag.CHILD_IS_BUSY)
                }
            }
            .addTo(widgetDisposableBag)

        // Start the widget
        scrapWidget.start()

        // TODO: Hold widget disposable
        dynamicDisposableBag[scrapWidget.getID()] = widgetDisposableBag
    }

    private fun stopScrapWidget(scrapWidget: ScrapWidget) {
        val widget = synchronized(lock) {
            scrapWidgets.firstOrNull { it.getID() == scrapWidget.getID() }
        }

        widget?.let {
            val id = it.getID()
            dynamicDisposableBag[id]?.dispose()
            dynamicDisposableBag.remove(id)

            // Stop the scrap
            it.stop()

            // Clear focus
            if (focusScrapWidget == it) {
                focusScrapWidget = null
            }
        }
    }
}
