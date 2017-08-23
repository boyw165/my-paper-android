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

package com.cardinalblue.lib.doodle.event;

import android.graphics.PointF;

import com.cardinalblue.lib.doodle.protocol.IMatrix;

import java.util.Locale;

public class PinchEvent extends GestureEvent {

    /**
     * Pointer #1 observing from the parent world.
     */
    public final PointF pointer1;
    /**
     * Pointer #2 observing from the parent world.
     */
    public final PointF pointer2;

    public static PinchEvent start(IMatrix parentToTargetMatrix,
                                   float x1, float y1,
                                   float x2, float y2) {
        return new PinchEvent(true, false,
                              parentToTargetMatrix,
                              x1, y1, x2, y2);
    }

    public static PinchEvent doing(IMatrix parentToTargetMatrix,
                                   float x1, float y1,
                                   float x2, float y2) {
        return new PinchEvent(false, true,
                              parentToTargetMatrix,
                              x1, y1, x2, y2);
    }

    public static PinchEvent stop(IMatrix parentToTargetMatrix,
                                  float x1, float y1,
                                  float x2, float y2) {
        return new PinchEvent(false, false,
                              parentToTargetMatrix,
                              x1, y1, x2, y2);
    }

    @Override
    public String toString() {
        return "PinchEvent{" +
               "justStart=" + justStart +
               ", doing=" + doing +
               ", stop=" + !(justStart || doing) +
               ", " + stringOfPointers() +
               '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private PinchEvent(boolean justStart,
                       boolean doing,
                       IMatrix parentToTargetMatrix,
                       float x1, float y1,
                       float x2, float y2) {
        super(justStart, doing, parentToTargetMatrix);

        this.pointer1 = new PointF(x1, y1);
        this.pointer2 = new PointF(x2, y2);
    }

    private String stringOfPointers() {
        return String.format(
            Locale.ENGLISH,
            "pointers=(x=%.3f, y=%.3f), (x=%.3f, y=%.3f)",
            pointer1.x, pointer1.y,
            pointer2.x, pointer2.y);
    }
}
