// Copyright (c) 2017-present CardinalBlue
//
// Author: jack.huang@cardinalblue.com
//         boy@cardinalblue.com
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
import com.jakewharton.rxbinding2.view.RxView
import com.paper.navigation.Flow1Navigator
import com.paper.router.IMyRouterProvider
import com.paper.router.INavigator
import com.paper.router.Router
import com.paper.router.NavigationContract
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.lang.ref.WeakReference

class ExampleOfFlow1Page1Activity : AppCompatActivity() {

    // View.
    private val mBtnBack: View by lazy { findViewById<View>(R.id.btn_back) }
    private val mBtnNext: View by lazy { findViewById<View>(R.id.btn_next) }

    // Router and router holder.
    private val mRouter: Router
        get() = (application as IMyRouterProvider).router
    // Navigator.
    private val mNavigator: INavigator by lazy {
        Flow1Navigator(WeakReference(this@ExampleOfFlow1Page1Activity))
    }

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_flow1_page1)

        // Next.
        mDisposablesOnCreate.add(
            RxView.clicks(mBtnNext)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mRouter.navigateTo(NavigationContract.SCREEN_OF_FLOW1_PAGE2)
                })

        // Back.
        mDisposablesOnCreate.add(
            Observable
                .merge(mOnClickSystemBack,
                       RxView.clicks(mBtnBack))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mRouter.exit()
                })
    }

    override fun onDestroy() {
        super.onDestroy()

        mDisposablesOnCreate.clear()
    }

    override fun onResume() {
        super.onResume()

        // Set navigator.
        mRouter.setNavigator(Router.LEVEL_ACTIVITY, mNavigator)

        // Get the buffered Activity result.
        mRouter.dispatchResultOnResume()
    }

    override fun onPause() {
        super.onPause()

        // Remove navigator.
        mRouter.unsetNavigator(Router.LEVEL_ACTIVITY)
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////
}
