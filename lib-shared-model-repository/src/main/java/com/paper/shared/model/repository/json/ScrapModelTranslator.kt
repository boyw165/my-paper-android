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

package com.paper.shared.model.repository.json

import com.google.gson.*
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.sketch.SketchModel
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
        root.addProperty("width", src.width)
        root.addProperty("height", src.height)

        root.addProperty("scale", src.scale)
        root.addProperty("rotationInRadians", src.rotationInRadians)

        // See SketchModelTranslator.kt
        root.add("sketch", context.serialize(src.sketch))

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
        model.width = root.get("width").asFloat
        model.height = root.get("height").asFloat

        model.scale = root.get("scale").asFloat
        model.rotationInRadians = root.get("rotationInRadians").asFloat

        // See SketchModelTranslator.kt
        model.sketch = context.deserialize(root.get("sketch"), SketchModel::class.java)

        return model
    }
}
