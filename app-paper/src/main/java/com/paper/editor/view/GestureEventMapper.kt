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
import com.cardinalblue.gesture.IAllGesturesListener
import com.cardinalblue.gesture.MyMotionEvent

class GestureEventMapper : SimpleGestureListener() {

    private val mNumbersMap = floatArrayOf(0f, 0f)
    private var mMapper: Mapper? = null

    private var mListener: IAllGesturesListener? = null

    fun setNumberMapper(mapper: Mapper?) {
        mMapper = mapper
    }

    fun map(x: Float, y: Float): Pair<Float, Float> {
        mNumbersMap[0] = x
        mNumbersMap[1] = y
        mMapper?.map(mNumbersMap)
        return Pair(mNumbersMap[0],
                    mNumbersMap[1])
    }

    fun invertMap(x: Float, y: Float): Pair<Float, Float> {
        mNumbersMap[0] = x
        mNumbersMap[1] = y
        mMapper?.invertMap(mNumbersMap)
        return Pair(mNumbersMap[0],
                    mNumbersMap[1])
    }

    fun invertScale(num: Float): Float {
        mNumbersMap[0] = num
        mMapper?.invertMap(mNumbersMap)
        return mNumbersMap[0]
    }

    fun setGestureListener(listener: IAllGesturesListener?) {
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

    // Tap ////////////////////////////////////////////////////////////////////

    override fun onSingleTap(event: MyMotionEvent,
                             target: Any?, context:
                             Any?) {
        mListener?.onSingleTap(normalizeEvent(event), target, context)
    }

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
                               // FIXME: Get the scale from the matrix?
                               velocityX,
                               velocityY)
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
                mMapper?.map(mNumbersMap)
                mNumbersMap[0]
            }),
            downYs = FloatArray(downYs.size, { i ->
                mNumbersMap[0] = downYs[i]
                mMapper?.map(mNumbersMap)
                mNumbersMap[0]
            }),
            isUp = event.isUp,
            upX = event.upX.let { x ->
                mNumbersMap[0] = x
                mMapper?.map(mNumbersMap)
                mNumbersMap[0]
            },
            upY = event.upY.let { y ->
                mNumbersMap[0] = y
                mMapper?.map(mNumbersMap)
                mNumbersMap[0]
            })
    }

    private fun normalizePointer(pt: PointF): PointF {
        mNumbersMap[0] = pt.x
        mNumbersMap[1] = pt.y
        mMapper?.map(mNumbersMap)

        return PointF(mNumbersMap[0],
                      mNumbersMap[1])
    }

    private fun normalizePointers(pts: Array<PointF>): Array<PointF> {
        return Array(pts.size, { i ->
            mNumbersMap[0] = pts[i].x
            mNumbersMap[1] = pts[i].y
            mMapper?.map(mNumbersMap)

            PointF(mNumbersMap[0],
                   mNumbersMap[1])
        })
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    interface Mapper {

        fun map(nums: FloatArray)

        fun invertMap(nums: FloatArray)
    }
}
