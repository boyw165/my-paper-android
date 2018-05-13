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
import com.paper.model.repository.json.SketchStrokeJSONTranslator
import com.paper.model.sketch.SketchStroke
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SketchStrokeJSONTranslatorTest {

    private val SKETCH_STROKE_1 = "{\"penColor\":\"#FFED4956\",\"penSize\":0.09569436,\"path\":\"(0.18075603,0.25663146,0)\"}"

    @Test
    fun deserializeDummyScrap() {
        val translator = GsonBuilder()
            .registerTypeAdapter(SketchStroke::class.java, SketchStrokeJSONTranslator())
            .create()

        val sketchStroke = translator.fromJson(SKETCH_STROKE_1, SketchStroke::class.java)

        Assert.assertEquals(Color.parseColor("#FFED4956"), sketchStroke.penColor)
        Assert.assertEquals(0.09569436f, sketchStroke.penSize)
        Assert.assertEquals(1, sketchStroke.pointList.size)
    }
}

