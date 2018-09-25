// Copyright Aug 2018-present SodaLabs
//
// Author: tc@sodalabs.co
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

package com.paper.domain.ui.manipulator.scrap

import com.paper.domain.ui.ScrapWidget
import com.paper.domain.ui.manipulator.ICommandOutManipulator
import com.paper.model.Frame
import com.paper.model.command.UpdateScrapFrameCommand
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.useful.rx.DragEvent
import io.useful.rx.GestureEvent
import java.util.concurrent.atomic.AtomicReference

class ScrapDragManipulator(private val scrapWidget: ScrapWidget)
    : ICommandOutManipulator {

    private val startFrame = AtomicReference<Frame>()
    private val displacement = AtomicReference<Frame>()

    override fun apply(touchSequence: Observable<GestureEvent>): Maybe<WhiteboardCommand> {
        return Maybe.create { emitter ->
            val disposableBag = CompositeDisposable()

            emitter.setCancellable { disposableBag.dispose() }

            touchSequence
                // TODO: Use matchFirstOrStop operator
                .firstElement()
                .subscribe { event ->
                    if (event !is DragEvent) {
                        emitter.onComplete()
                        return@subscribe
                    }

                    scrapWidget.markBusy()

                    startFrame.set(scrapWidget.getFrame())
                }
                .addTo(disposableBag)

            touchSequence
                // TODO: Use matchFirstOrStop operator
                .ofType(DragEvent::class.java)
                .skip(1)
                .subscribe { event ->
                    updateDisplacement(event)
                }
                .addTo(disposableBag)

            touchSequence
                .ofType(DragEvent::class.java)
                .lastElement()
                .subscribe {
                    scrapWidget.markNotBusy()

                    val startFrame = this.startFrame.get()

                    // Offer command
                    emitter.onSuccess(UpdateScrapFrameCommand(
                        scrapID = scrapWidget.id,
                        fromFrame = startFrame,
                        toFrame = startFrame.add(displacement.get())))
                }
                .addTo(disposableBag)
        }
    }

    private fun updateDisplacement(event: GestureEvent) {
        val nextDisplacement = when (event) {
            is DragEvent -> Frame(
                x = event.stopPointer.first - event.startPointer.first,
                y = event.stopPointer.second - event.startPointer.first)
            else -> displacement.get()
        }

        displacement.set(nextDisplacement)
        scrapWidget.setFrameDisplacement(displacement.get())
        if (event is DragEvent) {
        }
    }
}
