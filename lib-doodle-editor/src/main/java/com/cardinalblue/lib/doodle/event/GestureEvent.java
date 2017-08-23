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

public class GestureEvent {

    /**
     * A matrix for converting points observing from the parent world to the
     * target world.
     */
    public final IMatrix parentToTargetMatrix;

    public final boolean justStart;
    public final boolean doing;

    public final static GestureEvent START = new GestureEvent(true, false, null);
    public final static GestureEvent DOING = new GestureEvent(false, true, null);
    public final static GestureEvent STOP = new GestureEvent(false, false, null);
    // TODO: Remove it!
    public final static GestureEvent IDLE = new GestureEvent(false, false, null);

    @Override
    public String toString() {
        if (START.equals(this)) {
            return "GestureEvent.START";
        } else if (DOING.equals(this)) {
            return "GestureEvent.DOING";
        } else if (STOP.equals(this)) {
            return "GestureEvent.STOP";
        } else {
            return "GestureEvent{" +
                   "justStart=" + justStart +
                   ", doing=" + doing +
                   ", stop=" + !(justStart || doing) +
                   '}';
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    protected GestureEvent(boolean justStart,
                           boolean doing,
                           IMatrix parentToTargetMatrix) {
        this.parentToTargetMatrix = parentToTargetMatrix;

        this.justStart = justStart;
        this.doing = doing;
    }
}
