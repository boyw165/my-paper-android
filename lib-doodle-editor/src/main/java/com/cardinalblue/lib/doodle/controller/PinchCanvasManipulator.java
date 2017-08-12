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

import com.cardinalblue.lib.doodle.data.PointF;
import com.cardinalblue.lib.doodle.event.PinchEvent;
import com.cardinalblue.lib.doodle.protocol.ILogger;
import com.cardinalblue.lib.doodle.protocol.IMatrix;
import com.cardinalblue.lib.doodle.protocol.SketchContract;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;

/**
 * Apply the pinch-in and pinch-out gesture to the given target. The given
 * target is an {@link IMatrix} instance.
 * <br/>
 * See {@link PinchCanvasManipulator#PinchCanvasManipulator(ILogger)}
 */
public class PinchCanvasManipulator implements SketchContract.IPinchCanvasManipulator {

    private final ILogger mLogger;

    /**
     * A cache remembering the start matrix observing points inside the
     * target from the target coordinate system.
     */
    private IMatrix mStartMatrixInTarget;
    /**
     * Matrix for reverting the change observing from the target coordinate
     * system.
     */
    private IMatrix mCalculationMatrixInTarget;

    /**
     * The start vector from finger pointer 1 to finger pointer 2.
     */
    private final PointF mStartVectorInParent = new PointF();
    /**
     * The stop (current) vector from finger pointer 1 to finger pointer 2.
     */
    private final PointF mStopVectorInParent = new PointF();

    /**
     * The pivot point between the start finger pointer 1 and start finger
     * pointer 2.
     * <br/>
     * Note: The value is obtained by observing from the "caller" coordinate
     * system.
     */
    private final PointF mStartPivotInParent = new PointF();
    /**
     * The pivot point between the start finger pointer 1 and start finger
     * pointer 2.
     * <br/>
     * Note: The value is obtained by observing from the "target" coordinate
     * system.
     */
    private final PointF mStartPivotInTarget = new PointF();
    /**
     * The pivot point between the stop (current) finger pointer 1 and stop
     * (current) finger pointer 2.
     * <br/>
     * Note: The value is obtained by observing from the "caller" coordinate
     * system.
     */
    private final PointF mStopPivotInParent = new PointF();

    public PinchCanvasManipulator(ILogger logger) {
        mLogger = logger;
    }

    @Override
    public ObservableTransformer<PinchEvent, PinchEvent> pinchCanvas() {
        return new ObservableTransformer<PinchEvent, PinchEvent>() {
            @Override
            public ObservableSource<PinchEvent> apply(Observable<PinchEvent> upstream) {
                return upstream.map(new Function<PinchEvent, PinchEvent>() {
                    @Override
                    public PinchEvent apply(PinchEvent event)
                        throws Exception {
                        // Handle the model if it carries an PinchEvent.
                        if (event.justStart) {
//                            Log.d("xyz", "pinch start");
//                            Log.d("xyz", "-----------");

                            // Start matrix.
                            mStartMatrixInTarget = event.parentToTargetMatrix;
                            // This cached matrix is used for later calculation.
                            mCalculationMatrixInTarget = event.parentToTargetMatrix.clone();

                            // Start pivot (observes the pivot in the
                            // target coordinate system).
                            mStartPivotInParent.set((event.pointer1.x + event.pointer2.x) / 2f,
                                                    (event.pointer1.y + event.pointer2.y) / 2f);
                            // In order to get the pivot observing from the
                            // target coordinate system, we transform the
                            // point with the matrix.
                            final float[] pivot = new float[]{
                                mStartPivotInParent.x,
                                mStartPivotInParent.y
                            };
                            mStartMatrixInTarget.mapPoints(pivot);
                            mStartPivotInTarget.set(pivot[0], pivot[1]);

                            // Start vector between two finger pointers.
                            mStartVectorInParent.set(event.pointer2.x - event.pointer1.x,
                                                     event.pointer2.y - event.pointer1.y,
                                                     true);

//                    mLogger.d("xyz", "--- PinchStart ---");
//                    mLogger.d("xyz", String.format(Locale.ENGLISH, mStartMatrixInTarget.toString()));

                            return event;
                        } else if (event.doing) {
//                            Log.d("xyz", "pinching...");
                            // TODO: Seems like the unpaired event is received
                            // TODO: sometimes.
                            if (mStartMatrixInTarget == null ||
                                mCalculationMatrixInTarget == null) {
                                return event;
                            }

                            final float[] transform = getTransformFromEvent(event);
                            final float tx = transform[TRANS_X];
                            final float ty = transform[TRANS_Y];
                            final float sx = transform[SCALE_X];
                            final float sy = transform[SCALE_Y];
                            final float degree = transform[ROTATION];
                            final float px = mStartPivotInTarget.x;
                            final float py = mStartPivotInTarget.y;

                            mCalculationMatrixInTarget
                                .reset()
                                .postRotate(-degree, px, py)
                                .postScale(1f / sx, 1f / sy, px, py);

                            final IMatrix newMatrix = mStartMatrixInTarget.clone();
                            newMatrix
                                .set(mStartMatrixInTarget)
                                .postConcat(mCalculationMatrixInTarget)
                                .invert()
                                .postTranslate(tx, ty);

                            // Pass the new matrix to downstream along with
                            // everything else unchanged.
                            return PinchEvent.doing(newMatrix,
                                                    event.pointer1.x, event.pointer1.y,
                                                    event.pointer2.x, event.pointer2.y);
                        } else {
//                            Log.d("xyz", "---- pinch stop ----");
                            // Send analytics event.
                            mLogger.sendEvent("Doodle editor - change canvas size");

                            // Necessary to clear the reference to cloned matrix.
                            mStartMatrixInTarget = null;
                            mCalculationMatrixInTarget = null;

//                    mLogger.d("xyz", "--- PinchStop ---");
//                    mLogger.d("xyz", String.format(Locale.ENGLISH,
//                                                   "sx=%.3f, sy=%.3f, " +
//                                                   "tx=%.3f, ty=%.3f, " +
//                                                   "rot=%.3f",
//                                                   mMatrixInParent.getScaleX(),
//                                                   mMatrixInParent.getScaleY(),
//                                                   mMatrixInParent.getTranslationX(),
//                                                   mMatrixInParent.getTranslationY(),
//                                                   mMatrixInParent.getRotation()));
                            return event;
                        }
                    }
                });
            }
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private static final int TRANS_X = 0;
    private static final int TRANS_Y = 1;
    private static final int SCALE_X = 2;
    private static final int SCALE_Y = 3;
    private static final int ROTATION = 4;

    /**
     * Get an array of [tx, ty, sx, sy, rotation] sequence representing
     * the transformation from the given event.
     */
    private float[] getTransformFromEvent(PinchEvent event) {
        float[] transform = new float[]{0f, 0f, 1f, 1f, 0f};

        // Stop pointer 1.
        final float stopX1 = event.pointer1.x;
        final float stopY1 = event.pointer1.y;
        // Stop pointer 2.
        final float stopX2 = event.pointer2.x;
        final float stopY2 = event.pointer2.y;
        // Update the end vector between two touch pointers.
        mStopVectorInParent.set(stopX2 - stopX1,
                                stopY2 - stopY1,
                                true);
        // Update the end pivot between two touch pointers.
        // Start pivot.
        mStopPivotInParent.set((stopX1 + stopX2) / 2f,
                               (stopY1 + stopY2) / 2f);

        // Calculate the translation.
        transform[TRANS_X] = mStopPivotInParent.x - mStartPivotInParent.x;
        transform[TRANS_Y] = mStopPivotInParent.y - mStartPivotInParent.y;
        // Calculate the rotation degree.
        transform[ROTATION] = (float) Math.toDegrees(
            Math.atan2(mStopVectorInParent.y, mStopVectorInParent.x) -
            Math.atan2(mStartVectorInParent.y, mStartVectorInParent.x));
        // Calculate the scale change.
        final float scale = mStopVectorInParent.length / mStartVectorInParent.length;
        transform[SCALE_X] = scale;
        transform[SCALE_Y] = scale;
//        mLogger.d("xyz", String.format(Locale.ENGLISH,
//                                    "getTransform: " +
//                                    "tx=%.3f, ty=%.3f, " +
//                                    "scale=%.3f, " +
//                                    "rot=%.3f",
//                                    transform[TRANS_X], transform[TRANS_Y],
//                                    transform[SCALE_X],
//                                    transform[ROTATION]));

        return transform;
    }
}
