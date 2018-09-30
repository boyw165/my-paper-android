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
import com.paper.domain.ui.manipulator.editor.EditorDragManipulator
import com.paper.domain.ui.manipulator.editor.EditorPinchManipulator
import com.paper.domain.ui.manipulator.editor.EditorTapManipulator
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.useful.rx.GestureEvent

/**
 * The manipulator which is mainly used by [IWhiteboardEditorWidget].
 */
class EditorManipulator(private val editorWidget: IWhiteboardEditorWidget)
    : IUserTouchManipulator {

    override fun apply(gestureSequence: Observable<Observable<GestureEvent>>): Completable {
        val whiteboardSrc = editorWidget.whiteboardWidget.whiteboardStore.whiteboard.toObservable()

        return gestureSequence
            .withLatestFrom(whiteboardSrc)
            .switchMapCompletable { (touchSequence, whiteboard) ->
                val whiteboardStore = editorWidget.whiteboardWidget.whiteboardStore
                val whiteboardWidget = editorWidget.whiteboardWidget

                Completable.fromObservable(
                    // Dispatch manipulators
                    Observable
                        .concat(listOf(
                            EditorTapManipulator(editorWidget).apply(touchSequence).toObservable(),
                            EditorDragManipulator(whiteboard = whiteboard,
                                                  highestZ = whiteboardWidget.highestZ)
                                .apply(touchSequence)
                                .toObservable(),
                            EditorPinchManipulator(editorWidget).apply(touchSequence).toObservable()))
                        .doOnError { err ->
                            println(err)
                        }
                        .doOnNext { command ->
                            whiteboardStore.offerCommandDoo(command)
                        })
            }
    }
}
