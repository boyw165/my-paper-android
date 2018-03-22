// Copyright Mar 2018-present boyw165@gmail.com
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
import com.cardinalblue.gesture.MyMotionEvent

class GestureEventNormalizationHelper(
    private val mConverter: ToCanvasWorldConverter)
    : SimpleGestureListener() {

    private var mXFactor: Float = 1f
    private var mYFactor: Float = 1f

    private var mListener: SimpleGestureListener? = null

    private val mNumbersMap = floatArrayOf(0f, 0f)

    fun setNormalizationFactors(x: Float, y: Float) {
        mXFactor = x
        mYFactor = y
    }

    fun inverseNormalizationToX(x: Float): Float {
        return x / mXFactor
    }

    fun inverseNormalizationToY(y: Float): Float {
        return y / mYFactor
    }

    fun setGestureListener(listener: SimpleGestureListener?) {
        mListener = listener
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gesture Lifecycle //////////////////////////////////////////////////////

    override fun onActionBegin(event: MyMotionEvent,
                               target: Any?,
                               context: Any?) {
        mListener?.onActionBegin(normalizeEvent(event), target, context)
    }

    override fun onActionEnd(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        mListener?.onActionEnd(normalizeEvent(event), target, context)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Drag ///////////////////////////////////////////////////////////////////

    override fun onDragBegin(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        mListener?.onDragBegin(normalizeEvent(event), target, context)
    }

    override fun onDrag(event: MyMotionEvent,
                        target: Any?,
                        context: Any?,
                        startPointer: PointF,
                        stopPointer: PointF) {
        mListener?.onDrag(normalizeEvent(event), target, context,
                          normalizePointer(startPointer),
                          normalizePointer(stopPointer))
    }

    override fun onDragFling(event: MyMotionEvent,
                             target: Any?,
                             context: Any?,
                             startPointer: PointF,
                             stopPointer: PointF,
                             velocityX: Float,
                             velocityY: Float) {
        mListener?.onDragFling(normalizeEvent(event), target, context,
                               normalizePointer(startPointer),
                               normalizePointer(stopPointer),
                               velocityX / mXFactor,
                               velocityY / mYFactor)
    }

    override fun onDragEnd(event: MyMotionEvent,
                           target: Any?,
                           context: Any?,
                           startPointer: PointF,
                           stopPointer: PointF) {
        mListener?.onDragEnd(normalizeEvent(event), target, context,
                             normalizePointer(startPointer),
                             normalizePointer(stopPointer))
    }

    ///////////////////////////////////////////////////////////////////////////
    // Pinch //////////////////////////////////////////////////////////////////

    override fun onPinchBegin(event: MyMotionEvent,
                              target: Any?,
                              context: Any?,
                              startPointers: Array<PointF>) {
        mListener?.onPinchBegin(normalizeEvent(event), target, context,
                                normalizePointers(startPointers))
    }

    override fun onPinch(event: MyMotionEvent,
                         target: Any?,
                         context: Any?,
                         startPointers: Array<PointF>,
                         stopPointers: Array<PointF>) {
        mListener?.onPinch(normalizeEvent(event), target, context,
                           normalizePointers(startPointers),
                           normalizePointers(stopPointers))
    }

    override fun onPinchFling(event: MyMotionEvent,
                              target: Any?,
                              context: Any?) {
        mListener?.onPinchFling(normalizeEvent(event), target, context)
    }

    override fun onPinchEnd(event: MyMotionEvent,
                            target: Any?,
                            context: Any?,
                            startPointers: Array<PointF>,
                            stopPointers: Array<PointF>) {
        mListener?.onPinchEnd(normalizeEvent(event), target, context,
                              normalizePointers(startPointers),
                              normalizePointers(stopPointers))
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun normalizeEvent(event: MyMotionEvent): MyMotionEvent {
        val downXs = event.downXs ?: throw IllegalArgumentException("Null down x array")
        val downYs = event.downYs ?: throw IllegalArgumentException("Null down y array")

        return MyMotionEvent(
            maskedAction = event.maskedAction,
            downXs = FloatArray(downXs.size, { i ->
                mNumbersMap[0] = downXs[i]
                mConverter.mapToCanvasWorld(mNumbersMap)
                mNumbersMap[0] * mXFactor
            }),
            downYs = FloatArray(downYs.size, { i ->
                mNumbersMap[0] = downYs[i]
                mConverter.mapToCanvasWorld(mNumbersMap)
                mNumbersMap[0] * mYFactor
            }),
            isUp = event.isUp,
            upX = event.upX.let { x ->
                mNumbersMap[0] = x
                mConverter.mapToCanvasWorld(mNumbersMap)
                mNumbersMap[0] * mXFactor
            },
            upY = event.upY.let { y ->
                mNumbersMap[0] = y
                mConverter.mapToCanvasWorld(mNumbersMap)
                mNumbersMap[0] * mYFactor
            })
    }

    private fun normalizePointer(pt: PointF): PointF {
        mNumbersMap[0] = pt.x
        mNumbersMap[1] = pt.y
        mConverter.mapToCanvasWorld(mNumbersMap)

        return PointF(mNumbersMap[0] * mXFactor,
                      mNumbersMap[1] * mYFactor)
    }

    private fun normalizePointers(pts: Array<PointF>): Array<PointF> {
        return Array(pts.size, { i ->
            mNumbersMap[0] = pts[i].x
            mNumbersMap[1] = pts[i].y
            mConverter.mapToCanvasWorld(mNumbersMap)

            PointF(mNumbersMap[0] * mXFactor,
                   mNumbersMap[1] * mYFactor)
        })
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    interface ToCanvasWorldConverter {

        fun mapToCanvasWorld(nums: FloatArray)
    }
}
