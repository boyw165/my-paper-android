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

package com.paper.view

import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.paper.R
import com.paper.exp.cicerone.CiceroneContract
import com.paper.exp.convexHull.ConvexHullContract
import com.paper.exp.convexHull.ConvexHullPresenter
import com.paper.protocol.IRouterProvider
import com.paper.router.MyRouter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import ru.terrakok.cicerone.Navigator
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.android.SupportAppNavigator

class ExampleOfConvexHullActivity : AppCompatActivity(),
                                    ConvexHullContract.View {

    // Cicerone.
    private val mRouter: MyRouter by lazy { (application as IRouterProvider).router }
    private val mNavigatorHolder: NavigatorHolder by lazy { (application as IRouterProvider).holder }

    // View.
    private val mBtnBack: View by lazy { findViewById<View>(R.id.btn_close) }
    private val mBtnRandom: View by lazy { findViewById<View>(R.id.btn_random) }
    private val mDotCanvas: DrawableView by lazy { findViewById<View>(R.id.canvas) as DrawableView }

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    private val mPresenter: ConvexHullPresenter by lazy {
        ConvexHullPresenter(mRouter,
                            Schedulers.io(),
                            AndroidSchedulers.mainThread())
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_example_of_cicerone1)

        if (savedState == null) {
            // Ask router to show the fragment.
            mRouter.navigateTo(CiceroneContract.SCREEN_NEW_FRAGMENT, 0)
        }

        // Start presenter.
        mPresenter.bindViewOnCreate(this)
    }

    override fun onResume() {
        super.onResume()

        // Set navigator.
        mNavigatorHolder.setNavigator(mNavigator)

        // Resume presenter.
        mPresenter.onResume()

        // Very important to get the buffered Activity result.
        mRouter.dispatchResultOnResume()
    }

    override fun onPause() {
        super.onPause()

        // Remove navigator.
        mNavigatorHolder.removeNavigator()

        // Pause presenter.
        mPresenter.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop presenter.
        mPresenter.unbindViewOnDestroy()
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    override fun getCanvasWidth(): Int {
        return 0
    }

    override fun getCanvasHeight(): Int {
        return 0
    }

    override fun showError(error: Throwable) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addDot(x: Float, y: Float) {
        mDotCanvas.addDot(PointF(x, y))
    }

    override fun removeDot(x: Float, y: Float) {
        mDotCanvas.removeDot(PointF(x, y))
    }

    override fun clearAllDots() {
        mDotCanvas.clearAllDots()
    }

    override fun onClickBack(): Observable<Any> {
        return Observable.merge(
            mOnClickSystemBack,
            RxView.clicks(mBtnBack))
    }

    override fun onClickRandom(): Observable<Any> {
        return RxView.clicks(mBtnRandom)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private val mNavigator: Navigator by lazy {
        object : SupportAppNavigator(this@ExampleOfConvexHullActivity,
                                     R.id.frame_container) {

            override fun createActivityIntent(screenKey: String,
                                              data: Any?): Intent? {
//                return when (screenKey) {
//                    CiceroneContract.SCREEN_NEW_ACTIVITY -> {
//                        Intent(this@ExampleOfConvexHullActivity,
//                               ExampleOfCiceroneActivity2::class.java)
//                            .putExtra(CiceroneContract.ACTIVITY_NUMBER_FLAG, data as Int)
//                    }
//                    else -> null
//                }
                return null
            }

            override fun createFragment(screenKey: String,
                                        data: Any?): Fragment? {
//                return when (screenKey) {
//                    CiceroneContract.SCREEN_NEW_FRAGMENT -> {
//                        CiceroneFragment1.create(data as Int)
//                    }
//                    else -> null
//                }
                return null
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // RxCancelContract.View //////////////////////////////////////////////////
}
