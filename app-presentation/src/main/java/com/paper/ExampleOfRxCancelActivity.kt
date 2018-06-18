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

package com.paper

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.paper.model.event.ProgressEvent
import com.paper.observables.BooleanDialogSingle
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

class ExampleOfRxCancelActivity : AppCompatActivity() {

    // Intents.
    companion object {
        const val INTENT_OF_CANCEL_EVERYTHING = -1
        const val INTENT_OF_DO_SOMETHING = 0
    }

    // View.
    private val mBtnClose: View by lazy { findViewById<View>(R.id.btn_close) }
    private val mBtnStart: View by lazy { findViewById<View>(R.id.btn_start) }
    private val mBtnClearLog: View by lazy { findViewById<View>(R.id.btn_clear_log) }
    private val mBtnCancel: View by lazy { findViewById<View>(R.id.btn_cancel) }
    private val mTxtLog: TextView by lazy { findViewById<TextView>(R.id.txt_log) }
    private val mTxtLogContainer: ScrollView by lazy { findViewById<ScrollView>(R.id.txt_log_container) }

    // Log.
    private val mLog: ArrayList<String> = arrayListOf()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    // Schedulers.
    private val mUiSchedulers = AndroidSchedulers.mainThread()
    private val mWorkerSchedulers = Schedulers.io()

    // Progress.
    private val mOnUpdateProgress: Subject<ProgressEvent> = PublishSubject.create()
    private val mOnThrowError: Subject<Throwable> = PublishSubject.create()

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_example_of_rx_cancel)

        // Start and cancel buttons:
        //
        //   start +
        //          \
        //           +----> something in between ----> end.
        //          /
        //  cancel +
        mDisposablesOnCreate.add(
            Observable
                .merge(
                    // Start button.
                    onClickStart()
                        .debounce(150, TimeUnit.MILLISECONDS)
                        .throttleFirst(1000, TimeUnit.MILLISECONDS)
                        .map { _ -> INTENT_OF_DO_SOMETHING },
                    // Cancel button.
                    onClickCancel()
                        .debounce(150, TimeUnit.MILLISECONDS)
                        .map { _ -> INTENT_OF_CANCEL_EVERYTHING })
                // Create do-something intent or cancel intent.
                .switchMap { intent ->
                    when (intent) {
                        INTENT_OF_DO_SOMETHING -> toShareAction()
                        INTENT_OF_CANCEL_EVERYTHING -> toCancelAction()
                        else -> toCancelAction()
                    }
                }
                .observeOn(mUiSchedulers)
                .subscribe { _ ->
                    printLog("all finished!")
                })

        // Clear log button.
        mDisposablesOnCreate.add(
            onClickClearLog()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(mUiSchedulers)
                .subscribe { _ ->
                    clearLog()
                })

        // Close button.
        mDisposablesOnCreate.add(
            onClickClose()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(mUiSchedulers)
                .subscribe { _ ->
                    gotoBack()
                })

        // Progress.
        mDisposablesOnCreate.add(
            mOnUpdateProgress
                .observeOn(mUiSchedulers)
                .subscribe { state ->
                    when {
                        state.justStart -> printLog("--- START ---")
                        state.justStop -> printLog("---!!! STOP !!!---")
                        state.doing -> printLog(
                            "doing %d%%...".format(
                                state.progress))
                    }
                })

        // Error.
        mDisposablesOnCreate.add(
            mOnThrowError
                .observeOn(mUiSchedulers)
                .subscribe { error ->
                    showError(error)
                })
    }

    override fun onDestroy() {
        super.onDestroy()

        mDisposablesOnCreate.clear()
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    // RxCancelContract.View //////////////////////////////////////////////////

    private fun clearLog() {
        mLog.clear()
        mTxtLog.text = ""
    }

    private fun printLog(message: String) {
        // Add to log pool and clear message too old.
        mLog.add(message)
        while (mLog.size > 256) {
            mLog.removeAt(0)
        }

        // Format the log message and print it out.
        val builder = StringBuilder()
        for (msg in mLog) {
            builder.append(msg)
            builder.append("\n")
        }
        mTxtLog.text = builder.toString()

        // Scroll to bottom.
        mTxtLogContainer.fullScroll(View.FOCUS_DOWN)
    }

    private fun showError(error: Throwable) {
        Toast.makeText(this@ExampleOfRxCancelActivity, error.toString(), Toast.LENGTH_LONG).show()
    }

    private fun showConfirmDialog(): Single<Boolean> {
        val builder = AlertDialog.Builder(this@ExampleOfRxCancelActivity)
            .setTitle(R.string.sample_dialog_title)
            .setMessage(R.string.sample_dialog_message)
            .setCancelable(true)

        return BooleanDialogSingle(builder,
                                   getString(R.string.yes),
                                   getString(R.string.no))
            .subscribeOn(AndroidSchedulers.mainThread())
    }

    private fun showProgressBar() {
        printLog("showProgressBar")
    }

    private fun updateProgressBar(progress: Int) {
        printLog(progress.toString())
    }

    private fun hideProgressBar() {
        printLog("hideProgressBar")
    }

    private fun onClickClose(): Observable<Any> {
        return Observable.merge(
            RxView.clicks(mBtnClose),
            mOnClickSystemBack)
    }

    private fun onClickStart(): Observable<Any> {
        return RxView.clicks(mBtnStart)
    }

    private fun onClickCancel(): Observable<Any> {
        return RxView.clicks(mBtnCancel)
    }

    private fun onClickClearLog(): Observable<Any> {
        return RxView.clicks(mBtnClearLog)
    }

    // INavigator /////////////////////////////////////////////////////////////

    private fun gotoBack() {
        finish()
    }

    private fun gotoTarget(target: Int) {
        // DUMMY.
    }

    // Sample /////////////////////////////////////////////////////////////////

    /**
     * Returns a DO-SOMETHING action.
     */
    private fun toShareAction(): Observable<Any> {
        // #1 observable simulating an arbitrary long-run process.
        return generateBmp()
            .compose(handleError(ProgressEvent(justStop = true)))
            // #2 observable that shows a dialog.
            .switchMap { _ ->
                showConfirmDialog()
                    // The following code is just for logging.
                    .doOnSubscribe { _ -> printLog("Show a confirmation dialog...") }
                    .subscribeOn(mUiSchedulers)
                    .observeOn(mUiSchedulers)
                    .doOnSuccess { v -> printLog("Confirmation dialog returns %s".format(v)) }
                    .toObservable()
            }
            // #3
            .switchMap { b: Boolean ->
                if (b) {
                    shareToFacebook()
                        .compose(handleError(ProgressEvent(justStop = true)))
                } else {
                    Observable.just(ProgressEvent(justStop = true))
                }
            }
    }

    /**
     * An observable emitting the status of generating the Bitmap.
     */
    private fun generateBmp(): Observable<ProgressEvent> {
        return getSimulatingLongRunProcess()
    }

    /**
     * An observable emitting the status of sharing.
     */
    private fun shareToFacebook(): Observable<ProgressEvent> {
        return getSimulatingLongRunProcess()
    }

    /**
     * Returns a CANCEL action.
     */
    private fun toCancelAction(): Observable<ProgressEvent> {
        return Observable
            .just(ProgressEvent(justStop = true))
            .doOnNext { state -> mOnUpdateProgress.onNext(state) }
    }

    /**
     * Returns an Observable that emitting [ProgressEvent].
     */
    private fun getSimulatingLongRunProcess(): Observable<ProgressEvent> {
        return Observable
            // The first simulated long-run process.
            .intervalRange(
                // Start progress.
                1,
                // End progress.
                100,
                // Start delay.
                0,
                // Interval period.
                25, TimeUnit.MILLISECONDS)
            .map { value ->
                ProgressEvent(doing = true,
                                                    progress = value.toInt())
            }
            .compose(handleProgress())
            .compose(goUntilPreviousTaskStops())
    }

    /**
     * A transformer that massage [ProgressEvent] from upstream and bypass to
     * [mOnUpdateProgress] channel.
     */
    private fun handleProgress(): ObservableTransformer<ProgressEvent, ProgressEvent> {
        return ObservableTransformer { upstream ->
            upstream
                // Create a "start" state.
                .startWith(ProgressEvent(justStart = true))
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

    /**
     * A transformer that filters START and DOING [ProgressEvent] state.
     */
    private fun goUntilPreviousTaskStops(): ObservableTransformer<ProgressEvent, ProgressEvent> {
        return ObservableTransformer { upstream ->
            upstream
                .filter { state -> state.justStop }
                .debounce(300, TimeUnit.MILLISECONDS)
        }
    }

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
