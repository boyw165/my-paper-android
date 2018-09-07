// Copyright Aug 2018-present Paper
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

package com.paper.domain.ui.manipulator

import com.cardinalblue.gesture.rx.DragBeginEvent
import com.cardinalblue.gesture.rx.DragDoingEvent
import com.cardinalblue.gesture.rx.DragEndEvent
import com.cardinalblue.gesture.rx.GestureEvent
import com.paper.domain.ui.SVGScrapWidget
import com.paper.domain.ui.WhiteboardWidget
import com.paper.domain.ui_event.AddScrapEvent
import com.paper.model.*
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Single
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.addTo
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class SketchManipulator(private val editor: WhiteboardWidget,
                        private val paper: Single<Whiteboard>,
                        private val highestZ: Int,
                        private val schedulers: ISchedulers)
    : BaseManipulator() {

    override fun apply(touchSequence: Observable<GestureEvent>): ObservableSource<WhiteboardCommand> {
        return autoStop { emitter ->
            disposableBag.clear()

            // TODO: 1. Add widget to whiteboard widget
            // TODO: 2. Add point to widget sketch displacement
            // TODO: 3. Produce add-scrap-command

            val id = UUID.randomUUID()
            // Observe widget creation
            val widgetSignal = editor.observeScraps()
                .filter { event ->
                    event is AddScrapEvent &&
                    event.scrapWidget.getID() == id
                }
                .firstElement()
                .map { event ->
                    event as AddScrapEvent
                    event.scrapWidget as SVGScrapWidget
                }
                .cache()
            val startX = AtomicReference(0f)
            val startY = AtomicReference(0f)

            // First touch
            val firstTouch = touchSequence.firstOrError()

            // Begin, add scrap and create corresponding widget
            Singles.zip(paper,
                        firstTouch)
                .observeOn(schedulers.main())
                .subscribe { (paper, event) ->
                    if (!(event is DragBeginEvent ||
                          event is DragDoingEvent)) {
                        emitter.onComplete()
                    }

                    event as DragBeginEvent

                    val (x, y) = event.startPointer
                    val scrap = createSVGScrap(id, x, y)

                    // Remember start x-y for later point correction
                    startX.set(x)
                    startY.set(y)

                    paper.addScrap(scrap)
                }
                .addTo(disposableBag)

            // Normalized touch sequence
            val nTouchSequence = touchSequence.map { event ->
                when (event) {
                    is DragBeginEvent -> {
                        val (x, y) = event.startPointer
                        val nx = x - startX.get()
                        val ny = y - startY.get()

                        Point(nx, ny)
                    }
                    is DragDoingEvent -> {
                        val (x, y) = event.stopPointer
                        val nx = x - startX.get()
                        val ny = y - startY.get()

                        Point(nx, ny)
                    }
                    is DragEndEvent -> {
                        val (x, y) = event.stopPointer
                        val nx = x - startX.get()
                        val ny = y - startY.get()

                        Point(nx, ny)
                    }
                    else -> TODO()
                }
            }

            Observables.combineLatest(widgetSignal.toObservable(),
                                      nTouchSequence)
                .observeOn(schedulers.main())
                .subscribe { (widget, p) ->
                    widget.addPointToSketchDisplacement(p)

                    // Produce command
                    // TODO
                }
                .addTo(disposableBag)
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
                                      z = highestZ + 1))
    }
}
