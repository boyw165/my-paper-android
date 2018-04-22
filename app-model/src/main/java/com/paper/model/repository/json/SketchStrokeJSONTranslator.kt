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
import com.paper.model.sketch.SketchStroke
import java.lang.reflect.Type

class SketchStrokeJSONTranslator : JsonSerializer<SketchStroke>,
                                   JsonDeserializer<SketchStroke> {

    override fun serialize(src: SketchStroke,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        // Stroke color, #ARGB
        root.addProperty("color", "#${Integer.toHexString(src.color)}")
        // Stroke width.
        root.addProperty("width", src.width)

        // Save via SVG format
        val svgTransJson = SVGTranslator.toSVG(src.pathTupleList)
        root.addProperty("path", svgTransJson)

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): SketchStroke {
        val root = json.asJsonObject
        val model = SketchStroke()

        // Parse path-tuple
        val pathString = root.get("path").asString
        // Add tuplePath to the stroke.
        model.addAllPathTuple(SVGTranslator.fromSVG(pathString))

        // Stroke color, #ARGB
        val colorTicket = root.get("color").asString
        model.color = Color.parseColor(colorTicket)

        // Stroke width.
        model.width = root.get("width").asFloat

        return model
    }
}
