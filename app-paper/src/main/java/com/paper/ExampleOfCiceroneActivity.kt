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

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import com.paper.exp.cicerone.CiceroneContract
import com.paper.exp.cicerone.view.CiceroneFragment1
import com.paper.protocol.IRouterProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import ru.terrakok.cicerone.Navigator
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.android.SupportAppNavigator
import java.util.concurrent.TimeUnit

class ExampleOfCiceroneActivity : AppCompatActivity(),
                                  CiceroneContract.CiceroneProvider {

    // Cicerone.
    private val mRouter: MyRouter by lazy { (application as IRouterProvider).router }
    private val mNavigatorHolder: NavigatorHolder by lazy { (application as IRouterProvider).holder }

    // View.
    private val mBtnNewFrag: View by lazy { findViewById<View>(R.id.btn_back_prev_frag) }
    private val mBtnNewActivity: View by lazy { findViewById<View>(R.id.btn_new_activity) }
    private val mTvActivityNumber: TextView by lazy { findViewById<TextView>(R.id.tv_activity_number) }
    // TODO: test the result listener
    private val mTvResult: TextView by lazy { findViewById<TextView>(R.id.tv_result) }
//    private val mContainer: FrameLayout by lazy { findViewById<FrameLayout>(R.id.frame_container) }

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    private var mActivityNumber = 0

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_cicerone)

        if (intent.extras != null){
            val newActivityNumber = intent.extras[CiceroneContract.ACTIVITY_NUMBER_FLAG]
            mActivityNumber = newActivityNumber as Int
        }

        if (savedState == null) {
            // Ask router to go.
            mRouter.navigateTo(CiceroneContract.SCREEN_NEW_FRAGMENT, 0)
        }

        mRouter.setResultListener(CiceroneContract.ACTIVITY_RESULT_CODE, { resultData ->
            val resultStr: String? = resultData.toString()
            if (resultStr != null) {
                mTvResult.visibility = View.VISIBLE
                mTvResult.text = "Get Result: " + resultStr
            } else {
                mTvResult.visibility = View.INVISIBLE
            }
        })

        mTvActivityNumber.text = "Activity " + mActivityNumber.toString()

        // Exp new fragment button.
        mDisposablesOnCreate.add(
                RxView.clicks(mBtnNewFrag)
//                        .debounce(150, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { _ ->
                            var currentFragNum =
                                    if (supportFragmentManager.fragments.isEmpty()) -1
                                    else (supportFragmentManager.fragments[0] as CiceroneFragment1).getCurrentNumber()
                            mRouter.navigateTo(CiceroneContract.SCREEN_NEW_FRAGMENT, currentFragNum + 1)
                        })

        // Exp new activity button.
        mDisposablesOnCreate.add(
                RxView.clicks(mBtnNewActivity)
                        .debounce(150, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { _ ->
                            mRouter.navigateTo(CiceroneContract.SCREEN_NEW_ACTIVITY, mActivityNumber + 1)
                        })

        mDisposablesOnCreate.add(
                mOnClickSystemBack.subscribe{
                    mRouter.exit()
                })
    }

    override fun onResume() {
        super.onResume()

        // Set navigator.
        mNavigatorHolder.setNavigator(mNavigator)
        mRouter.dispatchResultOnResume(CiceroneContract.ACTIVITY_RESULT_CODE)
    }

    override fun onPause() {
        super.onPause()

        // Remove navigator.
        mNavigatorHolder.removeNavigator()

    }

    override fun onDestroy() {
        super.onDestroy()

        mDisposablesOnCreate.clear()
        mRouter.removeResultListener(CiceroneContract.ACTIVITY_RESULT_CODE)
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    override fun getRouter(): MyRouter {
        return mRouter
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private val mNavigator: Navigator by lazy {
        object : SupportAppNavigator(this@ExampleOfCiceroneActivity,
                                     R.id.frame_container) {

            override fun createActivityIntent(screenKey: String,
                                              data: Any?): Intent? {
                return when (screenKey) {
                    CiceroneContract.SCREEN_NEW_ACTIVITY -> {
                        // TODO: set the data to intent.
                        Intent(this@ExampleOfCiceroneActivity, ExampleOfCiceroneActivity2::class.java)
                                .putExtra(CiceroneContract.ACTIVITY_NUMBER_FLAG, data as Int)
                    }
//                    else -> throw IllegalArgumentException("Unknown screen key.")
                    else -> null
                }
            }

            override fun createFragment(screenKey: String,
                                        data: Any?): Fragment? {
                return when (screenKey) {
                    CiceroneContract.SCREEN_NEW_FRAGMENT -> {
                        CiceroneFragment1.create(data as Int)
                    }
//                    else -> throw IllegalArgumentException("Unknown screen key.")
                    else -> null
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // RxCancelContract.View //////////////////////////////////////////////////
}
