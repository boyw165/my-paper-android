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

import io.useful.changed
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class ScrapTest : BaseModelTest() {

    @Test
    fun `copy, ID should be different`() {
        val tester1 = Scrap()
        val tester2 = tester1.copy()
        tester2.frame = Frame(x = 100f, y = 200f)

        Assert.assertNotEquals(tester2.id, tester1.id)
        Assert.assertNotEquals(tester2.frame, tester1.frame)
    }

    @Test
    fun `set frame`() {
        val scrap = Scrap()

        scrap.frame = Frame(x = 100f, y = 200f)

        Assert.assertEquals(100f, scrap.frame.x)
        Assert.assertEquals(200f, scrap.frame.y)
    }

    @Test
    fun `observe frame`() {
        val scrap = Scrap(frame = Frame(x = -50f, y = -50f))

        val frameTestObserver = scrap::frame
            .changed()
            .test()
        scrap.frame = Frame(x = 100f, y = 200f)
        scrap.frame = Frame(x = 300f, y = 400f)

        frameTestObserver.assertValues(Frame(x = -50f, y = -50f),
                                       Frame(x = 100f, y = 200f),
                                       Frame(x = 300f, y = 400f))
    }
}

