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
import com.paper.exp.cicerone.CiceroneContract
import com.paper.exp.cicerone.view.CiceroneFragment1
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import ru.terrakok.cicerone.Cicerone
import ru.terrakok.cicerone.Navigator
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.Router
import ru.terrakok.cicerone.android.SupportAppNavigator

class ExampleOfCiceroneActivity : AppCompatActivity(),
                                  CiceroneContract.CiceroneProvider {

    // Cicerone.
    private val mCicerone: Cicerone<Router> by lazy { Cicerone.create() }
    private val mRouter: Router by lazy { mCicerone.router }
    private val mNavigatorHolder: NavigatorHolder by lazy { mCicerone.navigatorHolder }

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_cicerone)

        if (savedState == null) {
            // Assign navigator.
            mNavigatorHolder.setNavigator(mNavigator)
            // Ask router to go.
            mRouter.navigateTo(CiceroneContract.SCREEN_FRAGMENT_1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove navigator.
        mNavigatorHolder.removeNavigator()
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    override fun getCicerone(): Cicerone<Router> {
        return mCicerone
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private val mNavigator: Navigator by lazy {
        object : SupportAppNavigator(this@ExampleOfCiceroneActivity,
                                     R.id.frame_container) {

            override fun createActivityIntent(screenKey: String,
                                              data: Any?): Intent {

            }

            override fun createFragment(screenKey: String,
                                        data: Any?): Fragment {
                return when (screenKey) {
                    CiceroneContract.SCREEN_FRAGMENT_1 -> {
                        CiceroneFragment1.create()
                    }
                    else -> throw IllegalArgumentException("Unknown screen key.")
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // RxCancelContract.View //////////////////////////////////////////////////

    private fun bindViewOnCreate() {
        //        mDisposablesOnCreate.
    }

    private fun unBindViewOnDestroy() {
        mDisposablesOnCreate.clear()
    }


}
