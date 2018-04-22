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

import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ScrapJSONTranslatorTest {

    private val SKETCH_WITH_THREE_DOTS = "{\"strokes\":[{\"color\":\"#FFED4956\",\"width\":0.09569436,\"path\":\"M0.18075603,0.25663146 Z\"},{\"color\":\"#FF70C050\",\"width\":0.09569436,\"path\":\"M0.5118275,0.5168306 Z\"},{\"color\":\"#FF3897F0\",\"width\":0.09569436,\"path\":\"M0.8192441,0.74336857 Z\"}]}"

//    @Test
//    fun serializeDummyScrap() {
//        val translator = GsonBuilder()
//            .registerTypeAdapter(ScrapModel::class.java, ScrapJSONTranslator())
//            .create()
//
//        val model = ScrapModel()
//
//        model.x = 100f
//        model.y = 200f
//
//        model.addStrokeToSketch(SketchStroke(color = Color.parseColor("#FF0000"),
//                                             width = 0.5f,
//                                             isEraser = false)
//                                    .addAllPathTuple(listOf(PathTuple(0.18075603f,
//                                                                      0.25663146f),
//                                                            PathTuple())))
//    }
//
//    @Test
//    fun deserializeSketch_With_ThreeDots() {
//        val gson = GsonBuilder()
//            .registerTypeAdapter(SketchModel::class.java, SketchModelTranslator())
//            .create()
//        val model = gson.fromJson<SketchModel>(SKETCH_WITH_THREE_DOTS, SketchModel::class.java)
//
//        // There should be just 3 strokes.
//        Assert.assertEquals(3, model.strokeSize)
//
//        // Check color of the first stroke.
//        Assert.assertEquals(0xFFED4956.toInt(), model.getStrokeAt(0).color)
//        // Check color of the second stroke.
//        Assert.assertEquals(0xFF70C050.toInt(), model.getStrokeAt(1).color)
//        // Check color of the third stroke.
//        Assert.assertEquals(0xFF3897F0.toInt(), model.getStrokeAt(2).color)
//
//        // Every dot stroke has just one tuple-point.
//        for (stroke in model.allStrokes) {
//            Assert.assertEquals(1, stroke.pathTupleSize())
//        }
//
//        // Every dot stroke's tuple-point has just one point.
//        for (stroke in model.allStrokes) {
//            Assert.assertEquals(1, stroke.firstPathTuple.pointSize)
//        }
//
//        // Match x-y pair exactly.
//        for ((i, stroke) in model.allStrokes.withIndex()) {
//            when (i) {
//                0 -> {
//                    Assert.assertEquals(0.18075603f, stroke.firstPathTuple.firstPoint.x)
//                    Assert.assertEquals(0.25663146f, stroke.firstPathTuple.firstPoint.y)
//                }
//                1 -> {
//                    Assert.assertEquals(0.5118275f, stroke.firstPathTuple.firstPoint.x)
//                    Assert.assertEquals(0.5168306f, stroke.firstPathTuple.firstPoint.y)
//                }
//                2 -> {
//                    Assert.assertEquals(0.8192441f, stroke.firstPathTuple.firstPoint.x)
//                    Assert.assertEquals(0.74336857f, stroke.firstPathTuple.firstPoint.y)
//                }
//            }
//        }
//    }
//
//    @Test
//    fun deserializeAndThenSerialize_stringShouldBeSame() {
//        val gson = GsonBuilder()
//            .registerTypeAdapter(SketchModel::class.java, SketchModelTranslator())
//            .create()
//        // From JSON.
//        val model = gson.fromJson<SketchModel>(SKETCH_WITH_THREE_DOTS, SketchModel::class.java)
//        // To Json.
//        val jsonString = gson.toJson(model, SketchModel::class.java)
//
//        // Two strings should be the same.
//        Assert.assertEquals(SKETCH_WITH_THREE_DOTS, jsonString)
//    }
}

