// Copyright (c) 2017-present Cardinalblue
//
// Author: boy@cardinalblue.com
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

package com.cardinalblue.lib.doodle.view;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

import com.cardinalblue.lib.doodle.R;

public class BrushSizeSeekBar extends AppCompatSeekBar {

    // Preview.
    protected int mPreviewFillColor;
    protected final int mPreviewBorderColor;
    protected final float mMinPreviewRadius;
    protected final float mMaxPreviewRadius;
    protected final float mPreviewBorderWidth;
    protected int mPreviewAlpha;
    protected Paint mPreviewPaint;

    // Thumb.
    protected float mThumbRadius;
    protected float mThumbBorderWidth;
    protected Drawable mThumbDrawable;
    // Track.
    protected TrackDrawable mTrackDrawable;

    // Animation for preview.
    protected final AnimatorSet mAnimInSet = new AnimatorSet();
    protected final AnimatorSet mAnimOutSet = new AnimatorSet();

    public BrushSizeSeekBar(Context context) {
        this(context, null);
    }

    public BrushSizeSeekBar(Context context,
                            AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrushSizeSeekBar(Context context,
                            AttributeSet attrs,
                            int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Init thumb resource.
        mThumbRadius = context.getResources().getDimension(
            R.dimen.stroke_seek_bar_thumb_size) / 2;
        mThumbBorderWidth = context.getResources().getDimension(
            R.dimen.stroke_seek_bar_thumb_border);
        mThumbDrawable = ContextCompat.getDrawable(
            context, R.drawable.bg_seek_bar_thumb);
        setThumb(mThumbDrawable);

        // Init track drawable.
        final int color = ContextCompat.getColor(context, R.color.white);
        final float startHeight = getResources().getDimension(
            R.dimen.stroke_seek_bar_track_start_height);
        final float stopHeight = getResources().getDimension(
            R.dimen.stroke_seek_bar_track_stop_height);
        mTrackDrawable = new TrackDrawable(color, startHeight, stopHeight);
        mTrackDrawable.setAlpha(255 / 2);
        setProgressDrawable(mTrackDrawable);

        // Init preview resource.
        mMinPreviewRadius = getResources().getDimension(
            R.dimen.sketch_min_stroke_width) / 2f;
        mMaxPreviewRadius = getResources().getDimension(
            R.dimen.sketch_max_stroke_width) / 2f;
        mPreviewBorderColor = ContextCompat.getColor(
            getContext(), R.color.white);
        mPreviewBorderWidth = getResources().getDimension(
            R.dimen.stroke_seek_bar_preview_border);
        mPreviewPaint = new Paint();
        mPreviewPaint.setAntiAlias(true);
    }

    public void showStrokeColor(int color) {
        mPreviewFillColor = color;
        postInvalidate();
    }

    public void showStrokeColorAndWidthPreview(int color) {
        showStrokeColor(color);

        // Run the fade-in animation if there's no animation running.
        if (!mAnimInSet.isRunning()) {
            ValueAnimator toOpaque = ValueAnimator.ofInt(mPreviewAlpha, 255);
            toOpaque.setDuration(150);
            toOpaque.addUpdateListener(mPreviewAnimUpdater);
            mAnimInSet.cancel();
            mAnimInSet.play(toOpaque);
            mAnimInSet.start();
        }
    }

    public void hidePreviewColor() {
        // Always setup a new fade-out animation.
        ValueAnimator toTransparent = ValueAnimator.ofInt(255, 0);
        toTransparent.setDuration(250);
        toTransparent.addUpdateListener(mPreviewAnimUpdater);
        mAnimOutSet.cancel();
        mAnimOutSet.play(toTransparent);
        mAnimOutSet.setStartDelay(700);
        mAnimOutSet.start();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    @Override
    protected void onLayout(boolean changed,
                            int left,
                            int top,
                            int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mTrackDrawable.setBounds(left + getPaddingLeft(),
                                 top + getPaddingTop(),
                                 right - getPaddingLeft(),
                                 bottom - getPaddingBottom());

        // Determine the fill path.
        mTrackDrawable.updateTrackFillPath();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw preview thumb.
        if (mPreviewAlpha > 0f) {
            final int saveCount = canvas.save();

            final float progress = (float) getProgress() / 100f;
            final float radius = mMinPreviewRadius + progress * (mMaxPreviewRadius - mMinPreviewRadius);
            // Translate the padding. For the x, we need to allow the thumb to
            // draw in its extra space
            canvas.translate(getPaddingLeft() + getThumb().getBounds().left,
                             getPaddingTop() - 2f * radius);

            // Draw fill.
            mPreviewPaint.setStyle(Paint.Style.FILL);
            mPreviewPaint.setColor(mPreviewFillColor);
//            mPreviewPaint.setShadowLayer(radius, 0, 0, Color.BLACK);
//            setLayerType(View.LAYER_TYPE_SOFTWARE, mPreviewPaint);
            // The paint implementation merges the RGB and Alpha channels
            // together, so changing alpha of Color.TRANSPARENT is actually
            // equal to changing the alpha of Color.BLACK.
            if (Color.alpha(mPreviewFillColor) == 0) {
                mPreviewPaint.setAlpha(0);
            } else {
                mPreviewPaint.setAlpha(mPreviewAlpha);
            }
            canvas.drawCircle(0, 0, radius, mPreviewPaint);

            // Draw border.
            mPreviewPaint.setStyle(Paint.Style.STROKE);
            mPreviewPaint.setStrokeWidth(mPreviewBorderWidth);
            mPreviewPaint.setColor(mPreviewBorderColor);
            mPreviewPaint.setAlpha(mPreviewAlpha);
            canvas.drawCircle(0, 0, radius + mPreviewBorderWidth / 2f, mPreviewPaint);

            canvas.restoreToCount(saveCount);
        }

        // Draw color fill preview on the thumb.
        final int complementalAlpha = 255 - mPreviewAlpha;
        if (complementalAlpha > 0) {
            final int saveCount = canvas.save();
            // Translate the padding. For the x, we need to allow the thumb to
            // draw in its extra space
            final float progress = (float) getProgress() / 100f;
            final float progressOffset = progress * (getWidth() - getPaddingLeft() - getPaddingRight());

            canvas.translate(getPaddingLeft() + progressOffset, getHeight() / 2);
            mPreviewPaint.setStyle(Paint.Style.FILL);
            mPreviewPaint.setColor(mPreviewFillColor);
            // The paint implementation merges the RGB and Alpha channels
            // together, so changing alpha of Color.TRANSPARENT is actually
            // equal to changing the alpha of Color.BLACK.
            if (Color.alpha(mPreviewFillColor) == 0) {
                mPreviewPaint.setAlpha(0);
            } else {
                mPreviewPaint.setAlpha(complementalAlpha);
            }

            float radius = mThumbRadius - mPreviewBorderWidth;
            canvas.drawCircle(0, 0, radius, mPreviewPaint);
            canvas.restoreToCount(saveCount);
        }
    }

    protected final ValueAnimator.AnimatorUpdateListener mPreviewAnimUpdater =
        new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator anim) {
                mPreviewAlpha = (int) (Integer) anim.getAnimatedValue();
                postInvalidate();
            }
        };

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private static class TrackDrawable extends Drawable {

        final float mStartHeight;
        final float mStopHeight;
        final Paint mTrackPaint;

        Path mTrackFillPath;

        TrackDrawable(int color,
                      float startHeight,
                      float stopHeight) {
            mTrackPaint = new Paint();
            mTrackPaint.setColor(color);
            mTrackPaint.setAntiAlias(true);
            mTrackPaint.setStyle(Paint.Style.FILL);

            mStartHeight = startHeight;
            mStopHeight = stopHeight;

            mTrackFillPath = new Path();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.drawPath(mTrackFillPath, mTrackPaint);
        }

        @Override
        public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
            mTrackPaint.setAlpha(alpha);
        }

        @Override
        public int getAlpha() {
            return mTrackPaint.getAlpha();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mTrackPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        void updateTrackFillPath() {
            final float width = getBounds().width();
            final float height = getBounds().height();
            final float middle = height / 2f;

            mTrackFillPath.setFillType(Path.FillType.WINDING);
            mTrackFillPath.moveTo(0f, middle - mStartHeight / 2f);
            mTrackFillPath.lineTo(width, middle - mStopHeight / 2f);
            mTrackFillPath.lineTo(width, middle + mStopHeight / 2f);
            mTrackFillPath.lineTo(0f, middle + mStartHeight / 2f);
        }
    }
}
