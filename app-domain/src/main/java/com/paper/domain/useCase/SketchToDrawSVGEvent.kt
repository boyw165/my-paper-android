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

package com.paper.domain.useCase

import com.paper.domain.event.DrawSVGEvent
import com.paper.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class SketchToDrawSVGEvent(sketch: List<SketchStroke>) : Observable<DrawSVGEvent>() {

    private val mSketch = sketch.toList()

    override fun subscribeActual(observer: Observer<in DrawSVGEvent>) {
        val d = SimpleDisposable()
        observer.onSubscribe(d)

        var sleepCount = 0

        for (stroke in mSketch) {
            if (d.isDisposed) break

            val points = stroke.pointList
            for ((i, pt) in points.iterator().withIndex()) {
                if (d.isDisposed) break

                when (i) {
                    0 -> observer.onNext(DrawSVGEvent(
                        action = DrawSVGEvent.Action.MOVE,
                        point = pt,
                        penColor = stroke.color,
                        penSize = stroke.width))
                    stroke.pointList.lastIndex -> observer.onNext(DrawSVGEvent(
                        action = DrawSVGEvent.Action.CLOSE,
                        point = pt,
                        penColor = stroke.color,
                        penSize = stroke.width))
                    else -> observer.onNext(DrawSVGEvent(
                        action = DrawSVGEvent.Action.LINE_TO,
                        point = pt,
                        penColor = stroke.color,
                        penSize = stroke.width))
                }

                try {
                    if (++sleepCount == 64) {
                        Thread.sleep(66)
                        sleepCount = 0
                    }
                } catch (err: Throwable) {
                    // IGNORE
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    internal class SimpleDisposable : Disposable {

        @Volatile
        var disposed = false

        override fun isDisposed(): Boolean {
            return disposed
        }

        override fun dispose() {
            disposed = true
        }

    }
}
