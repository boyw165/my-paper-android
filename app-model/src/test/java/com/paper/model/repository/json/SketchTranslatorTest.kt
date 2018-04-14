package com.paper.model.repository.json

// Copyright Mar 2017-present boyw165@gmail.com
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

import com.google.gson.GsonBuilder
import com.paper.model.sketch.SketchModel
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.io.FileReader

@RunWith(MockitoJUnitRunner::class)
class SketchTranslatorTest {

    @Test
    fun deserializeSketch_With_ThreeDots() {
        val gson = GsonBuilder()
            .registerTypeAdapter(SketchModel::class.java, SketchModelTranslator())
            .create()
        val model = gson.fromJson<SketchModel>(SKETCH_WITH_THREE_DOTS, SketchModel::class.java)

        // There should be just 3 strokes.
        Assert.assertEquals(3, model.strokeSize)

        // Check color of the first stroke.
        Assert.assertEquals(0xFFED4956.toInt(), model.getStrokeAt(0).color)
        // Check color of the second stroke.
        Assert.assertEquals(0xFF70C050.toInt(), model.getStrokeAt(1).color)
        // Check color of the third stroke.
        Assert.assertEquals(0xFF3897F0.toInt(), model.getStrokeAt(2).color)

        // Every dot stroke has just one tuple-point.
        for (stroke in model.allStrokes) {
            Assert.assertEquals(1, stroke.pathTupleSize())
        }

        // Every dot stroke's tuple-point has just one point.
        for (stroke in model.allStrokes) {
            Assert.assertEquals(1, stroke.firstPathTuple.pointSize)
        }

        // Match x-y pair exactly.
        for ((i, stroke) in model.allStrokes.withIndex()) {
            when (i) {
                0 -> {
                    Assert.assertEquals(0.18075603f, stroke.firstPathTuple.firstPoint.x)
                    Assert.assertEquals(0.25663146f, stroke.firstPathTuple.firstPoint.y)
                }
                1 -> {
                    Assert.assertEquals(0.5118275f, stroke.firstPathTuple.firstPoint.x)
                    Assert.assertEquals(0.5168306f, stroke.firstPathTuple.firstPoint.y)
                }
                2 -> {
                    Assert.assertEquals(0.8192441f, stroke.firstPathTuple.firstPoint.x)
                    Assert.assertEquals(0.74336857f, stroke.firstPathTuple.firstPoint.y)
                }
            }
        }
    }

    @Test
    fun deserializeAndThenSerialize_stringShouldBeSame() {
        val gson = GsonBuilder()
            .registerTypeAdapter(SketchModel::class.java, SketchModelTranslator())
            .create()
        // From JSON.
        val model = gson.fromJson<SketchModel>(SKETCH_WITH_THREE_DOTS, SketchModel::class.java)
        // To Json.
        val jsonString = gson.toJson(model, SketchModel::class.java)

        // Two strings should be the same.
        Assert.assertEquals(SKETCH_WITH_THREE_DOTS, jsonString)
    }

//    @Test
//    fun deserializeJson_From_IosClient() {
//        val gson = GsonBuilder()
//            .registerTypeAdapter(SketchModel::class.java, SketchModelTranslator())
//            .create()
//        val classLoader = javaClass.classLoader
//        // The JSON is belong to the sketch (ID is 166170906) from the server.
//        val resource = classLoader.getResource("json/sketch_created_from_ios_client.json")
//        val file = File(resource.path)
//        val model = gson.fromJson(FileReader(file), SketchModel::class.java)
//
//        // Exactly has 27 strokes.
//        Assert.assertEquals(27, model.strokeSize)
//
//        // Exactly the same x-y pair for the first point of the first path-tuple
//        // of the first stroke.
//        Assert.assertEquals(0.178925558924675f, model.firstStroke.firstPathTuple.firstPoint.x)
//        Assert.assertEquals(0.24099662899971008f, model.firstStroke.firstPathTuple.firstPoint.y)
//    }

    companion object {
        private val SKETCH_WITH_THREE_DOTS = "{\"strokes\":[{\"color\":\"#FFED4956\",\"width\":0.09569436,\"path\":\"M0.18075603,0.25663146 Z\"},{\"color\":\"#FF70C050\",\"width\":0.09569436,\"path\":\"M0.5118275,0.5168306 Z\"},{\"color\":\"#FF3897F0\",\"width\":0.09569436,\"path\":\"M0.8192441,0.74336857 Z\"}]}"
    }
}

