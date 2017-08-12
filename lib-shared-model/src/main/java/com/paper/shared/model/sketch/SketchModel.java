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

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Collections;
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
public final class SketchModel {

    private final Object mMutex = new Object();

    private long mId;
    private int mWidth, mHeight;
    private List<SketchStrokeModel> mStrokes;
    private boolean mStrokesBoundDirty = true;
    private RectF mStrokesBound = new RectF();

    public SketchModel(int width,
                       int height) {
        this(0, width, height,
             Collections.<SketchStrokeModel>emptyList());
    }

    public SketchModel(long id,
                       int width,
                       int height) {
        this(id, width, height,
             Collections.<SketchStrokeModel>emptyList());
    }

    public SketchModel(long id,
                       int width,
                       int height,
                       List<SketchStrokeModel> strokes) {
        mId = id;
        mWidth = width;
        mHeight = height;

        if (strokes != null) {
            mStrokes = new ArrayList<>(strokes);
        }
    }

    public SketchModel(SketchModel other) {
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

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setSize(int width, int height) {
        synchronized (mMutex) {
            mWidth = width;
            mHeight = height;

            mStrokesBoundDirty = true;
        }
    }

    public int getStrokeSize() {
        synchronized (mMutex) {
            return mStrokes.size();
        }
    }

    public SketchStrokeModel getStrokeAt(int position) {
        synchronized (mMutex) {
            return mStrokes.get(position);
        }
    }

    public void addStroke(SketchStrokeModel stroke) {
        synchronized (mMutex) {
            mStrokes.add(stroke);
            mStrokesBoundDirty = true;
        }
    }

    public List<SketchStrokeModel> getAllStrokes() {
        synchronized (mMutex) {
            return mStrokes;
        }
    }

    public void clearStrokes() {
        synchronized (mMutex) {
            mStrokes.clear();
        }
    }

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
                for (SketchStrokeModel stroke : mStrokes) {
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

    public void setStrokes(List<SketchStrokeModel> strokes) {
        synchronized (mMutex) {
            mStrokes.clear();

            if (strokes != null) {
                mStrokes.addAll(strokes);
            }

            mStrokesBoundDirty = true;
        }
    }

    @Override
    public SketchModel clone() {
        return new SketchModel(this);
    }

    @Override
    public String toString() {
        return "SketchModel{" +
               ", width=" + mWidth +
               ", height=" + mHeight +
               ", strokes=[" + mStrokes + "]" +
               '}';
    }
}
