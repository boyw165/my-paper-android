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

import android.graphics.PointF
import com.cardinalblue.gesture.IAllGesturesListener
import com.cardinalblue.gesture.MyMotionEvent

open class SimpleGestureListener : IAllGesturesListener {

    override fun onActionBegin(event: MyMotionEvent,
                               target: Any?,
                               context: Any?) {
        // DO NOTHING.
    }

    override fun onActionEnd(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        // DO NOTHING.
    }

    override fun onSingleTap(event: MyMotionEvent, target: Any?, context: Any?) {
        // DO NOTHING.
    }

    override fun onDoubleTap(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        // DO NOTHING.
    }

    override fun onMoreTap(event: MyMotionEvent, target: Any?, context: Any?, tapCount: Int) {
        // DO NOTHING.
    }

    override fun onLongPress(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        // DO NOTHING.
    }

    override fun onLongTap(event: MyMotionEvent, target: Any?, context: Any?) {
        // DO NOTHING.
    }

    override fun onDragBegin(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        // DO NOTHING.
    }

    override fun onDrag(event: MyMotionEvent,
                        target: Any?,
                        context: Any?,
                        startPointer: PointF,
                        stopPointer: PointF) {
        // DO NOTHING.
    }

    override fun onDragFling(event: MyMotionEvent, target: Any?, context: Any?, startPointer: PointF, stopPointer: PointF, velocityX: Float, velocityY: Float) {
        // DO NOTHING.
    }

    override fun onDragEnd(event: MyMotionEvent, target: Any?, context: Any?, startPointer: PointF, stopPointer: PointF) {
        // DO NOTHING.
    }

    override fun onPinchBegin(event: MyMotionEvent, target: Any?, context: Any?, startPointers: Array<PointF>) {
        // DO NOTHING.
    }

    override fun onPinch(event: MyMotionEvent,
                         target: Any?,
                         context: Any?,
                         startPointers: Array<PointF>,
                         stopPointers: Array<PointF>) {
        // DO NOTHING.
    }

    override fun onPinchFling(event: MyMotionEvent, target: Any?, context: Any?) {
        // DO NOTHING.
    }

    override fun onPinchEnd(event: MyMotionEvent, target: Any?, context: Any?, startPointers: Array<PointF>, stopPointers: Array<PointF>) {
        // DO NOTHING.
    }
}
