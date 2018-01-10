// Copyright (c) 2017-present WANG, TAI-CHUN
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

package com.paper.exp.convexHull

import com.paper.model.ProgressState
import com.paper.protocol.IPresenter
import com.paper.router.Router
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

class ConvexHullPresenter(private val mRouter: Router,
                          private val mWorkerSchedulers: Scheduler,
                          private val mUiSchedulers: Scheduler)
    : IPresenter<ConvexHullContract.View> {

    // View
    private lateinit var mView: ConvexHullContract.View

    // Progress.
    private val mOnUpdateProgress: Subject<ProgressState> = PublishSubject.create()
    private val mOnThrowError: Subject<Throwable> = PublishSubject.create()

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    override fun bindViewOnCreate(view: ConvexHullContract.View) {
        mView = view

        // Back button.
        mDisposablesOnCreate.add(
            mView.onClickBack()
                .observeOn(mUiSchedulers)
                .subscribe { _ ->
                    mRouter.exit()
                })

        // Random button.
        mDisposablesOnCreate.add(
            mView.onClickRandom()
                .throttleFirst(1000, TimeUnit.MILLISECONDS)
                .observeOn(mUiSchedulers)
                .subscribe { _ ->
                    val width = mView.getCanvasWidth().toFloat()
                    val height = mView.getCanvasHeight().toFloat()


                })

        // Error.
        mDisposablesOnCreate.add(
            mOnThrowError
                .observeOn(mUiSchedulers)
                .subscribe { error ->
                    mView.showError(error)
                })
    }

    override fun unBindViewOnDestroy() {
        mDisposablesOnCreate.clear()
    }

    override fun onResume() {
        // DO NOTHING.
    }

    override fun onPause() {
        // DO NOTHING.
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    /**
     * A transformer that catches the exception and convert it to an ERROR state.
     */
    private fun <T> handleError(item: T): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream ->
            upstream.onErrorReturn { error: Throwable ->
                // Bypass the error to the error channel so that someone interested
                // to it get notified.
                mOnThrowError.onNext(error)

                // Return whatever you want~~~
                item
            }
        }
    }
}
