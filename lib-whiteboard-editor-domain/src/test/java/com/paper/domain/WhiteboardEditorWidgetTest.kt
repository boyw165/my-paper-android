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

package com.paper.domain

import co.sodalabs.delegate.rx.itemAdded
import co.sodalabs.delegate.rx.itemRemoved
import com.nhaarman.mockitokotlin2.only
import com.paper.domain.ui.IWidget
import com.paper.domain.ui.WhiteboardEditorWidget
import com.paper.domain.ui.WhiteboardWidget
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class WhiteboardEditorWidgetTest : BaseEditorDomainTest() {

    @Before
    override fun setup() {
        super.setup()
    }

    @After
    override fun clean() {
        super.clean()
    }

    @Test
    fun `add a random picker widget, should see addition and widget start`() {
        val whiteboardWidget = WhiteboardWidget(whiteboardStore = mockWhiteboardStore,
                                                schedulers = mockSchedulers)
        val candidate = WhiteboardEditorWidget(whiteboardWidget = whiteboardWidget,
                                               undoWidget = mockUndoWidget,
                                               penPrefsRepo = mockPenPrefsRepo,
                                               schedulers = mockSchedulers)
        candidate.start()
        moveScheduler()

        val additionTester = candidate::pickerWidgets
            .itemAdded()
            .test()

        val pickerWidget = Mockito.mock(IWidget::class.java)
        candidate.pickerWidgets.add(pickerWidget)

        moveScheduler()

        // Addition
        additionTester.assertValueCount(1)
        // Widget start
        Mockito.verify(pickerWidget, only()).start()
    }

    @Test
    fun `remove a pre-added picker widget, should see removal and widget stop`() {
        val whiteboardWidget = WhiteboardWidget(whiteboardStore = mockWhiteboardStore,
                                                schedulers = mockSchedulers)
        val candidate = WhiteboardEditorWidget(whiteboardWidget = whiteboardWidget,
                                               undoWidget = mockUndoWidget,
                                               penPrefsRepo = mockPenPrefsRepo,
                                               schedulers = mockSchedulers)
        candidate.start()
        moveScheduler()

        val removalTester = candidate::pickerWidgets
            .itemRemoved()
            .test()

        val pickerWidget = Mockito.mock(IWidget::class.java)
        Mockito.`when`(pickerWidget.start())
            .then {
                println("picker widget starts")
            }
        candidate.pickerWidgets.add(pickerWidget)
        moveScheduler()

        candidate.pickerWidgets.remove(pickerWidget)
        moveScheduler()

        // Removal
        removalTester.assertValueCount(1)
        // Widget stop
        Mockito.verify(pickerWidget).stop()
    }

    @Test
    fun `sketching, editor should be busy`() {
//        val candidate = WhiteboardWidget(schedulers = mockSchedulers)
//
//        val busyTest = candidate
//            .onBusy()
//            .test()
//
//        // Start widget
//        candidate.inject(mockWhiteboard)
//        candidate.start().test().assertSubscribed()
//
//        // Make sure the stream moves
//        moveScheduler()
//
//        // Sketch (by simulating the gesture interpreter behavior)
//        val widget = SketchScrapWidget(
//            scrap = SVGScrap(frame = createRandomFrame()),
//            schedulers = mockSchedulers)
//        candidate.handleDomainEvent(GroupEditorEvent(
//            listOf(AddScrapEvent(widget),
//                   FocusScrapEvent(widget.getID()),
//                   StartSketchEvent(0f, 0f))))
//
//        // Trigger for see sketch event
//        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)
//
//        busyTest.assertValueAt(busyTest.valueCount() - 1, true)
    }
}
