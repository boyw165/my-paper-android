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

package com.paper.domain.vm

import com.paper.domain.DomainConst
import com.paper.domain.ISchedulerProvider
import com.paper.domain.event.*
import com.paper.model.CubicPointTuple
import com.paper.model.Frame
import com.paper.model.LinearPointTuple
import com.paper.model.sketch.SVGStyle
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.*

class SVGScrapWidget(uuid: UUID,
                     frame: Frame,
                     svgs: List<VectorGraphics> = emptyList(),
                     schedulers: ISchedulerProvider)
    : BaseScrapWidget(uuid,
                      frame,
                      schedulers),
      ISVGScrapWidget {

    private val mDirtyFlag = SVGDirtyFlag(SVGDirtyFlag.SVG_INITIALIZING)

    override fun start() {
        super.start()

        println("${DomainConst.TAG}: Start \"${javaClass.simpleName}\"")

        synchronized(mLock) {
            // Initialize SVG
            val drawEvents = mutableListOf<CanvasEvent>()
            mSVGList.forEach { svg ->
                drawEvents.add(AddSvgEvent(svg))
            }
            mDrawSVGSignal.onNext(GroupCanvasEvent(drawEvents))
        }
    }

    override fun stop() {
        super.stop()

        println("${DomainConst.TAG}: Stop \"${javaClass.simpleName}\"")
    }

    // Drawing ////////////////////////////////////////////////////////////////

    private val mSVGList = svgs.toMutableList()
    private lateinit var mWorkingSVG: VectorGraphics

    override fun setSVGs(src: List<VectorGraphics>) {
        synchronized(mLock) {
            mSVGList.clear()
            mSVGList.addAll(src)
        }
    }

    override fun getSVGs(): List<VectorGraphics> {
        synchronized(mLock) {
            return mSVGList.toList()
        }
    }

    override fun moveTo(x: Float,
                        y: Float,
                        style: Set<SVGStyle>) {
        // Mark drawing
        mDirtyFlag.markDirty(SVGDirtyFlag.SVG_DRAWING)

        // Clear all the delayed execution.
        mCancelSignal.onNext(0)

        val workingSVG = VectorGraphics(
                style = style,
                tupleList = mutableListOf(LinearPointTuple(x, y)))

        synchronized(mLock) {
            mWorkingSVG = workingSVG
            mSVGList.add(workingSVG)
        }

        // Signal out
        mDrawSVGSignal.onNext(StartSketchEvent(workingSVG))
    }

    override fun lineTo(x: Float,
                        y: Float) {
        val workingSVG = synchronized(mLock) {mWorkingSVG}
        val point = LinearPointTuple(x, y)

        synchronized(mLock) {
            workingSVG.addTuple(point)
        }

        // Signal out
        mDrawSVGSignal.onNext(OnSketchEvent(workingSVG.id,
                                            point))
    }

    override fun cubicTo(previousControlX: Float,
                         previousControlY: Float,
                         currentControlX: Float,
                         currentControlY: Float,
                         currentEndX: Float,
                         currentEndY: Float) {
        val workingSVG = synchronized(mLock) {mWorkingSVG}
        val point = CubicPointTuple(previousControlX,
                                    previousControlY,
                                    currentControlX,
                                    currentControlY,
                                    currentEndX,
                                    currentEndY)

        synchronized(mLock) {
            workingSVG.addTuple(point)
        }

        // Signal out
        mDrawSVGSignal.onNext(OnSketchEvent(workingSVG.id,
                                            point))
    }

    override fun close() {
        // Signal out
        mDrawSVGSignal.onNext(StopSketchEvent)

        // Mark drawing finished
        mDirtyFlag.markNotDirty(SVGDirtyFlag.SVG_DRAWING)
    }

    /**
     * The signal for the external world to know this widget wants to draw
     * VectorGraphics.
     */
    private val mDrawSVGSignal = PublishSubject.create<CanvasEvent>().toSerialized()

    override fun onDrawSVG(): Observable<CanvasEvent> {
        return mDrawSVGSignal
    }
}
