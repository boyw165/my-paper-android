//  Copyright Oct 2017-present CardinalBlue
//
//  Author: boy@cardinalblue.com
//          jack.huang@cardinalblue.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.my.widget.gesture;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import com.my.widget.R;

public class MyGestureDetector implements Handler.Callback {

    private int mTouchSlopSquare;
    private int mTapSlopSquare;
    private int mMinimumFlingVelocity;
    private int mMaximumFlingVelocity;

    /**
     * Defines the default duration in milliseconds before a press turns into
     * a long press.
     */
    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    /**
     * The duration in milliseconds we will wait to see if a touch event is a
     * tap or a scroll. If the user does not move within this interval, it is
     * considered to be a tap.
     */
    private static final int TAP_TIMEOUT = Math.max(150, ViewConfiguration.getTapTimeout());

    // constants for Message.what used by GestureHandler below
    private static final int MSG_ACTION_BEGIN = 0x0;
    private static final int MSG_ACTION_END = 0xFFFFFFFF;
    private static final int MSG_LONG_PRESS = 0xA1;
    private static final int MSG_LONG_TAP = 0xA2;
    private static final int MSG_TAP = 0xA3;
    private static final int MSG_FLING = 0xB1;
    private static final int MSG_DRAG_BEGIN = 0xB2;
    private static final int MSG_DRAGGING = 0xB3;
    private static final int MSG_DRAG_END = 0xB4;
    private static final int MSG_PINCH_BEGIN = 0xC1;
    private static final int MSG_PINCHING = 0xC2;
    private static final int MSG_PINCH_END = 0xC3;

    private final Handler mHandler;
    private final GestureListener mListener;

    private boolean mIsDragging;
    private boolean mHadLongPress;
    private boolean mAlwaysInTapRegion;

    private MotionEvent mPreviousUpEvent;
    private MotionEvent mCurrentUpEvent;
    private MotionEvent mCurrentDownEvent;

    private int mTapCount;

    private float mLastFocusX;
    private float mLastFocusY;
    private float mDownFocusX;
    private float mDownFocusY;

    private boolean mIsTapEnabled = true;
    private boolean mIsLongPressEnabled = true;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    /**
     * Creates a MyGestureDetector with the supplied listener.
     * You may only use this constructor from a {@link android.os.Looper} thread.
     *
     * @param context the application's context
     * @throws NullPointerException if {@code listener} is null.
     * @see android.os.Handler#Handler()
     */
    public MyGestureDetector(Context context,
                             GestureListener listener) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "The detector should be always initialized on the main thread.");
        }

        mHandler = new GestureHandler(this);
        mListener = listener;

        init(context);
    }

    public void setIsTapEnabled(boolean enabled) {
        mIsTapEnabled = enabled;
    }

    /**
     * Set whether long-press is enabled, if this is enabled when a user
     * presses and holds down you get a long-press event and nothing further.
     * If it's disabled the user can press and hold down and then later
     * moved their finger and you will get scroll events. By default
     * long-press is enabled.
     *
     * @param enabled whether long-press should be enabled.
     */
    public void setIsLongpressEnabled(boolean enabled) {
        mIsLongPressEnabled = enabled;
    }

    /**
     * Analyzes the given motion event and if applicable triggers the
     * appropriate callbacks on the {@link GestureListener}
     * supplied.
     *
     * @param event The current motion event.
     * @return true if the {@link GestureListener} consumed
     * the event, else false.
     */
    public boolean onTouchEvent(MotionEvent event,
                                Object touchingObject,
                                Object touchingContext) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final int action = event.getActionMasked();
        final boolean pointerUp = (action == MotionEvent.ACTION_POINTER_UP);
        final int skipIndex = pointerUp ? event.getActionIndex() : -1;

        // Determine focal point
        float sumX = 0, sumY = 0;
        final int count = event.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (skipIndex == i) continue;
            sumX += event.getX(i);
            sumY += event.getY(i);
        }
        final int div = pointerUp ? count - 1 : count;
        final float focusX = sumX / div;
        final float focusY = sumY / div;

        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;

                // Cancel long press and taps
                cancelLongPress();
                cancelTaps();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;

                // Check the dot product of current velocities.
                // If the pointer that left was opposing another velocity vector, clear.
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                final int upIndex = event.getActionIndex();
                final int upId = event.getPointerId(upIndex);
                final float upX = mVelocityTracker.getXVelocity(upId);
                final float upY = mVelocityTracker.getYVelocity(upId);
                for (int i = 0; i < count; i++) {
                    if (i == upIndex) continue;

                    final int otherId = event.getPointerId(i);
                    final float otherX = upX * mVelocityTracker.getXVelocity(otherId);
                    final float otherY = upY * mVelocityTracker.getYVelocity(otherId);

                    final float dot = otherX + otherY;
                    if (dot < 0) {
                        mVelocityTracker.clear();
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_DOWN:
                boolean hadTapMessage = mHandler.hasMessages(MSG_TAP);
                if (hadTapMessage) {
                    // Remove unhandled tap.
                    mHandler.removeMessages(MSG_TAP);
                }

                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(event);
                mAlwaysInTapRegion = true;
                mHadLongPress = false;
                mIsDragging = false;

                // Handle long-press.
                if (mIsLongPressEnabled) {
                    mHandler.removeMessages(MSG_LONG_PRESS);
                    mHandler.sendMessageAtTime(
                        obtainMessageWithPayload(
                            MSG_LONG_PRESS, event, touchingObject, touchingContext),
                        mCurrentDownEvent.getDownTime() + TAP_TIMEOUT + LONG_PRESS_TIMEOUT);
                }

                // If there is a TAP recorded in the action session, don't fire
                // duplicated BEGIN event.
                if (!hadTapMessage) {
                    mHandler.sendMessage(obtainMessageWithPayload(MSG_ACTION_BEGIN,
                                                                  event,
                                                                  touchingObject,
                                                                  touchingContext));
                }
                break;

            case MotionEvent.ACTION_MOVE:
                final float scrollX = mLastFocusX - focusX;
                final float scrollY = mLastFocusY - focusY;

                if (mAlwaysInTapRegion) {
                    final int deltaX = (int) (focusX - mDownFocusX);
                    final int deltaY = (int) (focusY - mDownFocusY);
                    final int distance = (deltaX * deltaX) + (deltaY * deltaY);

                    if (distance > mTouchSlopSquare) {
                        mLastFocusX = focusX;
                        mLastFocusY = focusY;

                        // Cancel taps.
                        cancelTaps();
                        // Cancel long-press.
                        cancelLongPress();

                        // Indicate that the current pointer is over the tap slope.
                        mAlwaysInTapRegion = false;
                        mHandler.sendMessage(
                            obtainMessageWithPayload(MSG_DRAG_BEGIN,
                                                     event,
                                                     touchingObject,
                                                     touchingContext));
                    }
                } else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                    // Begin the drag.
                    mHandler.sendMessage(
                        obtainMessageWithPayload(MSG_DRAGGING,
                                                 event,
                                                 touchingObject,
                                                 touchingContext));

                    mIsDragging = true;

                    mLastFocusX = focusX;
                    mLastFocusY = focusY;
                }
                break;

            case MotionEvent.ACTION_UP:
                // Hold events.
                if (mPreviousUpEvent != null) {
                    mPreviousUpEvent.recycle();
                    mPreviousUpEvent = null;
                }
                mPreviousUpEvent = mCurrentUpEvent;
                mCurrentUpEvent = MotionEvent.obtain(event);

                // TODO: Two continuous taps far away doesn't count as a double tap.
                if (mHadLongPress) {
                    mHandler.sendMessage(
                        obtainMessageWithPayload(MSG_LONG_TAP,
                                                 event,
                                                 touchingObject,
                                                 touchingContext));
                } else if (mAlwaysInTapRegion) {
                    // Accumulate the tap count if the current is closed to .
                    if (isConsideredCloseTap(mPreviousUpEvent, mCurrentUpEvent)) {
                        ++mTapCount;
                    } else {
                        // FIXME: Shall we reset the tap-count in this case?
//                        mTapCount = 0;
                    }

                    mHandler.sendMessageDelayed(
                        obtainMessageWithPayload(MSG_TAP,
                                                 event,
                                                 touchingObject,
                                                 touchingContext),
                        TAP_TIMEOUT);
                } else if (mIsDragging) {
                    mHandler.sendMessage(
                        obtainMessageWithPayload(MSG_DRAG_END,
                                                 event,
                                                 touchingObject,
                                                 touchingContext));
                } else {
                    mHandler.sendMessage(
                        obtainMessageWithPayload(MSG_ACTION_END,
                                                 event,
                                                 touchingObject,
                                                 touchingContext));
                }
//                else if (!mIgnoreNextUpEvent) {
//
//                    // A fling must travel the minimum tap distance
//                    final VelocityTracker velocityTracker = mVelocityTracker;
//                    final int pointerId = event.getPointerId(0);
//                    velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
//                    final float velocityY = velocityTracker.getYVelocity(pointerId);
//                    final float velocityX = velocityTracker.getXVelocity(pointerId);
//
//                    if ((Math.abs(velocityY) > mMinimumFlingVelocity)
//                        || (Math.abs(velocityX) > mMinimumFlingVelocity)) {
//                        handled = mOldListener.onFling(mCurrentDownEvent, event, velocityX, velocityY);
//                    }
//                }
                if (mVelocityTracker != null) {
                    // This may have been cleared when we called out to the
                    // application above.
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                cancelLongPress();
                cancelDrag();
                break;

            case MotionEvent.ACTION_CANCEL:
                cancelAll();
                break;
        }

        return mIsTapEnabled | mIsLongPressEnabled;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_ACTION_BEGIN: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                if (mListener != null) {
                    mListener.onActionBegin(payload.event,
                                            payload.touchingTarget,
                                            payload.touchingContext);
                }
                return true;
            }

            case MSG_ACTION_END: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                dispatchActionEnd(payload);
                return true;
            }

            case MSG_LONG_PRESS: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                // Notify the detector that it's NOT a tap anymore.
                cancelTaps();
                mHadLongPress = true;

                if (mIsLongPressEnabled && mListener != null) {
                    mListener.onLongPress(payload.event,
                                          payload.touchingTarget,
                                          payload.touchingContext);
                }
                return true;
            }

            case MSG_LONG_TAP: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                // Notify the detector that it's NOT a tap anymore.
                cancelTaps();

                if (mIsLongPressEnabled && mListener != null) {
                    mListener.onLongTap(payload.event,
                                        payload.touchingTarget,
                                        payload.touchingContext);
                }
                mHadLongPress = false;

                // Dispatch ACTION_UP (ACTION_CANCEL).
                dispatchActionEnd(payload);

                return true;
            }

            case MSG_TAP: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                if (mIsTapEnabled && mTapCount > 0) {
                    if (mTapCount == 1) {
                        mListener.onSingleTap(payload.event,
                                              payload.touchingTarget,
                                              payload.touchingContext);
                    } else if (mTapCount == 2) {
                        mListener.onDoubleTap(payload.event,
                                              payload.touchingTarget,
                                              payload.touchingContext);
                    } else {
                        mListener.onMoreTap(payload.event,
                                            payload.touchingTarget,
                                            payload.touchingContext,
                                            mTapCount);
                    }
                }

                // Notify the detector that this async-callback is consumed.
                cancelTaps();

                // Dispatch ACTION_UP (ACTION_CANCEL).
                dispatchActionEnd(payload);

                return true;
            }

            case MSG_DRAG_BEGIN: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                if (mListener != null) {
                    mListener.onDragBegin(payload.event,
                                          payload.touchingTarget,
                                          payload.touchingContext,
                                          0, 0);
                }

                return true;
            }

            case MSG_DRAGGING: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                if (mListener != null) {
                    mListener.onDrag(payload.event,
                                     payload.touchingTarget,
                                     payload.touchingContext,
                                     new float[2]);
                }

                return true;
            }

            case MSG_DRAG_END: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                if (mListener != null) {
                    mListener.onDragEnd(payload.event,
                                        payload.touchingTarget,
                                        payload.touchingContext,
                                        new float[2]);
                }

                // Dispatch ACTION_UP (ACTION_CANCEL).
                dispatchActionEnd(payload);

                return true;
            }

            default:
                return false;
        }
    }

    private void dispatchActionEnd(MyMessagePayload payload) {
        // Before dispatch the END callbacks, recycle the cached MotionEvent.
        if (mPreviousUpEvent != null) {
            mPreviousUpEvent.recycle();
            mPreviousUpEvent = null;
        }
        if (mCurrentDownEvent != null) {
            mCurrentDownEvent.recycle();
            mCurrentDownEvent = null;
        }
        if (mCurrentUpEvent != null) {
            mCurrentUpEvent.recycle();
            mCurrentUpEvent = null;
        }

        if (mListener != null) {
            mListener.onActionEnd(payload.event,
                                  payload.touchingTarget,
                                  payload.touchingContext,
                                  payload.event.maskedAction == MotionEvent.ACTION_CANCEL);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void init(Context context) {
        // Fallback to support pre-donuts releases
        int touchSlop, tapSlop;

        if (context == null) {
            // noinspection deprecation
            touchSlop = ViewConfiguration.getTouchSlop();
            // Hack rather than adding a hidden method for this
            tapSlop = 3 * ViewConfiguration.getTouchSlop();

            // noinspection deprecation
            mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
            mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
        } else {
            final ViewConfiguration configuration = ViewConfiguration.get(context);

            touchSlop = (int) Math.min(context.getResources().getDimension(R.dimen.touch_slop),
                                       configuration.getScaledTouchSlop());
            tapSlop = (int) Math.min(context.getResources().getDimension(R.dimen.tap_slop),
                                     configuration.getScaledDoubleTapSlop());

            mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
            mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        }

        mTouchSlopSquare = touchSlop * touchSlop;
        mTapSlopSquare = tapSlop * tapSlop;
    }

    private void cancelAll() {
        mHandler.removeMessages(MSG_LONG_PRESS);
        mHandler.removeMessages(MSG_LONG_TAP);

        mHandler.removeMessages(MSG_TAP);

        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mAlwaysInTapRegion = false;
        mHadLongPress = false;
    }

    private void cancelLongPress() {
        mHandler.removeMessages(MSG_LONG_PRESS);

        mHadLongPress = false;
    }

    private void cancelTaps() {
        mHandler.removeMessages(MSG_TAP);

        mTapCount = 0;
//        mAlwaysInTapRegion = false;
    }

    private void cancelDrag() {
        mIsDragging = false;
    }

    private Message obtainMessageWithPayload(int what,
                                             MotionEvent event,
                                             Object touchingObject,
                                             Object touchingContext) {
        // TODO: Remember to pass x and y array.
        MyMotionEvent eventClone = new MyMotionEvent(event.getActionMasked(), null, null);

        final Message msg = mHandler.obtainMessage(what);
        msg.obj = new MyMessagePayload(eventClone,
                                       touchingObject,
                                       touchingContext);

        return msg;
    }

    private Message obtainMessageWithPayload(int what,
                                             MyMessagePayload payload) {
        final Message msg = mHandler.obtainMessage(what);
        msg.obj = payload;

        return msg;
    }

    private boolean isConsideredCloseTap(MotionEvent previousUp,
                                         MotionEvent currentUp) {
        if (previousUp == null) return true;

        if (mAlwaysInTapRegion) {
//            final long deltaTime = currentUp.getEventTime() - previousUp.getEventTime();
//            if (deltaTime > TAP_TIMEOUT) {
//                // The previous-up is too long ago, it is definitely a new tap!
//                return false;
//            }

            final int deltaX = (int) currentUp.getX() - (int) previousUp.getX();
            final int deltaY = (int) currentUp.getY() - (int) previousUp.getY();
            return (deltaX * deltaX + deltaY * deltaY < mTapSlopSquare);
        } else {
            return false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    public interface GestureListener {

        void onActionBegin(MyMotionEvent event,
                           Object touchingObject,
                           Object touchingContext);

        void onActionEnd(MyMotionEvent event,
                         Object touchingObject,
                         Object touchingContext,
                         boolean isCancel);

        void onSingleTap(MyMotionEvent event,
                         Object touchingObject,
                         Object touchingContext);

        void onDoubleTap(MyMotionEvent event,
                         Object touchingObject,
                         Object touchingContext);

        void onMoreTap(MyMotionEvent event,
                       Object touchingObject,
                       Object touchingContext,
                       int tapCount);

        void onLongTap(MyMotionEvent event,
                       Object touchingObject,
                       Object touchingContext);

        void onLongPress(MyMotionEvent event,
                         Object touchingObject,
                         Object touchingContext);

        // Drag ///////////////////////////////////////////////////////////////

        boolean onDragBegin(MyMotionEvent event,
                            Object touchingObject,
                            Object touchingContext,
                            float xInCanvas,
                            float yInCanvas);

        void onDrag(MyMotionEvent event,
                    Object touchingObject,
                    Object touchingContext,
                    float[] translationInCanvas);

        void onDragEnd(MyMotionEvent event,
                       Object touchingObject,
                       Object touchingContext,
                       float[] translationInCanvas);

        // Fling //////////////////////////////////////////////////////////////

        /**
         * Notified of a fling event when it occurs with the initial on down
         * {@link MotionEvent} and the matching up {@link MotionEvent}. The
         * calculated velocity is supplied along the x and y axis in pixels per
         * second.
         *
         * @param event                The MotionEvent alternative.
         * @param startPointerInCanvas The first down pointer that started the
         *                             fling.
         * @param stopPointerInCanvas  The move pointer that triggered the
         *                             current onFling.
         * @param velocityX            The velocity of this fling measured in
         *                             pixels per second along the x axis.
         * @param velocityY            The velocity of this fling measured in
         */
        boolean onFling(MyMotionEvent event,
                        Object touchingObject,
                        Object touchContext,
                        float[] startPointerInCanvas,
                        float[] stopPointerInCanvas,
                        float velocityX,
                        float velocityY);

        // Pinch //////////////////////////////////////////////////////////////

        boolean onPinchBegin(MyMotionEvent event,
                             Object touchingObject,
                             Object touchContext,
                             float pivotXInCanvas,
                             float pivotYInCanvas);

        void onPinch(MyMotionEvent event,
                     Object touchingObject,
                     Object touchContext,
                     float[] startPointerOneInCanvas,
                     float[] startPointerTwoInCanvas,
                     float[] stopPointerOneInCanvas,
                     float[] stopPointerTwoInCanvas);

        void onPinchEnd(MyMotionEvent event,
                        Object touchingObject,
                        Object touchContext,
                        float[] startPointerOneInCanvas,
                        float[] startPointerTwoInCanvas,
                        float[] stopPointerOneInCanvas,
                        float[] stopPointerTwoInCanvas);
    }

    private static class GestureHandler extends Handler {

        GestureHandler(Callback callback) {
            super(callback);
        }
    }

    public static final class MyMotionEvent {

        public final int maskedAction;

        public final float[] xArray;
        public final float[] yArray;

        private MyMotionEvent(int maskedAction,
                              float[] xArray,
                              float[] yArray) {
            this.maskedAction = maskedAction;

            this.xArray = xArray;
            this.yArray = yArray;
        }
    }

    private static class MyMessagePayload {

        final MyMotionEvent event;
        final Object touchingTarget;
        final Object touchingContext;

        MyMessagePayload(MyMotionEvent event,
                         Object touchingTarget,
                         Object touchingContext) {
            this.event = event;
            this.touchingTarget = touchingTarget;
            this.touchingContext = touchingContext;
        }
    }
}

