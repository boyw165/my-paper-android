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
import com.paper.model.repository.json.PaperJSONTranslator
import com.paper.model.repository.json.SketchStrokeJSONTranslator
import com.paper.model.sketch.SketchStroke
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PaperJSONTranslatorTest {

    private val PAPER_1 = "{\n" +
                          "    \"sketch\": [\n" +
                          "        {\n" +
                          "            \"color\": \"#123456\",\n" +
                          "            \"width\": 0.2,\n" +
                          "            \"path\": \"0.0,0.0,0 0.5,0.5,100\"\n" +
                          "        },\n" +
                          "        {\n" +
                          "            \"color\": \"#654321\",\n" +
                          "            \"width\": 0.8,\n" +
                          "            \"path\": \"0.5,0.5,0\"\n" +
                          "        }\n" +
                          "    ],\n" +
                          "    \"scraps\": []\n" +
                          "}"

    @Test
    fun deserializeDummyPaperWithSketchOnly() {
        val translator = GsonBuilder()
            .registerTypeAdapter(PaperModel::class.java, PaperJSONTranslator())
            .registerTypeAdapter(SketchStroke::class.java, SketchStrokeJSONTranslator())
            .create()

        val paper = translator.fromJson(PAPER_1, PaperModel::class.java)

        Assert.assertEquals(0, paper.scraps.size)
        Assert.assertEquals(2, paper.sketch.size)

        // Stroke #1
        Assert.assertEquals(Color.parseColor("#123456"), paper.sketch[0].color)
        Assert.assertEquals(0.2f, paper.sketch[0].width)
        Assert.assertEquals(2, paper.sketch[0].pointList.size)

        // Stroke #2
        Assert.assertEquals(Color.parseColor("#654321"), paper.sketch[1].color)
        Assert.assertEquals(0.8f, paper.sketch[1].width)
        Assert.assertEquals(1, paper.sketch[1].pointList.size)
    }

    @Test
    fun serializeDummyPaperWithSketchOnly() {
        val translator = GsonBuilder()
            .registerTypeAdapter(PaperModel::class.java, PaperJSONTranslator())
            .registerTypeAdapter(SketchStroke::class.java, SketchStrokeJSONTranslator())
            .create()

        val paper = PaperModel()
        paper.pushStroke(SketchStroke(color = Color.parseColor("#123456"),
                                      width = 0.5f,
                                      isEraser = false)
                                    .addPath(Point(0f, 0f, 0))
                                    .addPath(Point(1f, 1f, 100)))


        Assert.assertEquals("{\"sketch\":[{\"color\":\"#ff123456\",\"width\":0.5,\"path\":\"(0.0,0.0,0) (1.0,1.0,100)\"}],\"scraps\":[]}",
                            translator.toJson(paper, PaperModel::class.java))
    }
}

