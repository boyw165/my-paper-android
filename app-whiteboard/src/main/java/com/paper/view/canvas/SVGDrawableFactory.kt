// Copyright Aug 2018-present Paper
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

import androidx.annotation.IntDef
import java.util.*

object SVGDrawableFactory {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(LINEAR,
            CUBIC_HERMITE,
            CUBIC_BEZIER)
    annotation class Interpolator

    const val LINEAR = 1.shl(0)
    const val CUBIC_HERMITE = 1.shl(1)
    const val CUBIC_BEZIER = 1.shl(2)

    fun createSVGDrawable(strokeID: UUID,
                          context: IPaperContext,
                          penColor: Int,
                          penSize: Float,
                          @Interpolator interpolator: Int): SVGDrawable {
        return when (interpolator) {
            CUBIC_HERMITE -> {
                SVGHermiteCubicDrawable(id = strokeID,
                                        context = context,
                                        penColor = penColor,
                                        penSize = penSize)
            }
            CUBIC_BEZIER -> {
                SVGCubicBezierDrawable(id = strokeID,
                                       context = context,
                                       penColor = penColor,
                                       penSize = penSize)
            }
            LINEAR -> {
                SVGLinearDrawable(id = strokeID,
                                  context = context,
                                  penColor = penColor,
                                  penSize = penSize)
            }
            else -> throw IllegalArgumentException("Invalid path interpolator ID")
        }
    }
}
