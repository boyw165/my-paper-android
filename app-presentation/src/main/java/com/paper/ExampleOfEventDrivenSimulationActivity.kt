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
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.paper.domain.ISystemTime
import com.paper.view.collisionSimulation.CollisionSystem
import com.paper.view.collisionSimulation.CollisionSystemView
import com.paper.view.collisionSimulation.Particle
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

class ExampleOfEventDrivenSimulationActivity : AppCompatActivity() {

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

    // Collision system.
    private val mCollisionSystem: CollisionSystem = CollisionSystem()

    // Clock.
    private var mClock: Long = 0L

    // Error.
    private val mOnThrowError: Subject<Throwable> = PublishSubject.create()

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_example_of_event_driven_simulation)

        mDisposablesOnCreate.add(
            onClickBack()
                .throttleWithTimeout(1, TimeUnit.SECONDS)
                .subscribe { _ ->
                    gotoBack()
                })
    }

    override fun onDestroy() {
        super.onDestroy()

        mDisposablesOnCreate.clear()
    }

    override fun onResume() {
        super.onResume()

        schedulePeriodicRendering(listener = object
            : CollisionSystemView.SimulationListener {
            override fun onUpdateSimulation(canvas: Canvas) {
                if (!mCollisionSystem.isStarted()) {
                    // Init the collision system.
                    mCollisionSystem.start(createParticles())

                    // Init the clock.
                    mClock = 0L
                } else {
                    val current = mSystemTime.getCurrentTimeMillis()

                    // Update system clock.
                    val lastClock = if (mClock == 0L) current else mClock
                    mClock = current

                    // Simulate the collision.
                    val particles = mCollisionSystem.simulate(
                        (mClock - lastClock).toDouble() / 1000.0)

                    // Draw particles.
                    drawParticles(canvas, particles)

                    // Debug info.
                    drawDebugText(
                        canvas,
                        ("particle number = %d\n" +
                         "event number = %d").format(
                            mCollisionSystem.particlesSize,
                            mCollisionSystem.collisionEventsSize))
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()

        unScheduleAll()
        mCollisionSystem.stop()

        mDisposablesOnResume.clear()
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    // *Contract.View /////////////////////////////////////////////////////////

    private fun getCanvasWidth(): Int {
        return mCollisionSystemView.canvasWidth
    }

    private fun getCanvasHeight(): Int {
        return mCollisionSystemView.canvasHeight
    }

    private fun onClickBack(): Observable<Any> {
        return Observable.merge(mOnClickSystemBack,
                                RxView.clicks(mBtnClose))
    }

    private fun schedulePeriodicRendering(listener: CollisionSystemView.SimulationListener) {
        mCollisionSystemView.schedulePeriodicRendering(listener)
    }

    private fun unScheduleAll() {
        mCollisionSystemView.unScheduleAll()
    }

    private fun showToast(text: String) {
        runOnUiThread {
            Toast.makeText(this@ExampleOfEventDrivenSimulationActivity,
                           "The collision system is initialized.",
                           Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun drawDebugText(canvas: Canvas, text: String) {
        mCollisionSystemView.drawDebugText(canvas, text)
    }

    private fun drawParticles(canvas: Canvas, particles: List<Particle>) {
        mCollisionSystemView.drawParticles(canvas, particles)
    }

    // INavigator /////////////////////////////////////////////////////////////

    private fun gotoBack() {
        finish()
    }

    private fun gotoTarget(target: Int) {
        // DUMMY.
    }

    // Sample /////////////////////////////////////////////////////////////////

    private fun createParticles(): Array<Particle> {
        val mass = 0.5
        val radius = 0.02
        val padding = 2.5 * radius
        // Use uniform-Possion-distribution to populate the particles.
        val sampler = UniformPoissonDiskSampler(
            RectF(padding.toFloat(), padding.toFloat(),
                  (1.0 - padding).toFloat(), (1.0 - padding).toFloat()),
            3f * radius,
            PointF(padding.toFloat(), padding.toFloat()))

        return Array(25, { i ->
            if (i == 0) {
                val scale = 6.0
                val scaledRadius = scale * radius
                val vecX = 1.3 * ((100.0 * Math.random() - 50.0) / 100.0)

                Particle(1.0 - scaledRadius - padding, 0.5,
                         vecX, 0.0,
                         scaledRadius,
                         scale * mass)
            } else {
                val pt = sampler.sample() ?: throw IllegalStateException(
                    "Insufficient space to populate the particle.")
                val x = pt.x.toDouble()
                val y = pt.y.toDouble()
                val vecX = 1.3 * ((100.0 * Math.random() - 50.0) / 100.0)
                val vecY = 1.3 * ((100.0 * Math.random() - 50.0) / 100.0)

                Particle(x, y,
                         vecX, vecY,
                         radius,
                         mass)
            }
        })
    }
}
