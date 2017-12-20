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

package com.paper.exp.rxCancel

import com.paper.protocol.INavigator
import com.paper.protocol.IPresenter
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

class RxCancelPresenter(navigator: INavigator,
                        workerSchedulers: Scheduler,
                        uiScheduler: Scheduler)
    : IPresenter<RxCancelContract.View> {

    // Navigator.
    private val mNavigator = navigator

    // Schedulers.
    private val mWorkerSchedulers = workerSchedulers
    private val mUiSchedulers = uiScheduler

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    override fun bindViewOnCreate(view: RxCancelContract.View) {
        // Close button.
        mDisposablesOnCreate.add(
            view.onClickClose()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(mUiSchedulers)
                .subscribe { _ ->
                    mNavigator.gotoBack()
                })

        // Start button.
        mDisposablesOnCreate.add(
            view.onClickStart()
                .debounce(150, TimeUnit.MILLISECONDS)
                .throttleFirst(1000, TimeUnit.MILLISECONDS)
                .observeOn(mUiSchedulers)
                .subscribe { _ ->
                    view.printLog("start")
                })

        // Cancel button.
        mDisposablesOnCreate.add(
            view.onClickCancel()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(mUiSchedulers)
                .subscribe { _ ->
                    view.printLog("cancel")
                })
    }

    override fun unBindViewOnDestroy() {
        mDisposablesOnCreate.clear()
    }

    override fun onResume() {
    }

    override fun onPause() {
    }
}
