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

package com.cardinalblue.lib.doodle.view;

import android.animation.TypeEvaluator;
import android.graphics.Matrix;

import com.cardinalblue.lib.doodle.IMatrix;

import java.util.Locale;

/**
 * The android implementation for {@link IMatrix}.
 */
public class AndroidMatrix implements IMatrix {

    public final Matrix matrix = new Matrix();

    private final float[] mValues = new float[9];

    public AndroidMatrix() {
        // EMPTY.
    }

    public AndroidMatrix(Matrix otherMatrix) {
        this.matrix.set(otherMatrix);
    }

    @Override
    public Object getMatrix() {
        return matrix;
    }

    @Override
    public float getTranslationX() {
        matrix.getValues(mValues);

        return mValues[Matrix.MTRANS_X];
    }

    @Override
    public float getTranslationY() {
        matrix.getValues(mValues);

        return mValues[Matrix.MTRANS_Y];
    }

    @Override
    public float getRotation() {
        matrix.getValues(mValues);

        // TODO: Has to take the negative scale into account.
        // [a, b, tx]   [ sx*cos  -sy*sin  ? ]
        // [c, d, ty] = [ sx*sin   sy*cos  ? ]
        // [0, 0,  1]   [    0        0    1 ]
        //  ^  ^   ^
        //  i  j   k hat (axis vector)
        final float a = mValues[Matrix.MSCALE_X];
        final float c = mValues[Matrix.MSKEW_Y];
        // From -pi to +pi.
        float radian = (float) Math.atan2(c, a);

        return (float) Math.toDegrees(radian);
    }

    @Override
    public float getScaleX() {
        matrix.getValues(mValues);

        // TODO: Has to take the negative scale into account.
        // [a, b, tx]   [ sx*cos  -sy*sin  ? ]
        // [c, d, ty] = [ sx*sin   sy*cos  ? ]
        // [0, 0,  1]   [    0        0    1 ]
        //  ^  ^   ^
        //  i  j   k hat (axis vector)
        final float a = mValues[Matrix.MSCALE_X];
        final float b = mValues[Matrix.MSKEW_X];

        return (float) Math.hypot(a, b);
    }

    @Override
    public float getScaleY() {
        matrix.getValues(mValues);

        // TODO: Has to take the negative scale into account.
        // [a, b, tx]   [ sx*cos  -sy*sin  ? ]
        // [c, d, ty] = [ sy*sin   sy*cos  ? ]
        // [0, 0,  1]   [    0        0    1 ]
        //  ^  ^   ^
        //  i  j   k hat (axis vector)
        final float c = mValues[Matrix.MSKEW_Y];
        final float d = mValues[Matrix.MSCALE_Y];

        return (float) Math.hypot(c, d);
    }

    @Override
    public IMatrix getInverse() {
        final Matrix inverse = new Matrix();

        if (matrix.invert(inverse)) {
            return new AndroidMatrix(inverse);
        } else {
            return this;
        }
    }

    @Override
    public IMatrix invert() {
        final Matrix inverse = new Matrix();

        if (matrix.invert(inverse)) {
            matrix.set(inverse);
        }

        return this;
    }

    @Override
    public IMatrix clone() {
        return new AndroidMatrix(matrix);
    }

    @Override
    public IMatrix reset() {
        matrix.reset();

        return this;
    }

    @Override
    public IMatrix set(IMatrix other) {
        matrix.set((Matrix) other.getMatrix());

        return this;
    }

    @Override
    public IMatrix postConcat(IMatrix other) {
        matrix.postConcat((Matrix) other.getMatrix());

        return this;
    }

    @Override
    public IMatrix preConcat(IMatrix other) {
        matrix.preConcat((Matrix) other.getMatrix());

        return this;
    }

    @Override
    public IMatrix postScale(float sx, float sy, float px, float py) {
        matrix.postScale(sx, sy, px, py);

        return this;
    }

    @Override
    public IMatrix postRotate(float degrees, float px, float py) {
        matrix.postRotate(degrees, px, py);

        return this;
    }

    @Override
    public IMatrix postTranslate(float dx, float dy) {
        matrix.postTranslate(dx, dy);

        return this;
    }

    @Override
    public void mapPoints(float[] pts) {
        matrix.mapPoints(pts);
    }

    @Override
    public float mapRadius(float radius) {
        return matrix.mapRadius(radius);
    }

    @Override
    public void getValues(float[] values) {
        matrix.getValues(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AndroidMatrix that = (AndroidMatrix) o;

        return matrix != null ? matrix.equals(that.matrix) : that.matrix == null;

    }

    @Override
    public int hashCode() {
        int result = matrix != null ? matrix.hashCode() : 0;
        return result;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                             "AndroidMatrix{sx=%.3f, sy=%.3f, tx=%.3f, ty=%.3f, d=%.3f}",
                             getScaleX(), getScaleY(), getTranslationX(), getTranslationY(),
                             getRotation());
    }

    /**
     * For {@link android.animation.Animator} to know how to tween this matrix
     * object.
     */
    public static TypeEvaluator<IMatrix> getTypeEvaluator() {
        return new TypeEvaluator<IMatrix>() {
            @Override
            public IMatrix evaluate(float fraction,
                                    IMatrix startValue,
                                    IMatrix endValue) {
                final float sx = startValue.getScaleX() +
                                 fraction * (endValue.getScaleX() - startValue.getScaleX());
                final float sy = startValue.getScaleY() +
                                 fraction * (endValue.getScaleY() - startValue.getScaleY());
                final float deltaDegrees = endValue.getRotation() - startValue.getRotation();
                final float degrees = startValue.getRotation() +
                                      fraction * deltaDegrees;

                final float tx = startValue.getTranslationX() +
                                 fraction * (endValue.getTranslationX() - startValue.getTranslationX());
                final float ty = startValue.getTranslationY() +
                                 fraction * (endValue.getTranslationY() - startValue.getTranslationY());

//                Log.d("matrix", String.format("f=%.3f, sx=%.3f, sy=%.3f, d=%.3f, tx=%.3f, ty=%.3f",
//                                              fraction, sx, sy, degrees, tx, ty));

                final AndroidMatrix transform = new AndroidMatrix();
                transform.postRotate(degrees, 0, 0);
                transform.postScale(sx, sy, 0, 0);
                transform.postTranslate(tx, ty);

                return transform;
            }
        };
    }
}
