// Copyright Apr 2017-present Paper
//
// Author: boyw165@gmail.com
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

package com.paper.view.collisionSimulation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

import com.paper.R

import io.reactivex.Observable

class CollisionSystemView : View {

    // Rendering.
    private val mParticlePaint = Paint()
    private val mTextPaint = Paint()
    private val mBoundPaint = Paint()
    private val mBoundRect = RectF()
    private val mOval = RectF()
    private var mTextSize = 0f
    private var mTextPadding = 0f

    private var mListener: SimulationListener? = null

    val canvasWidth: Int
        get() = width

    val canvasHeight: Int
        get() = height

    constructor(context: Context) : super(context) {}

    @JvmOverloads constructor(context: Context,
                              attrs: AttributeSet?,
                              defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {

        mParticlePaint.style = Paint.Style.FILL
        mParticlePaint.color = Color.BLACK

        mTextSize = context.resources.getDimension(R.dimen.debug_text_size_1)
        mTextPadding = context.resources.getDimension(R.dimen.debug_padding)

        mTextPaint.color = Color.parseColor("#006400")
        mTextPaint.textSize = mTextSize
        mTextPaint.textAlign = Paint.Align.LEFT

        mBoundPaint.color = Color.LTGRAY
        mBoundPaint.style = Paint.Style.FILL
    }

    fun schedulePeriodicRendering(listener: SimulationListener) {
        mListener = listener
        postInvalidate()
    }

    fun unScheduleAll() {
        mListener = null
    }

    fun onClickBack(): Observable<Any> {
        return Observable.just(0 as Any)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    override fun onMeasure(widthSpec: Int,
                           heightSpec: Int) {
        val width = View.MeasureSpec.getSize(widthSpec)
        val height = width

        mBoundRect.set(0f, 0f, width.toFloat(), height.toFloat())

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Boundary.
        canvas.drawRect(mBoundRect, mBoundPaint)

        if (mListener != null) {
            mListener!!.onUpdateSimulation(canvas)
            postInvalidate()
        }
    }

    fun showToast(text: String) {
        // DUMMY.
    }

    fun drawDebugText(canvas: Canvas,
                      text: String) {
        val x = mTextPadding
        var y = mTextPadding + mTextSize / 2f
        for (line in text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            canvas.drawText(line, x, y, mTextPaint)
            y += mTextSize
        }
    }

    fun drawParticles(canvas: Canvas,
                      particles: List<Particle>) {
        val canvasWidth = canvasWidth
        val canvasHeight = canvasHeight

        for (i in particles.indices) {
            val particle = particles[i]
            val x = particle.centerX
            val y = particle.centerY
            val r = particle.radius

            // Paint first one in red and the rest in black.
            if (i == 0) {
                mParticlePaint.color = Color.RED
            } else {
                mParticlePaint.color = Color.BLACK
            }

            mOval.set(((x - r) * canvasWidth).toFloat(),
                      ((y - r) * canvasHeight).toFloat(),
                      ((x + r) * canvasWidth).toFloat(),
                      ((y + r) * canvasHeight).toFloat())

            canvas.drawOval(mOval, mParticlePaint)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    interface SimulationListener {

        fun onUpdateSimulation(canvas: Canvas)
    }
}
