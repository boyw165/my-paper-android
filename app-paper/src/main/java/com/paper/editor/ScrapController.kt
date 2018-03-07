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

package com.paper.editor

import android.graphics.Matrix
import android.graphics.PointF
import com.cardinalblue.gesture.MyMotionEvent
import com.paper.editor.view.IScrapView
import com.paper.editor.view.SimpleGestureListener
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

class ScrapController(private val mUiScheduler: Scheduler,
                      private val mWorkerScheduler: Scheduler)
    : IScrapController,
      SimpleGestureListener() {

    // Model.
    private lateinit var mModel: ScrapModel

    // View.
    private var mView: IScrapView? = null

    // Gesture detector.
    private val mPointerMap: FloatArray = floatArrayOf(0f, 0f)
    private val mStartMatrixToParent: Matrix = Matrix()
    private val mStopMatrixToParent: Matrix = Matrix()
    private val mStopTransformToParent: TransformModel = TransformModel(0f, 0f, 1f, 1f, 0f)
    private val mTransformHelper: TransformUtils = TransformUtils()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    override fun loadScrap(model: ScrapModel) {
        mModel = model

        // Could do lazy initialization...
    }

    override fun bindView(view: IScrapView) {
        mView = view
        mView?.setGestureListener(this)
    }

    override fun unbindView() {
        mView?.setGestureListener(null)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gesture handling ///////////////////////////////////////////////////////

    override fun onDragBegin(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        holdStartTransform()
    }

    override fun onDrag(event: MyMotionEvent,
                        target: Any?,
                        context: Any?,
                        startPointer: PointF,
                        stopPointer: PointF) {
        // Map the coordinates from child world to the parent world.
        val startPointerInParent: PointF = convertPointToParentWorld(startPointer)
        val stopPointerInParent: PointF = convertPointToParentWorld(stopPointer)
        val delta = PointF(stopPointerInParent.x - startPointerInParent.x,
                           stopPointerInParent.y - startPointerInParent.y)

        // Update the RAW transform (without any modification).
        mStopMatrixToParent.postTranslate(delta.x, delta.y)

        // Prepare the transform for the view (might be modified).
        mTransformHelper.getValues(mStopMatrixToParent)
        mStopTransformToParent.translationX = mTransformHelper.translationX
        mStopTransformToParent.translationY = mTransformHelper.translationY
        mStopTransformToParent.scaleX = mTransformHelper.scaleX
        mStopTransformToParent.scaleY = mTransformHelper.scaleY
        mStopTransformToParent.rotationInRadians = mTransformHelper.rotationInRadians

        mView!!.setTransform(mStopTransformToParent.copy())
    }

    override fun onDragEnd(event: MyMotionEvent,
                           target: Any?,
                           context: Any?,
                           startPointer: PointF,
                           stopPointer: PointF) {
        // Commit the transform to the model (in memory).
        mModel.x = mStopTransformToParent.translationX
        mModel.y = mStopTransformToParent.translationY
    }

    override fun onPinchBegin(event: MyMotionEvent,
                              target: Any?,
                              context: Any?,
                              startPointers: Array<PointF>) {
        // Get the current transform from the view.
        holdStartTransform()
    }

    override fun onPinch(event: MyMotionEvent,
                         target: Any?,
                         context: Any?,
                         startPointers: Array<PointF>,
                         stopPointers: Array<PointF>) {
        // Map the coordinates from child world to the parent world.
        val startPointersInParent = Array(startPointers.size, { i ->
            convertPointToParentWorld(startPointers[i])
        })
        val stopPointersInParent = Array(stopPointers.size, { i ->
            convertPointToParentWorld(stopPointers[i])
        })

        // Calculate the transformation.
        val transform = TransformUtils.getTransformFromPointers(
            startPointersInParent, stopPointersInParent)

        val dx = transform[TransformUtils.DELTA_X]
        val dy = transform[TransformUtils.DELTA_Y]
        val dScale = transform[TransformUtils.DELTA_SCALE_X]
        val dRadians = transform[TransformUtils.DELTA_RADIANS]
        val pivotX = transform[TransformUtils.PIVOT_X]
        val pivotY = transform[TransformUtils.PIVOT_Y]

        // Update the RAW transform (without any modification).
        mStopMatrixToParent.postScale(dScale, dScale, pivotX, pivotY)
        mStopMatrixToParent.postRotate(Math.toDegrees(dRadians.toDouble()).toFloat(), pivotX, pivotY)
        mStopMatrixToParent.postTranslate(dx, dy)

        // Prepare the transform for the view (might be modified).
        mTransformHelper.getValues(mStopMatrixToParent)
        mStopTransformToParent.translationX = mTransformHelper.translationX
        mStopTransformToParent.translationY = mTransformHelper.translationY
        mStopTransformToParent.scaleX = mTransformHelper.scaleX
        mStopTransformToParent.scaleY = mTransformHelper.scaleY
        mStopTransformToParent.rotationInRadians = mTransformHelper.rotationInRadians

        mView!!.setTransform(mStopTransformToParent.copy())
    }

    override fun onPinchEnd(event: MyMotionEvent,
                            target: Any?,
                            context: Any?,
                            startPointers: Array<PointF>,
                            stopPointers: Array<PointF>) {
        // Commit the transform to the model (in memory).
        mModel.x = mStopTransformToParent.translationX
        mModel.y = mStopTransformToParent.translationY
        mModel.scale = mStopTransformToParent.scaleX
        mModel.rotationInRadians = mStopTransformToParent.rotationInRadians
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun holdStartTransform() {
        mStartMatrixToParent.reset()
        // TODO: Get from model?
        mStartMatrixToParent.set(mView!!.getTransformMatrix())
        mStopMatrixToParent.reset()
        mStopMatrixToParent.set(mStartMatrixToParent)
    }

    private fun convertPointToParentWorld(point: PointF): PointF {
        mPointerMap[0] = point.x
        mPointerMap[1] = point.y
        mView!!.convertPointToParentWorld(mPointerMap)

        return PointF(mPointerMap[0], mPointerMap[1])
    }
}
