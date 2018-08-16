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
import com.paper.model.sketch.PenType
import com.paper.model.sketch.VectorGraphics
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

private const val TEST_SCRAP_JSON = "{\"uuid\":\"f80f62e5-e85d-4a77-bc0f-e128a92b749d\",\"type\":\"svg\",\"x\":100.0,\"y\":200.0,\"z\":1,\"scaleX\":0.5,\"scaleY\":0.5,\"rotationInDegrees\":30.0,\"svg\":[{\"path\":\"(0.18075603,0.25663146,0) (0.5,0.5,100)\",\"penType\":\"pen\",\"penColor\":\"#00000000\",\"penSize\":0.5}]}"

@RunWith(MockitoJUnitRunner::class)
class ScrapJSONTranslatorTest {

    private val translator by lazy {
        GsonBuilder()
            .registerTypeAdapter(BaseScrap::class.java, ScrapJSONTranslator())
            .registerTypeAdapter(VectorGraphics::class.java, VectorGraphicsJSONTranslator())
            .create()
    }

    @Test
    fun `serialize svg scrap`() {
        val model = SVGScrap()
        val uuid = model.uuid
        model.setFrame(Frame(x = 100f,
                             y = 200f,
                             z = ModelConst.MOST_BOTTOM_Z,
                             scaleX = 0.1f,
                             scaleY = 0.2f,
                             rotationInDegrees = 30f))

        model.addSVG(VectorGraphics(penColor = Color.parseColor("#FF0000"),
                                    penSize = 0.5f,
                                    penType = PenType.PEN)
                         .addAllPath(listOf(Point(0.18075603f,
                                                  0.25663146f,
                                                  0),
                                            Point(0.5f,
                                                  0.5f,
                                                  100))))

        val jsonText = translator.toJson(model, BaseScrap::class.java)
        System.out.println("JSON output = $jsonText")

        Assert.assertTrue(jsonText.contains("\"uuid\":\"$uuid\""))
        Assert.assertTrue(jsonText.contains("\"type\":\"svg\""))

        Assert.assertTrue(jsonText.contains("\"x\":100.0"))
        Assert.assertTrue(jsonText.contains("\"y\":200.0"))
        Assert.assertTrue(jsonText.contains("\"z\":${ModelConst.MOST_BOTTOM_Z}"))
        Assert.assertTrue(jsonText.contains("\"scaleX\":0.1"))
        Assert.assertTrue(jsonText.contains("\"scaleY\":0.2"))
        Assert.assertTrue(jsonText.contains("\"rotationInDegrees\":30.0"))

        Assert.assertTrue(jsonText.contains("\"penType\":\"pen\""))
        Assert.assertTrue(jsonText.contains("\"penColor\":\"#FFFF0000\""))
        Assert.assertTrue(jsonText.contains("\"penSize\":0.5"))

        Assert.assertTrue(jsonText.contains("\"path\":\"(0.18075603,0.25663146,0) (0.5,0.5,100)\""))
    }

    @Test
    fun `deserialize svg scrap`() {
        val model = translator.fromJson<SVGScrap>(TEST_SCRAP_JSON, BaseScrap::class.java)

        Assert.assertEquals("f80f62e5-e85d-4a77-bc0f-e128a92b749d", model.getId().toString())

        Assert.assertEquals(100f, model.getFrame().x)
        Assert.assertEquals(200f, model.getFrame().y)
        Assert.assertEquals(1, model.getFrame().z)
        Assert.assertEquals(0.5f, model.getFrame().scaleX)
        Assert.assertEquals(0.5f, model.getFrame().scaleY)
        Assert.assertEquals(30f, model.getFrame().rotationInDegrees)

        // Every sketch has just one point.
        model.getSVGs().forEach { svg ->
            Assert.assertEquals(2, svg.pointList.size)
        }

        // Match x-y pair exactly.
        model.getSVGs().forEachIndexed { i, svg ->
            when (i) {
                0 -> {
                    Assert.assertEquals(0.18075603f, svg.pointList[i].x)
                    Assert.assertEquals(0.25663146f, svg.pointList[i].y)
                    Assert.assertEquals(0, svg.pointList[i].time)
                }
                1 -> {
                    Assert.assertEquals(0.5f, svg.pointList[i].x)
                    Assert.assertEquals(0.5f, svg.pointList[i].y)
                    Assert.assertEquals(100, svg.pointList[i].time)
                }
            }
        }
    }
}

