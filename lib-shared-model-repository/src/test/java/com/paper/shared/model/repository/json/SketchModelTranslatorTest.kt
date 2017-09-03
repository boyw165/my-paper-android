//  Copyright Sep 2017-present boyw165@gmail.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.paper.shared.model.repository.json

import com.google.gson.GsonBuilder
import com.paper.shared.model.sketch.SketchModel
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileReader

class SketchModelTranslatorTest {

    @Test
    fun deserializeSketch_With_ThreeDots() {
        val gson = GsonBuilder()
            .registerTypeAdapter(SketchModel::class.java, SketchModelTranslator())
            .create()
        val model = gson.fromJson<SketchModel>(SKETCH_WITH_THREE_DOTS, SketchModel::class.java)

        // There should be just 3 strokes.
        Assert.assertEquals(3, model.strokeSize)

        // Check color of the first stroke.
        Assert.assertEquals(0xFFED4956.toInt(), model.getStrokeAt(0).getColor())
        // Check color of the second stroke.
        Assert.assertEquals(0xFF70C050.toInt(), model.getStrokeAt(1).getColor())
        // Check color of the third stroke.
        Assert.assertEquals(0xFF3897F0.toInt(), model.getStrokeAt(2).getColor())

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

    @Test
    fun deserializeJson_From_IosClient() {
        val gson = GsonBuilder()
            .registerTypeAdapter(SketchModel::class.java, SketchModelTranslator())
            .create()
        val classLoader = javaClass.classLoader
        // The JSON is belong to the sketch (ID is 166170906) from the server.
        val resource = classLoader.getResource("json/sketch_created_from_ios_client.json")
        val file = File(resource.path)
        val model = gson.fromJson(FileReader(file), SketchModel::class.java)

        // Exactly has 27 strokes.
        Assert.assertEquals(27, model.strokeSize)

        // Exactly the same x-y pair for the first point of the first path-tuple
        // of the first stroke.
        Assert.assertEquals(0.178925558924675f, model.firstStroke.firstPathTuple.firstPoint.x)
        Assert.assertEquals(0.24099662899971008f, model.firstStroke.firstPathTuple.firstPoint.y)
    }

    companion object {
        private val SKETCH_WITH_THREE_DOTS = "{\"strokes\":[{\"color\":[0.92941177,0.28627452,0.3372549,1.0],\"width\":0.09569436,\"path_tuples\":[[[0.18075603,0.25663146]]]},{\"color\":[0.4392157,0.7529412,0.3137255,1.0],\"width\":0.09569436,\"path_tuples\":[[[0.5118275,0.5168306]]]},{\"color\":[0.21960784,0.5921569,0.9411765,1.0],\"width\":0.09569436,\"path_tuples\":[[[0.8192441,0.74336857]]]}]}"
    }
}
