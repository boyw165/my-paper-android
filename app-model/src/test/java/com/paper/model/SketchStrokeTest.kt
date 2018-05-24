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

import com.paper.model.sketch.SketchStroke
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SketchStrokeTest {

    @Test
    fun addPath_hashCodeShouldBeDifferent() {
        val stroke = SketchStroke()

        stroke.addPath(Point(0f, 0f))

        val hashCode1 = stroke.hashCode()

        stroke.addPath(Point(0f, 0f))

        val hashCode2 = stroke.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)
    }

    @Test
    fun updateZ_hashCodeShouldBeDifferent() {
        val stroke = SketchStroke()

        stroke.addPath(Point(0f, 0f))
        stroke.addPath(Point(1f, 1f))
        stroke.addPath(Point(2f, 2f))

        val hashCode1 = stroke.hashCode()

        stroke.z = 0

        val hashCode2 = stroke.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)
    }
}

