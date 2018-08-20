// Copyright Mar 2017-present Paper
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
import com.paper.model.*
import com.paper.model.sketch.SVGStyle
import com.paper.model.sketch.VectorGraphics
import java.lang.reflect.Type

class VectorGraphicsJSONTranslator : JsonSerializer<VectorGraphics>,
                                     JsonDeserializer<VectorGraphics> {

    companion object {
        const val POINT_SEPARATOR = "-"
    }

    override fun serialize(src: VectorGraphics,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        // Style
        root.add("style", encodeStyle(src.style))

        // Tuple list
        val pathTransJson = encodeTupleList(src.getTupleList())
        root.addProperty("path", pathTransJson)

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): VectorGraphics {
        val root = json.asJsonObject

        // Style
        val style = if (root.has("style")) {
            decodeStyle(root["style"].asJsonObject)
        } else {
            emptySet()
        }

        // Parse path-tuple
        val tupleList = if (root.has("path")) {
            val pathString = root.get("path").asString
            decodeTupleList(pathString)
        } else {
            mutableListOf()
        }

        return VectorGraphics(style = style,
                              tupleList = tupleList)
    }

    private fun encodeStyle(style: Set<SVGStyle>): JsonElement {
        val jsonObject = JsonObject()

        style.forEach { s ->
            when (s) {
                is SVGStyle.Stroke -> {
                    val attributes = JsonArray()

                    // Color is always first
                    attributes.add(Color.toHexString(s.color))
                    attributes.add(s.size)
                    attributes.add(s.closed)

                    jsonObject.add("stroke", attributes)
                }
                is SVGStyle.Fill -> {
                    val attributes = JsonArray()

                    // Color is always first
                    attributes.add(Color.toHexString(s.color))
                    attributes.add(s.rule)

                    jsonObject.add("fill", attributes)
                }
            }
        }

        return jsonObject
    }

    private fun decodeStyle(map: JsonObject): Set<SVGStyle> {
        val set = mutableSetOf<SVGStyle>()

        if (map.has("stroke")) {
            val attributes = map["stroke"].asJsonArray
            set.add(SVGStyle.Stroke(color = Color.parseColor(attributes[0].asString),
                                    size = attributes[1].asFloat,
                                    closed = attributes[2].asBoolean))
        }
        if (map.has("fill")) {
            val attributes = map["fill"].asJsonArray
            set.add(SVGStyle.Fill(color = Color.parseColor(attributes[0].asString),
                                  rule = attributes[1].asInt))
        }

        return set
    }

    private fun encodeTupleList(tupleList: List<PointTuple>): String {
        val builder = StringBuilder()

        tupleList.forEachIndexed { index, p ->
            when (p) {
                is LinearPointTuple -> {
                    builder.append("${p.x},${p.y}")
                }
                is CubicPointTuple -> {
                    builder.append("${p.prevControlX},${p.prevControlY}," +
                                   "${p.currentControlX},${p.currentControlY}," +
                                   "${p.currentEndX},${p.currentEndY}")
                }
            }
            if (index != tupleList.lastIndex) {
                builder.append(POINT_SEPARATOR)
            }
        }

        return builder.toString()
    }

    private fun decodeTupleList(jsonString: String): MutableList<PointTuple> {
        val out = mutableListOf<PointTuple>()
        val tupleStringList = jsonString.split(POINT_SEPARATOR)

        tupleStringList.forEach { p ->
            val pointList = p.split(",")

            if (pointList.size == 2) {
                out.add(LinearPointTuple(pointList[0].toFloat(),
                                         pointList[1].toFloat()))
            } else if (pointList.size == 6) {
                out.add(CubicPointTuple(pointList[0].toFloat(),
                                        pointList[1].toFloat(),
                                        pointList[2].toFloat(),
                                        pointList[3].toFloat(),
                                        pointList[4].toFloat(),
                                        pointList[5].toFloat()))
            }
        }

        return out
    }
}
