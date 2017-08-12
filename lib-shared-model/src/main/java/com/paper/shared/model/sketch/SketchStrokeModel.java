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

package com.paper.shared.model.sketch;

import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

/**
 * The sketch model. A sketch contains stroke(s), {@link SketchStrokeModel}. Each
 * stroke contains tuple(s), {@link PathTuple}. A tuple represents a node of
 * a path segment and contains at least one point, {@link PointF}. These
 * points are endpoints or control-points for describing a bezier curve.
 */
public final class SketchStrokeModel {

    // State.
    private int mColor;
    private float mWidth;
    private boolean mIsEraser;
    private List<PathTuple> mPathTupleList = new ArrayList<>();
    private RectF mBound = new RectF(Float.MAX_VALUE,
                                     Float.MAX_VALUE,
                                     Float.MIN_VALUE,
                                     Float.MIN_VALUE);

    public SketchStrokeModel() {
        // DO NOTHING.
    }

    public void setIsEraser(boolean value) {
        mIsEraser = value;
    }

    public boolean isEraser() {
        return false;
    }

    public SketchStrokeModel setWidth(float width) {
        mWidth = width;
        return this;
    }

    public float getWidth() {
        return mWidth;
    }

    public SketchStrokeModel setColor(int color) {
        mColor = color;
        return this;
    }

    public int getColor() {
        return mColor;
    }

    public boolean savePathTuple(PathTuple tuple) {
        if (tuple == null || tuple.getPointSize() == 0) return false;

        // Calculate the boundary by the last point of the given tuple.
        final PointF point = tuple.getPointAt(tuple.getPointSize() - 1);
        calculateBound(point.x, point.y);

        return mPathTupleList.add(tuple);
    }

    public PathTuple getPathTupleAt(int position) {
        return mPathTupleList.get(position);
    }

    public PathTuple getFirstPathTuple() {
        return mPathTupleList.get(0);
    }

    public PathTuple getLastPathTuple() {
        return mPathTupleList.get(mPathTupleList.size() - 1);
    }

    public int size() {
        return mPathTupleList.size();
    }

    public void add(PathTuple pathTuple) {
        final PointF point = pathTuple.getPointAt(0);

        // Calculate new boundary.
        calculateBound(point.x, point.y);

        mPathTupleList.add(pathTuple);
    }

    public void addAll(List<PathTuple> pathTupleList) {
        // Calculate new boundary.
        for (PathTuple pathTuple : pathTupleList) {
            final PointF point = pathTuple.getPointAt(0);
            calculateBound(point.x, point.y);
        }

        mPathTupleList.addAll(pathTupleList);
    }

    public List<PathTuple> getAllPathTuple() {
        return mPathTupleList;
    }

    public RectF getBound() {
        return mBound;
    }

    public String toString() {
        return "stroke{" +
               ", color=" + mColor +
               ", width=" + mWidth +
               ", pathTupleList=" + mPathTupleList +
               '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void calculateBound(float x, float y) {
        mBound.left = Math.min(mBound.left, x);
        mBound.top = Math.min(mBound.top, y);
        mBound.right = Math.max(mBound.right, x);
        mBound.bottom = Math.max(mBound.bottom, y);
    }
}
