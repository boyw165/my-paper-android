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
import com.paper.model.SketchScrap
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.useful.changed

open class SketchScrapWidget(scrap: SketchScrap)
    : ScrapWidget(scrap) {

    private val sketchScrap: SketchScrap get() = scrap as SketchScrap
    private val sketchDisposableBag = CompositeDisposable()

    override fun start() {
        // Add/remove
        sketchScrap::svg
            .changed()
            .subscribe { svg ->
                synchronized(lock) {
                    svgDisplacement = null
                }

                // Signal out
                svgSignal.onNext(svg)
            }
            .addTo(staticDisposableBag)

        println("${DomainConst.TAG}: Start \"${javaClass.simpleName}\"")
    }

    override fun stop() {
        super.stop()
        sketchDisposableBag.clear()
        println("${DomainConst.TAG}: Stop \"${javaClass.simpleName}\"")
    }

    // Drawing ////////////////////////////////////////////////////////////////

    private var svgDisplacement: VectorGraphics? = null
    private val svgSignal = PublishSubject.create<VectorGraphics>().toSerialized()

    fun observeSVG(): Observable<VectorGraphics> {
        return svgSignal
    }

    fun getSVG(): VectorGraphics {
        return synchronized(lock) {
            svgDisplacement ?: sketchScrap.svg
        }
    }

    fun setDisplacement(displacement: VectorGraphics) {
        synchronized(lock) {
            svgDisplacement = displacement

            svgSignal.onNext(displacement)
        }
    }
}
