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

package com.cardinalblue.lib.doodle.protocol;

import android.os.Parcelable;

import com.cardinalblue.lib.doodle.data.RectF;

import java.util.List;

public interface ISketchModel extends Parcelable {

    long getId();

    void setId(long id);


    int getWidth();

    int getHeight();

    void setSize(int width, int height);


    int getStrokeSize();

    ISketchStroke getStrokeAt(int position);

    void addStroke(ISketchStroke stroke);

    void setStrokes(List<ISketchStroke> strokes);

    List<ISketchStroke> getAllStrokes();

    void clearStrokes();

    /**
     * Get the normalized minimum boundary of strokes in the sketch world.
     * <br/>
     * See {@link #getWidth()} and {@link #getHeight()}.
     */
    RectF getStrokesBoundaryWithinCanvas();


    ISketchModel clone();
}
