// Copyright Feb 2018-present boyw165@gmail.com
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

package com.paper.domain.widget.editor

import com.paper.domain.data.DrawingMode
import com.paper.domain.event.DrawSVGEvent
import com.paper.model.IPaper
import com.paper.model.Rect
import io.reactivex.Observable
import java.io.File

interface IPaperCanvasWidget : IWidget<IPaper> {

    // For input //////////////////////////////////////////////////////////////
    // TODO: How to define the inbox?

    fun handleTouchBegin()

    fun handleTouchEnd()

    fun handleTap(x: Float, y: Float)

    fun handleDragBegin(x: Float, y: Float)

    fun handleDrag(x: Float, y: Float)

    fun handleDragEnd(x: Float, y: Float)

    fun setDrawingMode(mode: DrawingMode)

    fun setChosenPenColor(color: Int)

    fun setPenSize(size: Float)

    fun setThumbnail(bmpFile: File, bmpWidth: Int, bmpHeight: Int)

    /**
     * If the view wants to update the thumbnail but not immediately, call this
     * method to indicate the thumbnail is dirty. The dirty state is reset when
     * call [setThumbnail].
     */
    fun invalidateThumbnail()

    // For output /////////////////////////////////////////////////////////////

    fun onSetCanvasSize(): Observable<Rect>

    fun onAddScrapWidget(): Observable<IScrapWidget>

    fun onRemoveScrapWidget(): Observable<IScrapWidget>

    fun onDrawSVG(replayAll: Boolean = true): Observable<DrawSVGEvent>

    fun onPrintDebugMessage(): Observable<String>
}
