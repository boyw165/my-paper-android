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

import com.cardinalblue.gesture.rx.DragDoingEvent
import com.cardinalblue.gesture.rx.GestureEvent
import com.paper.domain.store.IWhiteboardStore
import com.paper.domain.ui.IUndoWidget
import com.paper.domain.ui.ScrapWidget
import com.paper.model.Frame
import com.paper.model.ISchedulers
import com.paper.model.command.UpdateScrapFrameCommand
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import java.util.concurrent.atomic.AtomicReference

class DragManipulator(private val scrapWidget: ScrapWidget,
                      whiteboardStore: IWhiteboardStore,
                      private val undoWidget: IUndoWidget?,
                      schedulers: ISchedulers)
    : Manipulator(whiteboardStore = whiteboardStore,
                  schedulers = schedulers) {

    private val startFrame = scrapWidget.getFrame()
    private val displacement = AtomicReference<Frame>()

    override fun onHandleTouchSequence(touchSequence: Observable<in GestureEvent>,
                                       completer: CompletableEmitter) {
        touchSequence
            .take(1)
            .observeOn(schedulers.main())
            .subscribe {
                scrapWidget.markBusy()
            }
            .addTo(disposableBag)

        touchSequence
            .observeOn(schedulers.main())
            .subscribe { event ->
                if (event is DragDoingEvent) {
                    displacement.set(Frame(
                        x = event.stopPointer.first - event.startPointer.first,
                        y = event.stopPointer.second - event.startPointer.first))
                    scrapWidget.setFrameDisplacement(displacement.get())
                }
            }
            .addTo(disposableBag)

        Completable.fromObservable(touchSequence)
            .observeOn(schedulers.main())
            .subscribe {
                val command = UpdateScrapFrameCommand(
                    scrapID = scrapWidget.getID(),
                    toFrame = startFrame.add(displacement.get()))

                // Offer command
                undoWidget?.putOperation(command)
                whiteboardStore.offerCommandDoo(command)

                // Complete job
                completer.onComplete()
            }
    }
}
