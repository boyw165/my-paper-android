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
import com.paper.model.sketch.PenType
import com.paper.model.sketch.SketchStroke
import java.lang.reflect.Type

class SketchStrokeJSONTranslator : JsonSerializer<SketchStroke>,
                                   JsonDeserializer<SketchStroke> {

    override fun serialize(src: SketchStroke,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        // Pen type
        root.addProperty("penType", encodePenType(src.penType))
        // Pen color, #ARGB
        root.addProperty("penColor", "#${Integer.toHexString(src.penColor)}")
        // Pen size
        root.addProperty("penSize", src.penSize)

        // Save via points format
        val pathTransJson = PathTranslator.toPath(src.pointList)
        root.addProperty("path", pathTransJson)

        // Z index
        root.addProperty("z", src.z)

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): SketchStroke {
        val root = json.asJsonObject

        // Pen type
        val penType = if (root.has("penType")) {
            decodePenType(root["penType"].asString)
        } else {
            PenType.PEN
        }
        // Pen color, #ARGB
        val penColor = Color.parseColor(root.get("penColor").asString)
        // Pen width
        val penSize = root.get("penSize").asFloat

        val model = SketchStroke(penColor = penColor,
                                 penSize = penSize,
                                 penType = penType)

        // Parse path-tuple
        val pathString = root.get("path").asString
        // Add pointPath to the stroke.
        model.addAllPath(PathTranslator.fromPath(pathString))

        // Z index
        if (root.has("z")) {
            model.z = root["z"].asLong
        }

        return model
    }

    private fun encodePenType(type: PenType): String {
        return when (type) {
            PenType.PEN -> "pen"
            PenType.ERASER -> "eraser"
        }
    }

    private fun decodePenType(typeString: String): PenType {
        return when (typeString) {
            "pen" -> PenType.PEN
            "eraser" -> PenType.ERASER
            else -> PenType.PEN
        }
    }
}
