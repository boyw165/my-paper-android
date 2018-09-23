// Copyright Sep 2018-present Paper
//
// Author: boyw165@gmail.com
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
import com.paper.model.Frame
import com.paper.model.ModelConst
import java.lang.reflect.Type

class FrameJSONTranslator : JsonSerializer<Frame>,
                            JsonDeserializer<Frame> {

    override fun serialize(frame: Frame,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        root.addProperty("x", frame.x)
        root.addProperty("y", frame.y)
        root.addProperty("width", frame.width)
        root.addProperty("height", frame.height)

        root.addProperty("z", frame.z)

        root.addProperty("scaleX", frame.scaleX)
        root.addProperty("scaleY", frame.scaleY)
        root.addProperty("rotationInDegrees", frame.rotationInDegrees)

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): Frame {
        val root = json.asJsonObject

        // Positioning, scale, and rotation
        return Frame(x = if (root.has("x")) root.get("x").asFloat else 0f,
                     y = if (root.has("y")) root.get("y").asFloat else 0f,
                     width = if (root.has("width")) root.get("width").asFloat else 0f,
                     height = if (root.has("height")) root.get("height").asFloat else 0f,
                     scaleX = if (root.has("scaleX")) root.get("scaleX").asFloat else 1f,
                     scaleY = if (root.has("scaleY")) root.get("scaleY").asFloat else 1f,
                     rotationInDegrees = if (root.has("rotationInDegrees")) root.get("rotationInDegrees").asFloat else 0f,
                     z = if (root.has("z")) root["z"].asInt else ModelConst.MOST_BOTTOM_Z)
    }
}
