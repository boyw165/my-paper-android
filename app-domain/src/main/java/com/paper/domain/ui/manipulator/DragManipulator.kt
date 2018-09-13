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

import com.cardinalblue.gesture.rx.DragBeginEvent
import com.cardinalblue.gesture.rx.DragDoingEvent
import com.cardinalblue.gesture.rx.DragEndEvent
import com.cardinalblue.gesture.rx.GestureEvent
import com.paper.domain.ui.ScrapWidget
import com.paper.model.Frame
import com.paper.model.ISchedulers
import com.paper.model.command.UpdateScrapFrameCommand
import com.paper.model.command.WhiteboardCommand
import java.util.concurrent.atomic.AtomicReference

class DragManipulator(private val scrapWidget: ScrapWidget,
                      schedulers: ISchedulers)
    : Manipulator(schedulers = schedulers) {

    private val startFrame = AtomicReference<Frame>()
    private val displacement = AtomicReference<Frame>()

    override fun isMyBusiness(event: GestureEvent): Boolean {
        return event is DragBeginEvent ||
               event is DragDoingEvent
    }

    override fun onFirst(event: GestureEvent) {
        scrapWidget.markBusy()

        startFrame.set(scrapWidget.getFrame())
    }

    override fun onInBetweenFirstAndLast(event: GestureEvent) {
        updateDisplacement(event)
    }

    override fun onLast(event: GestureEvent): WhiteboardCommand {
        updateDisplacement(event)
        scrapWidget.markNotBusy()

        val startFrame = this.startFrame.get()

        // Offer command
        return UpdateScrapFrameCommand(
            scrapID = scrapWidget.getID(),
            toFrame = startFrame.add(displacement.get()))
    }

    private fun updateDisplacement(event: GestureEvent) {
        val nextDisplacement = when (event) {
            is DragDoingEvent -> Frame(
                x = event.stopPointer.first - event.startPointer.first,
                y = event.stopPointer.second - event.startPointer.first)
            is DragEndEvent -> Frame(
                x = event.stopPointer.first - event.startPointer.first,
                y = event.stopPointer.second - event.startPointer.first)
            else -> displacement.get()
        }

        displacement.set(nextDisplacement)
        scrapWidget.setFrameDisplacement(displacement.get())
        if (event is DragDoingEvent ||
            event is DragEndEvent) {
        }
    }
}
