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
import com.paper.model.command.AddScrapCommand
import com.paper.domain.ui_event.SketchCubicToEvent
import com.paper.domain.ui_event.SketchMoveToEvent
import com.paper.domain.ui_event.UpdateSVGEvent
import com.paper.model.*
import com.paper.model.command.WhiteboardCommand
import com.paper.model.sketch.SVGStyle
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Maybes
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject

open class SVGScrapWidget(scrap: SVGScrap,
                          private val newSVGPenStyle: Observable<Set<SVGStyle>>,
                          schedulers: ISchedulers)
    : BaseScrapWidget(scrap,
                      schedulers),
      IWidget {

    private val svgScrap: SVGScrap get() = scrap as SVGScrap
    private val sketchDisposableBag = CompositeDisposable()

    override fun start(): Observable<Boolean> {
        return autoStop {
            // Add/remove
            svgScrap.observeAddSVG()
                .subscribe {
                    // TODO: Yet support, but possible for shape completion
                }
                .addTo(staticDisposableBag)
            svgScrap.observeRemoveSVG()
                .subscribe {
                    // TODO: Yet support, but possible for shape completion
                }
                .addTo(staticDisposableBag)

            println("${DomainConst.TAG}: Start \"${javaClass.simpleName}\"")
        }
    }

    override fun stop() {
        super.stop()
        sketchDisposableBag.clear()
        println("${DomainConst.TAG}: Stop \"${javaClass.simpleName}\"")
    }

    // Drawing ////////////////////////////////////////////////////////////////

    private val svgSignal = PublishSubject.create<UpdateSVGEvent>().toSerialized()

    fun observeSVG(): Observable<UpdateSVGEvent> {
        return svgSignal
    }

    fun getSVGs(): List<VectorGraphics> {
        synchronized(lock) {
            return svgScrap.getSVGs()
        }
    }

    fun handleSketch(src: Observable<Point>) {
        sketchDisposableBag.clear()

        // Cache SVG
        lateinit var svg: VectorGraphics

        // Begin
        Maybes.zip(newSVGPenStyle.lastElement(),
                   src.firstElement())
            .subscribe { (style, point) ->
                // Mark drawing
                dirtyFlag.markDirty(ScrapDirtyFlag.GENERAL_BUSY)

                svg = VectorGraphics(style = style)
                svg.addTuple(LinearPointTuple(point.x,
                                              point.y))

                svgSignal.onNext(SketchMoveToEvent(point.x,
                                                   point.y,
                                                   style))
            }
            .addTo(sketchDisposableBag)

        // Doing
        src.skip(1)
            .skipLast(1)
            .subscribe { point ->
                val pointTuple = calculateNextCubicPointTuple(svg.getTupleList(),
                                                              point)

                svg.addTuple(pointTuple)

                svgSignal.onNext(SketchCubicToEvent(pointTuple.prevControlX,
                                                    pointTuple.prevControlY,
                                                    pointTuple.currentControlX,
                                                    pointTuple.currentControlY,
                                                    pointTuple.currentEndX,
                                                    pointTuple.currentEndY))
            }
            .addTo(sketchDisposableBag)

        // End
        src.lastElement()
            .subscribe { point ->
                val pointTuple = calculateNextCubicPointTuple(svg.getTupleList(),
                                                              point)

                svg.addTuple(pointTuple)

                svgSignal.onNext(SketchCubicToEvent(pointTuple.prevControlX,
                                                    pointTuple.prevControlY,
                                                    pointTuple.currentControlX,
                                                    pointTuple.currentControlY,
                                                    pointTuple.currentEndX,
                                                    pointTuple.currentEndY))

                // Commit to model
                svgScrap.addSVG(svg)

                // Mark not drawing
                dirtyFlag.markNotDirty(ScrapDirtyFlag.GENERAL_BUSY)
            }
            .addTo(sketchDisposableBag)
    }

    private fun calculateNextCubicPointTuple(now: List<PointTuple>,
                                             nextPoint: Point): CubicPointTuple {
        val previous = now.last()
        val previousX = when (previous) {
            is LinearPointTuple -> previous.x
            is CubicPointTuple -> previous.currentEndX
        }
        val previousY = when (previous) {
            is LinearPointTuple -> previous.x
            is CubicPointTuple -> previous.currentEndY
        }

        // TODO: Smooth curve
        return CubicPointTuple(prevControlX = nextPoint.x,
                               prevControlY = nextPoint.y,
                               currentControlX = previousX,
                               currentControlY = previousY,
                               currentEndX = nextPoint.x,
                               currentEndY = nextPoint.y)
    }
}
