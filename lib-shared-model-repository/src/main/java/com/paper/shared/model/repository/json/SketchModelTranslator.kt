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
import com.paper.shared.model.PaperModel
import com.paper.shared.model.repository.sqlite.SketchTable
import com.paper.shared.model.sketch.SketchModel
import java.lang.reflect.Type

class SketchModelTranslator : JsonSerializer<SketchModel>,
                              JsonDeserializer<SketchModel> {

    override fun serialize(src: SketchModel,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        root.addProperty(SketchTable.COL_WIDTH, src.width)
        root.addProperty(SketchTable.COL_HEIGHT, src.height)

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): SketchModel {
        val model = SketchModel()

        val root = json.asJsonObject

        // width.
        if (root.has(SketchTable.COL_WIDTH)) {
            model.width = root.get(SketchTable.COL_WIDTH).asInt
        }
        // height.
        if (root.has(SketchTable.COL_HEIGHT)) {
            model.height = root.get(SketchTable.COL_HEIGHT).asInt
        }

        return model
    }
}
