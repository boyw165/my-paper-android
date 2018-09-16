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
import com.paper.domain.ui.WhiteboardWidget
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class WhiteboardWidgetTest : BaseWhiteboardKitDomainTest() {

    @Before
    override fun setup() {
        super.setup()
    }

    @After
    override fun clean() {
        super.clean()
    }

    @Test
    fun `see busy at the first and free at the end`() {
        val candidate = WhiteboardWidget(whiteboardStore = mockWhiteboardStore,
                                         schedulers = mockSchedulers)

        val tester = candidate
            .busy
            .test()

        // Start widget
        candidate.start()

        // Make sure the stream moves
        moveScheduler()

        tester.assertValueAt(0, true)
        tester.assertValueAt(tester.valueCount() - 1, false)
    }

    @Test
    fun `inflation process test`() {
        val candidate = WhiteboardWidget(whiteboardStore = mockWhiteboardStore,
                                         schedulers = mockSchedulers)

        val tester = candidate::scrapWidgets
            .itemAdded()
            .test()

        // Start widget
        candidate.start()

        // Make sure the stream moves
        moveScheduler()

        tester.assertValueCount(mockWhiteboard.getScraps().size)
    }

    @Test
    fun `dispose before MODEL is inflated`() {
        val candidate = WhiteboardWidget(whiteboardStore = mockWhiteboardStore,
                                         schedulers = mockSchedulers)

        // Start widget
        candidate.start()

        val tester = candidate::scrapWidgets
            .itemAdded()
            .test()

        candidate.stop()

        // Make sure the stream moves
        moveScheduler()

        tester.assertNoValues()
    }

    @Test
    fun `remove scrap, should see event and scrap stop`() {
        val candidate = WhiteboardWidget(whiteboardStore = mockWhiteboardStore,
                                         schedulers = mockSchedulers)

        val tester = candidate::scrapWidgets
            .itemRemoved()
            .test()

        // Start widget
        val document = mockWhiteboardStore.whiteboard.blockingGet()
        candidate.start()

        // Make sure the stream moves
        moveScheduler()

        // Remove a scrap
        document.removeScrap(document.getScraps()[0])

        // Make sure the stream moves
        moveScheduler()

        tester.assertValueCount(1)
    }
}
