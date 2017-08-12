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

package com.cardinalblue.lib.doodle.protocol;

public interface IMotionEvent {

    /**
     * Constant for {@link #getActionMasked}: A pressed gesture has started, the
     * motion contains the initial starting location.
     */
    int ACTION_DOWN();
    /**
     * Constant for {@link #getActionMasked}: A pressed gesture has finished, the
     * motion contains the final release location as well as any intermediate
     * points since the last down or move event.
     */
    int ACTION_UP();
    /**
     * Constant for {@link #getActionMasked}: A change has happened during a
     * press gesture (between {@link #ACTION_DOWN} and {@link #ACTION_UP}).
     * The motion contains the most recent point, as well as any intermediate
     * points since the last down or move event.
     */
    int ACTION_MOVE();
    /**
     * Constant for {@link #getActionMasked}: The current gesture has been aborted.
     * You will not receive any more points in it.  You should treat this as
     * an up event, but not perform any action that you normally would.
     */
    int ACTION_CANCEL();
    /**
     * Constant for {@link #getActionMasked}: A non-primary pointer has gone down.
     */
    int ACTION_POINTER_DOWN();
    /**
     * Constant for {@link #getActionMasked}: A non-primary pointer has gone up.
     */
    int ACTION_POINTER_UP();

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the masked action being performed, without pointer index information.
     * Use {@link #getActionIndex} to return the index associated with pointer actions.
     * @return The action, such as
     * {@link android.view.MotionEvent#ACTION_DOWN} or
     * {@link android.view.MotionEvent#ACTION_POINTER_DOWN}.
     */
    int getActionMasked();

    /**
     * For {@link android.view.MotionEvent#ACTION_POINTER_DOWN} or
     * {@link android.view.MotionEvent#ACTION_POINTER_UP} as returned by
     * {@link #getActionMasked}, this returns the associated pointer index.
     * The index may be used with {@link #getPointerId(int)},
     * {@link #getX(int)}, {@link #getY(int)}, {@link #getPressure(int)},
     * and {@link #getSize(int)} to get information about the pointer that has
     * gone down or up.
     * @return The index associated with the action.
     */
    int getActionIndex();

    /**
     * {@link #getX(int)} for the first pointer index (may be an
     * arbitrary pointer identifier).
     */
    float getX();

    /**
     * Returns the X coordinate of this event for the given pointer
     * <em>index</em> (use {@link #getPointerId(int)} to find the pointer
     * identifier for this index).
     * Whole numbers are pixels; the
     * value may have a fraction for input devices that are sub-pixel precise.
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     * (the first pointer that is down) to {@link #getPointerCount()}-1.
     */
    float getX(int pointerIndex);

    /**
     * {@link #getY(int)} for the first pointer index (may be an
     * arbitrary pointer identifier).
     */
    float getY();

    /**
     * Returns the Y coordinate of this event for the given pointer
     * <em>index</em> (use {@link #getPointerId(int)} to find the pointer
     * identifier for this index).
     * Whole numbers are pixels; the
     * value may have a fraction for input devices that are sub-pixel precise.
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     * (the first pointer that is down) to {@link #getPointerCount()}-1.
     */
    float getY(int pointerIndex);

    /**
     * The number of pointers of data contained in this event.  Always
     * >= 1.
     */
    int getPointerCount();

    /**
     * Return the pointer identifier associated with a particular pointer
     * data index in this event.  The identifier tells you the actual pointer
     * number associated with the data, accounting for individual pointers
     * going up and down since the start of the current gesture.
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     * (the first pointer that is down) to {@link #getPointerCount()}-1.
     */
    int getPointerId(int pointerIndex);

    /**
     * {@link #getSize(int)} for the first pointer index (may be an
     * arbitrary pointer identifier).
     */
    float getSize();

    /**
     * Returns a scaled value of the approximate size for the given pointer
     * <em>index</em> (use {@link #getPointerId(int)} to find the pointer
     * identifier for this index).
     * This represents some approximation of the area of the screen being
     * pressed; the actual value in pixels corresponding to the
     * touch is normalized with the device specific range of values
     * and scaled to a value between 0 and 1. The value of size can be used to
     * determine fat touch events.
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     * (the first pointer that is down) to {@link #getPointerCount()}-1.
     */
    float getSize(int pointerIndex);

    /**
     * {@link #getPressure(int)} for the first pointer index (may be an
     * arbitrary pointer identifier).
     */
    float getPressure();

    /**
     * Returns the current pressure of this event for the given pointer
     * <em>index</em> (use {@link #getPointerId(int)} to find the pointer
     * identifier for this index).
     * The pressure generally
     * ranges from 0 (no pressure at all) to 1 (normal pressure), however
     * values higher than 1 may be generated depending on the calibration of
     * the input device.
     * @param pointerIndex Raw index of pointer to retrieve.  Value may be from 0
     * (the first pointer that is down) to {@link #getPointerCount()}-1.
     */
    float getPressure(int pointerIndex);
}
