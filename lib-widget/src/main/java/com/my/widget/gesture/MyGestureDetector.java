package com.my.widget.gesture;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class MyGestureDetector {

    private int mTouchSlopSquare;
    private int mDoubleTapTouchSlopSquare;
    private int mDoubleTapSlopSquare;
    private int mMinimumFlingVelocity;
    private int mMaximumFlingVelocity;

    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    //    private static final int DOUBLE_TAP_MIN_TIME = ViewConfiguration.getDoubleTapMinTime();
    private static final int DOUBLE_TAP_MIN_TIME = ViewConfiguration.getDoubleTapTimeout();

    // constants for Message.what used by GestureHandler below
    private static final int MSG_FINGER_DOWN = -1;
    private static final int MSG_FINGER_UP_OR_CANCEL = 0;
    private static final int MSG_SHOW_PRESS = 1;
    private static final int MSG_LONG_PRESS = 2;
    private static final int MSG_SINGLE_TAP = 3;
    private static final int MSG_DOUBLE_TAP = 4;

    private final Handler mHandler;
    private final MyGestureDetector.OnGestureListener mOldListener;
    private final MyGestureDetector.MyGestureListener mNewListener;
    private MyGestureDetector.OnDoubleTapListener mDoubleTapListener;

    private boolean mStillDown;
    private boolean mDeferConfirmSingleTap;
    private boolean mInLongPress;
    private boolean mAlwaysInTapRegion;
    private boolean mAlwaysInBiggerTapRegion;
    private boolean mIgnoreNextUpEvent;

    private MotionEvent mCurrentDownEvent;
    private MotionEvent mPreviousUpEvent;

    /**
     * True when the user is still touching for the second tap (down, move, and
     * up events). Can only be true if there is a double tap listener attached.
     */
    private boolean mIsDoubleTapping;

    private float mLastFocusX;
    private float mLastFocusY;
    private float mDownFocusX;
    private float mDownFocusY;

    private boolean mIsLongPressEnabled;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    /**
     * Creates a MyGestureDetector with the supplied listener.
     * You may only use this constructor from a {@link android.os.Looper} thread.
     *
     * @param context     the application's context
     * @param oldListener the listener invoked for all the callbacks, this must
     *                    not be null.
     * @throws NullPointerException if {@code listener} is null.
     * @see android.os.Handler#Handler()
     */
    public MyGestureDetector(Context context,
                             MyGestureDetector.OnGestureListener oldListener,
                             MyGestureDetector.MyGestureListener newListener) {
        this(context, oldListener, newListener, null);
    }

    /**
     * Creates a MyGestureDetector with the supplied listener that runs deferred events on the
     * thread associated with the supplied {@link android.os.Handler}.
     *
     * @param context     the application's context
     * @param oldListener the listener invoked for all the callbacks, this must
     *                    not be null.
     * @param handler     the handler to use for running deferred listener events.
     * @throws NullPointerException if {@code listener} is null.
     * @see android.os.Handler#Handler()
     */
    public MyGestureDetector(Context context,
                             MyGestureDetector.OnGestureListener oldListener,
                             MyGestureDetector.MyGestureListener newListener,
                             Handler handler) {
        if (handler != null) {
            mHandler = new GestureHandler(handler, newListener);
        } else {
            mHandler = new GestureHandler(newListener);
        }
        mOldListener = oldListener;
        mNewListener = newListener;
        if (oldListener instanceof MyGestureDetector.OnDoubleTapListener) {
            setOnDoubleTapListener((MyGestureDetector.OnDoubleTapListener) oldListener);
        }
        init(context);
    }

    private void init(Context context) {
        if (mOldListener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
        mIsLongPressEnabled = true;

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

    /**
     * Sets the listener which will be called for double-tap and related
     * gestures.
     *
     * @param onDoubleTapListener the listener invoked for all the callbacks, or
     *                            null to stop listening for double-tap gestures.
     */
    public void setOnDoubleTapListener(MyGestureDetector.OnDoubleTapListener onDoubleTapListener) {
        mDoubleTapListener = onDoubleTapListener;
    }

    /**
     * Set whether longpress is enabled, if this is enabled when a user
     * presses and holds down you get a longpress event and nothing further.
     * If it's disabled the user can press and hold down and then later
     * moved their finger and you will get scroll events. By default
     * longpress is enabled.
     *
     * @param isLongpressEnabled whether longpress should be enabled.
     */
    public void setIsLongpressEnabled(boolean isLongpressEnabled) {
        mIsLongPressEnabled = isLongpressEnabled;
    }

    /**
     * @return true if longpress is enabled, else false.
     */
    public boolean isLongpressEnabled() {
        return mIsLongPressEnabled;
    }

    /**
     * Analyzes the given motion event and if applicable triggers the
     * appropriate callbacks on the {@link MyGestureDetector.OnGestureListener}
     * supplied.
     *
     * @param event The current motion event.
     * @return true if the {@link MyGestureDetector.OnGestureListener} consumed
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
//                if (mDoubleTapListener != null) {
//                    boolean hadTapMessage = mHandler.hasMessages(MSG_SINGLE_TAP);
//                    if (hadTapMessage) mHandler.removeMessages(MSG_SINGLE_TAP);
//                    if ((mCurrentDownEvent != null) && (mPreviousUpEvent != null) && hadTapMessage &&
//                        isConsideredDoubleTap(mCurrentDownEvent, mPreviousUpEvent, event)) {
//                        // This is a second tap
//                        mIsDoubleTapping = true;
//                        // Give a callback with the first tap of the double-tap
//                        handled |= mDoubleTapListener.onDoubleTap(mCurrentDownEvent);
//                        // Give a callback with down event of the double-tap
//                        handled |= mDoubleTapListener.onDoubleTapEvent(event);
//                    } else {
//                        // This is a first tap
//                        mHandler.sendEmptyMessageDelayed(MSG_SINGLE_TAP, DOUBLE_TAP_TIMEOUT);
//                    }
//                }

                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(event);
                mAlwaysInTapRegion = true;
                mAlwaysInBiggerTapRegion = true;
                mStillDown = true;
                mInLongPress = false;
                mDeferConfirmSingleTap = false;

//                if (mIsLongPressEnabled) {
//                    mHandler.removeMessages(MSG_LONG_PRESS);
//                    mHandler.sendEmptyMessageAtTime(MSG_LONG_PRESS, mCurrentDownEvent.getDownTime()
//                                                                    + TAP_TIMEOUT + LONGPRESS_TIMEOUT);
//                }
//                mHandler.sendEmptyMessageAtTime(MSG_SHOW_PRESS, mCurrentDownEvent.getDownTime() + TAP_TIMEOUT);
//                handled |= mOldListener.onDown(event);

                // TODO:
                if (!ifIgnoreFingerDown()) {
                    mHandler.sendMessage(obtainMessageWithPayload(MSG_FINGER_DOWN,
                                                                  event,
                                                                  touchingObject,
                                                                  touchingContext));
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mInLongPress) {
                    break;
                }
                final float scrollX = mLastFocusX - focusX;
                final float scrollY = mLastFocusY - focusY;
                if (mIsDoubleTapping) {
                    // Give the move events of the double-tap
                    handled |= mDoubleTapListener.onDoubleTapEvent(event);
                } else if (mAlwaysInTapRegion) {
                    final int deltaX = (int) (focusX - mDownFocusX);
                    final int deltaY = (int) (focusY - mDownFocusY);
                    int distance = (deltaX * deltaX) + (deltaY * deltaY);
                    if (distance > mTouchSlopSquare) {
                        handled = mOldListener.onScroll(mCurrentDownEvent, event, scrollX, scrollY);
                        mLastFocusX = focusX;
                        mLastFocusY = focusY;
                        mAlwaysInTapRegion = false;
                        mHandler.removeMessages(MSG_SINGLE_TAP);
                        mHandler.removeMessages(MSG_DOUBLE_TAP);
                        mHandler.removeMessages(MSG_SHOW_PRESS);
                        mHandler.removeMessages(MSG_LONG_PRESS);

                        // TODO: Why not just use cancelTaps()?
//                        cancelTaps();
                    }
                    if (distance > mDoubleTapTouchSlopSquare) {
                        mAlwaysInBiggerTapRegion = false;
                    }
                } else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                    // TODO: onDragBegin, onDrag, and onDragEnd.
                    handled = mOldListener.onScroll(mCurrentDownEvent, event, scrollX, scrollY);
                    mLastFocusX = focusX;
                    mLastFocusY = focusY;
                }
                break;

            case MotionEvent.ACTION_UP:
                mStillDown = false;
                MotionEvent currentUpEvent = MotionEvent.obtain(event);
                if (mIsDoubleTapping) {
                    // Finally, give the up event of the double-tap
                    handled |= mDoubleTapListener.onDoubleTapEvent(event);
                } else if (mInLongPress) {
                    mHandler.removeMessages(MSG_SINGLE_TAP);
                    mHandler.removeMessages(MSG_DOUBLE_TAP);
                    mInLongPress = false;
                } else if (mAlwaysInTapRegion && !mIgnoreNextUpEvent) {

                    // TODO: WIP...

                    handled |= true;
                    if (handled) {
                        mHandler.sendMessageDelayed(
                            obtainMessageWithPayload(MSG_SINGLE_TAP,
                                                     event,
                                                     touchingObject,
                                                     touchingContext),
                            TAP_TIMEOUT);
                    }

//                    handled = mOldListener.onSingleTapUp(event);
//                    if (mDeferConfirmSingleTap && mDoubleTapListener != null) {
//                        mDoubleTapListener.onSingleTapConfirmed(event);
//                    }
                } else if (!mIgnoreNextUpEvent) {

                    // A fling must travel the minimum tap distance
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    final int pointerId = event.getPointerId(0);
                    velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                    final float velocityY = velocityTracker.getYVelocity(pointerId);
                    final float velocityX = velocityTracker.getXVelocity(pointerId);

                    if ((Math.abs(velocityY) > mMinimumFlingVelocity)
                        || (Math.abs(velocityX) > mMinimumFlingVelocity)) {
                        handled = mOldListener.onFling(mCurrentDownEvent, event, velocityX, velocityY);
                    }
                }
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
                mIsDoubleTapping = false;
                mDeferConfirmSingleTap = false;
                mIgnoreNextUpEvent = false;
                mHandler.removeMessages(MSG_SHOW_PRESS);
                mHandler.removeMessages(MSG_LONG_PRESS);
                break;

            case MotionEvent.ACTION_CANCEL:
                cancel();
                break;
        }

        return handled;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void cancel() {
        mHandler.removeMessages(MSG_SHOW_PRESS);
        mHandler.removeMessages(MSG_LONG_PRESS);

        mHandler.removeMessages(MSG_SINGLE_TAP);
        mHandler.removeMessages(MSG_DOUBLE_TAP);

        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mIsDoubleTapping = false;
        mStillDown = false;
        mAlwaysInTapRegion = false;
        mAlwaysInBiggerTapRegion = false;
        mDeferConfirmSingleTap = false;
        mInLongPress = false;
        mIgnoreNextUpEvent = false;
    }

    private void cancelTaps() {
        mHandler.removeMessages(MSG_SHOW_PRESS);
        mHandler.removeMessages(MSG_LONG_PRESS);
        mHandler.removeMessages(MSG_SINGLE_TAP);
        mHandler.removeMessages(MSG_DOUBLE_TAP);

        mIsDoubleTapping = false;
        mAlwaysInTapRegion = false;
        mAlwaysInBiggerTapRegion = false;
        mDeferConfirmSingleTap = false;
        mInLongPress = false;
        mIgnoreNextUpEvent = false;
    }

    private boolean ifIgnoreFingerDown() {
        return false;
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

    private void recycleEventClones() {

    }

    private boolean isConsideredDoubleTap(MotionEvent firstDown,
                                          MotionEvent firstUp,
                                          MotionEvent secondDown) {
        if (!mAlwaysInBiggerTapRegion) {
            return false;
        }

        final long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
        if (deltaTime > DOUBLE_TAP_TIMEOUT || deltaTime < DOUBLE_TAP_MIN_TIME) {
            return false;
        }

        int deltaX = (int) firstDown.getX() - (int) secondDown.getX();
        int deltaY = (int) firstDown.getY() - (int) secondDown.getY();
        return (deltaX * deltaX + deltaY * deltaY < mDoubleTapSlopSquare);
    }

    private void dispatchLongPress() {
        mHandler.removeMessages(MSG_SINGLE_TAP);
        mHandler.removeMessages(MSG_DOUBLE_TAP);
        mDeferConfirmSingleTap = false;
        mInLongPress = true;
        mOldListener.onLongPress(mCurrentDownEvent);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    public interface MyGestureListener {

        void onFingerDown(MyMotionEvent event,
                          Object touchingObject,
                          Object touchingContext);

        void onFingerUpOrCancel(MyMotionEvent event,
                                Object touchingObject,
                                Object touchingContext,
                                boolean isCancel);

//        boolean onPreSingleTap(MyMotionEvent event,
//                               Object touchingObject,
//                               Object touchingContext);

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

    /**
     * The listener that is used to notify when gestures occur.
     * If you want to listen for all the different gestures then implement
     * this interface.
     */
    public interface OnGestureListener {

        /**
         * Notified when a tap occurs with the down {@link MotionEvent}
         * that triggered it. This will be triggered immediately for
         * every down event. All other events should be preceded by this.
         *
         * @param e The down motion event.
         */
        boolean onDown(MotionEvent e);

        /**
         * The user has performed a down {@link MotionEvent} and not performed
         * a move or up yet. This event is commonly used to provide visual
         * feedback to the user to let them know that their action has been
         * recognized i.e. highlight an element.
         *
         * @param e The down motion event
         */
        void onShowPress(MotionEvent e);

        /**
         * Notified when a tap occurs with the up {@link MotionEvent}
         * that triggered it.
         *
         * @param e The up motion event that completed the first tap
         * @return true if the event is consumed, else false
         */
        boolean onSingleTapUp(MotionEvent e);

        /**
         * Notified when a scroll occurs with the initial on down {@link MotionEvent} and the
         * current move {@link MotionEvent}. The distance in x and y is also supplied for
         * convenience.
         *
         * @param e1        The first down motion event that started the scrolling.
         * @param e2        The move motion event that triggered the current onScroll.
         * @param distanceX The distance along the X axis that has been scrolled since the last
         *                  call to onScroll. This is NOT the distance between {@code e1}
         *                  and {@code e2}.
         * @param distanceY The distance along the Y axis that has been scrolled since the last
         *                  call to onScroll. This is NOT the distance between {@code e1}
         *                  and {@code e2}.
         * @return true if the event is consumed, else false
         */
        boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);

        /**
         * Notified when a long press occurs with the initial on down {@link MotionEvent}
         * that trigged it.
         *
         * @param e The initial on down motion event that started the longpress.
         */
        void onLongPress(MotionEvent e);

        /**
         * Notified of a fling event when it occurs with the initial on down {@link MotionEvent}
         * and the matching up {@link MotionEvent}. The calculated velocity is supplied along
         * the x and y axis in pixels per second.
         *
         * @param e1        The first down motion event that started the fling.
         * @param e2        The move motion event that triggered the current onFling.
         * @param velocityX The velocity of this fling measured in pixels per second
         *                  along the x axis.
         * @param velocityY The velocity of this fling measured in pixels per second
         *                  along the y axis.
         * @return true if the event is consumed, else false
         */
        boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
    }

    /**
     * The listener that is used to notify when a double-tap or a confirmed
     * single-tap occur.
     */
    public interface OnDoubleTapListener {
        /**
         * Notified when a single-tap occurs.
         * <p>
         * Unlike {@link MyGestureDetector.OnGestureListener#onSingleTapUp(MotionEvent)}, this
         * will only be called after the detector is confident that the user's
         * first tap is not followed by a second tap leading to a double-tap
         * gesture.
         *
         * @param e The down motion event of the single-tap.
         * @return true if the event is consumed, else false
         */
        boolean onSingleTapConfirmed(MotionEvent e);

        /**
         * Notified when a double-tap occurs.
         *
         * @param e The down motion event of the first tap of the double-tap.
         * @return true if the event is consumed, else false
         */
        boolean onDoubleTap(MotionEvent e);

        /**
         * Notified when an event within a double-tap gesture occurs, including
         * the down, move, and up events.
         *
         * @param e The motion event that occurred during the double-tap gesture.
         * @return true if the event is consumed, else false
         */
        boolean onDoubleTapEvent(MotionEvent e);
    }

    private static class GestureHandler extends Handler {

        final MyGestureListener mListener;

        GestureHandler(MyGestureListener listener) {
            super();

            mListener = listener;
        }

        GestureHandler(Handler handler,
                       MyGestureListener listener) {
            super(handler.getLooper());

            mListener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_SHOW_PRESS: {
//                    mOldListener.onShowPress(mCurrentDownEvent);
                    break;
                }

                case MSG_LONG_PRESS: {
//                    dispatchLongPress();
                    break;
                }

                case MSG_FINGER_DOWN: {
                    final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                    mListener.onFingerDown(payload.event,
                                           payload.touchingTarget,
                                           payload.touchingContext);
                    break;
                }

                case MSG_FINGER_UP_OR_CANCEL: {
                    final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                    mListener.onFingerUpOrCancel(payload.event,
                                                 payload.touchingTarget,
                                                 payload.touchingContext,
                                                 payload.event.maskedAction == MotionEvent.ACTION_CANCEL);
                    break;
                }

                case MSG_SINGLE_TAP: {
                    final MyMessagePayload payload = (MyMessagePayload) msg.obj;

                    mListener.onSingleTap(payload.event,
                                          payload.touchingTarget,
                                          payload.touchingContext);
                    mListener.onFingerUpOrCancel(payload.event,
                                                 payload.touchingTarget,
                                                 payload.touchingContext,
                                                 payload.event.maskedAction == MotionEvent.ACTION_CANCEL);

//                    // If the user's finger is still down, do not count it as a tap
//                    if (mDoubleTapListener != null) {
//                        if (!mStillDown) {
//                            mDoubleTapListener.onSingleTapConfirmed(mCurrentDownEvent);
//                        } else {
//                            mDeferConfirmSingleTap = true;
//                        }
//                    }
                    break;
                }

                default:
                    throw new RuntimeException("Unknown message " + msg); //never
            }
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

