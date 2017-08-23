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

import com.cardinalblue.lib.doodle.protocol.IMatrix;

import java.util.Locale;

public class SingleTapEvent extends GestureEvent {

    public final float x;
    public final float y;

    public static SingleTapEvent just(IMatrix parentToTargetMatrix,
                                      float x,
                                      float y) {
        return new SingleTapEvent(parentToTargetMatrix, x, y);
    }

    @Override
    public String toString() {
        return "SingleTapEvent{" +
               String.format(Locale.ENGLISH, "x=%.3f, y=%.3f", x, y) +
               '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private SingleTapEvent(IMatrix parentToTargetMatrix,
                           float x,
                           float y) {
        super(false, false, parentToTargetMatrix);

        this.x = x;
        this.y = y;
    }
}
