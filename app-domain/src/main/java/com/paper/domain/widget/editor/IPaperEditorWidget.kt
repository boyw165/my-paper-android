// Copyright July 2018-present Paper
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

package com.paper.domain.widget.editor

import com.paper.domain.event.UndoRedoEvent
import com.paper.model.event.ProgressEvent
import io.reactivex.Observable
import java.io.File

interface IPaperEditorWidget {

    fun start(paperID: Long)

    fun stop()

    /**
     * Request to stop; It is granted to stop when receiving a true; vice versa.
     */
    fun requestStop(bmpFile: File,
                    bmpWidth: Int,
                    bmpHeight: Int): Observable<Boolean>

    // Canvas widget & functions //////////////////////////////////////////////

    fun eraseCanvas(): Observable<Boolean>

    fun onCanvasWidgetReady(): Observable<IPaperCanvasWidget>

    // Edit panel widget //////////////////////////////////////////////////////

    fun onEditPanelWidgetReady(): Observable<PaperEditPanelWidget>

    // Undo & redo ////////////////////////////////////////////////////////////

    fun addUndoSignal(source: Observable<Any>)

    fun addRedoSignal(source: Observable<Any>)

    fun onGetUndoRedoEvent(): Observable<UndoRedoEvent>

    // Progress & error & Editor status ///////////////////////////////////////

    fun onUpdateProgress(): Observable<ProgressEvent>
}
