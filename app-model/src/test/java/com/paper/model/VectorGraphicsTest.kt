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

import com.paper.model.sketch.VectorGraphics
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VectorGraphicsTest {

    @Test
    fun `equal hash code`() {
        val randomList = mutableListOf<Point>()
        val hashCode1 = randomList.hashCode()

        randomList.add(Point(1f, 2f))
        val hashCode2 = randomList.hashCode()

        randomList.remove(Point(1f, 2f))
        val hashCode3 = randomList.hashCode()

        Assert.assertNotEquals(hashCode1, hashCode2)
        Assert.assertEquals(hashCode1, hashCode3)
    }

    @Test
    fun `add path, hash code should change`() {
        val stroke1 = VectorGraphics()
        val stroke2 = stroke1.addTuple(LinearPointTuple(0f, 0f))

        Assert.assertNotEquals(stroke2.hashCode(), stroke1.hashCode())
    }
}

