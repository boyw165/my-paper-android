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

import com.cardinalblue.gesture.rx.GestureEvent
import com.paper.domain.ui.IManipulator
import com.paper.domain.ui_event.EditorEvent
import io.reactivex.Observable
import io.reactivex.ObservableSource

class SketchManipulator : IManipulator {

    override fun apply(upstream: Observable<GestureEvent>): ObservableSource<EditorEvent> {
        TODO()
//        return upstream.flatMap { event ->
//            when (event) {
//                is DragBeginEvent -> {
//                    val x = event.startPointer.x
//                    val y = event.startPointer.y
//                    val (nx, ny) = mapper.mapPointToDomain(Pair(x, y))
//
//                    // Create widget
//                    val widget = SVGScrapWidget(
//                        scrap = SVGScrap(frame = Frame(nx, ny)),
//                        schedulers = schedulers)
//
//                    // To a sequence of events:
//                    // 1. clear focus
//                    // 2. add scrap
//                    // 3. focus scrap
//                    // 4. start sketch
//                    Observable.just(GroupEditorEvent(
//                        listOf(ClearFocusEvent,
//                               AddScrapEvent(widget),
//                               FocusScrapEvent(widget.getID()),
//                               StartSketchEvent(nx, ny))))
//                }
//                is OnDragEvent -> {
//                    val x = event.stopPointer.x
//                    val y = event.stopPointer.y
//                    val (nx, ny) = mapper.mapPointToDomain(Pair(x, y))
//
//                    Observable.just(DoSketchEvent(nx, ny))
//                }
//                is DragEndEvent -> {
//                    Observable.just(StopSketchEvent,
//                                    ClearFocusEvent)
//                }
//                else -> TODO()
//            }
//        }
    }
}
