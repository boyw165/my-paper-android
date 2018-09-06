// Copyright Apr 2018-present boyw165@gmail.com
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

import com.google.gson.GsonBuilder
import com.paper.model.repository.json.FrameJSONTranslator
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class FrameJSONTranslatorTest {

    private val translator by lazy {
        GsonBuilder()
            .registerTypeAdapter(Frame::class.java, FrameJSONTranslator())
            .create()
    }

    @Test
    fun `serialize frame`() {
        val jsonString = translator.toJson(
            Frame(x = 123f,
                  y = 456f,
                  width = 7f,
                  height = 8f,
                  z = 1,
                  scaleX = 1f,
                  scaleY = 1f,
                  rotationInDegrees = 30f),
            Frame::class.java)

        System.out.println("JSON = $jsonString")

        Assert.assertTrue(jsonString, jsonString.contains("\"x\":123.0"))
        Assert.assertTrue(jsonString, jsonString.contains("\"y\":456.0"))
        Assert.assertTrue(jsonString, jsonString.contains("\"width\":7.0"))
        Assert.assertTrue(jsonString, jsonString.contains("\"height\":8.0"))
        Assert.assertTrue(jsonString, jsonString.contains("\"z\":1"))
        Assert.assertTrue(jsonString, jsonString.contains("\"scaleX\":1.0"))
        Assert.assertTrue(jsonString, jsonString.contains("\"scaleY\":1.0"))
        Assert.assertTrue(jsonString, jsonString.contains("\"rotationInDegrees\":30.0"))
    }

    @Test
    fun `deserialize frame`() {
        val frame = translator.fromJson<Frame>(
            "{\"x\":123.0,\"y\":456.0,\"width\":7.0,\"height\":8.0,\"z\":1,\"scaleX\":1.0,\"scaleY\":1.0,\"rotationInDegrees\":30.0}",
            Frame::class.java)

        Assert.assertEquals(123f, frame.x)
        Assert.assertEquals(456f, frame.y)
        Assert.assertEquals(7f, frame.width)
        Assert.assertEquals(8f, frame.height)
        Assert.assertEquals(1, frame.z)
        Assert.assertEquals(1f, frame.scaleX)
        Assert.assertEquals(1f, frame.scaleY)
        Assert.assertEquals(30f, frame.rotationInDegrees)
    }
}
