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

import android.graphics.Paint

/**
 *
 */
class SceneBuffer(bufferSize: Int,
                  canvasWidth: Int,
                  canvasHeight: Int,
                  bitmapPaint: Paint,
                  eraserPaint: Paint) {

    // TODO: Use priority-queue
    private val mScenes = Array(bufferSize, { _ ->
        Scene(canvasWidth = canvasWidth,
              canvasHeight = canvasHeight,
              bitmapPaint = bitmapPaint,
              eraserPaint = eraserPaint)
    })
    private var mCurrentScene: Scene = mScenes[0]

    @Synchronized
    fun getCurrentScene(): Scene {
        return mCurrentScene
    }

    @Synchronized
    fun setCurrentScene(scene: Scene) {
        if (-1 == mScenes.indexOf(scene)) throw IllegalArgumentException("Invalid scene")

        mCurrentScene = scene
    }

    @Synchronized
    fun getEmptyScene(): Scene {
        if (mScenes.size <= 1) throw NoSuchElementException("Can't find empty scene")

        return mScenes.first { scene -> scene != mCurrentScene }
    }
}
