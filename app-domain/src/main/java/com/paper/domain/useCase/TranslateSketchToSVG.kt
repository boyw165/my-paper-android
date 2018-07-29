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

import com.paper.domain.event.*
import com.paper.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

/**
 * Produce a reactive stream of strokes enclosing by a [InitializationBeginEvent]
 * and [InitializationEndEvent].
 */
class TranslateSketchToSVG(strokes: List<SketchStroke>) : Observable<CanvasEvent>() {

    private val mStrokes = strokes.toList()

    override fun subscribeActual(observer: Observer<in CanvasEvent>) {
        val d = SimpleDisposable()
        observer.onSubscribe(d)

        observer.onNext(InitializationBeginEvent())
        observer.onNext(EraseCanvasEvent())

        for (stroke in mStrokes) {
            if (d.isDisposed) break

            val points = stroke.pointList
            for ((i, pt) in points.iterator().withIndex()) {
                if (d.isDisposed) break

                when (i) {
                    0 -> observer.onNext(
                        StartSketchEvent(
                            strokeID = stroke.id,
                            point = pt,
                            penColor = stroke.penColor,
                            penSize = stroke.penSize,
                            penType = stroke.penType))
                    stroke.pointList.lastIndex -> {
                        observer.onNext(OnSketchEvent(strokeID = stroke.id,
                                                      point = pt))
                        observer.onNext(StopSketchEvent())
                    }
                    else -> observer.onNext(
                        OnSketchEvent(strokeID = stroke.id,
                                      point = pt))
                }
            }
        }

        observer.onNext(InitializationEndEvent())
        observer.onComplete()
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
