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

package com.cardinalblue.lib.doodle.data;

public class PointF {

    public float x;
    public float y;
    public float length;

    public PointF() {
        this.x = 0f;
        this.y = 0f;
        this.length = 0f;
    }

    public PointF(float x,
                  float y) {
        this.x = x;
        this.y = y;
        this.length = 0f;
    }

    public PointF(float x,
                  float y,
                  boolean updateLength) {
        this.x = x;
        this.y = y;

        if (updateLength) {
            updateLength();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PointF pointF = (PointF) o;

        if (Float.compare(pointF.x, x) != 0) return false;
        if (Float.compare(pointF.y, y) != 0) return false;
        return Float.compare(pointF.length, length) == 0;

    }

    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        result = 31 * result + (length != +0.0f ? Float.floatToIntBits(length) : 0);
        return result;
    }

    public void set(float x,
                    float y) {
        this.x = x;
        this.y = y;
        this.length = 0f;
    }

    public void set(float x,
                    float y,
                    boolean updateLength) {
        this.x = x;
        this.y = y;

        if (updateLength) {
            updateLength();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void updateLength() {
        this.length = (float) Math.hypot(x, y);
    }
}
