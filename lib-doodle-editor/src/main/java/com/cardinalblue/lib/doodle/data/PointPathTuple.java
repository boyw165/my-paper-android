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
import android.os.Parcel;

import com.cardinalblue.lib.doodle.protocol.IPathTuple;

import java.util.ArrayList;
import java.util.List;

/**
 * A stroke contains one or multiple path tuple. A tuple is like the "node"
 * for rendering a path or a dot.
 */
public class PointPathTuple implements IPathTuple {

    private List<PointF> mPoints = new ArrayList<>();

    public PointPathTuple() {
        // EMPTY.
    }

    public PointPathTuple(final float x,
                          final float y) {
        addPoint(x, y);
    }

    public PointPathTuple(List<PointF> points) {
        mPoints.clear();
        mPoints.addAll(points);
    }

    @Override
    public void addPoint(float x, float y) {
        mPoints.add(new PointF(x, y));
    }

    @Override
    public PointF getPointAt(int position) {
        return mPoints.get(position);
    }

    @Override
    public PointF getLastPoint() {
        return mPoints.get(getPointSize() - 1);
    }

    @Override
    public int getPointSize() {
        return mPoints.size();
    }

    @Override
    public List<PointF> getAllPoints() {
        return mPoints;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(mPoints);
    }

    @Override
    public String toString() {
        return "PointPathTuple[" +
               mPoints +
               ']';
    }

    public static final Creator<PointPathTuple> CREATOR = new Creator<PointPathTuple>() {
        @Override
        public PointPathTuple createFromParcel(Parcel source) {
            return new PointPathTuple(source);
        }

        @Override
        public PointPathTuple[] newArray(int size) {
            return new PointPathTuple[size];
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private PointPathTuple(Parcel in) {
        in.readList(mPoints, PointF.class.getClassLoader());
    }
}
