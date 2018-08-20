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
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.sketch.SVGStyle.Fill
import com.paper.model.sketch.SVGStyle.Stroke
import com.paper.model.sketch.VectorGraphics
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VectorGraphicsJSONTranslatorTest {

    private val pointSeparator = VectorGraphicsJSONTranslator.POINT_SEPARATOR
    private val translator by lazy {
        GsonBuilder()
            .registerTypeAdapter(VectorGraphics::class.java, VectorGraphicsJSONTranslator())
            .create()
    }

    @Test
    fun `serialize style`() {
        val graphics = VectorGraphics(style = mutableSetOf(Fill(color = Color.RED,
                                                                rule = Fill.NONE_ZERO),
                                                           Stroke(size = 1.5f,
                                                                  color = Color.GREEN,
                                                                  closed = true)))
        val jsonString = translator.toJson(graphics)

        System.out.println("JSON = $jsonString")

        Assert.assertTrue(jsonString, jsonString.contains("\"style\""))
        Assert.assertTrue(jsonString, jsonString.contains("\"fill\":[\"#FFFF0000\",${Fill.NONE_ZERO}]"))
        Assert.assertTrue(jsonString, jsonString.contains("\"stroke\":[\"#FF00FF00\",1.5,true]"))
    }

    @Test
    fun `deserialize style`() {
        val svg = translator.fromJson("{\"style\":{\"fill\":[\"#FFFF0000\",1],\"stroke\":[\"#FF00FF00\",1.5,true]}}", VectorGraphics::class.java)

        svg.style.forEach { s ->
            when (s) {
                is Fill -> {
                    Assert.assertEquals(Color.RED, s.color)
                    Assert.assertEquals(Fill.NONE_ZERO, s.rule)
                }
                is Stroke -> {
                    Assert.assertEquals(Color.GREEN, s.color)
                    Assert.assertEquals(1.5f, s.size)
                    Assert.assertEquals(true, s.closed)
                }
            }
        }
    }

    @Test
    fun `serialize linear path`() {
        val graphics = VectorGraphics(style = mutableSetOf(Fill(color = Color.RED,
                                                                rule = Fill.NONE_ZERO),
                                                           Stroke(size = 1.5f,
                                                                  color = Color.GREEN,
                                                                  closed = true)),
                                      tupleList = mutableListOf(LinearPointTuple(0f, 0f),
                                                                LinearPointTuple(1f, 1f)))
        val jsonString = translator.toJson(graphics)

        System.out.println("JSON = $jsonString")

        Assert.assertTrue(jsonString, jsonString.contains("\"path\":\"0.0,0.0${pointSeparator}1.0,1.0\""))
    }

    @Test
    fun `serialize cubic path`() {
        val graphics = VectorGraphics(style = mutableSetOf(Fill(color = Color.RED,
                                                                rule = Fill.NONE_ZERO),
                                                           Stroke(size = 1.5f,
                                                                  color = Color.GREEN,
                                                                  closed = true)),
                                      tupleList = mutableListOf(LinearPointTuple(0f, 0f),
                                                                CubicPointTuple(1f, 1f,
                                                                                2f, 2f,
                                                                                3f, 3f)))
        val jsonString = translator.toJson(graphics)

        System.out.println("JSON = $jsonString")

        Assert.assertTrue(jsonString, jsonString.contains("\"path\":\"" +
                                                          "0.0,0.0$pointSeparator" +
                                                          "1.0,1.0,2.0,2.0,3.0,3.0" +
                                                          "\""))
    }

    @Test
    fun `deserialize cubic path`() {
        val svg = translator.fromJson("{\"path\":\"" +
                                      "0.1,0.2$pointSeparator" +
                                      "1.1,1.2,2.1,2.2,3.1,3.2" +
                                      "\"}", VectorGraphics::class.java)

        Assert.assertTrue(svg.getTupleAt(0) is LinearPointTuple)
        Assert.assertEquals(0.1f, (svg.getTupleAt(0) as LinearPointTuple).x)
        Assert.assertEquals(0.2f, (svg.getTupleAt(0) as LinearPointTuple).y)

        Assert.assertTrue(svg.getTupleAt(1) is CubicPointTuple)
        Assert.assertEquals(1.1f, (svg.getTupleAt(1) as CubicPointTuple).prevControlX)
        Assert.assertEquals(1.2f, (svg.getTupleAt(1) as CubicPointTuple).prevControlY)
        Assert.assertEquals(2.1f, (svg.getTupleAt(1) as CubicPointTuple).currentControlX)
        Assert.assertEquals(2.2f, (svg.getTupleAt(1) as CubicPointTuple).currentControlY)
        Assert.assertEquals(3.1f, (svg.getTupleAt(1) as CubicPointTuple).currentEndX)
        Assert.assertEquals(3.2f, (svg.getTupleAt(1) as CubicPointTuple).currentEndY)
    }
}
