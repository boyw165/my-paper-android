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

import android.view.MotionEvent;

import com.cardinalblue.lib.doodle.event.UiTouchEvent;
import com.cardinalblue.lib.doodle.IMotionEvent;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;

/**
 * For mapping from {@link MotionEvent} to {@link UiTouchEvent}.
 * <br/>
 * Note: It's not unit-testable but integration-testable.
 */
public class MotionEvent2TouchEventMapper
    implements ObservableTransformer<MotionEvent, UiTouchEvent> {

    public MotionEvent2TouchEventMapper() {
        // DUMMY CONSTRUCTOR.
    }

    @Override
    public ObservableSource<UiTouchEvent> apply(Observable<MotionEvent> upstream) {
        return upstream.map(new Function<MotionEvent, UiTouchEvent>() {
            @Override
            public UiTouchEvent apply(MotionEvent event)
                throws Exception {
                // TODO: Maybe could use immutable variable to save memory
                // TODO: allocation.
                return new UiTouchEvent(new MotionEventWrap(event));
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private static class MotionEventWrap implements IMotionEvent {

        final MotionEvent source;

        MotionEventWrap(MotionEvent source) {
            this.source = source;
        }

        @Override
        public int ACTION_DOWN() {
            return MotionEvent.ACTION_DOWN;
        }

        @Override
        public int ACTION_UP() {
            return MotionEvent.ACTION_UP;
        }

        @Override
        public int ACTION_MOVE() {
            return MotionEvent.ACTION_MOVE;
        }

        @Override
        public int ACTION_CANCEL() {
            return MotionEvent.ACTION_CANCEL;
        }

        @Override
        public int ACTION_POINTER_DOWN() {
            return MotionEvent.ACTION_POINTER_DOWN;
        }

        @Override
        public int ACTION_POINTER_UP() {
            return MotionEvent.ACTION_POINTER_UP;
        }

        @Override
        public int getActionMasked() {
            return source.getActionMasked();
        }

        @Override
        public int getActionIndex() {
            return source.getActionIndex();
        }

        @Override
        public float getX() {
            return source.getX();
        }

        @Override
        public float getX(int pointerIndex) {
            return source.getX(pointerIndex);
        }

        @Override
        public float getY() {
            return source.getY();
        }

        @Override
        public float getY(int pointerIndex) {
            return source.getY(pointerIndex);
        }

        @Override
        public int getPointerCount() {
            return source.getPointerCount();
        }

        @Override
        public int getPointerId(int pointerIndex) {
            return source.getPointerId(pointerIndex);
        }

        @Override
        public float getSize() {
            return source.getSize();
        }

        @Override
        public float getSize(int pointerIndex) {
            return source.getSize(pointerIndex);
        }

        @Override
        public float getPressure() {
            return source.getPressure();
        }

        @Override
        public float getPressure(int pointerIndex) {
            return source.getPressure(pointerIndex);
        }
    }
}
