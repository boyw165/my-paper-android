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

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.annotation.IntRange
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatSeekBar
import android.util.AttributeSet
import com.paper.R
import com.paper.view.with

class PenSizeSeekBar : AppCompatSeekBar {

    // Preview.
    private val mPreviewBorderWidth = resources.getDimension(R.dimen.pen_seek_bar_preview_border)
    private var mPreviewFillColor: Int = 0
    private var mPreviewPaint: Paint

    // Thumb.
    private val mThumbRadius = context.resources.getDimension(R.dimen.pen_seek_bar_thumb_size) / 2
    private val mThumbDrawable = ContextCompat.getDrawable(context, R.drawable.bg_seek_bar_thumb)
    // Track.
    private val mTrackDrawable: TrackDrawable

    constructor(context: Context) : this(context, null)
    constructor(context: Context,
                attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // Init thumb resource.
        thumb = mThumbDrawable

        // Init track drawable.
        val color = ContextCompat.getColor(context, R.color.black_30)
        val startHeight = resources.getDimension(R.dimen.pen_seek_bar_track_start_height)
        val stopHeight = resources.getDimension(R.dimen.pen_seek_bar_track_stop_height)
        mTrackDrawable = TrackDrawable(color, startHeight, stopHeight)
        mTrackDrawable.alpha = 255 / 2
        progressDrawable = mTrackDrawable

        // Init preview resource.
        mPreviewPaint = Paint()
        mPreviewPaint.isAntiAlias = true
    }

    override fun onLayout(changed: Boolean,
                          left: Int,
                          top: Int,
                          right: Int,
                          bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        mTrackDrawable.setBounds(left + paddingLeft,
                                 top + paddingTop,
                                 right - paddingLeft,
                                 bottom - paddingBottom)

        // Determine the fill path.
        mTrackDrawable.updateTrackFillPath()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw color fill preview on the thumb.
        canvas.with {
            // Translate the padding. For the x, we need to allow the thumb to
            // draw in its extra space
            val progress = progress.toFloat() / 100f
            val progressOffset = progress * (width - paddingLeft - paddingRight)

            canvas.translate(paddingLeft + progressOffset, (height / 2).toFloat())
            mPreviewPaint.style = Paint.Style.FILL
            mPreviewPaint.color = mPreviewFillColor
            // The paint implementation merges the RGB and Alpha channels
            // together, so changing alpha of Color.TRANSPARENT is actually
            // equal to changing the alpha of Color.BLACK.
            if (Color.alpha(mPreviewFillColor) == 0) {
                mPreviewPaint.alpha = 0
            }

            val radius = mThumbRadius - mPreviewBorderWidth
            canvas.drawCircle(0f, 0f, radius, mPreviewPaint)
        }
    }

    fun showPenColor(color: Int) {
        mPreviewFillColor = color
        postInvalidate()
    }

    // Clazz //////////////////////////////////////////////////////////////////

    private class TrackDrawable(color: Int,
                                val startHeight: Float,
                                val stopHeight: Float) : Drawable() {
        val trackPaint = Paint()
        val mTrackFillPath = Path()

        init {
            trackPaint.color = color
            trackPaint.isAntiAlias = true
            trackPaint.style = Paint.Style.FILL
        }

        override fun draw(canvas: Canvas) {
            canvas.drawPath(mTrackFillPath, trackPaint)
        }

        override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
            trackPaint.alpha = alpha
        }

        override fun getAlpha(): Int {
            return trackPaint.alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            trackPaint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int {
            return PixelFormat.TRANSLUCENT
        }

        internal fun updateTrackFillPath() {
            val width = bounds.width().toFloat()
            val height = bounds.height().toFloat()
            val middle = height / 2f

            mTrackFillPath.fillType = Path.FillType.WINDING
            mTrackFillPath.moveTo(0f, middle - startHeight / 2f)
            mTrackFillPath.lineTo(width, middle - stopHeight / 2f)
            mTrackFillPath.lineTo(width, middle + stopHeight / 2f)
            mTrackFillPath.lineTo(0f, middle + startHeight / 2f)
        }
    }
}
