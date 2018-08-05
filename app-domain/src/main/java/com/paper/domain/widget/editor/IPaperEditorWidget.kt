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

// TODO: It is more like a presenter
interface IPaperEditorWidget {

    /**
     * The entry for initialization.
     *
     * @return A set of signals, where most of them provide the widget which is
     * ready for binding with view.
     */
    fun start(paperID: Long): Observable<OnStart>

    /**
     * The entry for destruction.
     */
    fun stop()

    /**
     * The set the signals returned by [start], where most of them provide the
     * widget which is ready for binding with view.
     */
    data class OnStart(val onCanvasWidgetReady: Observable<IPaperCanvasWidget>,
                       val onMenuWidgetReady: Observable<PaperEditPanelWidget>,
                       val onGetUndoRedoEvent: Observable<UndoRedoEvent>)

    /**
     * Request to stop; It is granted to stop when receiving a true; vice versa.
     */
    fun requestStop(bmpFile: File,
                    bmpWidth: Int,
                    bmpHeight: Int): Observable<Boolean>

    // Canvas widget & functions //////////////////////////////////////////////

    fun eraseCanvas(): Observable<Boolean>

    // Undo & redo ////////////////////////////////////////////////////////////

    // FIXME: Use subject and flat operator
    fun addUndoSignal(source: Observable<Any>)

    // FIXME: Use subject and flat operator
    fun addRedoSignal(source: Observable<Any>)

    // Progress & error & Editor status ///////////////////////////////////////

    fun onUpdateProgress(): Observable<ProgressEvent>
}
