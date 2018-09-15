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

@RunWith(MockitoJUnitRunner::class)
class PointTest {

    @Test
    fun differentValue() {
        Assert.assertNotEquals(Point(1f, 2f), Point(2f, 2f))
    }

    @Test
    fun sameValue() {
        Assert.assertEquals(Point(1f, 2f), Point(1f, 2f))
    }

    @Test
    fun norm() {
        Assert.assertEquals(1f, Point(0f, 1f).norm().toFloat())
        Assert.assertEquals(1f, Point(1f, 0f).norm().toFloat())
    }

    @Test
    fun dotProduct() {
        Assert.assertEquals(0f, Point(0f, 1f).doProduct(Point(1f, 0f)).toFloat())
        Assert.assertEquals(2f, Point(1f, 1f).doProduct(Point(1f, 1f)).toFloat())
    }
}

