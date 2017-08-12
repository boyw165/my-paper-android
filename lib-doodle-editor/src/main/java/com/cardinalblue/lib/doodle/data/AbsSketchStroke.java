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

package com.cardinalblue.lib.doodle.data;

import android.graphics.PointF;
import android.graphics.RectF;

import com.cardinalblue.lib.doodle.protocol.IPathTuple;
import com.cardinalblue.lib.doodle.protocol.ISketchStroke;

import java.util.ArrayList;
import java.util.List;

/**
 * See {@link SketchBrushFactory}.
 */
abstract class AbsSketchStroke implements ISketchStroke {

    // State.
    protected int mColor;
    protected float mWidth;
    protected List<IPathTuple> mPathTupleList = new ArrayList<>();
    protected RectF mBound = new RectF(Float.MAX_VALUE,
                                       Float.MAX_VALUE,
                                       Float.MIN_VALUE,
                                       Float.MIN_VALUE);

    AbsSketchStroke() {}

    @Override
    public boolean isEraser() {
        return false;
    }

    @Override
    public ISketchStroke setWidth(float width) {
        mWidth = width;
        return this;
    }

    @Override
    public float getWidth() {
        return mWidth;
    }

    @Override
    public ISketchStroke setColor(int color) {
        mColor = color;
        return this;
    }

    @Override
    public int getColor() {
        return mColor;
    }

    @Override
    public boolean savePathTuple(IPathTuple tuple) {
        if (tuple == null || tuple.getPointSize() == 0) return false;

        // Calculate the boundary by the last point of the given tuple.
        final PointF point = tuple.getPointAt(tuple.getPointSize() - 1);
        calculateBound(point.x, point.y);

        return mPathTupleList.add(tuple);
    }

    @Override
    public IPathTuple getPathTupleAt(int position) {
        return mPathTupleList.get(position);
    }

    @Override
    public IPathTuple getFirstPathTuple() {
        return mPathTupleList.get(0);
    }

    @Override
    public IPathTuple getLastPathTuple() {
        return mPathTupleList.get(mPathTupleList.size() - 1);
    }

    @Override
    public int size() {
        return mPathTupleList.size();
    }

    @Override
    public void add(IPathTuple pathTuple) {
        final PointF point = pathTuple.getPointAt(0);

        // Calculate new boundary.
        calculateBound(point.x, point.y);

        mPathTupleList.add(pathTuple);
    }

    @Override
    public void addAll(List<IPathTuple> pathTupleList) {
        // Calculate new boundary.
        for (IPathTuple pathTuple : pathTupleList) {
            final PointF point = pathTuple.getPointAt(0);
            calculateBound(point.x, point.y);
        }

        mPathTupleList.addAll(pathTupleList);
    }

    @Override
    public List<IPathTuple> getAllPathTuple() {
        return mPathTupleList;
    }

    @Override
    public RectF getBound() {
        return mBound;
    }

    @Override
    public String toString() {
        return "stroke{" +
               ", color=" + mColor +
               ", width=" + mWidth +
               ", pathTupleList=" + mPathTupleList +
               '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    protected void calculateBound(float x, float y) {
        mBound.left = Math.min(mBound.left, x);
        mBound.top = Math.min(mBound.top, y);
        mBound.right = Math.max(mBound.right, x);
        mBound.bottom = Math.max(mBound.bottom, y);
    }
}
