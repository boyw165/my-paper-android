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

@RunWith(MockitoJUnitRunner.Silent::class)
class FrameTest {

    @Test
    fun `add another frame`() {
        val tester1 = Frame()
        val tester2 = tester1.add(Frame(x = 1f,
                                        y = 1f,
                                        width = 100f,
                                        height = 100f,
                                        scaleX = 0.2f,
                                        scaleY = 0.2f,
                                        rotationInDegrees = 10f,
                                        z = 1))

        Assert.assertEquals(Frame(x = 1f,
                                  y = 1f,
                                  width = 100f,
                                  height = 100f,
                                  scaleX = 0.2f,
                                  scaleY = 0.2f,
                                  rotationInDegrees = 10f,
                                  z = 1), tester2)
    }

    @Test
    fun `subtract another frame`() {
        val tester1 = Frame()
        val tester2 = tester1.sub(Frame(x = 1f,
                                        y = 1f,
                                        width = 100f,
                                        height = 100f,
                                        scaleX = 0.2f,
                                        scaleY = 0.2f,
                                        rotationInDegrees = 10f,
                                        z = 1))

        Assert.assertEquals(Frame(x = -1f,
                                  y = -1f,
                                  width = -100f,
                                  height = -100f,
                                  scaleX = -0.2f,
                                  scaleY = -0.2f,
                                  rotationInDegrees = -10f,
                                  z = -1), tester2)
    }
}

