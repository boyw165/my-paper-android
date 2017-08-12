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

package com.my.demo.doodle.data;

import com.my.demo.doodle.protocol.ISketchBrush;
import com.my.demo.doodle.protocol.ISketchStroke;

import java.util.ArrayList;
import java.util.List;

public class ColorPenBrushFactory {

    // Config to build the shared stroke.
    private float mStrokeWidth = 10f;

    private final List<Integer> mColors = new ArrayList<>();

    public ColorPenBrushFactory setStrokeWidth(final float width) {
        mStrokeWidth = width;

        return this;
    }

    public ColorPenBrushFactory addColor(final int color) {
        mColors.add(color);

        return this;
    }

    public List<ISketchBrush> build() {
        if (mColors.isEmpty()) {
            throw new IllegalStateException("Should at least add one color");
        }

        final List<ISketchBrush> brushes = new ArrayList<>();

        // Construct the brushes.
        for (Integer color : mColors) {
            brushes.add(new PenBrush()
                .setStrokeWidth(mStrokeWidth)
                .setStrokeColor(color));
        }

        return brushes;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private static class PenBrush implements ISketchBrush {

        private static float sStrokeWidth = 10f;
        private int mStrokeColor = 0x123456;

        @Override
        public ISketchStroke newStroke() {
            return new PenSketchStroke()
                .setWidth(getStrokeWidth())
                .setColor(getStrokeColor());
        }

        @Override
        public float getStrokeWidth() {
            return sStrokeWidth;
        }

        @Override
        public ISketchBrush setStrokeWidth(float width) {
            sStrokeWidth = width;
            return this;
        }

        @Override
        public int getStrokeColor() {
            return mStrokeColor;
        }

        @Override
        public ISketchBrush setStrokeColor(int color) {
            mStrokeColor = color;
            return this;
        }
    }
}
