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

package com.paper.domain.action

import com.paper.model.repository.IWhiteboardRepository
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable

// .------.
// |      | ----> true or false
// |  ob  |
// |      | ----> IntProgressEvent
// '------'

/**
 * An observable to load paper from DB and then start widget with the model.
 * True if the model is successfully loaded and binding is done. False means
 * neither the model loading nor binding works.
 *
 * There is also a side-effect that it sends IntProgressEvent through the given
 * progress signal.
 */
class DeletePaperSingle(paperID: Long,
                        paperRepo: IWhiteboardRepository,
                        errorSignal: Observer<Throwable>? = null)
    : Single<Boolean>() {

    private val mPaperID = paperID
    private val mPaperRepo = paperRepo

    private val mErrorSignal = errorSignal

    override fun subscribeActual(observer: SingleObserver<in Boolean>) {
        val actualSrc = mPaperRepo
            .deleteBoardById(id = mPaperID)
            .toObservable()
            .publish()
        val actualDisposable = actualSrc
            .subscribe(
                { event ->
                    observer.onSuccess(event.successful)
                },
                { err ->
                    observer.onSuccess(false)
                    mErrorSignal?.onError(err)
                })

        val d = InnerDisposable(actualDisposable = actualDisposable)
        observer.onSubscribe(d)

        // Start to work!
        actualSrc.connect()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    internal class InnerDisposable(actualDisposable: Disposable) : Disposable {

        private var mDisposed = false

        private val mActualDisposable = actualDisposable

        override fun isDisposed(): Boolean {
            return mDisposed
        }

        override fun dispose() {
            mActualDisposable.dispose()
            mDisposed = true
        }
    }
}
