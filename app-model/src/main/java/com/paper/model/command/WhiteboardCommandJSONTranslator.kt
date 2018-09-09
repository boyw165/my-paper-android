// Copyright Sep 2018-present Whiteboard
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

package com.paper.model.command

import com.google.gson.*
import com.paper.model.Scrap
import com.paper.model.Frame
import java.lang.reflect.Type
import java.util.*

class WhiteboardCommandJSONTranslator : JsonSerializer<WhiteboardCommand>,
                                        JsonDeserializer<WhiteboardCommand> {

    companion object {

        const val KEY_SIGNATURE = "signature"
        const val KEY_COMMAND_ID = "command_id"
        const val KEY_SCRAP_ID = "scrap_id"
        const val KEY_SCRAP = "scrap"
        const val KEY_SCRAP_FRAME_DELTA = "scrap_frame_delta"
    }

    override fun serialize(command: WhiteboardCommand,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val json = JsonObject()

        when (command) {
            is AddScrapCommand -> {
                json.addProperty(KEY_SIGNATURE, "AddScrapCommand")
                json.addProperty(KEY_COMMAND_ID, command.commandID.toString())
                json.add(KEY_SCRAP, context.serialize(command.scrap, Scrap::class.java))
            }
            is RemoveScrapCommand -> {
                json.addProperty(KEY_SIGNATURE, "RemoveScrapCommand")
                json.addProperty(KEY_COMMAND_ID, command.commandID.toString())
                json.add(KEY_SCRAP, context.serialize(command.scrap, Scrap::class.java))
            }
            is UpdateScrapFrameCommand -> {
                json.addProperty(KEY_SIGNATURE, "UpdateScrapFrameCommand")
                json.addProperty(KEY_COMMAND_ID, command.commandID.toString())
                json.addProperty(KEY_SCRAP_ID, command.scrapID.toString())

                val frameJson = context.serialize(command.toFrame, Frame::class.java)
                json.add(KEY_SCRAP_FRAME_DELTA, frameJson)
            }
        }

        return json
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): WhiteboardCommand {
        val signature = json.asJsonObject[KEY_SIGNATURE].asString

        return when (signature) {
            "AddScrapCommand" -> {
                val commandID = UUID.fromString(json.asJsonObject[KEY_COMMAND_ID].asString)
                val scrapJson = json.asJsonObject[KEY_SCRAP]
                AddScrapCommand(commandID = commandID,
                                scrap = context.deserialize(scrapJson, Scrap::class.java))
            }
            "RemoveScrapCommand" -> {
                val commandID = UUID.fromString(json.asJsonObject[KEY_COMMAND_ID].asString)
                val scrapJson = json.asJsonObject[KEY_SCRAP]
                RemoveScrapCommand(commandID = commandID,
                                   scrap = context.deserialize(scrapJson, Scrap::class.java))
            }
            "UpdateScrapFrameCommand" -> {
                val commandID = UUID.fromString(json.asJsonObject[KEY_COMMAND_ID].asString)
                val scrapID = UUID.fromString(json.asJsonObject[KEY_SCRAP_ID].asString)
                val frameJson = json.asJsonObject[KEY_SCRAP_FRAME_DELTA]
                UpdateScrapFrameCommand(commandID = commandID,
                                        scrapID = scrapID,
                                        toFrame = context.deserialize(frameJson, Frame::class.java))
            }
            else -> TODO()
        }
    }
}
