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

package com.cardinalblue.lib.doodle;

/**
 * The mirror immitation class to {@link android.graphics.Matrix}.
 */
public interface IMatrix {

    /**
     * @return The real matrix. It's usually {@link android.graphics.Matrix}.
     */
    Object getMatrix();

    /**
     * The horizontal location of this canvas relative to its left position.
     * This position is post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @return The horizontal position of this canvas relative to its left
     * position, in pixels.
     */
    float getTranslationX();

    /**
     * The vertical location of this canvas relative to its top position.
     * This position is post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @return The vertical position of this canvas relative to its top position,
     * in pixels.
     */
    float getTranslationY();

    /**
     * The degrees that the canvas is rotated around the pivot point.
     *
     * @return The degrees of rotation.
     */
    float getRotation();

    /**
     * The amount that the canvas is scaled in x around the pivot point, as a
     * proportion of the canvas's unscaled width. A value of 1, the default,
     * means that no scaling is applied.
     * <p>
     * <p>By default, this is 1.0f.
     *
     * @return The scaling factor.
     */
    float getScaleX();

    /**
     * The amount that the canvas is scaled in y around the pivot point, as a
     * proportion of the canvas's unscaled height. A value of 1, the default,
     * means that no scaling is applied.
     * <p>
     * <p>By default, this is 1.0f.
     *
     * @return The scaling factor.
     */
    float getScaleY();

    /**
     * If this matrix can be inverted, return true and if inverse is not null,
     * set inverse to be the inverse of this matrix. If this matrix cannot be
     * inverted, ignore inverse and return false.
     */
    IMatrix getInverse();

    /**
     * Invert the matrix.
     */
    IMatrix invert();

    /**
     * Clone itself and return.
     */
    IMatrix clone();

    /**
     * Set the matrix to identity
     */
    IMatrix reset();

    /**
     * (deep) copy the src matrix into this matrix.
     */
    IMatrix set(IMatrix other);

    /**
     * Postconcats the matrix with the specified matrix.
     * <br/>
     * M' = other * M
     */
    IMatrix postConcat(IMatrix other);

    /**
     * Preconcats the matrix with the specified matrix.
     * <br/>
     * M' = M * other
     */
    IMatrix preConcat(IMatrix other);

    /**
     * Postconcats the matrix with the specified scale.
     * M' = S(sx, sy, px, py) * M
     */
    IMatrix postScale(float sx, float sy, float px, float py);

    /**
     * Postconcats the matrix with the specified rotation.
     * M' = R(degrees, px, py) * M
     */
    IMatrix postRotate(float degrees, float px, float py);

    /**
     * Postconcats the matrix with the specified translation.
     * M' = T(x, y) * M
     */
    IMatrix postTranslate(float dx, float dy);

    /**
     * Apply this matrix to the array of 2D points, and write the transformed
     * points back into the array
     *
     * @param pts The array [x0, y0, x1, y1, ...] of points to transform.
     */
    void mapPoints(float[] pts);

    /**
     * Return the mean radius of a circle after it has been mapped by
     * this matrix. NOTE: in perspective this value assumes the circle
     * has its center at the origin.
     */
    float mapRadius(float radius);

    /**
     * Copy 9 values from the matrix into the array.
     */
    void getValues(float[] values);
}
