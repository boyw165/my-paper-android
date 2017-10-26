package com.my.widget.gesture;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class MyGestureDetector implements Handler.Callback {

    private int mTouchSlopSquare;
    private int mDoubleTapTouchSlopSquare;
    private int mDoubleTapSlopSquare;
    private int mMinimumFlingVelocity;
    private int mMaximumFlingVelocity;

    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

    // constants for Message.what used by GestureHandler below
    private static final int MSG_FINGER_DOWN = 0;
    private static final int MSG_FINGER_UP_OR_CANCEL = 1;
    private static final int MSG_LONG_PRESS = 2;
    private static final int MSG_LONG_TAP = 3;
    private static final int MSG_TAP = 4;

    private final Handler mHandler;
    private final MyGestureDetector.MyGestureListener mNewListener;

    private boolean mStillDown;
    private boolean mDeferConfirmSingleTap;
    private boolean mHadLongPress;
    private boolean mAlwaysInTapRegion;
    private boolean mAlwaysInBiggerTapRegion;

    private MotionEvent mCurrentDownEvent;
    private MotionEvent mPreviousUpEvent;

    protected int mTapCount;

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
                             MyGestureDetector.MyGestureListener newListener) {
        this(context, newListener, null);
    }

    /**
     * Creates a MyGestureDetector with the supplied listener that runs deferred events on the
     * thread associated with the supplied {@link android.os.Handler}.
     *
     * @param context the application's context
     * @param handler the handler to use for running deferred listener events.
     * @throws NullPointerException if {@code listener} is null.
     * @see android.os.Handler#Handler()
     */
    public MyGestureDetector(Context context,
                             MyGestureDetector.MyGestureListener newListener,
                             Handler handler) {
        if (handler != null) {
            mHandler = new GestureHandler(handler, this);
        } else {
            mHandler = new GestureHandler(this);
        }
        mNewListener = newListener;
        init(context);
    }

    private void init(Context context) {
        // Fallback to support pre-donuts releases
        int touchSlop, doubleTapSlop, doubleTapTouchSlop;
        if (context == null) {
            //noinspection deprecation
            touchSlop = ViewConfiguration.getTouchSlop();
            doubleTapTouchSlop = touchSlop; // Hack rather than adding a hiden method for this
//            doubleTapSlop = ViewConfiguration.getDoubleTapSlop();
            doubleTapSlop = ViewConfiguration.getTouchSlop();
            //noinspection deprecation
            mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
            mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
        } else {
            final ViewConfiguration configuration = ViewConfiguration.get(context);
            touchSlop = configuration.getScaledTouchSlop();
//            doubleTapTouchSlop = configuration.getScaledDoubleTapTouchSlop();
            doubleTapTouchSlop = configuration.getTouchSlop();
            doubleTapSlop = configuration.getScaledDoubleTapSlop();
            mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
            mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        }
        mTouchSlopSquare = touchSlop * touchSlop;
        mDoubleTapTouchSlopSquare = doubleTapTouchSlop * doubleTapTouchSlop;
        mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
    }

    public void setIsTapEnabled(boolean enabled) {
        mIsTapEnabled = enabled;
    }

    /**
     * Set whether longpress is enabled, if this is enabled when a user
     * presses and holds down you get a longpress event and nothing further.
     * If it's disabled the user can press and hold down and then later
     * moved their finger and you will get scroll events. By default
     * longpress is enabled.
     *
     * @param enabled whether long-press should be enabled.
     */
    public void setIsLongpressEnabled(boolean enabled) {
        mIsLongPressEnabled = enabled;
    }

    /**
     * Analyzes the given motion event and if applicable triggers the
     * appropriate callbacks on the {@link MyGestureDetector.MyGestureListener}
     * supplied.
     *
     * @param event The current motion event.
     * @return true if the {@link MyGestureDetector.MyGestureListener} consumed
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

        boolean handled = false;

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

                // Accumulate the tap count.
                ++mTapCount;

                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(event);
                mAlwaysInTapRegion = true;
                mAlwaysInBiggerTapRegion = true;
                mStillDown = true;
                mHadLongPress = false;
                mDeferConfirmSingleTap = false;

                if (mIsLongPressEnabled) {
                    mHandler.removeMessages(MSG_LONG_PRESS);
                    mHandler.sendMessageAtTime(
                        obtainMessageWithPayload(
                            MSG_LONG_PRESS, event, touchingObject, touchingContext),
                        mCurrentDownEvent.getDownTime() + TAP_TIMEOUT + LONGPRESS_TIMEOUT);
                }

                // TODO:
                if (!hadTapMessage) {
                    mHandler.sendMessage(obtainMessageWithPayload(MSG_FINGER_DOWN,
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
                    int distance = (deltaX * deltaX) + (deltaY * deltaY);
                    if (distance > mTouchSlopSquare) {
//                        handled = mOldListener.onScroll(mCurrentDownEvent, event, scrollX, scrollY);
                        mLastFocusX = focusX;
                        mLastFocusY = focusY;

                        // Cancel taps.
                        cancelTaps();

                        // Cancel long-press.
                        cancelLongPress();

                        // Indicate that the current pointer is over the tap slope.
                        mAlwaysInTapRegion = false;
                    }
                    if (distance > mDoubleTapTouchSlopSquare) {
                        mAlwaysInBiggerTapRegion = false;
                    }
                } else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                    // TODO: onDragBegin, onDrag, and onDragEnd.
//                    handled = mOldListener.onScroll(mCurrentDownEvent, event, scrollX, scrollY);
                    mLastFocusX = focusX;
                    mLastFocusY = focusY;
                }
                break;

            case MotionEvent.ACTION_UP:
                mStillDown = false;
                MotionEvent currentUpEvent = MotionEvent.obtain(event);

                // TODO: Two continuous taps far away doesn't count as a double tap.
                if (mHadLongPress) {
                    mHandler.sendMessage(
                        obtainMessageWithPayload(MSG_LONG_TAP,
                                                 event,
                                                 touchingObject,
                                                 touchingContext));
                } else if (mAlwaysInTapRegion) {
                    handled |= mIsTapEnabled;

                    mHandler.sendMessageDelayed(
                        obtainMessageWithPayload(MSG_TAP,
                                                 event,
                                                 touchingObject,
                                                 touchingContext),
                        TAP_TIMEOUT);
                } else {
                    mHandler.sendMessage(
                        obtainMessageWithPayload(MSG_FINGER_UP_OR_CANCEL,
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
                if (mPreviousUpEvent != null) {
                    mPreviousUpEvent.recycle();
                }
                // Hold the event we obtained above - listeners may have changed the original.
                mPreviousUpEvent = currentUpEvent;
                if (mVelocityTracker != null) {
                    // This may have been cleared when we called out to the
                    // application above.
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                mDeferConfirmSingleTap = false;

                cancelLongPress();
                break;

            case MotionEvent.ACTION_CANCEL:
                cancelAll();
                break;
        }

        return handled;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {

            case MSG_FINGER_DOWN: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                mNewListener.onActionDown(payload.event,
                                          payload.touchingTarget,
                                          payload.touchingContext);
                return true;
            }

            case MSG_FINGER_UP_OR_CANCEL: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                mNewListener.onActionUpOrCancel(payload.event,
                                                payload.touchingTarget,
                                                payload.touchingContext,
                                                payload.event.maskedAction == MotionEvent.ACTION_CANCEL);
                return true;
            }

            case MSG_LONG_PRESS: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                // Notify the detector that it's NOT a tap anymore.
                cancelTaps();
                mHadLongPress = true;

                if (mIsLongPressEnabled) {
                    mNewListener.onLongPress(payload.event,
                                             payload.touchingTarget,
                                             payload.touchingContext);
                }
                return true;
            }

            case MSG_LONG_TAP: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                // Notify the detector that it's NOT a tap anymore.
                cancelTaps();

                if (mIsLongPressEnabled) {
                    mNewListener.onLongTap(payload.event,
                                             payload.touchingTarget,
                                             payload.touchingContext);
                }
                mHadLongPress = false;

                // Dispatch ACTION_UP (ACTION_CANCEL).
                mNewListener.onActionUpOrCancel(payload.event,
                                                payload.touchingTarget,
                                                payload.touchingContext,
                                                payload.event.maskedAction == MotionEvent.ACTION_CANCEL);

                return true;
            }

            case MSG_TAP: {
                final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                if (mIsTapEnabled) {
                    if (mTapCount > 1) {
                        mNewListener.onDoubleTap(payload.event,
                                                 payload.touchingTarget,
                                                 payload.touchingContext);
                    } else {
                        mNewListener.onSingleTap(payload.event,
                                                 payload.touchingTarget,
                                                 payload.touchingContext);
                    }
                }

                // Dispatch ACTION_UP (ACTION_CANCEL).
                mNewListener.onActionUpOrCancel(payload.event,
                                                payload.touchingTarget,
                                                payload.touchingContext,
                                                payload.event.maskedAction == MotionEvent.ACTION_CANCEL);

                // Notify the detector that this async-callback is consumed.
                cancelTaps();

                return true;
            }

            default:
                return false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void cancelAll() {
        mHandler.removeMessages(MSG_LONG_PRESS);
        mHandler.removeMessages(MSG_LONG_TAP);

        mHandler.removeMessages(MSG_TAP);

        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mStillDown = false;
        mAlwaysInTapRegion = false;
        mAlwaysInBiggerTapRegion = false;
        mDeferConfirmSingleTap = false;
        mHadLongPress = false;
    }

    private void cancelLongPress() {
        mHandler.removeMessages(MSG_LONG_PRESS);
//        mHandler.removeMessages(MSG_LONG_TAP);

        mHadLongPress = false;
    }

    private void cancelTaps() {
        mHandler.removeMessages(MSG_TAP);

        mTapCount = 0;

//        mAlwaysInTapRegion = false;
        mAlwaysInBiggerTapRegion = false;
        mDeferConfirmSingleTap = false;
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

//    private boolean isConsideredDoubleTap(MotionEvent firstDown,
//                                          MotionEvent firstUp,
//                                          MotionEvent secondDown) {
//        if (!mAlwaysInBiggerTapRegion) {
//            return false;
//        }
//
//        final long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
//        if (deltaTime > DOUBLE_TAP_TIMEOUT || deltaTime < DOUBLE_TAP_MIN_TIME) {
//            return false;
//        }
//
//        int deltaX = (int) firstDown.getX() - (int) secondDown.getX();
//        int deltaY = (int) firstDown.getY() - (int) secondDown.getY();
//        return (deltaX * deltaX + deltaY * deltaY < mDoubleTapSlopSquare);
//    }

    private void dispatchLongPress() {
        mHandler.removeMessages(MSG_TAP);
        mDeferConfirmSingleTap = false;
        mHadLongPress = true;
//        mOldListener.onLongPress(mCurrentDownEvent);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    public interface MyGestureListener {

        void onActionDown(MyMotionEvent event,
                          Object touchingObject,
                          Object touchingContext);

        void onActionUpOrCancel(MyMotionEvent event,
                                Object touchingObject,
                                Object touchingContext,
                                boolean isCancel);

        void onSingleTap(MyMotionEvent event,
                         Object touchingObject,
                         Object touchingContext);

        boolean onDoubleTap(MyMotionEvent event,
                            Object touchingObject,
                            Object touchingContext);

        boolean onLongTap(MyMotionEvent event,
                          Object touchingObject,
                          Object touchingContext);

        boolean onLongPress(MyMotionEvent event,
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
         * @param event
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

        GestureHandler(Handler handler,
                       Callback callback) {
            super(handler.getLooper(), callback);
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

        public final MyMotionEvent event;
        public final Object touchingTarget;
        public final Object touchingContext;

        MyMessagePayload(MyMotionEvent event,
                         Object touchingTarget,
                         Object touchingContext) {
            this.event = event;
            this.touchingTarget = touchingTarget;
            this.touchingContext = touchingContext;
        }
    }
}

