// Copyright (c) 2017-present Cardinalblue
//
// Author: boy@cardinalblue.com
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.paper.observables

import com.paper.router.MyRouter
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.MainThreadDisposable
import ru.terrakok.cicerone.result.ResultListener

/**
 * An observable encapsulate [MyRouter] and emit a boolean to downstream.
 */
class RouterResultSingle(
    private val mSource: MyRouter,
    private val mRequestCode: Int)
    : Single<Any>() {

    override fun subscribeActual(observer: SingleObserver<in Any>) {
        val disposable = Disposable(observer, mSource, mRequestCode)

        mSource.addResultListener(mRequestCode, disposable)

        observer.onSubscribe(disposable)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private class Disposable internal constructor(
        private val observer: SingleObserver<in Any>,
        private val actual: MyRouter,
        private val requestCode: Int)
        : MainThreadDisposable(),
          ResultListener {

        override fun onDispose() {
            actual.removeResultListener(requestCode)
        }

        override fun onResult(resultData: Any) {
            observer.onSuccess(resultData)
        }
    }
}
