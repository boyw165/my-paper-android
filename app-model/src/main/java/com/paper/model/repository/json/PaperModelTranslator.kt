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
import com.paper.model.PaperModel
import java.lang.reflect.Type

class PaperModelTranslator : JsonSerializer<PaperModel>,
                             JsonDeserializer<PaperModel> {

    override fun serialize(src: PaperModel,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        // e.g.: root.addProperty(PaperTable.COL_WIDTH, src.widthOverHeight)

        // Serialize the scraps.

        // TODO: Meta-data?

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): PaperModel {
        val model = PaperModel()

        // Deserialize.
        val root = json.asJsonObject

        // width over height.
        //        if (root.has(PaperTable.COL_WIDTH)) {
        //            model.widthOverHeight = root.get(PaperTable.COL_WIDTH).asFloat
        //        }

        return model
    }
}
