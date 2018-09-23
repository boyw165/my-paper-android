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

import com.paper.domain.ui.IWhiteboardWidget
import com.paper.model.*
import com.paper.model.command.AddScrapCommand
import com.paper.model.command.WhiteboardCommand
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.useful.rx.DragEvent
import io.useful.rx.GestureEvent
import java.util.*

class SketchManipulator(private val whiteboardWidget: IWhiteboardWidget,
                        private val highestZ: Int)
    : IUserTouchCommandOutManipulator {

    @Volatile
    private lateinit var scrap: SketchScrap

//    @Volatile
//    private lateinit var svgDisplacement: VectorGraphics
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
                .subscribe { event ->
                    if (event !is DragEvent) {
                        emitter.onComplete()
                        return@subscribe
                    }

                    val (x, y) = event.startPointer

                    startPoint = Point(x, y)
                    lastPoint = startPoint

                    scrap = createSketchScrap(UUID.randomUUID(), x, y)

                    // TODO: Mark editor busy

                    // Most importantly, add widget
                    whiteboardWidget
                        .whiteboardStore
                        .whiteboard
                        ?.scraps
                        ?.add(scrap)
                }
                .addTo(disposableBag)

            sharedTouchSequence
                .skip(1)
                .subscribe { event ->
                    scrap.svg = calculateNewSVG(event as DragEvent)
                }
                .addTo(disposableBag)

            sharedTouchSequence
                .lastElement()
                .subscribe { event ->
                    scrap.svg = calculateNewSVG(event as DragEvent)

                    // TODO: Mark not busy

                    // Offer command
                    emitter.onSuccess(AddScrapCommand(scrap = scrap))
                }
                .addTo(disposableBag)

            sharedTouchSequence.connect()
        }
    }

    private fun calculateNewSVG(event: DragEvent): VectorGraphics {
        val (x, y) = event.stopPointer
        val nx = x - startPoint.x
        val ny = y - startPoint.y
        val p = Point(nx, ny)
        val tupleCopy = scrap.svg
            .getTupleList()
            .toMutableList()
        tupleCopy.add(CubicPointTuple(prevControlX = p.x,
                                      prevControlY = p.y,
                                      currentControlX = lastPoint.x,
                                      currentControlY = lastPoint.y,
                                      currentEndX = p.x,
                                      currentEndY = p.y))

        lastPoint = p

        return scrap.svg.copy(tupleList = tupleCopy)
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
}
