// Copyright Mar 2018-present boyw165@gmail.com
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

package com.paper.model.repository.json

import com.google.gson.*
import com.paper.model.Color
import com.paper.model.ScrapModel
import com.paper.model.sketch.SketchStroke
import java.lang.reflect.Type
import java.util.*

class ScrapModelTranslator : JsonSerializer<ScrapModel>,
                             JsonDeserializer<ScrapModel> {

    override fun serialize(src: ScrapModel,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        root.addProperty("uuid", src.uuid.toString())

        root.addProperty("x", src.x)
        root.addProperty("y", src.y)

        root.addProperty("scale", src.scale)
        root.addProperty("rotationInRadians", src.rotationInRadians)

        // See SketchModelTranslator.kt
        root.add("sketch", serializeSketch(src.sketch))

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): ScrapModel {
        val root = json.asJsonObject

        val model = ScrapModel(
            uuid = UUID.fromString(root.get("uuid").asString))

        model.x = root.get("x").asFloat
        model.y = root.get("y").asFloat

        model.scale = root.get("scale").asFloat
        model.rotationInRadians = root.get("rotationInRadians").asFloat

        // See SketchModelTranslator.kt
        if (root.has("sketch")) {
            val sketch = deserializeSketch(root["sketch"])
            sketch.forEach { model.addStrokeToSketch(it) }
        }

        return model
    }

    private fun serializeSketch(sketch: List<SketchStroke>): JsonElement {
        val root = JsonObject()
        val strokeListObj = JsonArray()

        // A sketch contains several strokes.
        root.add("strokes", strokeListObj)
        // {"strokes": [{stroke}, {}, ...]
        for (stroke in sketch) {
            val strokeObj = JsonObject()

            strokeListObj.add(strokeObj)

            // Stroke color, #ARGB
            strokeObj.addProperty("color", "#${Integer.toHexString(stroke.color)}")
            // Stroke width.
            strokeObj.addProperty("width", stroke.width)

            // Save via SVG format
            val svgTransJson = SVGTranslator.toSVG(stroke.pathTupleList)
            strokeObj.addProperty("path", svgTransJson)
        }

        return root
    }

    private fun deserializeSketch(src: JsonElement): List<SketchStroke> {
        val sketch = mutableListOf<SketchStroke>()
        val strokeArray = src.asJsonArray

        // Parse stroke object.
        for (i in 0 until strokeArray.size()) {
            val strokeJson = strokeArray.get(i).asJsonObject
            val stroke = SketchStroke()

            // Parse path-tuple
            val pathString = strokeJson.get("path").asString
            // Add tuplePath to the stroke.
            stroke.addAllPathTuple(SVGTranslator.fromSVG(pathString))

            // Stroke color, #ARGB
            val colorTicket = strokeJson.get("color").asString
            stroke.color = Color.parseColor(colorTicket)

            // Stroke width.
            stroke.width = strokeJson.get("width").asFloat

            // Add stroke to the sketch.
            sketch.add(stroke)
        }

        return sketch
    }
}
