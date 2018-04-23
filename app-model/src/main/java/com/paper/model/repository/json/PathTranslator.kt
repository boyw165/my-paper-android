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

package com.paper.model.repository.json

import com.paper.model.Point


object PathTranslator {

    fun toPath(pathPointList: List<Point>): String {
        val builder = StringBuilder()

        pathPointList.forEachIndexed { index, p ->
            builder.append("${p.x},${p.y},${p.time}")
            if (index != pathPointList.lastIndex) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    fun fromPath(pointsString: String): List<Point> {
        val pathPoints: MutableList<Point> = mutableListOf()
        val points = pointsString.split(" ").toTypedArray()

        points.forEach { p ->
            val point = p.split(",")
            pathPoints.add(Point(point[0].toFloat(), point[1].toFloat(), point[2].toLong()))
        }

        return pathPoints
    }
}
