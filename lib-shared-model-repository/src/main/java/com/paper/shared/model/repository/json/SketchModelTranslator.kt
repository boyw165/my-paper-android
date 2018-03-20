//  Copyright Aug 2017-present boyw165@gmail.com
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

import com.google.gson.*
import com.paper.shared.model.sketch.PathTuple
import com.paper.shared.model.sketch.SketchModel
import com.paper.shared.model.sketch.SketchStroke
import java.lang.reflect.Type

class SketchModelTranslator : JsonSerializer<SketchModel>,
                              JsonDeserializer<SketchModel> {

    override fun serialize(src: SketchModel,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()
        val strokeListObj = JsonArray()

        // A sketch contains several strokes.
        root.add("strokes", strokeListObj)
        // {"strokes": [{stroke}, {}, ...]
        for (stroke in src.allStrokes) {
            val strokeObj = JsonObject()

            strokeListObj.add(strokeObj)

            // Stroke color. The format is [R,G,B,A].
            val colorJson = JsonArray()
            colorJson.add((stroke.getColor() shr 16 and 0xFF).toFloat() / 0xFF)
            colorJson.add((stroke.getColor() shr 8 and 0xFF).toFloat() / 0xFF)
            colorJson.add((stroke.getColor() and 0xFF).toFloat() / 0xFF)
            colorJson.add((stroke.getColor() shr 24 and 0xFF).toFloat() / 0xFF)
            strokeObj.add("color", colorJson)
            // Stroke width.
            strokeObj.addProperty("width", stroke.getWidth())

            // ... and several tuple.
            val tuplesJson = JsonArray()
            strokeObj.add("path_tuples", tuplesJson)
            for (tuple in stroke.allPathTuple) {
                val tupleJson = JsonArray()

                // Add tuple to the tuple list.
                tuplesJson.add(tupleJson)

                // A tuple contains several points.
                for (point in tuple.allPoints) {
                    val pointObj = JsonArray()

                    pointObj.add(point.x)
                    pointObj.add(point.y)

                    // Add point to the tuple.
                    tupleJson.add(pointObj)
                }
            }
        }

        return root
    }

    override fun deserialize(src: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): SketchModel {
        val model = SketchModel()
        val rootJson = src.asJsonObject

        // Get "strokes" attribute.
        val strokesJson = rootJson.getAsJsonArray("strokes")
        // Parse stroke object.
        for (i in 0 until strokesJson.size()) {
            val strokeJson = strokesJson.get(i).asJsonObject
            val stroke = SketchStroke()

            // Parse path-tuple
            val pathTuplesJson = strokeJson.get("path_tuples").asJsonArray
            for (j in 0 until pathTuplesJson.size()) {
                val pathTupleJson = pathTuplesJson.get(j).asJsonArray
                val pathTuple = PathTuple()

                // Add point to the tuple.
                // It's a heterogeneous array,
                // could be [f, f] or [[f, f], [f, f], ...].
                if (pathTupleJson.get(0).isJsonPrimitive) {
                    // If it is [f, f]...
                    pathTuple.addPoint(
                        pathTupleJson.get(0).asFloat,
                        pathTupleJson.get(1).asFloat)
                } else if (pathTupleJson.get(0).isJsonArray) {
                    // If it is [[f, f], [f, f], ...]...
                    for (k in 0 until pathTupleJson.size()) {
                        val pointJson = pathTupleJson.get(k).asJsonArray
                        pathTuple.addPoint(
                            pointJson.get(0).asFloat,
                            pointJson.get(1).asFloat)
                    }
                }

                // Add tuple to the stroke.
                stroke.add(pathTuple)
            }

            // Stroke color.
            val colorJson = strokeJson.get("color")
            if (colorJson.isJsonPrimitive) {
                stroke.setColor(colorJson.asInt)
            } else if (colorJson.isJsonArray) {
                val rgbaJson = colorJson.asJsonArray

                // [R,G,B,A] and each element is a normalized number.
                val r = (rgbaJson.get(0).asFloat * 0xFF).toInt()
                val g = (rgbaJson.get(1).asFloat * 0xFF).toInt()
                val b = (rgbaJson.get(2).asFloat * 0xFF).toInt()
                val a = (rgbaJson.get(3).asFloat * 0xFF).toInt()
                stroke.setColor((a shl 24).or(r shl 16).or(g shl 8).or(b))
            }

            // Stroke width.
            stroke.setWidth(strokeJson.get("width").asFloat)

            // Add stroke to the sketch.
            model.addStroke(stroke)
        }

        return model
    }
}
