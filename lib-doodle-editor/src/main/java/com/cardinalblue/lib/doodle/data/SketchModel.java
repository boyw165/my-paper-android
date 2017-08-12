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
import android.os.Parcel;

import com.cardinalblue.lib.doodle.protocol.IPathTuple;
import com.cardinalblue.lib.doodle.protocol.ISketchModel;
import com.cardinalblue.lib.doodle.protocol.ISketchStroke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The sketch model. A sketch contains stroke(s), {@link ISketchStroke}. Each
 * stroke contains tuple(s), {@link IPathTuple}. A tuple represents a node of
 * a path segment and contains at least one point, {@link PointF}. These
 * points are endpoints or control-points for describing a bezier curve.
 */
public class SketchModel implements ISketchModel {

    private final Object mMutex = new Object();

    private long mId;
    private int mWidth, mHeight;
    private List<ISketchStroke> mStrokes;
    private boolean mStrokesBoundDirty = true;
    private RectF mStrokesBound = new RectF();

    public SketchModel(int width,
                       int height) {
        this(0,
             width, height,
             Collections.<ISketchStroke>emptyList());
    }

    public SketchModel(long id,
                       int width,
                       int height) {
        this(id, width, height, Collections.<ISketchStroke>emptyList());
    }

    public SketchModel(long id,
                       int width,
                       int height,
                       List<ISketchStroke> strokes) {
        mId = id;
        mWidth = width;
        mHeight = height;

        if (strokes != null) {
            mStrokes = new ArrayList<>(strokes);
        }
    }

    public SketchModel(ISketchModel other) {
        if (other == null) {
            mId = 0;
            mWidth = 1440;
            mHeight = 1440;
            mStrokes = new ArrayList<>();
            mStrokesBoundDirty = true;
        } else {
            mId = other.getId();
            mWidth = other.getWidth();
            mHeight = other.getHeight();
            mStrokes = new ArrayList<>(other.getAllStrokes());
            mStrokesBound = new RectF(other.getStrokesBoundaryWithinCanvas());
            mStrokesBoundDirty = false;
        }
    }

    @Override
    public long getId() {
        return mId;
    }

    @Override
    public void setId(long id) {
        mId = id;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public void setSize(int width, int height) {
        synchronized (mMutex) {
            mWidth = width;
            mHeight = height;

            mStrokesBoundDirty = true;
        }
    }

    @Override
    public int getStrokeSize() {
        synchronized (mMutex) {
            return mStrokes.size();
        }
    }

    @Override
    public ISketchStroke getStrokeAt(int position) {
        synchronized (mMutex) {
            return mStrokes.get(position);
        }
    }

    @Override
    public void addStroke(ISketchStroke stroke) {
        synchronized (mMutex) {
            mStrokes.add(stroke);
            mStrokesBoundDirty = true;
        }
    }

    @Override
    public List<ISketchStroke> getAllStrokes() {
        synchronized (mMutex) {
            return mStrokes;
        }
    }

    @Override
    public void clearStrokes() {
        synchronized (mMutex) {
            mStrokes.clear();
        }
    }

    @Override
    public RectF getStrokesBoundaryWithinCanvas() {
        if (mStrokesBoundDirty) {
            synchronized (mMutex) {
                mStrokesBound.set(
                    Float.MAX_VALUE,
                    Float.MAX_VALUE,
                    Float.MIN_VALUE,
                    Float.MIN_VALUE
                );

                final float aspectRatio = (float) getWidth() / getHeight();
                for (ISketchStroke stroke : mStrokes) {
                    final RectF strokeBound = stroke.getBound();
                    // Also consider the stroke width.
                    final float halfStrokeWidth = stroke.getWidth() / 2;

                    mStrokesBound.left = Math.min(mStrokesBound.left, strokeBound.left - halfStrokeWidth);
                    mStrokesBound.top = Math.min(mStrokesBound.top, strokeBound.top - halfStrokeWidth * aspectRatio);
                    mStrokesBound.right = Math.max(mStrokesBound.right, strokeBound.right + halfStrokeWidth);
                    mStrokesBound.bottom = Math.max(mStrokesBound.bottom, strokeBound.bottom + halfStrokeWidth * aspectRatio);
                }

                // Constraint the boundary inside the canvas.
                mStrokesBound.left = Math.max(mStrokesBound.left, 0f);
                mStrokesBound.top = Math.max(mStrokesBound.top, 0f);
                mStrokesBound.right = Math.min(mStrokesBound.right, 1f);
                mStrokesBound.bottom = Math.min(mStrokesBound.bottom, 1f);

                mStrokesBoundDirty = false;
            }
        }

        return mStrokesBound;
    }

    @Override
    public void setStrokes(List<ISketchStroke> strokes) {
        synchronized (mMutex) {
            mStrokes.clear();

            if (strokes != null) {
                mStrokes.addAll(strokes);
            }

            mStrokesBoundDirty = true;
        }
    }

    @Override
    public ISketchModel clone() {
        return new SketchModel(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeList(mStrokes);
        dest.writeFloatArray(new float[]{mStrokesBound.left,
                                         mStrokesBound.top,
                                         mStrokesBound.right,
                                         mStrokesBound.bottom});
    }

    @Override
    public String toString() {
        return "SketchModel{" +
               ", width=" + mWidth +
               ", height=" + mHeight +
               ", strokes=[" + mStrokes + "]" +
               '}';
    }

    public static final Creator<SketchModel> CREATOR = new Creator<SketchModel>() {
        @Override
        public SketchModel createFromParcel(Parcel source) {
            return new SketchModel(source);
        }

        @Override
        public SketchModel[] newArray(int size) {
            return new SketchModel[size];
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    protected SketchModel(Parcel in) {
        mId = in.readLong();
        mWidth = in.readInt();
        mHeight = in.readInt();

        mStrokes = new ArrayList<>();
        in.readList(mStrokes, ISketchStroke.class.getClassLoader());

        final float[] bound = new float[4];
        in.readFloatArray(bound);
        mStrokesBound.set(bound[0], bound[1], bound[2], bound[3]);

        mStrokesBoundDirty = true;
    }
}
