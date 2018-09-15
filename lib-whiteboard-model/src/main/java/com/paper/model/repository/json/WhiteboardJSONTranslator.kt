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
import com.paper.model.Whiteboard
import com.paper.model.Scrap
import com.paper.model.Rect
import java.lang.reflect.Type

/**
 * Part of paper is stored as JSON.
 */
class WhiteboardJSONTranslator : JsonSerializer<Whiteboard>,
                                 JsonDeserializer<Whiteboard> {

    override fun serialize(src: Whiteboard,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        // Canvas size
        val (width, height) = src.getSize()
        root.addProperty("width", width)
        root.addProperty("height", height)

        // View port
        val viewPort = src.getViewPort()
        val viewPortJson = JsonArray()
        viewPortJson.add(viewPort.left)
        viewPortJson.add(viewPort.top)
        viewPortJson.add(viewPort.width)
        viewPortJson.add(viewPort.height)
        root.add("view-port", viewPortJson)

        // Scraps
        val scrapJson = JsonArray()
        src.getScraps().forEach { scrapJson.add(context.serialize(it, Scrap::class.java)) }
        root.add("scraps", scrapJson)

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): Whiteboard {
        val root = json.asJsonObject
        val paperDetails = Whiteboard()

        // Canvas size
        val width = root["width"].asFloat
        val height = root["height"].asFloat
        paperDetails.setSize(Pair(width, height))

        // View port
        val viewPortJson = root["view-port"].asJsonArray
        val vx = viewPortJson[0].asFloat
        val vy = viewPortJson[1].asFloat
        val vw = viewPortJson[2].asFloat
        val vh = viewPortJson[3].asFloat
        paperDetails.setViewPort(Rect(vx, vy,
                                      vx + vw,
                                      vy + vh))

        // Scraps
        if (root.has("scraps")) {
            root["scraps"].asJsonArray.forEach {
                paperDetails.addScrap(context.deserialize(
                    it, Scrap::class.java))
            }
        }

        return paperDetails
    }
}
