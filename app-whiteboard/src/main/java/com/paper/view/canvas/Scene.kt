// Copyright May 2018-present Paper
//
// Author: boyw165@gmail.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.view.canvas

import android.graphics.*
import com.paper.view.with

class Scene internal constructor(canvasWidth: Int,
                                 canvasHeight: Int,
                                 bitmapPaint: Paint,
                                 eraserPaint: Paint) {

    private val mMatrix = Matrix()
    private val mMatrixTmp = Matrix()
    private val mBitmap by lazy {
        Bitmap.createBitmap(canvasWidth,
                            canvasHeight,
                            Bitmap.Config.ARGB_8888)
    }
    private val mBitmapPaint = bitmapPaint
    private val mEraserPaint = eraserPaint
    private val mCanvas by lazy { Canvas(mBitmap) }

    private val width: Int
        get() = mBitmap.width
    private val height: Int
        get() = mBitmap.height
    private val cutoutBound = RectF(1f, 1f, width - 1f, height - 1f)

    fun resetTransform() {
        mMatrix.reset()
        mMatrixTmp.reset()
    }

    /**
     * A temporary transform that would be concatenate with [mMatrix] and applied
     * in the [print] drawing.
     */
    fun setNewTransform(m: Matrix) {
        mMatrixTmp.set(m)
    }

    /**
     * Permanently concatenate the temporary transform with [mMatrix].
     */
    fun commitNewTransform() {
        mMatrix.postConcat(mMatrixTmp)
        mMatrixTmp.reset()
    }

    /**
     * Draw in an isolated session without leaving any permanent change to the
     * canvas.
     */
    fun draw(lambda: (canvas: Canvas) -> Unit) {
        val count = mCanvas.save()

        lambda(mCanvas)

        mCanvas.restoreToCount(count)
    }

    /**
     * Erase the canvas first and then draw, see [draw]
     */
    fun eraseDraw(lambda: (canvas: Canvas) -> Unit) {
        // Erase the Bitmap
        mBitmap.eraseColor(Color.TRANSPARENT)

        draw(lambda)
    }

    /**
     * Print the content of this scene to the given canvas.
     */
    fun print(targetCanvas: Canvas) {
        targetCanvas.with { c ->
            c.clipRect(0f, 0f, width.toFloat(), height.toFloat())
            c.concat(mMatrixTmp)
            c.concat(mMatrix)

            // Cut a space for anti-aliasing drawing.
            c.drawRect(cutoutBound, mEraserPaint)

            c.drawBitmap(mBitmap, 0f, 0f, mBitmapPaint)
        }
    }
}
