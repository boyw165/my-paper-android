// Copyright Jan 2017-present Boy Wang
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

package com.paper

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.paper.exp.rxCancel.RxCancelContract
import com.paper.exp.rxCancel.RxCancelPresenter
import com.paper.observables.BooleanDialogSingle
import com.paper.router.IMyRouterHolderProvider
import com.paper.router.INavigator
import com.paper.router.MyRouter
import com.paper.router.MyRouterHolder
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import ru.terrakok.cicerone.commands.Back
import ru.terrakok.cicerone.commands.Command
import java.lang.StringBuilder

class ExampleOfRxCancelActivity : AppCompatActivity(),
                                  RxCancelContract.View {

    // View.
    private val mBtnClose: View by lazy { findViewById<View>(R.id.btn_close) }
    private val mBtnStart: View by lazy { findViewById<View>(R.id.btn_start) }
    private val mBtnClearLog: View by lazy { findViewById<View>(R.id.btn_clear_log) }
    private val mBtnCancel: View by lazy { findViewById<View>(R.id.btn_cancel) }
    private val mTxtLog: TextView by lazy { findViewById<TextView>(R.id.txt_log) }
    private val mTxtLogContainer: ScrollView by lazy { findViewById<ScrollView>(R.id.txt_log_container) }

    // Log.
    private val mLog: ArrayList<String> = arrayListOf()

    // Router and router holder.
    private val mRouter: MyRouter by lazy {
        MyRouter(Handler(Looper.getMainLooper()))
    }
    private val mRouterHolder: MyRouterHolder
        get() = (application as IMyRouterHolderProvider).holder

    // Presenter.
    private val mPresenter: RxCancelPresenter by lazy {
        RxCancelPresenter(mRouter,
                          Schedulers.io(),
                          AndroidSchedulers.mainThread())
    }

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_rx_cancel)

        // Link router to the router holder.
        mRouterHolder.pushAndBindParent(mRouter)

        // Presenter.
        mPresenter.bindViewOnCreate(this@ExampleOfRxCancelActivity)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Presenter.
        mPresenter.unBindViewOnDestroy()

        // Unlink router to the router holder.
        mRouterHolder.popAndUnbindParent()
    }

    override fun onResume() {
        super.onResume()

        // Navigator.
        mRouter.setNavigator(mNavigator)
    }

    override fun onPause() {
        super.onPause()

        // Navigator.
        mRouter.unsetNavigator()
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    ///////////////////////////////////////////////////////////////////////////
    // RxCancelContract.View //////////////////////////////////////////////////

    override fun clearLog() {
        mLog.clear()
        mTxtLog.text = ""
    }

    override fun printLog(message: String) {
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

    override fun showError(error: Throwable) {
        Toast.makeText(this@ExampleOfRxCancelActivity, error.toString(), Toast.LENGTH_LONG).show()
    }

    override fun showConfirmDialog(): Single<Boolean> {
        val builder = AlertDialog.Builder(this@ExampleOfRxCancelActivity)
            .setTitle(R.string.sample_dialog_title)
            .setMessage(R.string.sample_dialog_message)
            .setCancelable(true)

        return BooleanDialogSingle(builder,
                                   getString(R.string.yes),
                                   getString(R.string.no))
            .subscribeOn(AndroidSchedulers.mainThread())
    }

    override fun showProgressBar() {
        printLog("showProgressBar")
    }

    override fun updateProgressBar(progress: Int) {
        printLog(progress.toString())
    }

    override fun hideProgressBar() {
        printLog("hideProgressBar")
    }

    override fun onClickClose(): Observable<Any> {
        return Observable.merge(
            RxView.clicks(mBtnClose),
            mOnClickSystemBack)
    }

    override fun onClickStart(): Observable<Any> {
        return RxView.clicks(mBtnStart)
    }

    override fun onClickCancel(): Observable<Any> {
        return RxView.clicks(mBtnCancel)
    }

    override fun onClickClearLog(): Observable<Any> {
        return RxView.clicks(mBtnClearLog)
    }

    ///////////////////////////////////////////////////////////////////////////
    // INavigator /////////////////////////////////////////////////////////////

    private val mNavigator: INavigator = object : INavigator {

        override fun onEnter() {
            Log.d("rxCancel", "enter ---->")
        }

        override fun onExit() {
            Log.d("rxCancel", "exit <-----")
        }

        override fun applyCommandAndWait(command: Command,
                                         future: INavigator.FutureResult):Boolean {
            if (command is Back) {
                finish()
            }

            // Indicate the router this command is finished.
            future.finish()

            return true
        }
    }
}
