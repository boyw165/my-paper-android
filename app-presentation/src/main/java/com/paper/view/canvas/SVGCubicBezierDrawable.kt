// Copyright Apr 2018-present Paper
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

package com.paper.view.canvas

import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.paper.domain.interpolator.CubicBezierInterpolator
import com.paper.model.Point
import java.util.*

/**
 * Reference:
 * - http://www.malinc.se/m/MakingABezierSpline.php
 */
class SVGCubicBezierDrawable(id: UUID,
                             context: IPaperContext,
                             points: List<Point> = emptyList(),
                             penColor: Int = 0,
                             penSize: Float = 1f,
                             porterDuffMode: PorterDuffXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER))
    : SVGDrawable(id = id,
                  context = context,
                  points = points,
                  penColor = penColor,
                  penSize = penSize,
                  porterDuffMode = porterDuffMode) {

    override fun addSplineImpl(point: Point) {
        if (mPointList.size > 1) {
            val i = mPointList.lastIndex
            val start = mPointList[i - 1]
            val end = mPointList[i]

            val spline = if (mPointList.size == 2) {
                CubicBezierInterpolator(start = start,
                                        startControl = Point(0.67f * start.x + 0.33f * end.x,
                                                             0.67f * start.y + 0.33f * end.y),
                                        end = end,
                                        endControl = Point(0.33f * start.x + 0.67f * end.x,
                                                           0.33f * start.y + 0.67f * end.y))
            } else {
                // TODO: Use B-Bezier when the drawing architecture allows to
                // Current implementation is more like A-Frame cubic bezier
                val previousSpline = mSplineList.last() as CubicBezierInterpolator
                val previousEndControl = previousSpline.endControl

                // To get start control
                val startControl = Point(2f * start.x - previousEndControl.x,
                                         2f * start.y - previousEndControl.y)

                // To get end control

                val endControl = Point((startControl.x + end.x) / 2f,
                                       (startControl.y + end.y) / 2f)

                CubicBezierInterpolator(start = start,
                                        startControl = startControl,
                                        end = end,
                                        endControl = endControl)
            }

            mSplineList.add(spline)
        }
    }
}
