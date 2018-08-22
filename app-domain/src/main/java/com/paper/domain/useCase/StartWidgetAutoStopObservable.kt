// Copyright Apr 2018-present Paper
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

package com.paper.domain.useCase

import com.paper.domain.DomainConst
import com.paper.domain.vm.IWidget
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

/**
 * Bind the [IWidget] with the model and automatically destroy the binding if it
 * gets disposed.
 */
class StartWidgetAutoStopObservable(private val widget: IWidget,
                                    private val caughtErrorSignal: Observer<Throwable>? = null)
    : Observable<Boolean>() {

    override fun subscribeActual(observer: Observer<in Boolean>) {
        val d = UnbindDisposable(widget, caughtErrorSignal)
        observer.onSubscribe(d)

        if (!d.isDisposed) {
            try {
                widget.start()
                println("${DomainConst.TAG}: Widget [$widget] starts")

                observer.onNext(true)
            } catch (err: Throwable) {
                observer.onNext(false)

                caughtErrorSignal?.onNext(err)
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    internal class UnbindDisposable(private val widget: IWidget,
                                    private val caughtErrorSignal: Observer<Throwable>?)
        : Disposable {

        @Volatile
        private var disposed = false

        override fun isDisposed(): Boolean {
            return disposed
        }

        override fun dispose() {
            disposed = true

            try {
                widget.stop()
            } catch (err: Throwable) {
                caughtErrorSignal?.onNext(err)
            }
            println("${DomainConst.TAG}: Widget [$widget] stops")
        }
    }
}
