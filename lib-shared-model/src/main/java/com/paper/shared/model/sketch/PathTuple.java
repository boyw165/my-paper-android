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

import java.util.ArrayList;
import java.util.List;

/**
 * A path tuple represents a path node. It may contains more than one x-y
 * pair in order to draw Bezier curve. A x-y pair is called TuplePoint.
 */
public final class PathTuple {

    private List<PointF> mPoints = new ArrayList<>();

    public PathTuple() {
        // EMPTY.
    }

    public PathTuple(final float x,
                     final float y) {
        addPoint(x, y);
    }

    public PathTuple(List<PointF> points) {
        mPoints.clear();
        mPoints.addAll(points);
    }

    public void addPoint(float x, float y) {
        mPoints.add(new PointF(x, y));
    }

    public PointF getPointAt(int position) {
        return mPoints.get(position);
    }

    public PointF getLastPoint() {
        return mPoints.get(getPointSize() - 1);
    }

    public int getPointSize() {
        return mPoints.size();
    }

    public List<PointF> getAllPoints() {
        return mPoints;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "PathTuple[" +
               mPoints +
               ']';
    }
}
