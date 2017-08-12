// Copyright (c) 2017-present boyw165
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

package com.cardinalblue.lib.doodle.gesture;

import com.cardinalblue.lib.doodle.data.PointF;
import com.cardinalblue.lib.doodle.event.DragEvent;
import com.cardinalblue.lib.doodle.event.GestureEvent;
import com.cardinalblue.lib.doodle.event.PinchEvent;
import com.cardinalblue.lib.doodle.event.SingleTapEvent;
import com.cardinalblue.lib.doodle.event.UiTouchEvent;
import com.cardinalblue.lib.doodle.protocol.ILogger;
import com.cardinalblue.lib.doodle.protocol.IMatrixProvider;
import com.cardinalblue.lib.doodle.protocol.IMotionEvent;

import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

// TODO: Bug from PINCH.
public class GestureRecognizer implements ObservableTransformer<UiTouchEvent, GestureEvent> {

    // Given...
    private final IMatrixProvider mMatrixProvider;
    private final float mDragSlop;
    private final ILogger mLogger;

    // State.
    private boolean mCouldBeSingleTap;
    private boolean mIsAvailable;

    /**
     * Array of start point of a gesture, e.g. drag and pinch.
     */
    private final PointF[] mStartPoints = new PointF[]{
        new PointF(), new PointF()
    };

    private static final int IDLE = 0;
    private static final int DRAG = 1;
    private static final int PINCH = 2;
    private int mGesture = IDLE;

    // TODO: Inject this.
    public GestureRecognizer(IMatrixProvider matrixProvider,
                             float dragSlop,
                             ILogger logger) {
        mMatrixProvider = matrixProvider;
        mDragSlop = dragSlop;
        mLogger = logger;

//        // DEBUG.
//        mLogger.d("xyz", String.format(Locale.ENGLISH,
//                                       "drag slop=%.3f",
//                                       dragSlop));
    }

    @Override
    public ObservableSource<GestureEvent> apply(Observable<UiTouchEvent> upstream) {
        return upstream.flatMap(new Function<UiTouchEvent, ObservableSource<GestureEvent>>() {
            @Override
            public ObservableSource<GestureEvent> apply(UiTouchEvent uiEvent)
                throws Exception {
                final IMotionEvent event = uiEvent.bundle;
                final int action = event.getActionMasked();

//                // FIXME: This is DEBUG code.
//                mLogger.d("xyz", "--- MotionEvent ---");
//                for (int i = 0; i < event.getPointerCount(); ++i) {
//                    final float x = event.getX(i);
//                    final float y = event.getY(i);
//
//                    mLogger.d("xyz", String.format(Locale.ENGLISH,
//                                                   "#%d act=%s, x=%.3f, y=%.3f",
//                                                   i, stringOfAction(event), x, y));
//                }

                // If the matrix provider is not available at the ACTION_DOWN,
                // then the gesture should be unavailable during the gesture
                // lifecycle. The availability would be determined in the next
                // ACTION_DOWN.
                if (action == event.ACTION_DOWN()) {
                    mIsAvailable = mMatrixProvider.isAvailable();
                }

//                if (action == event.ACTION_DOWN()) {
//                    mLogger.d("xyz", "ACTION_DOWN, available=" + mIsAvailable);
//                } else if (action == event.ACTION_POINTER_DOWN()) {
//                    mLogger.d("xyz", "ACTION_POINTER_DOWN, available=" + mIsAvailable);
//                } else if (action == event.ACTION_MOVE()) {
//                    mLogger.d("xyz", "ACTION_MOVE, available=" + mIsAvailable);
//                } else if (action == event.ACTION_POINTER_UP()) {
//                    mLogger.d("xyz", "ACTION_POINTER_UP, available=" + mIsAvailable);
//                } else if (action == event.ACTION_UP()) {
//                    mLogger.d("xyz", "ACTION_UP, available=" + mIsAvailable);
//                } else if (action == event.ACTION_CANCEL()) {
//                    mLogger.d("xyz", "ACTION_CANCEL, available=" + mIsAvailable);
//                }

                // Ready to recognize the gesture.
                Observable<GestureEvent> ret;
                if (mIsAvailable) {
                    if (action == event.ACTION_DOWN()) {
                        ret = recognizeActionDown(event);
                    } else if (action == event.ACTION_UP() ||
                               action == event.ACTION_CANCEL()) {
                        ret = recognizeActionUp(event);
                    } else if (action == event.ACTION_POINTER_DOWN()) {
                        ret = recognizePointerDown(event);
                    } else if (action == event.ACTION_POINTER_UP()) {
                        ret = recognizePointerUp(event);
                    } else if (action == event.ACTION_MOVE()) {
                        ret = recognizeActionMove(event);
                    } else {
                        // IDLE for case don't know how to handle with.
                        ret = Observable.just(GestureEvent.IDLE);
                    }
                } else {
                    // IDLE because this gesture is labelled unavailable at the
                    // beginning.
                    ret = Observable.just(GestureEvent.IDLE);
                }

//                // DEBUG.
//                final String stringOfGesture;
//                switch (mGesture) {
//                    case DRAG:
//                        stringOfGesture = "DRAG";
//                        break;
//                    case PINCH:
//                        stringOfGesture = "PINCH";
//                        break;
//                    case IDLE:
//                        stringOfGesture = "IDLE";
//                        break;
//                    default:
//                        stringOfGesture = "DEFAULT";
//                        break;
//                }
//                mLogger.d("xyz", String.format(Locale.ENGLISH,
//                                               "gesture=%s",
//                                               stringOfGesture));

                return ret
                    // Ignore IDLE event.
                    .filter(new Predicate<GestureEvent>() {
                        @Override
                        public boolean test(GestureEvent event)
                            throws Exception {
                            return !event.equals(GestureEvent.IDLE);
                        }
                    });
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private Observable<GestureEvent> recognizeActionDown(IMotionEvent event) {
        // Record the first pointer.
        mStartPoints[0].set(event.getX(0), event.getY(0));

        mGesture = IDLE;
        mCouldBeSingleTap = true;

        // TODO: Reusable?
        return Observable.just(GestureEvent.start());
    }

    private Observable<GestureEvent> recognizeActionUp(IMotionEvent event) {
        Observable<GestureEvent> source;
        if (mGesture == DRAG) {
            source = Observable.just(
                DragEvent.stop(),
                GestureEvent.stop());
        } else if (mGesture == PINCH) {
            source = Observable.just(
                PinchEvent.stop(mMatrixProvider.getMatrixOfParentToTarget(),
                                event.getX(0), event.getY(0),
                                event.getX(1), event.getY(1)),
                GestureEvent.stop());
        } else {
            if (mCouldBeSingleTap && event.getPointerCount() == 1) {
                source = Observable.just(
                    SingleTapEvent.just(mMatrixProvider.getMatrixOfParentToTarget(),
                                        event.getX(0),
                                        event.getY(0)),
                    GestureEvent.stop());
            } else {
                source = Observable.just(
                    GestureEvent.stop());
            }
        }

        mGesture = IDLE;

        // TODO: Reusable?
        return source;
    }

    private Observable<GestureEvent> recognizePointerDown(IMotionEvent event) {
        // Since there's more than on finger-down, it's impossible to end up
        // with a single-tap-event.
        mCouldBeSingleTap = false;

        if (event.getPointerCount() <= 2) {
            // Renew the start pointers.
            mStartPoints[0].set(event.getX(0), event.getY(0));
            mStartPoints[1].set(event.getX(1), event.getY(1));

            Observable<GestureEvent> source;
            if (mGesture == DRAG) {
                source = Observable.just(
                    DragEvent.stop(),
                    PinchEvent.start(mMatrixProvider.getMatrixOfParentToTarget(),
                                     mStartPoints[0].x,
                                     mStartPoints[0].y,
                                     mStartPoints[1].x,
                                     mStartPoints[1].y));
            } else {
                // TODO: From unknown gesture to PINCH.
                source = Observable.<GestureEvent>just(
                    PinchEvent.start(mMatrixProvider.getMatrixOfParentToTarget(),
                                     mStartPoints[0].x,
                                     mStartPoints[0].y,
                                     mStartPoints[1].x,
                                     mStartPoints[1].y));
            }

            // It's always a PINCH if there're more than two fingers down.
            mGesture = PINCH;

            return source;
        } else {
            // Ignore for any finger-down if it's more than two.
            return Observable.just(GestureEvent.IDLE);
        }
    }

    private Observable<GestureEvent> recognizePointerUp(IMotionEvent event) {
        if (event.getPointerCount() <= 2) {
            final int indexOfChangedPointer = event.getActionIndex();
            final int indexOfNotChangedPointer = indexOfChangedPointer == 0 ? 1 : 0;

            final Observable<GestureEvent> source;
            if (mGesture == PINCH && event.getPointerCount() <= 2) {
                source = Observable.<GestureEvent>just(
                    PinchEvent.stop(mMatrixProvider.getMatrixOfParentToTarget(),
                                    event.getX(0), event.getY(0),
                                    event.getX(1), event.getY(1)));
            } else {
                // Unknown gesture.
                source = Observable.just(GestureEvent.IDLE);
            }

            // Whenever there's a finger up, reset state to IDLE so that it
            // determine gesture from a clean state.
            mGesture = IDLE;
            mStartPoints[0].set(event.getX(indexOfNotChangedPointer),
                                event.getY(indexOfNotChangedPointer));

            return source;
        } else {
            // TODO: Reusable?
            return Observable.just(GestureEvent.IDLE);
        }
    }

    private Observable<GestureEvent> recognizeActionMove(IMotionEvent event) {
        // Single pointer is always a DRAG.
        final float dx1 = event.getX(0) - mStartPoints[0].x;
        final float dy1 = event.getY(0) - mStartPoints[0].y;

        if (mGesture == DRAG) {
            // If it was a DRAG, it would be a DRAG too.
            return Observable.<GestureEvent>just(
                DragEvent.doing(mMatrixProvider.getMatrixOfParentToTarget(),
                                event.getX(0), event.getY(0)));
        } else if (mGesture == PINCH) {
            return Observable.<GestureEvent>just(
                PinchEvent.doing(mMatrixProvider.getMatrixOfParentToTarget(),
                                 event.getX(0), event.getY(0),
                                 event.getX(1), event.getY(1)));
        } else if (mGesture == IDLE) {
            if (Math.abs(dx1) > mDragSlop ||
                Math.abs(dy1) > mDragSlop) {
                mGesture = DRAG;
                mCouldBeSingleTap = false;

                mStartPoints[0].x = event.getX(0);
                mStartPoints[0].y = event.getY(0);

                return Observable.<GestureEvent>just(
                    DragEvent.start(mMatrixProvider.getMatrixOfParentToTarget(),
                                    mStartPoints[0].x,
                                    mStartPoints[0].y));
            } else {
                return Observable.just(GestureEvent.IDLE);
            }
        } else {
            // TODO: Complete it.
            return Observable.just(GestureEvent.IDLE);
        }
    }

    private String stringOfAction(IMotionEvent event) {
        final int action = event.getActionMasked();

        if (action == event.ACTION_DOWN()) {
            return "ACTION_DOWN";
        } else if (action == event.ACTION_UP()) {
            return "ACTION_UP";
        } else if (action == event.ACTION_CANCEL()) {
            return "ACTION_CANCEL";
        } else if (action == event.ACTION_POINTER_DOWN()) {
            return "ACTION_POINTER_DOWN";
        } else if (action == event.ACTION_POINTER_UP()) {
            return "ACTION_POINTER_UP";
        } else if (action == event.ACTION_MOVE()) {
            return "ACTION_MOVE";
        } else {
            return "N/A";
        }
    }
}
