// Copyright Sep 2018-present SodaLabs
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

package com.paper.domain

import com.paper.domain.store.WhiteboardStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class WhiteboardStoreTest : BaseWhiteboardKitDomainTest() {

    private val candidate by lazy {
        WhiteboardStore(whiteboardID = RANDOM_WHITEBOARD_ID,
                        whiteboardRepo = mockWhiteboardRepo,
                        schedulers = mockSchedulers)
    }

    @Before
    override fun setup() {
        super.setup()

        candidate.start()
    }

    @After
    override fun clean() {
        super.clean()

        candidate.stop()
    }

    @Test
    fun `observe busy, should see busy first and not busy at the end`() {
        val tester = candidate.busy.test()

        // Start
        moveScheduler()

        tester.assertValueAt(0, true)
        tester.assertValueAt(tester.valueCount() - 1, false)
    }
}
