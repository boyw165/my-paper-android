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
import com.paper.domain.store.IWhiteboardStore
import com.paper.domain.ui.*
import com.paper.model.*
import com.paper.model.command.AddScrapCommand
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import java.util.*

class SketchManipulator(private val editor: IWhiteboardEditorWidget,
                        private val highestZ: Int,
                        whiteboardStore: IWhiteboardStore,
                        private val undoWidget: IUndoWidget?,
                        schedulers: ISchedulers)
    : Manipulator(whiteboardStore = whiteboardStore,
                  schedulers = schedulers) {

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

    override fun onHandleTouchSequence(touchSequence: Observable<in GestureEvent>,
                                       completer: CompletableEmitter) {
        touchSequence
            .take(1)
            .observeOn(schedulers.main())
            .subscribe { event ->
                event as DragBeginEvent

                startPoint = Point(event.startPointer.first,
                                   event.startPointer.second)
                lastPoint = startPoint

                val (s, w) = addTemporarySketchWidget(event.startPointer.first,
                                                      event.startPointer.second)
                scrap = s
                scrapWidget = w
                // Mark widget busy
                scrapWidget.markBusy()

                // Sketch displacement
                svgDisplacement = w.getSVG()

                // Most importantly, add widget
                editor.addWidget(scrapWidget)
            }
            .addTo(disposableBag)

        touchSequence
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

        Completable.fromObservable(touchSequence)
            .observeOn(schedulers.main())
            .subscribe {
                // Prepare command with updated MODEL
                scrap.setSVG(svgDisplacement)
                val command = AddScrapCommand(scrap = scrap)

                // Offer command
                undoWidget?.putOperation(command)
                whiteboardStore.offerCommandDoo(command)

                // Mark widget available
                scrapWidget.markNotBusy()

                // Complete the job
                completer.onComplete()
            }
            .addTo(disposableBag)
    }

    private fun createSVGScrap(id: UUID,
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

    private fun addTemporarySketchWidget(x: Float,
                                         y: Float): Pair<SketchScrap, SketchScrapWidget> {
        val id = UUID.randomUUID()
        val scrap = createSVGScrap(id = id,
                                   x = x,
                                   y = y)
        val widget = ScrapWidgetFactory.createScrapWidget(
            scrap,
            schedulers)
        editor.addWidget(widget)

        return Pair(scrap, widget as SketchScrapWidget)
    }
}
