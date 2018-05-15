// Copyright Apr 2018-present boyw165@gmail.com
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

package com.paper.domain.event

import com.paper.model.Point
import com.paper.model.sketch.PenType

/**
 * The abstract drawing SVG event.
 */
sealed class DrawSVGEvent

/**
 * A starting sketch event, where it may provide the pen color, pen size, and
 * pen type information.
 */
data class StartSketchEvent(val point: Point,
                            val penColor: Int = 0,
                            val penSize: Float = 0f,
                            val penType: PenType) : DrawSVGEvent()
/**
 * An on-drawing sketch event.
 */
data class OnSketchEvent(val point: Point) : DrawSVGEvent()
/**
 * A stopping sketch event.
 */
class StopSketchEvent : DrawSVGEvent()
/**
 * A event to clear all the cached sketch.
 */
class ClearAllSketchEvent : DrawSVGEvent()
