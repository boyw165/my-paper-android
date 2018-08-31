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

package com.paper.domain.ui_event

import com.paper.domain.ui.IBaseScrapWidget
import com.paper.model.Frame
import com.paper.model.Rect
import com.paper.model.sketch.VectorGraphics
import java.util.*

sealed class EditorEvent

data class GroupEditorEvent(val events: List<EditorEvent>) : EditorEvent()

// Lifecycle //////////////////////////////////////////////////////////////////

abstract class EditorTouchLifecycleEvent : EditorEvent()

object TouchBeginEvent : EditorTouchLifecycleEvent()

object TouchEndEvent : EditorTouchLifecycleEvent()

// Add/remove/focus scrap /////////////////////////////////////////////////////

abstract class UpdateScrapEvent : EditorEvent()

data class GroupUpdateScrapEvent(val events: List<UpdateScrapEvent>) : UpdateScrapEvent()

data class AddScrapEvent(val scrap: IBaseScrapWidget) : UpdateScrapEvent()

data class RemoveScrapEvent(val scrap: IBaseScrapWidget) : UpdateScrapEvent()

object RemoveAllScrapsEvent : UpdateScrapEvent()

data class FocusScrapEvent(val scrapID: UUID) : UpdateScrapEvent()

object ClearFocusEvent : UpdateScrapEvent()

data class HighlightScrapEvent(val scrapID: UUID) : UpdateScrapEvent()

object ClearHighlightEvent : UpdateScrapEvent()

data class UpdateScrapFrameEvent(val scrapID: UUID,
                                 val frame: Frame) : UpdateScrapEvent()

// Draw ///////////////////////////////////////////////////////////////////////

abstract class UpdateScrapContentEvent : UpdateScrapEvent()

data class AddSketchEvent(val svg: VectorGraphics) : UpdateScrapContentEvent()

data class RemoveSketchEvent(val svg: VectorGraphics) : UpdateScrapContentEvent()

data class StartSketchEvent(val x: Float,
                            val y: Float) : UpdateScrapContentEvent()

data class DoSketchEvent(val x: Float,
                         val y: Float) : UpdateScrapContentEvent()

data class DoLineToEvent(val x: Float,
                         val y: Float) : UpdateScrapContentEvent()

data class DoCubicToEvent(val previousControlX: Float,
                          val previousControlY: Float,
                          val currentControlX: Float,
                          val currentControlY: Float,
                          val currentEndX: Float,
                          val currentEndY: Float) : UpdateScrapContentEvent()

object StopSketchEvent : UpdateScrapContentEvent()

// View-port //////////////////////////////////////////////////////////////////

/**
 * The action representing the operation to view-port.
 * @see [ViewPortBeginUpdateEvent]
 * @see [ViewPortOnUpdateEvent]
 * @see [ViewPortStopUpdateEvent]
 */
abstract class ViewPortEvent : EditorEvent()

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
