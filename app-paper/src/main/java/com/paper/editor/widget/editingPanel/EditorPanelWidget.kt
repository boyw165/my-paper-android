// Copyright Mar 2018-present boyw165@gmail.com
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

package com.paper.editor.widget.editingPanel

import com.paper.editor.data.DrawViewPortEvent
import com.paper.shared.model.Rect
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

class EditorPanelWidget(
    private val mUiScheduler: Scheduler,
    private val mWorkerScheduler: Scheduler) {

    private val mDisposables = CompositeDisposable()

    fun handleUpdateViewPort(canvas: Rect,
                                      viewPort: Rect) {
        TODO("not implemented")
    }

    fun handleChoosePrimaryFunction(id: Int) {
        TODO("not implemented")
    }

    fun onUpdateViewPort(): Observable<DrawViewPortEvent> {
        TODO("not implemented")
    }

    fun onChooseSecondaryTool(): Observable<Any> {
        TODO("not implemented")
    }
}
