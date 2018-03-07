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
import com.paper.shared.model.repository.sqlite.PaperTable
import java.lang.reflect.Type

class PaperModelTranslator : JsonSerializer<PaperModel>,
                             JsonDeserializer<PaperModel> {

//    class PaperModel {
//
//        var id: Long = 0L
//
//        var createdAt: Long = 0L
//        var modifiedAt: Long = 0L
//
//        var width: Int = 0
//        var height: Int = 0
//
//        var thumbnailPath: String? = null
//        var thumbnailWidth: Int = 0
//        var thumbnailHeight: Int = 0
//
//        var caption: String = ""
//
//        val scraps: List<PaperScrapModel> = ArrayList()
//    }

    override fun serialize(src: PaperModel,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        // Serialize.
//        root.addProperty(PaperTable.COL_WIDTH, src.widthOverHeight)

        // TODO: Serialize the scraps.

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
