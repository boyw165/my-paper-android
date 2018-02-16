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

package com.paper.exp.simulation

import android.graphics.Canvas
import com.paper.protocol.INavigator
import com.paper.protocol.IPresenter
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

class CollisionSystemPresenter(private val mNavigator: INavigator,
                               private val mWorkerSchedulers: Scheduler,
                               private val mUiScheduler: Scheduler)
    : IPresenter<CollisionSystemContract.View> {

    private val mCollisionSystem: CollisionSystem by lazy {
        CollisionSystem(Array(10, { _ -> Particle() }))
    }

    // View
    private lateinit var mView: CollisionSystemContract.View

    // Progress.
    private val mOnThrowError: Subject<Throwable> = PublishSubject.create()

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    override fun bindViewOnCreate(view: CollisionSystemContract.View) {
        mView = view

        mDisposablesOnCreate.add(
            mView.onClickBack()
                .throttleWithTimeout(1, TimeUnit.SECONDS)
                .subscribe { _ ->
                    mNavigator.gotoBack()
                })
    }

    override fun unBindViewOnDestroy() {
        mDisposablesOnCreate.clear()
    }

    override fun onResume() {
        Observable
            .fromCallable {
                // Initialize in the background.
                mCollisionSystem.start()
                0
            }
            .subscribeOn(mWorkerSchedulers)
            .observeOn(mUiScheduler)
            .subscribe { _ ->
                mView.showToast("The collision system is ready!")
            }

        mView.schedulePeriodicRendering(listener = object
            : CollisionSystemContract.SimulationListener {
            override fun onUpdateSimulation(canvas: Canvas) {
                mCollisionSystem.simulate(
                    canvas,
                    mView.getCanvasWidth(),
                    mView.getCanvasHeight(),
                    mView.getParticlePaint())
            }
        })
    }

    override fun onPause() {
        mView.unScheduleAll()

        mDisposablesOnResume.clear()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////
}
