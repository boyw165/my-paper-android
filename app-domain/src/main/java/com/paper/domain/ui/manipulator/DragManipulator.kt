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
import com.paper.domain.ui.ScrapWidget
import com.paper.model.Frame
import com.paper.model.command.UpdateScrapFrameCommand
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

class DragManipulator(private val widget: ScrapWidget) : BaseManipulator() {

    private val disposablesBag = CompositeDisposable()

    override fun apply(touchSequence: Observable<GestureEvent>): ObservableSource<WhiteboardCommand> {
        return autoStop { emitter ->
            val startFrame = widget.getFrame()

            // Begin
            // Any except the end
            touchSequence
                .skipLast(1)
                .subscribe { event ->
                    if (!(event is DragBeginEvent ||
                          event is DragDoingEvent)) {
                        emitter.onComplete()
                        return@subscribe
                    }

                    if (event is DragDoingEvent) {
                        val displacement = Frame(
                            x = event.stopPointer.first - event.startPointer.first,
                            y = event.stopPointer.second - event.startPointer.first)
                        widget.setFrameDisplacement(displacement)
                    }
                }
                .addTo(disposablesBag)

            // End
            touchSequence
                .lastElement()
                .subscribe { event ->
                    if (event !is DragEndEvent) {
                        emitter.onComplete()
                        return@subscribe
                    }

                    val displacement = Frame(
                        x = event.stopPointer.first - event.startPointer.first,
                        y = event.stopPointer.second - event.startPointer.first)
                    widget.setFrameDisplacement(displacement)

                    // Produce collage command
                    emitter.onNext(UpdateScrapFrameCommand(
                        scrapID = widget.getID(),
                        toFrame = startFrame.add(displacement)))
                    emitter.onComplete()
                }
                .addTo(disposablesBag)
        }
    }
}
