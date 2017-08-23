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

package com.cardinalblue.lib.doodle.controller;

import android.graphics.PointF;
import android.graphics.RectF;

import com.cardinalblue.lib.doodle.event.DragEvent;
import com.cardinalblue.lib.doodle.event.DrawStrokeEvent;
import com.cardinalblue.lib.doodle.event.SingleTapEvent;
import com.cardinalblue.lib.doodle.protocol.ILogger;
import com.cardinalblue.lib.doodle.protocol.ISketchBrush;
import com.cardinalblue.lib.doodle.protocol.SketchContract;
import com.paper.shared.model.sketch.PathTuple;
import com.paper.shared.model.sketch.SketchModel;
import com.paper.shared.model.sketch.SketchStrokeModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * For determining how to draw "straight" lines.
 */
public class DrawStrokeManipulator implements SketchContract.IDrawStrokeManipulator {

    private static final RectF sCanvasBound = new RectF(0f, 0f, 1f, 1f);

    private static final boolean FEATURE_OF_DRAWING_BEZIER_CURVE = false;

    // Given...
    private float mMinPathSegmentLength;
    private long mMinPathSegmentInterval;
    private final float mMinBrushSize;
    private final float mMaxBrushSize;
    private final Scheduler mWorkerScheduler;
    private final Scheduler mUiScheduler;
    private final ILogger mLogger;

    // Brush and stroke.
    private float mBrushSize;
    private ISketchBrush mBrush;
    private SketchStrokeModel mStroke;
    private final List<PointF> mCachedPoints = new ArrayList<>();

    // Drawing state.
    private final float[] mMappedPoint = new float[2];
    private final PointF mLastPoint = new PointF();
    private boolean mIfIntersectsWithCanvas;
    private long mPrevDrawTime;

    // Reusable resource.
    private final float[] mVector = new float[2];

    public DrawStrokeManipulator(float minPathSegmentLength,
                                 long minPathSegmentInterval,
                                 float minStrokeWidth,
                                 float maxStrokeWidth,
                                 Scheduler workerScheduler,
                                 Scheduler uiScheduler,
                                 ILogger logger) {
        mMinPathSegmentLength = minPathSegmentLength;
        mMinPathSegmentInterval = minPathSegmentInterval;
        mMinBrushSize = minStrokeWidth;
        mMaxBrushSize = maxStrokeWidth;
        mWorkerScheduler = workerScheduler;
        mUiScheduler = uiScheduler;
        mLogger = logger;
    }

    @Override
    public void setBrush(ISketchBrush brush) {
        mBrush = brush;
    }

    @Override
    public ISketchBrush getBrush() {
        return mBrush;
    }

    @Override
    public void setBrushSize(float baseWidth, int value) {
        mBrushSize = mMinBrushSize + (mMaxBrushSize - mMinBrushSize) * value / 100f;
        // Normalize the width.
        mBrushSize = mBrushSize / baseWidth;

        if (mBrush != null) {
            mBrush.setBrushSize(mBrushSize);
        }
    }

    @Override
    public ObservableTransformer<DragEvent, DrawStrokeEvent> drawStroke(final SketchContract.IModelProvider modelProvider) {
        return new ObservableTransformer<DragEvent, DrawStrokeEvent>() {
            @Override
            public ObservableSource<DrawStrokeEvent> apply(Observable<DragEvent> upstream) {
                return upstream
                    .flatMap(new Function<DragEvent, ObservableSource<DrawStrokeEvent>>() {
                        @Override
                        public ObservableSource<DrawStrokeEvent> apply(DragEvent event)
                            throws Exception {
                            if (mBrush == null) return Observable.just(DrawStrokeEvent.IDLE);

                            // The model.
                            final SketchModel model = modelProvider.getSketchModel();

                            // The x and y are observed from the parent world.
                            final float x = event.x;
                            final float y = event.y;
                            mMappedPoint[0] = x;
                            mMappedPoint[1] = y;

                            // TODO: Handle un-paired event properly.

                            if (event.justStart) {
                                // Start...

                                // Make sure the brush has right stroke width.
                                mBrush.setBrushSize(mBrushSize);

                                // Get the point relative to the canvas coordinate.
                                event.parentToTargetMatrix.mapPoints(mMappedPoint);
                                // Normalized x and y.
                                final float nx = mMappedPoint[0] / model.getWidth();
                                final float ny = mMappedPoint[1] / model.getHeight();

                                // New stroke and store the normalized value.
                                mStroke = mBrush.newStroke();
                                mStroke.savePathTuple(new PathTuple(nx, ny));

                                // Save as a record for first time.
                                mLastPoint.set(x, y);

                                // Init the point cache.
                                mCachedPoints.clear();

                                // Init the intersection flag.
                                mIfIntersectsWithCanvas = sCanvasBound.contains(nx, ny);

                                return Observable.just(DrawStrokeEvent.start(mStroke));
                            } else if (event.doing) {
                                // Drawing...

                                if (mStroke != null && addPathPredict(x, y)) {
                                    // Get the point relative to the canvas coordinate.
                                    event.parentToTargetMatrix.mapPoints(mMappedPoint);
                                    // Normalized x and y.
                                    final float nx = mMappedPoint[0] / model.getWidth();
                                    final float ny = mMappedPoint[1] / model.getHeight();

                                    // Flag intersection.
                                    if (!mIfIntersectsWithCanvas) {
                                        mIfIntersectsWithCanvas = sCanvasBound.contains(nx, ny);
                                    }

                                    if (FEATURE_OF_DRAWING_BEZIER_CURVE) {
                                        // Drawing bezier curves...

                                        // In order to draw bezier curve, we include
                                        // three points as a tuple.
                                        //     p2
                                        //     +       p3
                                        //  p1/ ____   +
                                        //   +''    '-- \p4
                                        // In the above figure, p1 the the last point
                                        // of the previous tuple; p2 and p3 are the
                                        // control points; p4 is the endpoint.
                                        mCachedPoints.add(new PointF(nx, ny));

                                        if (mCachedPoints.size() == 3) {
                                            mLogger.d("xyz", "DrawStrokeEvent.drawing()");

                                            // Smoothen the curve:
                                            // Solve the point causing the non-smooth
                                            // transition of adjacent Bezier segments.
                                            smoothenCurveJoint(mCachedPoints);

                                            // Save the position of latest tuple-path for
                                            // later use.
                                            final int from = mStroke.size() - 1;
                                            // Add tuple.
                                            mStroke.savePathTuple(new PathTuple(mCachedPoints));
                                            // Clear the cache.
                                            mCachedPoints.clear();

                                            return Observable.just(DrawStrokeEvent.drawing(mStroke, from));
                                        } else {
                                            return Observable.just(DrawStrokeEvent.IDLE);
                                        }
                                    } else {
                                        // Drawing normal lines...

                                        // Save the position of latest tuple-path for
                                        // later use.
                                        final int from = mStroke.size() - 1;
                                        // Add tuple.
                                        mStroke.savePathTuple(new PathTuple(nx, ny));

                                        return Observable.just(DrawStrokeEvent.drawing(mStroke, from));
                                    }
                                } else {
                                    return Observable.just(DrawStrokeEvent.IDLE);
                                }
                            } else {
                                // Stop...

                                // Determine whether to commit the result to
                                // model.
                                final Observable<DrawStrokeEvent> source;
                                if (mStroke != null && mIfIntersectsWithCanvas) {
                                    // Consume all the remaining cached points.
                                    if (!mCachedPoints.isEmpty()) {
                                        // Save the position of latest tuple-path for
                                        // later use.
                                        final int from = mStroke.size() - 1;
                                        mStroke.savePathTuple(new PathTuple(mCachedPoints));
                                        source = Observable.just(
                                            DrawStrokeEvent.drawing(mStroke, from),
                                            DrawStrokeEvent.stop(
                                                model.getAllStrokes(),
                                                // A boolean indicating whether
                                                // the model is changed.
                                                true));
                                    } else {
                                        source = Observable.just(
                                            DrawStrokeEvent.stop(
                                                model.getAllStrokes(),
                                                // A boolean indicating whether
                                                // the model is changed.
                                                true));
                                    }

                                    // Commit to the model.
                                    model.addStroke(mStroke);

                                    // Send analytics event.
                                    if (mStroke.isEraser()) {
                                        mLogger.sendEvent("Doodle editor - erase");
                                    } else {
                                        mLogger.sendEvent("Doodle editor - draw",
                                                          "color_hex", String.format("#%08X", mStroke.getColor()));
                                    }
                                } else {
                                    source = Observable.just(
                                        DrawStrokeEvent.stop(
                                            model.getAllStrokes(),
                                            false));
                                }

                                // Reset the drawing state.
                                mStroke = null;
                                mLastPoint.set(0, 0);
                                mPrevDrawTime = 0;
                                mIfIntersectsWithCanvas = false;

                                return source;
                            }
                        }
                    })
                    // Don't send IDLE event.
                    .filter(new Predicate<DrawStrokeEvent>() {
                        @Override
                        public boolean test(DrawStrokeEvent event)
                            throws Exception {
                            return event != DrawStrokeEvent.IDLE;
                        }
                    });
            }
        };
    }

    @Override
    public ObservableTransformer<SingleTapEvent, DrawStrokeEvent> drawDot(final SketchContract.IModelProvider modelProvider) {
        return new ObservableTransformer<SingleTapEvent, DrawStrokeEvent>() {
            @Override
            public ObservableSource<DrawStrokeEvent> apply(Observable<SingleTapEvent> upstream) {
                return upstream
                    .flatMap(new Function<SingleTapEvent, ObservableSource<DrawStrokeEvent>>() {
                        @Override
                        public ObservableSource<DrawStrokeEvent> apply(SingleTapEvent event)
                            throws Exception {
                            if (mBrush == null) return Observable.just(DrawStrokeEvent.IDLE);

                            // The model.
                            final SketchModel model = modelProvider.getSketchModel();

                            // The x and y are observed from the parent world.
                            final float x = event.x;
                            final float y = event.y;
                            mMappedPoint[0] = x;
                            mMappedPoint[1] = y;

                            // Make sure the brush has right stroke width.
                            mBrush.setBrushSize(mBrushSize);

                            // Get the point relative to the canvas coordinate.
                            event.parentToTargetMatrix.mapPoints(mMappedPoint);
                            // Normalized x and y.
                            final float nx = mMappedPoint[0] / model.getWidth();
                            final float ny = mMappedPoint[1] / model.getHeight();

                            if (sCanvasBound.contains(nx, ny)) {
                                // New stroke and store the normalized value.
                                final SketchStrokeModel stroke = mBrush.newStroke();

                                stroke.savePathTuple(new PathTuple(nx, ny));
                                // Commit to the model.
                                model.addStroke(stroke);

                                return Observable.fromArray(
                                    DrawStrokeEvent.start(stroke),
                                    DrawStrokeEvent.stop(model.getAllStrokes(), true)
                                );
                            } else {
                                return Observable.just(DrawStrokeEvent.IDLE);
                            }
                        }
                    })
                    // Don't send IDLE event.
                    .filter(new Predicate<DrawStrokeEvent>() {
                        @Override
                        public boolean test(DrawStrokeEvent event)
                            throws Exception {
                            return event != DrawStrokeEvent.IDLE;
                        }
                    });
            }
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    /**
     * A predictor that tell you given pair of x-y could be added as new path
     * to the stroke.
     *
     * @param x The given x.
     * @param y The given y.
     */
    private boolean addPathPredict(final float x,
                                   final float y) {
        final long duration = getCurrentTime() - mPrevDrawTime;

//        mLogger.d("xyz", "duration=" + duration + ", " +
//                         "distance=" + Math.hypot(x - mLastPoint.x, y - mLastPoint.y));
        if (duration >= mMinPathSegmentInterval &&
            Math.hypot(x - mLastPoint.x,
                       y - mLastPoint.y) > mMinPathSegmentLength) {
            mPrevDrawTime = getCurrentTime();
            mLastPoint.set(x, y);
            return true;
        } else {
            return false;
        }
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    /**
     * Reference: https://code.tutsplus.com/tutorials/smooth-freehand-drawing-on-ios--mobile-13164
     */
    private void smoothenCurveJoint(List<PointF> points) {
        if (points == null || points.size() < 2) return;

        // Must have previous tuple.
        final int strokesNum = mStroke.size();
        if (strokesNum == 0) return;

        // Previous tuple must have more than or equal to 2 points.
        final PathTuple prevTuple = mStroke.getPathTupleAt(strokesNum - 1);
        final int prevTupleSize = prevTuple.getPointSize();
        if (prevTupleSize < 2) return;

        final PointF prevEndpoint = prevTuple.getPointAt(prevTupleSize - 1);
        final PointF prevControlPoint = prevTuple.getPointAt(prevTupleSize - 2);
//        final double angle1 = Math.atan2(prevControlPoint.y - prevEndpoint.y,
//                                         prevControlPoint.x - prevEndpoint.x);
//        final double angle2 = Math.atan2(points.get(0).y - prevEndpoint.y,
//                                         points.get(0).x - prevEndpoint.x);
//        double angle = Math.abs(angle2 - angle1);
//        // Take smaller angle.
//        final double TWO_PI = 2f * Math.PI;
//        if (angle > Math.PI) {
//            angle = TWO_PI - angle;
//        }
//        mLogger.d("xyz", String.format(Locale.ENGLISH, "angle=%.3f", Math.toDegrees(angle)));
//        // Must be greater than 90 degrees.
//        if (angle <= Math.PI / 2f) return;
//
//        // Return if the angle is equal.
//        if (angle == Math.PI) return;
//
//        // Rotation matrix:
//        // [ cos  -sin ]
//        // [ sin   cos ]

        final PointF currControlPoint = points.get(0);
        mVector[0] = prevEndpoint.x - prevControlPoint.x;
        mVector[1] = prevEndpoint.y - prevControlPoint.y;

        currControlPoint.set(prevEndpoint.x + mVector[0],
                             prevEndpoint.y + mVector[1]);
    }
}
