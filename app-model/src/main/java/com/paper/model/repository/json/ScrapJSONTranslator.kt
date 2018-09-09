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
import com.paper.model.*
import com.paper.model.sketch.VectorGraphics
import java.lang.reflect.Type
import java.util.*

class ScrapJSONTranslator : JsonSerializer<Scrap>,
                            JsonDeserializer<Scrap> {

    override fun serialize(src: Scrap,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()
        val frame = src.getFrame()

        root.addProperty("uuid", src.getID().toString())

        root.addProperty("x", frame.x)
        root.addProperty("y", frame.y)
        root.addProperty("width", frame.width)
        root.addProperty("height", frame.height)

        root.addProperty("z", frame.z)

        root.addProperty("scaleX", frame.scaleX)
        root.addProperty("scaleY", frame.scaleY)
        root.addProperty("rotationInDegrees", frame.rotationInDegrees)

        when (src) {
            is SketchScrap -> {
                // If it is a VectorGraphics scrap ...
                root.addProperty("type", ScrapType.SKETCH)

                val svg = src.getSVG()
                root.add("svg", context.serialize(svg, VectorGraphics::class.java))
            }
            else -> TODO()
        }

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): Scrap {
        val root = json.asJsonObject
        val id = UUID.fromString(root.get("uuid").asString)
        val type = root["type"].asString

        // Positioning, scale, and rotation
        val frame = Frame(root.get("x").asFloat,
                          root.get("y").asFloat,
                          root.get("width").asFloat,
                          root.get("height").asFloat,
                          root.get("scaleX").asFloat,
                          root.get("scaleY").asFloat,
                          root.get("rotationInDegrees").asFloat,
                          if (root.has("z")) root["z"].asInt else ModelConst.MOST_BOTTOM_Z)

        val model = when (type) {
            ScrapType.SKETCH -> {
                // Must have a "svg" property
                val sketchJson = root["svg"]
                val svg = context.deserialize<VectorGraphics>(sketchJson, VectorGraphics::class.java)

                SketchScrap(uuid = id,
                            frame = frame,
                            svg = svg)
            }
            else -> TODO()
        }

        return model
    }
}
