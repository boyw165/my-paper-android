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
import com.paper.domain.interpolator.LinearInterpolator
import com.paper.model.Point
import java.util.*

class SvgLinearDrawable(
    id: UUID,
    context: IPaperContext,
    points: List<Point> = emptyList(),
    penColor: Int = 0,
    penSize: Float = 1f,
    porterDuffMode: PorterDuffXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER))
    : SvgDrawable(id = id,
                  context = context,
                  points = points,
                  penColor = penColor,
                  penSize = penSize,
                  porterDuffMode = porterDuffMode) {

    override fun addSplineImpl(point: Point) {
        if (mPointList.size > 1) {
            val i = mPointList.lastIndex
            val previous = mPointList[i - 1]
            val current = mPointList[i]

            val spline = LinearInterpolator(start = previous,
                                            end = current)

            mSplineList.add(spline)
        }
    }
}
