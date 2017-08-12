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

import com.cardinalblue.lib.doodle.protocol.ISketchStroke;

import java.util.Collections;
import java.util.List;

public class DrawStrokeEvent {

    public static final DrawStrokeEvent IDLE = new DrawStrokeEvent(false, false, null, 0, false);

    public final boolean justStart;
    public final boolean drawing;

    public final List<ISketchStroke> strokes;
    public final int from;

    public final boolean isModelChanged;

    /**
     * A start event of drawing the given stroke.
     */
    public static DrawStrokeEvent start(ISketchStroke stroke) {
        return new DrawStrokeEvent(true, false,
                                   Collections.singletonList(stroke), 0,
                                   false);
    }

    /**
     * An on-going event of drawing the given stroke.
     */
    public static DrawStrokeEvent drawing(ISketchStroke stroke,
                                          int from) {
        return new DrawStrokeEvent(false, true,
                                   Collections.singletonList(stroke), from,
                                   false);
    }

    /**
     * An stop event of drawing the given strokes. It's usually for post-process
     * of making strokes sharpen.
     */
    public static DrawStrokeEvent stop(List<ISketchStroke> strokes,
                                       boolean isModelChanged) {
        return new DrawStrokeEvent(false, false, strokes, 0, isModelChanged);
    }

    private DrawStrokeEvent(boolean justStart,
                            boolean drawing,
                            List<ISketchStroke> strokes,
                            int from,
                            boolean isModelChanged) {
        this.justStart = justStart;
        this.drawing = drawing;
        this.strokes = strokes;
        this.from = from;
        this.isModelChanged = isModelChanged;
    }

    @Override
    public String toString() {
        return "DrawStrokeEvent{" +
               "justStart=" + justStart +
               ", drawing=" + drawing +
               ", stop=" + !(justStart && drawing) +
               ", strokes=" + strokes +
               ", from=" + from +
               ", isModelChanged=" + isModelChanged +
               '}';
    }
}
