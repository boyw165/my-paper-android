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
import com.paper.domain.ui.IWhiteboardWidget
import com.paper.domain.ui.ScrapWidgetFactory
import com.paper.domain.ui.SketchScrapWidget
import com.paper.model.*
import com.paper.model.command.AddScrapCommand
import com.paper.model.command.WhiteboardCommand
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import java.util.*

class SketchManipulator(private val whiteboardWidget: IWhiteboardWidget,
                        private val highestZ: Int,
                        private val schedulers: ISchedulers)
    : IUserTouchCommandOutManipulator {

    @Volatile
    private lateinit var scrap: SketchScrap
    @Volatile
    private lateinit var scrapWidget: SketchScrapWidget

    @Volatile
    private lateinit var svgDisplacement: VectorGraphics
    @Volatile
    private lateinit var startPoint: Point
    @Volatile
    private lateinit var lastPoint: Point

    override fun apply(touchSequence: Observable<GestureEvent>): Maybe<WhiteboardCommand> {
        return Maybe.create { emitter ->
            val sharedTouchSequence = touchSequence.publish()
            val disposableBag = CompositeDisposable()

            emitter.setCancellable { disposableBag.dispose() }

            sharedTouchSequence
                .firstElement()
                .observeOn(schedulers.main())
                .subscribe { event ->
                    if (!(event is DragBeginEvent ||
                          event is DragDoingEvent)) {
                        emitter.onComplete()
                    }

                    event as DragBeginEvent

                    startPoint = Point(event.startPointer.first,
                                       event.startPointer.second)
                    lastPoint = startPoint

                    val (s, w) = createSketchScrapWidget(event.startPointer.first,
                                                         event.startPointer.second)
                    scrap = s
                    scrapWidget = w
                    // Mark widget busy
                    scrapWidget.markBusy()

                    // Sketch displacement
                    svgDisplacement = w.getSVG()

                    // Most importantly, add widget
                    whiteboardWidget.addWidget(scrapWidget)
                }
                .addTo(disposableBag)

            sharedTouchSequence
                .skip(1)
                .observeOn(schedulers.main())
                .subscribe { event ->
                    val p = when (event) {
                        is DragDoingEvent -> {
                            val (x, y) = event.stopPointer
                            val nx = x - startPoint.x
                            val ny = y - startPoint.y

                            Point(nx, ny)
                        }
                        is DragEndEvent -> {
                            val (x, y) = event.stopPointer
                            val nx = x - startPoint.x
                            val ny = y - startPoint.y

                            Point(nx, ny)
                        }
                        else -> TODO()
                    }
                    val tupleCopy = svgDisplacement.getTupleList()
                        .toMutableList()
                    tupleCopy.add(CubicPointTuple(prevControlX = p.x,
                                                  prevControlY = p.y,
                                                  currentControlX = lastPoint.x,
                                                  currentControlY = lastPoint.y,
                                                  currentEndX = p.x,
                                                  currentEndY = p.y))
                    val newDisplacement = svgDisplacement.copy(tupleList = tupleCopy)
                    svgDisplacement = newDisplacement

                    scrapWidget.setDisplacement(newDisplacement)
                }
                .addTo(disposableBag)

            Completable.fromObservable(sharedTouchSequence)
                .observeOn(schedulers.main())
                .subscribe {
                    // Prepare command with updated MODEL
                    scrap.setSVG(svgDisplacement)

                    // Mark widget available
                    scrapWidget.markNotBusy()

                    // Offer command
                    emitter.onSuccess(AddScrapCommand(scrap = scrap))
                }
                .addTo(disposableBag)

            sharedTouchSequence.connect()
        }
    }

    private fun createSketchScrap(id: UUID,
                                  x: Float,
                                  y: Float): SketchScrap {
        return SketchScrap(uuid = id,
                           frame = Frame(x = x,
                                         y = y,
                                         scaleX = 1f,
                                         scaleY = 1f,
                                         width = 1f,
                                         height = 1f,
                                         z = highestZ + 1),
                           svg = VectorGraphics(style = VectorGraphics.DEFAULT_STYLE,
                                                tupleList = mutableListOf(LinearPointTuple(0f, 0f))))
    }

    private fun createSketchScrapWidget(x: Float,
                                        y: Float): Pair<SketchScrap, SketchScrapWidget> {
        val id = UUID.randomUUID()
        val scrap = createSketchScrap(id = id,
                                      x = x,
                                      y = y)
        val widget = ScrapWidgetFactory.createScrapWidget(
            scrap,
            schedulers)
        whiteboardWidget.addWidget(widget)

        return Pair(scrap, widget as SketchScrapWidget)
    }
}
