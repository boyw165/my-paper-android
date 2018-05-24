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
import com.paper.model.Scrap
import com.paper.model.sketch.SketchStroke
import java.lang.reflect.Type
import java.util.*

class ScrapJSONTranslator : JsonSerializer<Scrap>,
                            JsonDeserializer<Scrap> {

    override fun serialize(src: Scrap,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        root.addProperty("uuid", src.uuid.toString())

        root.addProperty("x", src.x)
        root.addProperty("y", src.y)
        root.addProperty("z", src.z)

        root.addProperty("scale", src.scale)
        root.addProperty("rotationInRadians", src.rotationInRadians)

        // See SketchStrokeJSONTranslator.kt
        val sketchJson = JsonArray()
        src.sketch.forEach { sketchJson.add(context.serialize(it, SketchStroke::class.java)) }
        root.add("sketch", sketchJson)

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): Scrap {
        val root = json.asJsonObject

        val model = Scrap(
            uuid = UUID.fromString(root.get("uuid").asString))

        model.x = root.get("x").asFloat
        model.y = root.get("y").asFloat

        if (root.has("z")) {
            model.z = root["z"].asLong
        }

        model.scale = root.get("scale").asFloat
        model.rotationInRadians = root.get("rotationInRadians").asFloat

        // See SketchStrokeJSONTranslator.kt
        if (root.has("sketch")) {
            val sketchJson = root["sketch"].asJsonArray

            sketchJson.forEach {
                model.addStrokeToSketch(
                    context.deserialize(it, SketchStroke::class.java))
            }
        }

        return model
    }
}
