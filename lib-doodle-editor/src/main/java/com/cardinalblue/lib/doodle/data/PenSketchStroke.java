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

import java.util.ArrayList;

/**
 * See {@link SketchBrushFactory}.
 */
public class PenSketchStroke extends AbsSketchStroke {

    public PenSketchStroke() {
        super();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mColor);
        dest.writeFloat(mWidth);
        dest.writeList(mPathTupleList);
        // Serialize boundary.
        dest.writeFloatArray(new float[]{mBound.left, mBound.top,
                                         mBound.right, mBound.bottom});
    }

    public static final Creator<PenSketchStroke> CREATOR = new Creator<PenSketchStroke>() {
        @Override
        public PenSketchStroke createFromParcel(Parcel source) {
            return new PenSketchStroke(source);
        }

        @Override
        public PenSketchStroke[] newArray(int size) {
            return new PenSketchStroke[size];
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private PenSketchStroke(Parcel in) {
        super();

        mColor = in.readInt();
        mWidth = in.readFloat();
        mPathTupleList = new ArrayList<>();
        in.readList(mPathTupleList, PointPathTuple.class.getClassLoader());

        // Deserialize boundary.
        float[] bound = new float[4];
        in.readFloatArray(bound);
        mBound.set(bound[0], bound[1], bound[2], bound[3]);
    }
}
