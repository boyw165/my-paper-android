// Copyright Feb 2018-present boyw165@gmail.com
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

package com.paper.editor.view

import android.graphics.Matrix
import com.cardinalblue.gesture.GestureDetector
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel

interface ICanvasView : IScrapLifecycleListener {

    fun addViewBy(scrap: ScrapModel)

    fun removeViewBy(id: Long)

    fun removeAllViews()

    fun setScrapLifecycleListener(listener: IScrapLifecycleListener?)

    fun getTransform(): TransformModel

    fun getTransformMatrix(): Matrix

    fun getGestureDetector(): GestureDetector

    fun setTransform(transform: TransformModel)

    fun setTransformPivot(px: Float, py: Float)

    fun convertPointToParentWorld(point: FloatArray)
}
