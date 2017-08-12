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

import android.util.Log;

import com.cardinalblue.lib.doodle.protocol.ISketchBrush;
import com.cardinalblue.lib.doodle.protocol.ISketchStroke;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The factory for generating the brush with the "shared" stroke paint and
 * "shared" stroke width.
 * <br/>
 * See {@link PenBrush}.
 * <br/>
 * See {@link EraserBrush}.
 */
public class SketchBrushFactory {

    // Config to build the shared stroke.
    private final AtomicReference<Float> mSharedBrushSize = new AtomicReference<>(10f);

    private final List<ISketchBrush> mBrushes = new ArrayList<>();

    public SketchBrushFactory() {
//        mStrokePaint.get().setStrokeCap(Paint.Cap.ROUND);
//        mStrokePaint.get().setAntiAlias(true);
    }

    public SketchBrushFactory setStrokeWidth(final float width) {
        mSharedBrushSize.set(width);

        return this;
    }

    public SketchBrushFactory addEraserBrush() {
        mBrushes.add(new EraserBrush(mSharedBrushSize));

        return this;
    }

    public SketchBrushFactory addColorBrush(final int color) {
        mBrushes.add(new PenBrush(mSharedBrushSize, color));

        return this;
    }

    public List<ISketchBrush> build() {
        if (mBrushes.isEmpty()) {
            throw new IllegalStateException("Should at least add one color");
        }
        return mBrushes;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    /**
     * Normal color pen brush with "shared stroke width".
     */
    private static class PenBrush implements ISketchBrush {

        private final AtomicReference<Float> mSharedStrokeWidth;
        private int mStrokeColor = 0x123456;

        PenBrush(AtomicReference<Float> sharedStrokeWidth,
                 int color) {
            mSharedStrokeWidth = sharedStrokeWidth;
            mStrokeColor = color;
        }

        @Override
        public ISketchStroke newStroke() {
            return new PenSketchStroke()
                .setWidth(getBrushSize())
                .setColor(getBrushColor());
        }

        @Override
        public float getBrushSize() {
            return mSharedStrokeWidth.get();
        }

        @Override
        public ISketchBrush setBrushSize(float size) {
            mSharedStrokeWidth.set(size);
            return this;
        }

        @Override
        public int getBrushColor() {
            return mStrokeColor;
        }

        @Override
        public ISketchBrush setBrushColor(int color) {
            mStrokeColor = color;
            return this;
        }

        @Override
        public boolean isEraser() {
            return false;
        }
    }

    /**
     * Eraser brush with "shared" stroke width.
     */
    private class EraserBrush implements ISketchBrush {

        private final AtomicReference<Float> mSharedStrokeWidth;

        EraserBrush(AtomicReference<Float> sharedStrokeWidth) {
            mSharedStrokeWidth = sharedStrokeWidth;
        }

        @Override
        public ISketchStroke newStroke() {
            Log.d("eraser", "new stroke");
            return new EraserSketchStroke()
                .setWidth(getBrushSize());
        }

        @Override
        public float getBrushSize() {
            return mSharedStrokeWidth.get();
        }

        @Override
        public ISketchBrush setBrushSize(float size) {
            mSharedStrokeWidth.set(size);
            return this;
        }

        @Override
        public int getBrushColor() {
            // Eraser is always transparent.
            return 0;
        }

        @Override
        public ISketchBrush setBrushColor(int color) {
            // Eraser is always transparent.
            return this;
        }

        @Override
        public boolean isEraser() {
            return true;
        }
    }
}
