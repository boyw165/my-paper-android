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

import android.util.Log
import com.paper.model.ProgressState
import com.paper.protocol.INavigator
import com.paper.protocol.IPresenter
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

class RxCancelPresenter(navigator: INavigator,
                        workerSchedulers: Scheduler,
                        uiScheduler: Scheduler)
    : IPresenter<RxCancelContract.View> {

    // Intents.
    companion object {
        val INTENT_OF_CANCEL_EVERYTHING = -1
        val INTENT_OF_DO_SOMETHING = 0
    }

    // Navigator.
    private val mNavigator = navigator

    // Schedulers.
    private val mWorkerSchedulers = workerSchedulers
    private val mUiSchedulers = uiScheduler

    // Progress.
    private val mOnUpdateProgress: Subject<ProgressState> = PublishSubject.create()

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

        //
        //   start +
        //          \
        //           +----> something in between ----> end.
        //          /
        //  cancel +
        //
        mDisposablesOnCreate.add(
            Observable
                .merge(
                    // Start button.
                    view.onClickStart()
                        .debounce(150, TimeUnit.MILLISECONDS)
                        .throttleFirst(1000, TimeUnit.MILLISECONDS)
                        .map { _ -> INTENT_OF_DO_SOMETHING },
                    // Cancel button.
                    view.onClickCancel()
                        .debounce(150, TimeUnit.MILLISECONDS)
                        .map { _ -> INTENT_OF_CANCEL_EVERYTHING })
                // Create do-something intent or cancel intent.
                .switchMap { intent ->
                    when (intent) {
                        INTENT_OF_DO_SOMETHING -> toDoSmtAction(75)
                        INTENT_OF_CANCEL_EVERYTHING -> toCancelAction()
                        else -> toCancelAction()
                    }
                }
                .observeOn(mUiSchedulers)
                .subscribe { _ ->
                    view.printLog("all finished!")
                })

        // Progress.
        mDisposablesOnCreate.add(
            mOnUpdateProgress
                .observeOn(mUiSchedulers)
                .subscribe { state ->
                    when {
                        state.justStart -> view.printLog("--- START ---")
                        state.justStop -> view.printLog("---!!! STOP !!!---")
                        state.doing -> view.printLog(
                            "doing %d%%...".format(
                                state.progress))
                    }
                })
    }

    override fun unBindViewOnDestroy() {
        mDisposablesOnCreate.clear()
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun handleProgress(): ObservableTransformer<ProgressState, ProgressState> {
        return ObservableTransformer { upstream ->
            upstream
                // Create a "start" state.
                .startWith(ProgressState(justStart = true))
                .map { state ->
                    return@map if (state.doing && state.progress == 100) {
                        val stopState = state.copy(justStop = true)

                        // Dispatch the progress and "stop" state.
                        mOnUpdateProgress.onNext(state)
                        mOnUpdateProgress.onNext(stopState)

                        stopState
                    } else {
                        // Dispatch the progress.
                        mOnUpdateProgress.onNext(state)

                        state
                    }
                }
        }
    }

    private fun goUntilPreviousTaskStops(): ObservableTransformer<ProgressState, ProgressState> {
        return ObservableTransformer { upstream ->
            upstream
                .filter { state -> state.justStop }
                .debounce(300, TimeUnit.MILLISECONDS)
        }
    }

    private fun toDoSmtAction(period: Long): Observable<ProgressState> {
        return Observable
            .intervalRange(
                // Start progress.
                1,
                // End progress.
                100,
                // Start delay.
                0,
                // Interval period.
                period, TimeUnit.MILLISECONDS)
            .map { value ->
                Log.d("xyz", "real progress=%d%%".format(value.toInt()))
                ProgressState(doing = true,
                              progress = value.toInt())
            }
            .compose(handleProgress())
            .compose(goUntilPreviousTaskStops())
    }

    private fun toCancelAction(): Observable<ProgressState> {
        return Observable
            .just(ProgressState(justStop = true))
            .doOnNext { state -> mOnUpdateProgress.onNext(state) }
    }
}
