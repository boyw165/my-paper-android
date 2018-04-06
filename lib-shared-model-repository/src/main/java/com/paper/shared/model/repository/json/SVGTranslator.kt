// Copyright Apr 2018-present Paper
//
// Author: djken0106@gmail.com,
//         boyw165@gmail.com
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

import com.google.gson.JsonArray
import com.paper.shared.model.sketch.PathTuple

object SVGTranslator {

    fun pathTupleToSVG(tupleList: List<PathTuple>) : JsonArray {
        val pathTupleJson = JsonArray()
        var svgString = ""
        tupleList.forEachIndexed { index, tuple ->
            // FIXME current just need for the Path, it should be handle more cases
            if(index == 0) {
                tuple.allPoints.forEach { point ->
                    svgString += "M"
                    svgString += point.x
                    svgString += ","
                    svgString += point.y
                }
            } else {
                tuple.allPoints.forEach { point ->
                    svgString += " L"
                    svgString += point.x
                    svgString += ","
                    svgString += point.y
                }
            }
        }
        pathTupleJson.add(svgString)

        return pathTupleJson
    }

    fun svgToPathTuple(svgString: String) : List<PathTuple> {
        val pathTuples: MutableList<PathTuple> = mutableListOf()
        val tuples = svgString.split(" ").toTypedArray()

        tuples.forEach {
            // FIXME maybe need to add some parameters for PathTuple
            val prefix = it[0] // prefix case for different SVG
            when (prefix) {
                'M', 'm' -> {
                    val data = it.removeRange(0, 1)
                    val point = data.split(",")
                    pathTuples.add(PathTuple(point[0].toFloat(), point[1].toFloat()))
                }
                'L', 'l' -> {
                    val data = it.removeRange(0, 1)
                    val point = data.split(",")
                    pathTuples.add(PathTuple(point[0].toFloat(), point[1].toFloat()))
                }
                // there might be more case
            }
        }

        return pathTuples
    }
}
