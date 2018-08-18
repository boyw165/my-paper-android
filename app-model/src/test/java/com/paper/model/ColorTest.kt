// Copyright Apr 2018-present Paper
//
// Author: djken0106@gmail.com,
//         boyw165@gmail.com
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
class ColorTest {

    @Test
    fun `hex color palette to int`() {
        // Without explicit alpha
        Assert.assertEquals(0xFFFF0000.toInt(), Color.parseColor("#FF0000"))
        Assert.assertEquals(0xFF00FF00.toInt(), Color.parseColor("#00FF00"))
        Assert.assertEquals(0xFF0000FF.toInt(), Color.parseColor("#0000FF"))

        // WIth explicit alpha
        Assert.assertEquals(0xFFFF0000.toInt(), Color.parseColor("#FFFF0000"))
        Assert.assertEquals(0xFF00FF00.toInt(), Color.parseColor("#FF00FF00"))
        Assert.assertEquals(0xFF0000FF.toInt(), Color.parseColor("#FF0000FF"))
    }

    @Test
    fun `int color to hex string`() {
        Assert.assertEquals("#FFFF0000", Color.toHexString(Color.RED))
        Assert.assertEquals("#FF00FF00", Color.toHexString(Color.GREEN))
        Assert.assertEquals("#FF0000FF", Color.toHexString(Color.BLUE))
    }
}
