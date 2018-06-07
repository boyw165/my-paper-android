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
import com.paper.model.Rect
import com.paper.model.sketch.PenType

/**
 * Any operation relevant to the canvas.
 */
sealed class CanvasEvent

/**
 * Null event means do nothing with it.
 */
class NullCanvasEvent : CanvasEvent()

// Lifecycle //////////////////////////////////////////////////////////////////

/**
 * A start signal of initialization.
 */
class InitializationBeginEvent : CanvasEvent()

/**
 * A signal of on-going initialization.
 */
class InitializationDoingEvent : CanvasEvent()

/**
 * A stop signal of initialization.
 */
class InitializationEndEvent : CanvasEvent()

// Drawing ////////////////////////////////////////////////////////////////////

/**
 * Indicate the view to renew its rendering buffer.
 */
class InvalidationEvent : CanvasEvent()

/**
 * A starting sketch event, where it may provide the pen color, pen size, and
 * pen type information.
 */
data class StartSketchEvent(val point: Point,
                            val penColor: Int = 0,
                            val penSize: Float = 0f,
                            val penType: PenType) : CanvasEvent()
/**
 * An on-drawing sketch event.
 */
data class OnSketchEvent(val point: Point) : CanvasEvent()
/**
 * A stopping sketch event.
 */
class StopSketchEvent : CanvasEvent()
/**
 * A event to clear all the cached sketch.
 */
class ClearAllSketchEvent : CanvasEvent()

// View-port //////////////////////////////////////////////////////////////////

/**
 * The action representing the operation to view-port.
 * @see [ViewPortBeginUpdateEvent]
 * @see [ViewPortOnUpdateEvent]
 * @see [ViewPortStopUpdateEvent]
 */
abstract class ViewPortEvent : CanvasEvent()

/**
 * A start signal indicating the view-port is about to update.
 */
class ViewPortBeginUpdateEvent : ViewPortEvent()

/**
 * A doing signal indicating the view-port is about to update.
 *
 * @param bound The desired boundary for the view-port.
 */
data class ViewPortOnUpdateEvent(val bound: Rect) : ViewPortEvent()

/**
 * A stop signal indicating the end of the view-port operation.
 */
class ViewPortStopUpdateEvent : ViewPortEvent()
