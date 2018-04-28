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

package com.paper.view.canvas

import android.graphics.Canvas
import android.graphics.Matrix
import android.view.MotionEvent
import com.cardinalblue.gesture.IAllGesturesListener
import com.paper.domain.widget.editor.IScrapWidget
import java.util.*

interface IScrapWidgetView {

    fun bindWidget(widget: IScrapWidget)

    fun unbindWidget()

    fun addChild(child: IScrapWidgetView)

    fun removeChild(child: IScrapWidgetView)

    // Gesture ////////////////////////////////////////////////////////////////

    fun setGestureListener(listener: IAllGesturesListener?)

    // Rendering //////////////////////////////////////////////////////////////

    // TODO: Do the root canvas care about whether there is any children view
    // TODO: is dirty?

    // TODO: How to pass the current transform in the recursion call?

    fun dispatchDraw(canvas: Canvas,
                     previousXforms: Stack<Matrix>,
                     ifSharpenDrawing: Boolean)

    // Touch //////////////////////////////////////////////////////////////////

    fun dispatchTouch(event: MotionEvent)
}
