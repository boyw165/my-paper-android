// Copyright May 2018-present Paper
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

package com.paper.model

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.net.URI
import java.util.*

@RunWith(MockitoJUnitRunner.Silent::class)
class WhiteboardTest : BaseModelTest() {

    @Test
    fun `basic copy`() {
        val tester1 = Whiteboard(id = 1,
                                 uuid = UUID.randomUUID(),
                                 createdAt = 100L,
                                 modifiedAt = 200L,
                                 width = 500f,
                                 height = 500f,
                                 viewPort = Rect(0f, 0f, 500f, 500f))
        val tester2 = tester1.copy()
        tester2.setSize(Pair(300f, 300f))
        tester2.setViewPort(Rect(125f, 125f, 250f, 250f))
        tester2.setModifiedAt(300L)
        tester2.setThumbnail(URI("file:///foo"), 100, 100)

        Assert.assertNotEquals(tester2, tester1)
        Assert.assertNotEquals(tester2.getSize(), tester1.getSize())
        Assert.assertNotEquals(tester2.getViewPort(), tester1.getViewPort())
        Assert.assertNotEquals(tester2.getModifiedAt(), tester1.getModifiedAt())
    }

    @Test
    fun `scraps copy`() {
        val tester1 = Whiteboard()
        val tester2 = tester1.copy()
        tester2.addScrap(Scrap())

        Assert.assertNotEquals(tester2.getScraps(), tester1.getScraps())
    }

    @Test
    fun `observe add scrap`() {
        val tester = Whiteboard()

        val addTestObserver = tester.observeAddScrap().test()

        tester.addScrap(createRandomScrap())
        tester.addScrap(createRandomScrap())
        tester.addScrap(createRandomScrap())

        addTestObserver.assertValueCount(3)
    }

    @Test
    fun `observe remove scrap`() {
        val tester = Whiteboard(scraps = mutableListOf(createRandomScrap(),
                                                       createRandomScrap(),
                                                       createRandomScrap()))

        val removeTestObserver = tester.observeRemoveScrap().test()

        tester.removeScrap(tester.getScraps()[0])
        tester.removeScrap(tester.getScraps()[0])
        tester.removeScrap(tester.getScraps()[0])

        removeTestObserver.assertValueCount(3)
    }
}

