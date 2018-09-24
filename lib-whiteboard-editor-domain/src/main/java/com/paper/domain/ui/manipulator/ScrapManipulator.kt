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

import com.paper.domain.ui.IWhiteboardEditorWidget
import com.paper.domain.ui.ScrapWidget
import com.paper.domain.ui.manipulator.scrap.ScrapDragManipulator
import com.paper.domain.ui.manipulator.scrap.ScrapPinchManipulator
import com.paper.domain.ui.manipulator.scrap.ScrapTapManipulator
import com.paper.model.command.GroupCommand
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.addTo
import io.useful.rx.GestureEvent
import io.useful.rx.TouchBeginEvent
import io.useful.rx.TouchEndEvent

/**
 * The manipulator which is mainly used by [ScrapWidget].
 */
class ScrapManipulator(private val scrapWidget: ScrapWidget,
                       private val editorWidget: IWhiteboardEditorWidget)
    : IUserTouchManipulator {

    private val commandsForUndoWidget = mutableListOf<WhiteboardCommand>()

    override fun apply(gestureSequence: Observable<Observable<GestureEvent>>): Completable {
        return gestureSequence
            .flatMapCompletable { touchSequence ->
                Completable.fromObservable(
                    // Dispatch manipulators
                    Observable
                        .concat(listOf(
                            createTouchLifecycleManipulator().apply(touchSequence),
                            ScrapTapManipulator(scrapWidget).apply(touchSequence).toObservable(),
                            ScrapDragManipulator(scrapWidget).apply(touchSequence).toObservable(),
                            ScrapPinchManipulator(scrapWidget).apply(touchSequence).toObservable()))
                        .doOnNext { command ->
                            val store = editorWidget.whiteboardStore
                            store.offerCommandDoo(command)

                            commandsForUndoWidget.add(command)
                        })
            }
    }

    private fun createTouchLifecycleManipulator(): Function<Observable<GestureEvent>, ObservableSource<WhiteboardCommand>> {
        return Function { touchSequence ->
            Observable.create { emitter ->
                val disposableBag = CompositeDisposable()

                emitter.setCancellable { disposableBag.dispose() }

                touchSequence
                    .firstElement()
                    .subscribe { event ->
                        when (event) {
                            is TouchBeginEvent -> {
                                // New collection for a new touch session
                                commandsForUndoWidget.clear()

                                // Terminate stream
                                emitter.onComplete()
                            }
                            is TouchEndEvent -> {
                                // Group the collection of commands over the touch session
                                val undoWidget = editorWidget.undoWidget
                                val groupCommand = GroupCommand(commands = commandsForUndoWidget.toList())

                                undoWidget.offerCommand(groupCommand)

                                // Terminate stream
                                emitter.onComplete()
                            }
                            else -> emitter.onComplete()
                        }
                    }
                    .addTo(disposableBag)
            }
        }
    }
}
