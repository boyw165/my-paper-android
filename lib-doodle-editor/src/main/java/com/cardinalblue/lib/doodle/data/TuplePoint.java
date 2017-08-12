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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A path tuple contains one or multiple tuple point. An anchor is a data
 * structure for describing how to render a path or a dot.
 */
public final class TuplePoint implements Parcelable {

    public float x;
    public float y;

    public TuplePoint() {
        this.x = 0;
        this.y = 0;
    }

    public TuplePoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.x);
        dest.writeFloat(this.y);
    }

    @Override
    public String toString() {
        return "(" +
               "x=" + x +
               ", y=" + y +
               ')';
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static final Creator<TuplePoint> CREATOR = new Creator<TuplePoint>() {
        @Override
        public TuplePoint createFromParcel(Parcel source) {
            return new TuplePoint(source);
        }

        @Override
        public TuplePoint[] newArray(int size) {
            return new TuplePoint[size];
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private TuplePoint(Parcel in) {
        this.x = in.readFloat();
        this.y = in.readFloat();
    }
}
