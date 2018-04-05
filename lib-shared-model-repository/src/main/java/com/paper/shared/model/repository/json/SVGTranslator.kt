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
                'M','m' -> {
                    val data = it.removeRange(0,1)
                    val point = data.split(",")
                    pathTuples.add(PathTuple(point[0].toFloat(), point[1].toFloat()))
                }
                'L','l' -> {
                    val data = it.removeRange(0,1)
                    val point = data.split(",")
                    pathTuples.add(PathTuple(point[0].toFloat(), point[1].toFloat()))
                }
                // there might be more case
            }
        }

        return pathTuples
    }
}
