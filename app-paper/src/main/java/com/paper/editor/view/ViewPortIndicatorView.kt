// Copyright Feb 2018-present boyw165@gmail.com
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

package com.paper.editor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import com.paper.R
import com.paper.shared.model.Rect
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject

class ViewPortIndicatorView : View {

    private var mScaleM2V = Float.NaN
    private val mCanvasBound = RectF()
    private val mTmpBound = RectF()
    private val mViewPortBound = RectF()
    private val mCanvasPaint = Paint()
    private val mViewPortPaint = Paint()
    private val mOneDp by lazy { resources.getDimension(R.dimen.one_dp) }

    private val mSetViewPort = BehaviorSubject.create<Any>()
    private val mOnLayoutFinish = BehaviorSubject.create<Any>()
    private val mDisposables = CompositeDisposable()

    constructor(context: Context) : this(context, null)
    constructor(context: Context,
                attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        mCanvasPaint.color = ContextCompat.getColor(context, R.color.white)
        mCanvasPaint.style = Paint.Style.FILL
        mCanvasPaint.setShadowLayer(10f * mOneDp, 0f, 3f * mOneDp, Color.BLACK)

        mViewPortPaint.color = ContextCompat.getColor(context, R.color.accent)
        mViewPortPaint.style = Paint.Style.STROKE
        mViewPortPaint.strokeWidth = 3f * mOneDp
        mViewPortPaint.setShadowLayer(2f * mOneDp, 1f * mOneDp, 1f * mOneDp, Color.parseColor("#7F000000"))

        // FIXME: Making the view software layer so that Paint#setShadowLayer works.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        if (isInEditMode) {
            setCanvasAndViewPort(Rect(0f, 0f, 297f, 210f),
                                 Rect(20f, 20f, 80f, 100f))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        mDisposables.add(
            Observables.combineLatest(
                mOnLayoutFinish,
                mSetViewPort) {_, _ -> true}
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    updateScaleM2V()
                })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        mDisposables.clear()
    }

    override fun onLayout(changed: Boolean,
                          left: Int,
                          top: Int,
                          right: Int,
                          bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // Would trigger updateScaleM2V() and onDraw() if layout is done and view
        // port is set.
        mOnLayoutFinish.onNext(true)

        // Because the canvas bound and view port bound is mocked in editing mode
        // and the onAttachedToWindow() is not called, we have to manually trigger
        // the drawing our own.
        if (isInEditMode) {
            updateScaleM2V()
        }
    }

    fun setCanvasAndViewPort(canvas: Rect,
                             viewPort: Rect) {
        mCanvasBound.set(canvas.left, canvas.top, canvas.right, canvas.bottom)
        mViewPortBound.set(viewPort.left, viewPort.top, viewPort.right, viewPort.bottom)

        // Would trigger updateScaleM2V() and onDraw() if layout is done and view
        // port is set.
        mSetViewPort.onNext(true)
    }

    private fun updateScaleM2V() {
        val paddingStart = ViewCompat.getPaddingStart(this)
        val paddingEnd = ViewCompat.getPaddingEnd(this)
        val paddingTop = this.paddingTop
        val paddingBottom = this.paddingBottom
        val spaceW = this.width - paddingStart - paddingEnd
        val spaceH = this.height - paddingTop - paddingBottom

        mScaleM2V = Math.min(spaceW / mCanvasBound.width(),
                             spaceH / mCanvasBound.height())

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (hasSetCanvasAndViewPort()) {
            val mw = mScaleM2V * mCanvasBound.width()
            val mh = mScaleM2V * mCanvasBound.height()
            val mx = (width - mw) / 2f
            val my = (height - mh) / 2f

            mTmpBound.set(mx, my, mx + mw, my + mh)
            canvas.drawRoundRect(mTmpBound, 2f * mOneDp, 2f * mOneDp, mCanvasPaint)

            val vpW = mScaleM2V * mViewPortBound.width()
            val vpH = mScaleM2V * mViewPortBound.height()
            val vpX = mScaleM2V * mViewPortBound.left
            val vpY = mScaleM2V * mViewPortBound.top

            mTmpBound.set(mx + vpX,
                          my + vpY,
                          mx + vpX + vpW,
                          my + vpY + vpH)
            canvas.drawRoundRect(mTmpBound, 2f * mOneDp, 2f * mOneDp, mViewPortPaint)
        }
    }

    private fun hasSetCanvasAndViewPort(): Boolean {
        return mCanvasBound.width() > 0f && mCanvasBound.height() > 0f &&
               mViewPortBound.width() > 0f && mViewPortBound.height() > 0f &&
               mScaleM2V != Float.NaN
    }
}
