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
import com.paper.domain.ui.IWhiteboardWidget
import com.paper.domain.ui.ScrapWidget
import com.paper.model.ISchedulers
import io.reactivex.Completable
import io.reactivex.Observable
import io.useful.rx.GestureEvent

/**
 * The manipulator which is mainly used by [ScrapWidget].
 */
class ScrapWidgetManipulator(private val scrapWidget: ScrapWidget,
                             private val whiteboardWidget: IWhiteboardWidget,
                             private val editorWidget: IWhiteboardEditorWidget,
                             private val schedulers: ISchedulers)
    : IUserTouchManipulator {

    override fun apply(gestureSequence: Observable<Observable<GestureEvent>>): Completable {
        return gestureSequence
            .flatMapCompletable { touchSequence ->
                Completable.fromObservable(
                    DragManipulator(scrapWidget = scrapWidget,
                                    schedulers = schedulers)
                        .apply(touchSequence)
                        .observeOn(schedulers.main())
                        .doOnSuccess { command ->
                            whiteboardWidget
                                .whiteboardStore
                                .offerCommandDoo(command)
                            editorWidget
                                .undoWidget
                                .offerCommand(command)
                        }
                        .toObservable())
            }
    }
}
