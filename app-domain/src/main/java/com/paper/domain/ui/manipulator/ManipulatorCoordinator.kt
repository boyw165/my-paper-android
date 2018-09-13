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

package com.paper.domain.ui.manipulator

import com.cardinalblue.gesture.rx.GestureEvent
import com.cardinalblue.gesture.rx.TouchBeginEvent
import com.cardinalblue.gesture.rx.TouchEndEvent
import com.paper.domain.store.IWhiteboardStore
import com.paper.domain.ui.IUndoWidget
import com.paper.domain.ui.ScrapWidget
import com.paper.model.ISchedulers
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.addTo

abstract class ManipulatorCoordinator(protected val scrapWidget: ScrapWidget,
                                      protected val whiteboardStore: IWhiteboardStore?,
                                      protected val undoWidget: IUndoWidget?,
                                      protected val schedulers: ISchedulers)
    : Function<Observable<Observable<GestureEvent>>, CompletableSource> {

    private val commandCollection = mutableListOf<WhiteboardCommand>()

    private val staticDisposableBag = CompositeDisposable()

    final override fun apply(gestureSequence: Observable<Observable<GestureEvent>>): CompletableSource {
        val sharedGestureSequence = gestureSequence.publish()

        sharedGestureSequence
            .flatMap {
                it.publish { gesture ->
                    // Gesture is a sequence of touch event

                    // Init manipulators
                    val manipulators = offerManipulators()
                    val commandSequence = manipulators.map { m -> gesture.compose(m) }
                    Observable.merge(commandSequence)
                }
            }
            .subscribe { c ->
                commandCollection.add(c)
            }
            .addTo(staticDisposableBag)

        sharedGestureSequence
            .flatMap { it }
            .observeOn(schedulers.main())
            .subscribe { event ->
                when (event) {
                    is TouchBeginEvent -> {
                        onTouchBegin()

                        // Clear
                        commandCollection.clear()
                    }
                    is TouchEndEvent -> {
                        val c = onTouchEnd(commandCollection.toList())

                        // Clear
                        commandCollection.clear()

                        whiteboardStore?.offerCommandDoo(c)
                        undoWidget?.offerCommand(c)
                    }
                }
            }

        // Activate the shared upstream
        sharedGestureSequence
            .connect()
            .addTo(staticDisposableBag)

        return Completable.create { emitter ->
            // Enable auto-stop for downstream disposal
            emitter.setCancellable {
                staticDisposableBag.dispose()
            }
        }
    }

    /**
     * A callback of when the first finger is down.
     */
    abstract fun onTouchBegin()

    /**
     * A callback of when the first finger is down.
     */
    abstract fun offerManipulators(): List<Manipulator>

    abstract fun onTouchEnd(commandCollection: List<WhiteboardCommand>): WhiteboardCommand
}
