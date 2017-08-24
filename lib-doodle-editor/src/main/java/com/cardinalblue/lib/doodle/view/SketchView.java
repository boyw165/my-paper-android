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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.cardinalblue.lib.doodle.protocol.IMatrix;
import com.cardinalblue.lib.doodle.protocol.SketchContract;
import com.jakewharton.rxbinding2.view.RxView;
import com.my.reactive.AnimatorSetObservable;
import com.paper.shared.model.sketch.PathTuple;
import com.paper.shared.model.sketch.SketchStrokeModel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class SketchView
    extends View
    implements SketchContract.ISketchView {

    // View state.
    private final Observable<Object> mOnLayoutChanges;

    // Canvas state.
    private Canvas mStrokeCanvas = null;
    private Bitmap mStrokeCanvasBitmap = null;
    private Paint mCanvasBackgroundPaint = null;
    /**
     * Transform to display canvas. It's also a transform mapping the points
     * observing from canvas world to this view's world.
     */
    private AndroidMatrix mMatrix;
    private AndroidMatrix mStartMatrix;
    private float mMinScale, mMaxScale;
    private int mConstraintPaddingLeft, mConstraintPaddingTop, mConstraintPaddingRight, mConstraintPaddingBottom;
    private final RectF mStraightBound = new RectF();

    // Brushes and strokes.
    private final Paint mStrokePaint;
    private final Xfermode mEraserMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private final Path mStrokePath = new Path();
    private int mDrawFromPosition;
    private List<SketchStrokeModel> mTransientStrokes = new ArrayList<>();

    // Animation.
    private AnimatorSet mAnimSet;

    // Disposables.
    private final CompositeDisposable mDisposables = new CompositeDisposable();

    // DEBUG.
    private boolean mIsDebug;
    private List<SketchStrokeModel> mDebugStrokes = new ArrayList<>();
    private Paint mDebugPaint;

    public SketchView(Context context) {
        this(context, null);
    }

    public SketchView(Context context,
                      AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SketchView(Context context,
                      AttributeSet attrs,
                      int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Init stroke paint.
        mStrokePaint = new Paint();
        mStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        mStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setAntiAlias(true);
        mStrokePaint.setDither(true);

        // Init transform.
        mMatrix = new AndroidMatrix(getMatrix());

        // DEBUG.
        mDebugPaint = new Paint();
        mDebugPaint.setColor(Color.GREEN);
        mDebugPaint.setTextSize(24f);
        mDebugPaint.setStrokeWidth(5f);

        // Init others...
        mOnLayoutChanges = RxView.layoutChanges(this);
    }

    @Override
    public Observable<Object> createCanvasSource(int width,
                                                 int height,
                                                 int color) {
        if (mStrokeCanvasBitmap != null) {
            mStrokeCanvasBitmap.recycle();
            mStrokeCanvasBitmap = null;
        }

        // TODO: Handle oom.
        mStrokeCanvasBitmap = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888);
        mStrokeCanvas = new Canvas(mStrokeCanvasBitmap);
        mCanvasBackgroundPaint = new Paint();
        mCanvasBackgroundPaint.setAntiAlias(true);
        mCanvasBackgroundPaint.setFilterBitmap(false);
        mCanvasBackgroundPaint.setStyle(Paint.Style.FILL);
        mCanvasBackgroundPaint.setColor(color);

        // Will trigger updateCanvasMatrixByNewWidthAndHeight() to update the
        // canvas matrix with new canvas width and height.
        // The reason of requesting layout to trigger the update function is
        // because the view width and height is necessary for calculating the
        // transform.
        requestLayout();

        return mOnLayoutChanges;
    }

    @Override
    public int getCanvasWidth() {
        if (mStrokeCanvasBitmap != null && !mStrokeCanvasBitmap.isRecycled()) {
            return mStrokeCanvasBitmap.getWidth();
        } else {
            return 0;
        }
    }

    @Override
    public int getCanvasHeight() {
        if (mStrokeCanvasBitmap != null && !mStrokeCanvasBitmap.isRecycled()) {
            return mStrokeCanvasBitmap.getHeight();
        } else {
            return 0;
        }
    }

    @Override
    public void setBackground(InputStream background) {
        Bitmap bitmap = BitmapFactory.decodeStream(background);
        mCanvasBackgroundPaint.setShader(new BitmapShader(
            bitmap, TileMode.REPEAT, TileMode.REPEAT));
    }

    @Override
    public void setCanvasConstraintPadding(int left,
                                           int top,
                                           int right,
                                           int bottom) {
        mConstraintPaddingLeft = left;
        mConstraintPaddingTop = top;
        mConstraintPaddingRight = right;
        mConstraintPaddingBottom = bottom;
    }

    @Override
    public void eraseCanvas() {
        if (mStrokeCanvasBitmap != null) {
            mStrokeCanvasBitmap.eraseColor(Color.TRANSPARENT);
            invalidate();
        }
    }

    @Override
    public void drawStrokeFrom(SketchStrokeModel stroke,
                               int from) {
        mTransientStrokes.clear();
        mTransientStrokes.add(stroke);

        mDrawFromPosition = from;

        invalidate();
    }

    @Override
    public void drawStrokes(List<SketchStrokeModel> strokes) {
        mTransientStrokes.clear();
        mTransientStrokes.addAll(strokes);

        mDrawFromPosition = 0;

        invalidate();
    }

    @Override
    public void drawAndSharpenStrokes(List<SketchStrokeModel> strokes) {
        // TODO: Make the stroke sharpen.
//        if (strokes != null) {
//            mTransientStrokes.addAll(strokes);
//
//            invalidate();
//        }
    }

    @Override
    public void updateCanvasMatrix(IMatrix matrix) {
        mMatrix.set(matrix);
        invalidate();
    }

    @Override
    public void stopUpdatingCanvasMatrix(final float px,
                                         final float py) {
        // Constraint the transform so that canvas won't be placed offscreen,
        // be scaled to too small to see or be scaled to too blurry big.
        boolean isChanged = false;
        final IMatrix from = mMatrix.clone();
        final IMatrix to = mMatrix.clone();

        // Adjust scale...
        float scale = mMatrix.getScaleX();
        if (scale < mMinScale || scale > mMaxScale) {

            // Get the relative scale.
            if (scale < mMinScale) {
                scale = mMinScale / scale;
            } else if (scale > mMaxScale) {
                scale = mMaxScale / scale;
            }

            to.postScale(scale, scale, px, py);
            isChanged = true;
        }

        // Adjust translation...

        // Calculate the boundary observing in the view's world.
        RectF bound = getStraightBoundOfCanvas(to);

        // Horizontally.
        if (bound.width() <= (getWidth() - mConstraintPaddingLeft - mConstraintPaddingRight)) {
            if (bound.left < mConstraintPaddingLeft) {
                to.postTranslate(mConstraintPaddingLeft - bound.left, 0f);
                isChanged = true;
            } else if (bound.right > (getWidth() - mConstraintPaddingRight)) {
                to.postTranslate(((float) getWidth() - mConstraintPaddingRight) - bound.right, 0f);
                isChanged = true;
            }
        } else {
            if (bound.left > mConstraintPaddingLeft) {
                to.postTranslate(mConstraintPaddingLeft - bound.left, 0f);
                isChanged = true;
            } else if (bound.right < (getWidth() - mConstraintPaddingLeft - mConstraintPaddingRight)) {
                to.postTranslate(((float) getWidth() - mConstraintPaddingRight) - bound.right, 0f);
                isChanged = true;
            }
        }
        // Vertically.
        if (bound.height() <= (getHeight() - mConstraintPaddingTop - mConstraintPaddingBottom)) {
            if (bound.top < mConstraintPaddingTop) {
                to.postTranslate(0f, mConstraintPaddingTop - bound.top);
                isChanged = true;
            } else if (bound.bottom > (getHeight() - mConstraintPaddingBottom)) {
                to.postTranslate(0f, ((float) getHeight() - mConstraintPaddingBottom) - bound.bottom);
                isChanged = true;
            }
        } else {
            if (bound.top > mConstraintPaddingTop) {
                to.postTranslate(0f, mConstraintPaddingTop - bound.top);
                isChanged = true;
            } else if (bound.bottom < (getHeight() - mConstraintPaddingBottom)) {
                to.postTranslate(0f, ((float) getHeight() - mConstraintPaddingBottom) - bound.bottom);
                isChanged = true;
            }
        }

        // Ready to animate canvas.
        if (isChanged) {
            if (mAnimSet != null) {
                mAnimSet.cancel();
            }

            ValueAnimator xform = ValueAnimator.ofObject(
                AndroidMatrix.getTypeEvaluator(),
                // Because the pivot for scale, rotation is fixed in the
                // target world, it's easy to calculate the interpolation.
                from, to);
            xform.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator anim) {
                    mMatrix.set((AndroidMatrix) anim.getAnimatedValue());
                    invalidate();
                }
            });
            mAnimSet = new AnimatorSet();
            mAnimSet.setDuration(150);
            mAnimSet.playTogether(xform);
            mAnimSet.start();
        }
    }

    @Override
    public boolean isAnimating() {
        return mAnimSet != null && mAnimSet.isRunning();
    }

    @Override
    public Observable<?> resetAnimation() {
        if (mMatrix.equals(mStartMatrix)) {
            // If there's no transform, animation is not necessary.
            return Observable.just(100);
        } else {
            if (mAnimSet != null) {
                mAnimSet.cancel();
            }

            ValueAnimator xform = ValueAnimator.ofObject(
                AndroidMatrix.getTypeEvaluator(),
                // Because the pivot for scale, rotation is fixed in the
                // target world, it's easy to calculate the interpolation.
                mMatrix.clone(), mStartMatrix.clone());
            xform.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator anim) {
                    AndroidMatrix xform = (AndroidMatrix) anim.getAnimatedValue();

                    mMatrix.set(xform);
                    invalidate();
                }
            });

            mAnimSet = new AnimatorSet();
            mAnimSet.setDuration(350);
            mAnimSet.setInterpolator(new AccelerateInterpolator());
            mAnimSet.playTogether(xform);

            return new AnimatorSetObservable(mAnimSet)
                .subscribeOn(AndroidSchedulers.mainThread());
        }
    }

    @Override
    public boolean isDebug() {
        return mIsDebug;
    }

    @Override
    public void setDebug(boolean isDebug) {
        mIsDebug = isDebug;
    }

    @Override
    public void debugStrokes(List<SketchStrokeModel> strokes) {
        if (mIsDebug) {
            mDebugStrokes.clear();
            mDebugStrokes.addAll(strokes);

            invalidate();
        }
    }

    @Override
    public boolean isAvailable() {
        return !isAnimating();
    }

    @Override
    public IMatrix getMatrixOfTargetToParent() {
        return mMatrix.clone();
    }

    @Override
    public IMatrix getMatrixOfParentToTarget() {
        return mMatrix.getInverse();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Add onLayoutChange observable.
        mDisposables.add(
            mOnLayoutChanges
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        updateCanvasMatrixByNewWidthAndHeight();
                    }
                }));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mDisposables.clear();

        // Recycle canvas resource.
        if (mStrokeCanvasBitmap != null) {
            mStrokeCanvasBitmap.recycle();
        }
        mStrokeCanvas = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mStrokeCanvas == null || mStrokeCanvasBitmap == null) return;

        canvas.save();
        canvas.concat(mMatrix.matrix);

        // We have two groups for drawing:
        // The top one is responsible for rendering strokes and eraser;
        // The bottom one is "background".
        //
        //       +------------+
        //      /            /   ---> The strokes group.
        //     /            /-+
        //    /            / /
        //   /            / /    ---> The background group.
        //  +------------+ /
        //   /            /
        //  +------------+

        // Draw layer one (background) directly on the given system canvas.
        canvas.drawRect(0, 0, getCanvasWidth(), getCanvasHeight(),
                        mCanvasBackgroundPaint);

        // Draw layer two on the internal bitmap canvas and then draw the
        // bitmap on the given system canvas.
        // Drawing strokes on the internal bitmap is for supporting eraser
        // paint.
        // TODO: Draw new strokes only because old strokes are drawn on the
        // TODO: canvas already.
        // Render path strokes
        if (!mTransientStrokes.isEmpty()) {
            for (SketchStrokeModel stroke : mTransientStrokes) {
                drawPathTupleFrom(stroke, mDrawFromPosition);
            }
            mTransientStrokes.clear();
        }
        canvas.drawBitmap(mStrokeCanvasBitmap, 0, 0, null);

        canvas.restore();

        // DEBUG.
        if (mIsDebug) {
            final float startX = 16f;
            final float startY = 160f;

            // Ready to draw something with stroke only...
            mDebugPaint.setStyle(Paint.Style.STROKE);

            // The straight boundary.
            final RectF straightBound = getStraightBoundOfCanvas(mMatrix);
            canvas.drawRect(straightBound.left,
                            straightBound.top,
                            straightBound.right,
                            straightBound.bottom,
                            mDebugPaint);

            // Ready to draw something with fill only...
            mDebugPaint.setStyle(Paint.Style.FILL);

            // How many strokes.
            canvas.save();
            canvas.translate(startX, startY);
            canvas.drawText(String.format(Locale.ENGLISH,
                                          "strokes=%s",
                                          debugStringOfStrokes()),
                            0, 0, mDebugPaint);
            // The canvas.
            canvas.translate(0, 36f);
            canvas.drawText(String.format(Locale.ENGLISH,
                                          "canvas w=%d, h=%d",
                                          getCanvasWidth(),
                                          getCanvasHeight()),
                            0, 0, mDebugPaint);
            // The transform of the canvas.
            canvas.translate(0, 36f);
            canvas.drawText(String.format(Locale.ENGLISH,
                                          "tx=%.3f, ty=%.3f, sx=%.3f, sy=%.3f",
                                          mMatrix.getTranslationX(),
                                          mMatrix.getTranslationY(),
                                          mMatrix.getScaleX(),
                                          mMatrix.getScaleY()),
                            0, 0, mDebugPaint);
            canvas.restore();
        }
    }

    private void updateCanvasMatrixByNewWidthAndHeight() {
        // Determine the rendering matrix.
        final float scale = Math.min((float) getWidth() / getCanvasWidth(),
                                     (float) getHeight() / getCanvasHeight());
        final float dx = ((float) getWidth() - scale * getCanvasWidth()) / 2f;
        final float dy = ((float) getHeight() - scale * getCanvasHeight()) / 2f;

        mMatrix.reset()
               .postScale(scale, scale, 0, 0)
               .postTranslate(dx, dy);
        // Cache the start matrix right after layout.
        mStartMatrix = (AndroidMatrix) mMatrix.clone();

        // Determine the relative minimum scale.
        mMinScale = scale / 3f;
        // Determine the relative maximum scale.
        mMaxScale = scale * 4f;
    }

    private void drawPathTupleFrom(SketchStrokeModel stroke,
                                   int from) {
        if (from < 0) return;

        mStrokePaint.setColor(stroke.getColor());
        mStrokePaint.setStrokeWidth(stroke.getWidth() * getCanvasWidth());

        // Eraser or normal pen stroke.
        if (stroke.isEraser()) {
            mStrokePaint.setXfermode(mEraserMode);
        } else {
            mStrokePaint.setXfermode(null);
        }

        mStrokePath.reset();
        if (stroke.size() - from == 1) {
            // Draw a "dot"...
            final PointF point = stroke.getPathTupleAt(from).getPointAt(0);

            mStrokePath.moveTo(point.x * getCanvasWidth(),
                               point.y * getCanvasHeight());
            // Drawing a very short path segment looks like a dot. :)
            mStrokePath.lineTo(point.x * getCanvasWidth() + 1f,
                               point.y * getCanvasHeight());
        } else {
            // Draw a line path segment...

            // The last element of a tuple is the point the line is tangent with.
            // For example:
            // p1 is the start point; p2 and p3 is the control point on a cubic
            // curve; p4 is the end point on a cubic curve.
            // [                          p2
            //   [p1],                    +       p3
            //   [p2, p3, p4],   ==>   p1/ ____   +
            //   [p5, p6, p7],          +''    '-- \p4
            //   ...                              '--+
            // ]

            for (int i = from; i < stroke.size(); ++i) {
                final PathTuple pathTuple = stroke.getPathTupleAt(i);

                if (i == from) {
                    mStrokePath.moveTo(pathTuple.getLastPoint().x * getCanvasWidth(),
                                       pathTuple.getLastPoint().y * getCanvasHeight());
                } else {
                    if (pathTuple.getPointSize() == 3) {
                        mStrokePath.cubicTo(pathTuple.getPointAt(0).x * getCanvasWidth(),
                                            pathTuple.getPointAt(0).y * getCanvasHeight(),
                                            pathTuple.getPointAt(1).x * getCanvasWidth(),
                                            pathTuple.getPointAt(1).y * getCanvasHeight(),
                                            pathTuple.getPointAt(2).x * getCanvasWidth(),
                                            pathTuple.getPointAt(2).y * getCanvasHeight());
                    } else if (pathTuple.getPointSize() == 2) {
                        mStrokePath.quadTo(pathTuple.getPointAt(0).x * getCanvasWidth(),
                                           pathTuple.getPointAt(0).y * getCanvasHeight(),
                                           pathTuple.getPointAt(1).x * getCanvasWidth(),
                                           pathTuple.getPointAt(1).y * getCanvasHeight());
                    } else {
                        mStrokePath.lineTo(pathTuple.getLastPoint().x * getCanvasWidth(),
                                           pathTuple.getLastPoint().y * getCanvasHeight());
                    }
                }
            }
        }
        mStrokeCanvas.drawPath(mStrokePath, mStrokePaint);
    }

    private RectF getStraightBoundOfCanvas(IMatrix matrix) {
        final float[] leftTop = new float[]{0f, 0f};
        final float[] rightTop = new float[]{getCanvasWidth(), 0f};
        final float[] rightBottom = new float[]{getCanvasWidth(), getCanvasHeight()};
        final float[] leftBottom = new float[]{0f, getCanvasHeight()};
        matrix.mapPoints(leftTop);
        matrix.mapPoints(rightTop);
        matrix.mapPoints(rightBottom);
        matrix.mapPoints(leftBottom);

        mStraightBound.set(Float.MAX_VALUE,
                           Float.MAX_VALUE,
                           Float.MIN_VALUE,
                           Float.MIN_VALUE);
        for (float[] point : new float[][]{leftTop,
                                           rightTop,
                                           rightBottom,
                                           leftBottom}) {
            mStraightBound.left = Math.min(mStraightBound.left, point[0]);
            mStraightBound.top = Math.min(mStraightBound.top, point[1]);
            mStraightBound.right = Math.max(mStraightBound.right, point[0]);
            mStraightBound.bottom = Math.max(mStraightBound.bottom, point[1]);
        }

        return mStraightBound;
    }

    private String debugStringOfStrokes() {
        final StringBuilder builder = new StringBuilder("[");

        for (int i = 0; i < mDebugStrokes.size(); ++i) {
            builder.append(mDebugStrokes.get(i).size());

            if (i < mDebugStrokes.size() - 1) {
                builder.append(",");
            }
        }

        builder.append("]");

        // Stripe text.
        final int max = 48;
        if (builder.length() > max) {
            builder.delete(0, builder.length() - max);
            builder.insert(0, "[...");
        }

        return builder.toString();
    }
}
