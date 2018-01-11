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

import android.graphics.PointF
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.paper.exp.convexHull.ConvexHullContract
import com.paper.exp.convexHull.ConvexHullPresenter
import com.paper.router.IMyRouterProvider
import com.paper.router.Router
import com.paper.view.DrawableView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class ExampleOfConvexHullActivity : AppCompatActivity(),
                                    ConvexHullContract.View {

    // Router and router holder.
    private val mRouter: Router
        get() = (application as IMyRouterProvider).router

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

        setContentView(R.layout.activity_flow1_page1)

        // Start presenter.
        mPresenter.bindViewOnCreate(this)
    }

    override fun onResume() {
        super.onResume()

        // Set context to navigator.
        mRouter.bindContextToNavigator(Router.LEVEL_ACTIVITY,
                                       this@ExampleOfConvexHullActivity)

        // Resume presenter.
        mPresenter.onResume()

        // Very important to get the buffered Activity result.
        mRouter.dispatchResultOnResume()
    }

    override fun onPause() {
        super.onPause()

        // Remove navigator's context.
        mRouter.unBindContextFromNavigator(Router.LEVEL_ACTIVITY)

        // Pause presenter.
        mPresenter.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop presenter.
        mPresenter.unBindViewOnDestroy()
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
}
