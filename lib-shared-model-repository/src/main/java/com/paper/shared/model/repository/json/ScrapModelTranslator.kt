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
import java.lang.reflect.Type

class ScrapModelTranslator : JsonSerializer<ScrapModel>,
                             JsonDeserializer<ScrapModel> {

    //open class ScrapModel {
    //
    //    val uuid: UUID = UUID.randomUUID()
    //
    //    var x: Float = 0f
    //    var y: Float = 0f
    //    var width: Float = 1f
    //    var height: Float = 1f
    //
    //    var scale: Float = 1f
    //    var rotationInRadians: Float = 0f
    //
    //    var sketch: SketchModel? = null
    //}

    override fun serialize(src: ScrapModel,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        // e.g.: root.addProperty(PaperTable.COL_WIDTH, src.widthOverHeight)
        root.addProperty("uuid", src.uuid.toString())

        root.addProperty("x", src.x)
        root.addProperty("y", src.y)
        root.addProperty("width", src.width)
        root.addProperty("height", src.height)

        root.addProperty("scale", src.scale)
        root.addProperty("rotationInRadians", src.rotationInRadians)

        root.add("sketch", context.serialize(src.sketch))

        // Serialize the scraps.

        // TODO: Meta-data?

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): ScrapModel {
        val model = ScrapModel()

        // Deserialize.
        val root = json.asJsonObject

        // width over height.
        //        if (root.has(PaperTable.COL_WIDTH)) {
        //            model.widthOverHeight = root.get(PaperTable.COL_WIDTH).asFloat
        //        }

        return model
    }
}
