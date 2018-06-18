// Copyright Jun 2018-present Paper
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

package com.paper.view.editPanel

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import com.paper.R
import com.paper.domain.event.UpdatePenSizeEvent
import com.paper.model.event.EventLifecycle
import com.paper.view.with
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.withLatestFrom

class PenSizePreview : View {

    private val mMinPreviewRadius = resources.getDimension(R.dimen.min_pen_size) / 2f
    private val mMaxPreviewRadius = resources.getDimension(R.dimen.max_pen_size) / 2f
    private val mPreviewBorderColor = ContextCompat.getColor(context, R.color.black_30)
    private val mPreviewBorderWidth = resources.getDimension(R.dimen.pen_seek_bar_preview_border)
    private var mPreviewSize: Float = 0f
    private var mPreviewFillColor: Int = 0
    private var mPreviewAlpha: Int = 0
    private var mPreviewPaint = Paint()

    // Animation for preview.
    private var mAnimSet: AnimatorSet? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context,
                attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // Init preview resource.
        mPreviewPaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw preview thumb.
        if (mPreviewAlpha > 0f) {
            canvas.with {
                val radius = mMinPreviewRadius + mPreviewSize * (mMaxPreviewRadius - mMinPreviewRadius)
                // Translate the padding. For the x, we need to allow the thumb to
                // draw in its extra space
                val spaceWidth = width - ViewCompat.getPaddingStart(this) - ViewCompat.getPaddingEnd(this)
                canvas.translate(ViewCompat.getPaddingStart(this) + mPreviewSize * spaceWidth,
                                 height.toFloat() - paddingBottom - radius - mPreviewBorderWidth)

                // Draw fill.
                mPreviewPaint.style = Paint.Style.FILL
                mPreviewPaint.color = mPreviewFillColor
                // The paint implementation merges the RGB and Alpha channels
                // together, so changing alpha of Color.TRANSPARENT is actually
                // equal to changing the alpha of Color.BLACK.
                if (Color.alpha(mPreviewFillColor) == 0) {
                    mPreviewPaint.alpha = 0
                } else {
                    mPreviewPaint.alpha = mPreviewAlpha
                }
                canvas.drawCircle(0f, 0f, radius, mPreviewPaint)

                // Draw border.
                mPreviewPaint.style = Paint.Style.STROKE
                mPreviewPaint.strokeWidth = mPreviewBorderWidth
                mPreviewPaint.color = mPreviewBorderColor
                mPreviewPaint.alpha = mPreviewAlpha
                canvas.drawCircle(0f, 0f, radius + mPreviewBorderWidth / 2f, mPreviewPaint)
            }
        }
    }

    fun updatePenSize(sizeSrc: Observable<UpdatePenSizeEvent>,
                      colorSrc: Observable<Int>): Disposable {
        return sizeSrc
            .withLatestFrom(colorSrc)
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { (event, color) ->
                when (event.lifecycle) {
                    EventLifecycle.START -> {
                        setPenColor(color)
                        showPenSizePreview(event.size)
                    }
                    EventLifecycle.DOING -> {
                        updatePenSizePreviewSize(event.size)
                    }
                    EventLifecycle.STOP -> {
                        hidePenSizePreview()
                    }
                }
                Observable.never<Unit>()
            }
            .subscribe()
    }

    private fun setPenColor(color: Int) {
        mPreviewFillColor = color
        invalidate()
    }

    private fun showPenSizePreview(size: Float) {
        updatePenSizePreviewSize(size)

        // Run the fade-in animation
        val toOpaque = ValueAnimator.ofInt(mPreviewAlpha, 255)
        toOpaque.duration = 150
        toOpaque.addUpdateListener(mPreviewAnimUpdater)
        mAnimSet?.cancel()
        mAnimSet = AnimatorSet()
        mAnimSet?.play(toOpaque)
        mAnimSet?.start()
    }

    private fun updatePenSizePreviewSize(size: Float) {
        mPreviewSize = size
        invalidate()
    }

    private fun hidePenSizePreview() {
        // Always setup a new fade-out animation.
        val toTransparent = ValueAnimator.ofInt(mPreviewAlpha, 0)
        toTransparent.duration = 250
        toTransparent.addUpdateListener(mPreviewAnimUpdater)
        mAnimSet?.cancel()
        mAnimSet = AnimatorSet()
        mAnimSet?.play(toTransparent)
        mAnimSet?.startDelay = 700
        mAnimSet?.start()
    }

    private val mPreviewAnimUpdater = ValueAnimator.AnimatorUpdateListener { anim ->
        val alpha = anim.animatedValue as Int
        post {
            mPreviewAlpha = alpha
            invalidate()
        }
    }
}
