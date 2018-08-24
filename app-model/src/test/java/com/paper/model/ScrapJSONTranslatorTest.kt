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

import com.google.gson.GsonBuilder
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.sketch.VectorGraphics
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ScrapJSONTranslatorTest {

    private val translator by lazy {
        GsonBuilder()
            .registerTypeAdapter(BaseScrap::class.java, ScrapJSONTranslator())
            .registerTypeAdapter(VectorGraphics::class.java, VectorGraphicsJSONTranslator())
            .create()
    }

    @Test
    fun `serialize svg scrap with empty path tuple list`() {
        val model = SVGScrap(
            mutableFrame = Frame(x = 100f,
                                 y = 200f,
                                 width = 360f,
                                 height = 480f,
                                 scaleX = 0.1f,
                                 scaleY = 0.2f,
                                 rotationInDegrees = 30f,
                                 z = ModelConst.MOST_BOTTOM_Z))
        val uuid = model.uuid

        val jsonText = translator.toJson(model, BaseScrap::class.java)
        System.out.println("JSON = $jsonText")

        Assert.assertTrue(jsonText.contains("\"uuid\":\"$uuid\""))
        Assert.assertTrue(jsonText.contains("\"type\":\"svg\""))

        Assert.assertTrue(jsonText.contains("\"x\":100.0"))
        Assert.assertTrue(jsonText.contains("\"y\":200.0"))
        Assert.assertTrue(jsonText.contains("\"width\":360.0"))
        Assert.assertTrue(jsonText.contains("\"height\":480.0"))
        Assert.assertTrue(jsonText.contains("\"z\":${ModelConst.MOST_BOTTOM_Z}"))
        Assert.assertTrue(jsonText.contains("\"scaleX\":0.1"))
        Assert.assertTrue(jsonText.contains("\"scaleY\":0.2"))
        Assert.assertTrue(jsonText.contains("\"rotationInDegrees\":30.0"))

        Assert.assertTrue(jsonText.contains("\"svg\":[]"))
    }

    @Test
    fun `deserialize svg scrap with empty tuple list`() {
        val model = translator.fromJson<SVGScrap>("{\"uuid\":\"f80f62e5-e85d-4a77-bc0f-e128a92b749d\",\"type\":\"svg\",\"x\":100.0,\"y\":200.0,\"width\":360.0,\"height\":480.0,\"z\":1,\"scaleX\":0.5,\"scaleY\":0.5,\"rotationInDegrees\":30.0,\"svg\":[]}", BaseScrap::class.java)

        Assert.assertEquals("f80f62e5-e85d-4a77-bc0f-e128a92b749d", model.getID().toString())

        Assert.assertEquals(100f, model.getFrame().x)
        Assert.assertEquals(200f, model.getFrame().y)
        Assert.assertEquals(360f, model.getFrame().width)
        Assert.assertEquals(480f, model.getFrame().height)
        Assert.assertEquals(1, model.getFrame().z)
        Assert.assertEquals(0.5f, model.getFrame().scaleX)
        Assert.assertEquals(0.5f, model.getFrame().scaleY)
        Assert.assertEquals(30f, model.getFrame().rotationInDegrees)
    }
}

