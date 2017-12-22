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
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import com.paper.exp.rxCancel.RxCancelContract
import com.paper.exp.rxCancel.RxCancelPresenter
import com.paper.protocol.INavigator
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.lang.StringBuilder

class RxCancelActivity : AppCompatActivity(),
                         RxCancelContract.View,
                         INavigator {

    // View.
    private val mBtnClose: View by lazy { findViewById<View>(R.id.btn_close) }
    private val mBtnStart: View by lazy { findViewById<View>(R.id.btn_start) }
    private val mBtnClearLog: View by lazy { findViewById<View>(R.id.btn_clear_log) }
    private val mBtnCancel: View by lazy { findViewById<View>(R.id.btn_cancel) }
    private val mTxtLog: TextView by lazy { findViewById<TextView>(R.id.txt_log) }
    private val mTxtLogContainer: ScrollView by lazy { findViewById<ScrollView>(R.id.txt_log_container) }

    // Log.
    private val mLog: ArrayList<String> = arrayListOf()

    // Presenter.
    private val mPresenter: RxCancelPresenter by lazy {
        RxCancelPresenter(this@RxCancelActivity,
                          Schedulers.io(),
                          AndroidSchedulers.mainThread()) }

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_rx_cancel)

        mPresenter.bindViewOnCreate(this@RxCancelActivity)
    }

    override fun onDestroy() {
        super.onDestroy()

        mPresenter.unBindViewOnDestroy()
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
        while (mLog.size > 128) {
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

    override fun updateProgressBar(progress: Int) {
        printLog(progress.toString())
    }

    override fun showProgressBar() {
        printLog("showProgressBar")
    }

    override fun hideProgressBar() {
        printLog("hideProgressBar")
    }

    override fun onClickClose(): Observable<Any> {
        return Observable.merge(
            RxView.clicks (mBtnClose),
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

    override fun gotoBack() {
        finish()
    }

    override fun gotoTarget(target: Int) {
    }
}
