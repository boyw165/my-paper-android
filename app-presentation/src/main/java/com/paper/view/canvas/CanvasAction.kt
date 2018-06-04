// Copyright Jun 2018-present Paper
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

import com.paper.model.Rect

sealed class CanvasAction

/**
 * Null action
 */
class DummyCanvasAction : CanvasAction()

// View-port //////////////////////////////////////////////////////////////////

/**
 * The action representing the operation to view-port.
 * @see [ViewPortBeginUpdateAction]
 * @see [ViewPortOnUpdateAction]
 * @see [ViewPortStopUpdateAction]
 */
abstract class ViewPortAction : CanvasAction()

/**
 * A start signal indicating the view-port is about to update.
 */
class ViewPortBeginUpdateAction : ViewPortAction()

/**
 * A doing signal indicating the view-port is about to update.
 *
 * @param bound The desired boundary for the view-port.
 */
data class ViewPortOnUpdateAction(val bound: Rect) : ViewPortAction()

/**
 * A stop signal indicating the end of the view-port operation.
 */
class ViewPortStopUpdateAction : ViewPortAction()
