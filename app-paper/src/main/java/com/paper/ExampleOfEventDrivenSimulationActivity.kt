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

import android.graphics.Canvas
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.paper.exp.simulation.CollisionSystemContract
import com.paper.exp.simulation.CollisionSystemPresenter
import com.paper.exp.simulation.CollisionSystemView
import com.paper.exp.simulation.Particle
import com.paper.protocol.INavigator
import com.paper.protocol.ISystemTime
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class ExampleOfEventDrivenSimulationActivity : AppCompatActivity(),
                                               CollisionSystemContract.View,
                                               INavigator {

    // View.
    private val mBtnClose: View by lazy { findViewById<View>(R.id.btn_close) }
    private val mCollisionSystemView: CollisionSystemView by lazy {
        findViewById<CollisionSystemView>(R.id.collision_system_view)
    }

    // Log.
    private val mLog: ArrayList<String> = arrayListOf()

    // System time.
    private val mSystemTime: ISystemTime = object : ISystemTime {
        override fun getCurrentTimeMillis(): Long {
            return System.currentTimeMillis()
        }
    }

    // Presenter.
    private val mPresenter: CollisionSystemPresenter by lazy {
        CollisionSystemPresenter(this@ExampleOfEventDrivenSimulationActivity,
                                 mSystemTime,
                                 Schedulers.io(),
                                 AndroidSchedulers.mainThread())
    }

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_example_of_event_driven_simulation)

        mPresenter.bindViewOnCreate(this@ExampleOfEventDrivenSimulationActivity)
    }

    override fun onDestroy() {
        super.onDestroy()

        mPresenter.unBindViewOnDestroy()
    }

    override fun onResume() {
        super.onResume()

        mPresenter.onResume()
    }

    override fun onPause() {
        super.onPause()

        mPresenter.onPause()
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    ///////////////////////////////////////////////////////////////////////////
    // *Contract.View /////////////////////////////////////////////////////////

    override fun getCanvasWidth(): Int {
        return mCollisionSystemView.getCanvasWidth()
    }

    override fun getCanvasHeight(): Int {
        return mCollisionSystemView.getCanvasHeight()
    }

    override fun onClickBack(): Observable<Any> {
        return Observable.merge(mOnClickSystemBack,
                                RxView.clicks(mBtnClose))
    }

    override fun schedulePeriodicRendering(listener: CollisionSystemContract.SimulationListener) {
        mCollisionSystemView.schedulePeriodicRendering(listener)
    }

    override fun unScheduleAll() {
        mCollisionSystemView.unScheduleAll()
    }

    override fun showToast(text: String) {
        runOnUiThread {
            Toast.makeText(this@ExampleOfEventDrivenSimulationActivity,
                           "The collision system is initialized.",
                           Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun drawDebugText(canvas: Canvas, text: String) {
        mCollisionSystemView.drawDebugText(canvas, text)
    }

    override fun drawParticles(canvas: Canvas, particles: List<Particle>) {
        mCollisionSystemView.drawParticles(canvas, particles)
    }

    ///////////////////////////////////////////////////////////////////////////
    // INavigator /////////////////////////////////////////////////////////////

    override fun gotoBack() {
        finish()
    }

    override fun gotoTarget(target: Int) {
        // DUMMY.
    }
}
