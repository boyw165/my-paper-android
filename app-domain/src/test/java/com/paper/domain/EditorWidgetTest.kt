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

import com.paper.domain.ui.EditorWidget
import com.paper.domain.ui.SVGScrapWidget
import com.paper.domain.ui_event.*
import com.paper.model.Frame
import com.paper.model.SVGScrap
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner.Silent::class)
class EditorWidgetTest : BaseDomainTest() {

    @Test
    fun `initialization busy test`() {
        val tester = EditorWidget(schedulers = mockSchedulers)

        val busyTest = tester
            .onBusy()
            .test()

        // Start widget
        tester.inject(mockPaper)
        tester.start().test().assertSubscribed()

        // Trigger for initialization
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)

        busyTest.assertValueAt(0, true)
        busyTest.assertValueAt(busyTest.valueCount() - 1, false)
    }

    @Test
    fun `sketching, canvas should be busy`() {
        val tester = EditorWidget(schedulers = mockSchedulers)

        val busyTest = tester
            .onBusy()
            .test()

        // Start widget
        tester.inject(mockPaper)
        tester.start().test().assertSubscribed()

        // Trigger for initialization
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)

        // Sketch (by simulating the gesture interpreter behavior)
        val widget = SVGScrapWidget(
            scrap = SVGScrap(frame = createRandomFrame()),
            schedulers = mockSchedulers)
        tester.handleDomainEvent(GroupEditorEvent(
            listOf(AddScrapEvent(widget),
                   FocusScrapEvent(widget.getID()),
                   StartSketchEvent(0f, 0f))))

        // Trigger for see sketch event
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)

        busyTest.assertValueAt(busyTest.valueCount() - 1, true)
    }

    @Test
    fun `remove scrap, should see event and scrap stop`() {
        val tester = EditorWidget(schedulers = mockSchedulers)

        val scrapTest = tester
            .onUpdateScrap()
            .test()

        // Start widget
        tester.inject(mockPaper)
        tester.start().test().assertSubscribed()

        // Trigger to see the scrap addition
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)

        // Remove it
        val scrap = (scrapTest.events[0][0] as AddScrapEvent).scrap
        tester.handleDomainEvent(RemoveScrapEvent(scrap))

        // Trigger to see the scrap remove
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)

        scrapTest.assertValueAt(1) { event ->
            event is RemoveScrapEvent
        }
    }

    @Test
    fun `add scrap, should see event and scrap start`() {
        val tester = EditorWidget(schedulers = mockSchedulers)
        val scrapTest = tester
            .onUpdateScrap()
            .test()

        // Start widget
        tester.inject(mockPaper)
        tester.start().test().assertSubscribed()

        // Trigger
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)

        scrapTest.assertValue { event ->
            event is AddScrapEvent
        }
    }
}
