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

package com.paper.domain.ui.manipulator

import com.cardinalblue.gesture.rx.GestureEvent
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable

abstract class BaseManipulator : ObservableTransformer<GestureEvent, WhiteboardCommand> {

    protected val disposableBag = CompositeDisposable()

    open fun stop() {
        disposableBag.dispose()
    }

    /**
     * An observable that gives true after labmda runs successfully and automatically
     * calls [stop] when it gets disposed
     */
    protected fun autoStop(lambda: (emitter: ObservableEmitter<WhiteboardCommand>) -> Unit): Observable<WhiteboardCommand> {
        return Observable.create { emitter ->
            // Enable auto-stop
            emitter.setCancellable { stop() }
            // Run lambda
            lambda(emitter)
        }
    }
}
