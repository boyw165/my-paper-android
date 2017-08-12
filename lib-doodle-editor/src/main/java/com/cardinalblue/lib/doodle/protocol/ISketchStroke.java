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

import android.graphics.RectF;
import android.os.Parcelable;

import java.util.List;

/**
 * A sketch scrap could contains multiple strokes. Every stroke has an array of
 * tuple. PathTuple is the data describing the node in a path segment.
 * <br/>
 * A tuple could contains multiple x-y pairs. The design is for drawing either
 * straight line or Bezier curve.
 * <br/>
 * If it's a single element tuple, the line is straight.
 * <br/>
 * If it's a two elements tuple, the line is a Bezier curve.
 * <br/>
 * If it's a three elements tuple, the line is a Bezier curve with smoother
 * visualization.
 *
 * <pre>
 * A sketch stroke of a sketch scrap.
 * (+) is the tuple.
 * (-) is the straight/bezier line connects two tuple.
 * .-------------------.
 * |                   |
 * | +-+         +--+  |
 * |    \        |     |
 * |    +-+    +-+     |
 * |      |   /        |
 * |      +--+         |
 * |                   |
 * '-------------------'
 * </pre>
 */
// TODO: This interface is redundant since there is already a AbsSketchStroke.
public interface ISketchStroke extends Parcelable {

    boolean isEraser();

    ISketchStroke setWidth(final float width);
    float getWidth();

    ISketchStroke setColor(final int color);
    int getColor();

    boolean savePathTuple(IPathTuple tuple);

    IPathTuple getPathTupleAt(final int position);
    IPathTuple getFirstPathTuple();
    IPathTuple getLastPathTuple();

    int size();

    void add(final IPathTuple pathTuple);
    void addAll(final List<IPathTuple> pathTupleList);
    List<IPathTuple> getAllPathTuple();

    RectF getBound();
}
