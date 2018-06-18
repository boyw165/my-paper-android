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
import android.util.AttributeSet
import android.view.View
import com.paper.R
import com.paper.view.with

class PenSizePreview : View {

    private val mPreviewBorderColor = ContextCompat.getColor(
        getContext(), R.color.white)
    private val mMinPreviewRadius = resources.getDimension(
        R.dimen.min_pen_size) / 2f
    private val mMaxPreviewRadius = resources.getDimension(
        R.dimen.max_pen_size) / 2f
    private val mPreviewBorderWidth = resources.getDimension(
        R.dimen.pen_seek_bar_preview_border)
    private var mPreviewX: Float = 0f
    private var mPreviewSize: Float = 0f
    private var mPreviewFillColor: Int = 0
    private var mPreviewAlpha: Int = 0
    private var mPreviewPaint = Paint()

    // Animation for preview.
    private val mAnimInSet = AnimatorSet()
    private val mAnimOutSet = AnimatorSet()

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
                canvas.translate(paddingLeft + mPreviewX,
                                 paddingTop - 2f * radius)

                // Draw fill.
                mPreviewPaint.style = Paint.Style.FILL
                mPreviewPaint.color = mPreviewFillColor
                //            mPreviewPaint.setShadowLayer(radius, 0, 0, Color.BLACK);
                //            setLayerType(View.LAYER_TYPE_SOFTWARE, mPreviewPaint);
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

    fun setPenColor(color: Int) {
        mPreviewFillColor = color
        postInvalidate()
    }

    fun showPenSizePreview(x: Float, size: Float) {
        updatePenSizePreviewSize(x, size)

        // Run the fade-in animation if there's no animation running.
        if (!mAnimInSet.isRunning) {
            val toOpaque = ValueAnimator.ofInt(mPreviewAlpha, 255)
            toOpaque.duration = 150
            toOpaque.addUpdateListener(mPreviewAnimUpdater)
            mAnimInSet.cancel()
            mAnimInSet.play(toOpaque)
            mAnimInSet.start()
        }
    }

    fun updatePenSizePreviewSize(x: Float, size: Float) {
        mPreviewX = x
        mPreviewSize = size
    }

    fun hidePenSizePreview() {
        // Always setup a new fade-out animation.
        val toTransparent = ValueAnimator.ofInt(255, 0)
        toTransparent.duration = 250
        toTransparent.addUpdateListener(mPreviewAnimUpdater)
        mAnimOutSet.cancel()
        mAnimOutSet.play(toTransparent)
        mAnimOutSet.startDelay = 700
        mAnimOutSet.start()
    }

    private val mPreviewAnimUpdater = ValueAnimator.AnimatorUpdateListener { anim ->
        mPreviewAlpha = anim.animatedValue as Int
        postInvalidate()
    }
}
