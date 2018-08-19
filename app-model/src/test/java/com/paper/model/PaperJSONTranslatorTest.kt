// Copyright Aug 2018-present Paper
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

import com.google.gson.GsonBuilder
import com.paper.model.repository.json.PaperJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.sketch.VectorGraphics
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PaperJSONTranslatorTest {

    private val translator by lazy {
        GsonBuilder()
            .registerTypeAdapter(BasePaper::class.java,
                                 PaperJSONTranslator())
            .registerTypeAdapter(BaseScrap::class.java,
                                 ScrapJSONTranslator())
            .registerTypeAdapter(VectorGraphics::class.java,
                                 VectorGraphicsJSONTranslator())
            .create()
    }

    @Test
    fun `serialize paper without scraps`() {
        val model: IPaper = BasePaper()
        model.setSize(Pair(360f, 480f))
        model.setViewPort(Rect(100f, 100f, 360f, 480f))

        val jsonString = translator.toJson(model, BasePaper::class.java)

        Assert.assertTrue(jsonString.contains("\"width\":360.0"))
        Assert.assertTrue(jsonString.contains("\"height\":480.0"))
        Assert.assertTrue(jsonString.contains("\"view-port\":[100.0,100.0,260.0,380.0]"))

        Assert.assertTrue(jsonString.contains("\"scraps\":[]"))
    }

    @Test
    fun `deserialize paper without scraps`() {
        val model = translator.fromJson<BasePaper>("{\"width\":360.0,\"height\":480.0,\"view-port\":[100.0,100.0,260.0,380.0],\"scraps\":[]}", BasePaper::class.java)

        val (width, height) = model.getSize()
        Assert.assertEquals(360f, width)
        Assert.assertEquals(480f, height)

        val viewPort = model.getViewPort()
        Assert.assertEquals(100f, viewPort.left)
        Assert.assertEquals(100f, viewPort.top)
        Assert.assertEquals(360f, viewPort.right)
        Assert.assertEquals(480f, viewPort.bottom)
        Assert.assertEquals(260f, viewPort.width)
        Assert.assertEquals(380f, viewPort.height)
    }
}

