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
import com.paper.model.repository.json.SketchStrokeJSONTranslator
import com.paper.model.sketch.SketchStroke
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ScrapJSONTranslatorTest {

    @Test
    fun serializeDummyScrap() {
        val translator = GsonBuilder()
            .registerTypeAdapter(ScrapModel::class.java, ScrapJSONTranslator())
            .registerTypeAdapter(SketchStroke::class.java, SketchStrokeJSONTranslator())
            .create()

        val model = ScrapModel()
        val uuid = model.uuid
        model.x = 100f
        model.y = 200f
        model.scale = 0.5f

        model.addStrokeToSketch(SketchStroke(color = Color.parseColor("#FF0000"),
                                             width = 0.5f,
                                             isEraser = false)
                                    .addAllPath(listOf(Point(0.18075603f,
                                                             0.25663146f,
                                                             0),
                                                       Point(0.5f,
                                                             0.5f,
                                                             100))))

        Assert.assertEquals("{\"uuid\":\"$uuid\",\"x\":100.0,\"y\":200.0,\"scale\":0.5,\"rotationInRadians\":0.0,\"sketch\":[{\"color\":\"#ffff0000\",\"width\":0.5,\"path\":\"(0.18075603,0.25663146,0) (0.5,0.5,100)\"}]}",
                translator.toJson(model, ScrapModel::class.java))
    }

    private val TEST_SCRAP_JSON = "{\"uuid\":\"f80f62e5-e85d-4a77-bc0f-e128a92b749d\",\"x\":100.0,\"y\":200.0,\"scale\":0.5,\"rotationInRadians\":0.0,\"sketch\":[{\"path\":\"(0.18075603,0.25663146,0) (0.5,0.5,100)\",\"color\":\"#00000000\",\"width\":0.5,\"isEraser\":false}]}"

    @Test
    fun deserializeScrap_With_Sketch() {
        val gson = GsonBuilder()
            .registerTypeAdapter(ScrapModel::class.java, ScrapJSONTranslator())
            .registerTypeAdapter(SketchStroke::class.java, SketchStrokeJSONTranslator())
            .create()
        val model = gson.fromJson<ScrapModel>(TEST_SCRAP_JSON, ScrapModel::class.java)

        Assert.assertEquals("f80f62e5-e85d-4a77-bc0f-e128a92b749d", model.uuid.toString())

        Assert.assertEquals(100f, model.x)
        Assert.assertEquals(200f, model.y)
        Assert.assertEquals(0.5f, model.scale)

        // Every sketch has just one point.
        for (sketch in model.sketch) {
            Assert.assertEquals(2, sketch.pointList.size)
        }

        // Match x-y pair exactly.
        for ((i, stroke) in model.sketch.withIndex()) {
            when (i) {
                0 -> {
                    Assert.assertEquals(0.18075603f, stroke.pointList[0].x)
                    Assert.assertEquals(0.25663146f, stroke.pointList[0].y)
                    Assert.assertEquals(0, stroke.pointList[0].time)
                }
                1 -> {
                    Assert.assertEquals(0.5f, stroke.pointList[0].x)
                    Assert.assertEquals(0.5f, stroke.pointList[0].y)
                    Assert.assertEquals(100, stroke.pointList[0].time)
                }
            }
        }
    }
}

