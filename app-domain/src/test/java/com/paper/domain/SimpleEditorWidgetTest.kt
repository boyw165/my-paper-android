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

import com.paper.domain.ui.SimpleEditorWidget
import com.paper.domain.ui_event.AddScrapEvent
import com.paper.domain.ui_event.RemoveScrapEvent
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class SimpleEditorWidgetTest : BaseDomainTest() {

    @Test
    fun `see busy at the first and free at the end`() {
        val candidate = SimpleEditorWidget(paperID = 0,
                                           paperRepo = mockPaperRepo,
                                           caughtErrorSignal = caughtErrorSignal,
                                           schedulers = mockSchedulers)

        val busyTest = candidate
            .observeBusy()
            .test()

        // Start widget
        val lifecycleTest = candidate.start().test()
        lifecycleTest.assertSubscribed()

        // Make sure the stream moves
        moveScheduler()

        busyTest.assertValueAt(0, true)
        busyTest.assertValueAt(busyTest.valueCount() - 1, false)
    }

    @Test
    fun `inflation process test`() {
        val candidate = SimpleEditorWidget(paperID = 0,
                                           paperRepo = mockPaperRepo,
                                           caughtErrorSignal = caughtErrorSignal,
                                           schedulers = mockSchedulers)

        val scrapTester = candidate.observeScraps().test()

        // Start widget
        val lifecycleTest = candidate.start().test()
        lifecycleTest.assertSubscribed()

        // Make sure the stream moves
        moveScheduler()

        scrapTester.assertValueCount(mockPaper.getScraps().size)
    }

    @Test
    fun `dispose before MODEL is inflated`() {
        val candidate = SimpleEditorWidget(paperID = 0,
                                           paperRepo = mockPaperRepo,
                                           caughtErrorSignal = caughtErrorSignal,
                                           schedulers = mockSchedulers)

        // Start widget
        val lifecycleTester = candidate.start().test()
        lifecycleTester.assertSubscribed()
        lifecycleTester.dispose()

        val addTester = candidate.observeScraps().test()

        // Make sure the stream moves
        moveScheduler()

        addTester.assertNoValues()
    }

    @Test
    fun `add scrap, should see event and scrap start`() {
        val candidate = SimpleEditorWidget(paperID = 0,
                                           paperRepo = mockPaperRepo,
                                           caughtErrorSignal = caughtErrorSignal,
                                           schedulers = mockSchedulers)
        val scrapTest = candidate
            .observeScraps()
            .test()

        // Start widget
        val paper = mockPaper
        candidate.start().test().assertSubscribed()

        // Make sure the stream moves
        moveScheduler()

        paper.getScraps().forEachIndexed { i, _ ->
            scrapTest.assertValueAt(i) { event ->
                event is AddScrapEvent
            }
        }
    }

    @Test
    fun `remove scrap, should see event and scrap stop`() {
        val candidate = SimpleEditorWidget(paperID = 0,
                                           paperRepo = mockPaperRepo,
                                           caughtErrorSignal = caughtErrorSignal,
                                           schedulers = mockSchedulers)

        val scrapTest = candidate
            .observeScraps()
            .test()

        // Start widget
        val paper = mockPaper
        candidate.start().test().assertSubscribed()

        // Make sure the stream moves
        moveScheduler()

        // Remove a scrap
        paper.removeScrap(paper.getScraps()[0])

        // Make sure the stream moves
        moveScheduler()

        scrapTest.assertValueAt(scrapTest.valueCount() - 1) { event ->
            event is RemoveScrapEvent
        }
    }
}
