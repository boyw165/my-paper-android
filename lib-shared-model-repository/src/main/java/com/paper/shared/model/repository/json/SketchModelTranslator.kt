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

            // Stroke color, #ARGB
            strokeObj.addProperty("color", olorIntToHexString(stroke.color))
            // Stroke width.
            strokeObj.addProperty("width", stroke.width)

            // Save via SVG format
            val svgTransJson = SVGTranslator.toSVG(stroke.pathTupleList)
            strokeObj.addProperty("path", svgTransJson)
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
            val pathString = strokeJson.get("path").asString
            // Add tuplePath to the stroke.
            stroke.addAllPathTuple(SVGTranslator.fromSVG(pathString))

            // Stroke color, #ARGB
            val colorTicket = strokeJson.get("color").asString
            stroke.color = hexStringToColorInt(colorTicket)

            // Stroke width.
            stroke.width = strokeJson.get("width").asFloat

            // Add stroke to the sketch.
            model.addStroke(stroke)
        }

        return model
    }

    private fun olorIntToHexString(color: Int): String {
        return "#${Integer.toHexString(color).toUpperCase()}"
    }

    private fun hexStringToColorInt(ticket: String): Int {
        val hex = ticket.substring(1)
        return Integer.parseInt(hex.substring(0, 2), 16).shl(24) +
               Integer.parseInt(hex.substring(2), 16)
    }
}
