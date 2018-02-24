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
import com.paper.editor.view.ICanvasView
import com.paper.editor.view.IScrapLifecycleListener
import com.paper.editor.view.IScrapView
import com.paper.shared.model.PaperModel
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

class PaperController(private val mUiScheduler: Scheduler,
                      private val mWorkerScheduler: Scheduler)
    : IScrapLifecycleListener {

    // View.
    private var mCanvasView: ICanvasView? = null

    // Model.
    private var mModel: PaperModel? = null

    // Sub controllers.
    private val mControllers: HashMap<Long, IScrapController> = hashMapOf()

    // Gesture detector.
    private val mPointerMap: FloatArray = floatArrayOf(0f, 0f)
    private val mStartMatrixToParent: Matrix = Matrix()
    private val mStopMatrixToParent: Matrix = Matrix()
    private val mStopTransformToParent: TransformModel = TransformModel(0f, 0f, 1f, 1f, 0f)
    private val mTransformHelper: TransformUtils = TransformUtils()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    fun inflateModel(model: PaperModel) {
        mControllers.clear()

        mModel = model
        // Create the scrap controller.
        mModel!!.scraps.forEach { scrap ->
            val controller = SketchController(mUiScheduler,
                                              mWorkerScheduler)

            mControllers[scrap.id] = controller
        }
    }

    fun bindView(view: ICanvasView) {
        val model = mModel ?: throw IllegalStateException("No model.")

        mCanvasView = view

        // TODO: Encapsulate the inflation with a custom Observable.
        mCanvasView?.setScrapLifecycleListener(this@PaperController)
        mCanvasView?.inflateViewBy(model)
    }

    fun unbindView() {
        mDisposablesOnCreate.clear()

        mCanvasView?.setScrapLifecycleListener(null)
        mCanvasView = null
    }

    ///////////////////////////////////////////////////////////////////////////
    // Scrap lifecycle ////////////////////////////////////////////////////////

    override fun onAttachToCanvas(view: IScrapView) {
        val controller = mControllers[view.getScrapId()]

        controller!!.bindView(view)
    }

    override fun onDetachFromCanvas(view: IScrapView) {
        val controller = mControllers[view.getScrapId()]

        controller!!.unbindView()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gesture handling ///////////////////////////////////////////////////////

//    override fun onActionBegin(event: MyMotionEvent,
//                               touchingObject: Any?,
//                               touchingContext: Any?) {
//        mCanvasView!!.setTransformPivot(0f, 0f)
//
//        // TODO: Create the sketch model, view, and controller.
//    }
//
//    override fun onActionEnd(event: MyMotionEvent,
//                             touchingObject: Any?,
//                             touchingContext: Any?) {
//        // TODO: Commit the temporary model to persistent model.
//    }
//
//    override fun onSingleTap(event: MyMotionEvent,
//                             touchingObject: Any?,
//                             touchingContext: Any?) {
//    }
//
//    override fun onDoubleTap(event: MyMotionEvent,
//                             touchingObject: Any?,
//                             touchingContext: Any?) {
//    }
//
//    override fun onLongTap(event: MyMotionEvent,
//                           touchingObject: Any?,
//                           touchingContext: Any?) {
//    }
//
//    override fun onLongPress(event: MyMotionEvent,
//                             touchingObject: Any?,
//                             touchingContext: Any?) {
//    }
//
//    override fun onMoreTap(event: MyMotionEvent,
//                           touchingObject: Any?,
//                           touchingContext: Any?,
//                           tapCount: Int) {
//    }
//
//    override fun onDragBegin(event: MyMotionEvent,
//                             touchingObject: Any?,
//                             touchingContext: Any?) {
////        // Get the current transform from the view.
////        holdStartTransform()
////
////        Log.d("xyz", "drag start: tx=%.3f, ty=%.3f, scaleX=%.3f, scaleY=%.3f, rotation in degrees=%.3f".format(
////            TransformUtils.getTranslationX(mStartMatrixToParent),
////            TransformUtils.getTranslationY(mStartMatrixToParent),
////            TransformUtils.getScaleX(mStartMatrixToParent),
////            TransformUtils.getScaleY(mStartMatrixToParent),
////            TransformUtils.getRotationInDegrees(mStartMatrixToParent)))
//
//        // TODO: Create the temporary model for view to observe.
//    }
//
//    override fun onDrag(event: MyMotionEvent,
//                        touchingObject: Any?,
//                        touchingContext: Any?,
//                        startPointer: PointF,
//                        stopPointer: PointF) {
////        // Map the coordinates from child world to the parent world.
////        val startPointerInParent: PointF = convertPointToParentWorld(startPointer)
////        val stopPointerInParent: PointF = convertPointToParentWorld(stopPointer)
////        val delta = PointF(stopPointerInParent.x - startPointerInParent.x,
////                           stopPointerInParent.y - startPointerInParent.y)
////
////        // Update the RAW transform (without any modification).
////        mStopMatrixToParent.postTranslate(delta.x, delta.y)
////
////        // Prepare the transform for the view (might be modified).
////        mTransformHelper.getValues(mStopMatrixToParent)
////        mStopTransformToParent.translationX = mTransformHelper.translationX
////        mStopTransformToParent.translationY = mTransformHelper.translationY
////        mStopTransformToParent.scaleX = mTransformHelper.scaleX
////        mStopTransformToParent.scaleY = mTransformHelper.scaleY
////        mStopTransformToParent.rotationInRadians = mTransformHelper.rotationInRadians
////
////        Log.d("xyz", "------------------------")
////        Log.d("xyz", "start=%s, stop=%s".format(startPointer, stopPointer))
////        Log.d("xyz", "drag: child(dx=%.3f, dy=%.3f), parent(dx=%.3f, dy=%.3f)".format(
////            stopPointer.x - startPointer.x, stopPointer.y - startPointer.y,
////            delta.x, delta.y))
////        Log.d("xyz", "drag: x=%.3f, y=%.3f".format(
////            mStopTransformToParent.translationX, mStopTransformToParent.translationY))
////
////        mCanvasView!!.setTransform(mStopTransformToParent.copy())
//    }
//
//    override fun onDragFling(event: MyMotionEvent,
//                             touchingObject: Any?,
//                             touchContext: Any?,
//                             startPointerInCanvas: PointF,
//                             stopPointerInCanvas: PointF,
//                             velocityX: Float,
//                             velocityY: Float) {
//    }
//
//    override fun onDragEnd(event: MyMotionEvent,
//                           touchingObject: Any?,
//                           touchingContext: Any?,
//                           startPointerInCanvas: PointF,
//                           stopPointerInCanvas: PointF) {
//    }
//
//    override fun onPinchBegin(event: MyMotionEvent,
//                              touchingObject: Any?,
//                              touchContext: Any?,
//                              startPointers: Array<out PointF>) {
//        // Get the current transform from the view.
//        holdStartTransform()
//
////        Log.d("xyz", "pinch start: tx=%.3f, ty=%.3f, scaleX=%.3f, scaleY=%.3f, rotation in degrees=%.3f".format(
////            TransformUtils.getTranslationX(mStartMatrixToParent),
////            TransformUtils.getTranslationY(mStartMatrixToParent),
////            TransformUtils.getScaleX(mStartMatrixToParent),
////            TransformUtils.getScaleY(mStartMatrixToParent),
////            TransformUtils.getRotationInDegrees(mStartMatrixToParent)))
//    }
//
//    override fun onPinch(event: MyMotionEvent,
//                         touchingObject: Any?,
//                         touchContext: Any?,
//                         startPointers: Array<PointF>,
//                         stopPointers: Array<PointF>) {
//        // Map the coordinates from child world to the parent world.
//        val startPointersInParent = Array(startPointers.size, { i ->
//            convertPointToParentWorld(startPointers[i])
//        })
//        val stopPointersInParent = Array(stopPointers.size, { i ->
//            convertPointToParentWorld(stopPointers[i])
//        })
//
//        // Calculate the transformation.
//        val transform = TransformUtils.getTransformFromPointers(
//            startPointersInParent, stopPointersInParent)
//
//        val dx = transform[TransformUtils.DELTA_X]
//        val dy = transform[TransformUtils.DELTA_Y]
//        val dScale = transform[TransformUtils.DELTA_SCALE_X]
//        val dRadians = transform[TransformUtils.DELTA_RADIANS]
//        val pivotX = transform[TransformUtils.PIVOT_X]
//        val pivotY = transform[TransformUtils.PIVOT_Y]
//
//        // Update the RAW transform (without any modification).
//        mStopMatrixToParent.postScale(dScale, dScale, pivotX, pivotY)
//        mStopMatrixToParent.postRotate(Math.toDegrees(dRadians.toDouble()).toFloat(), pivotX, pivotY)
//        mStopMatrixToParent.postTranslate(dx, dy)
//
//        // Prepare the transform for the view (might be modified).
//        mTransformHelper.getValues(mStopMatrixToParent)
//        mStopTransformToParent.translationX = mTransformHelper.translationX
//        mStopTransformToParent.translationY = mTransformHelper.translationY
//        mStopTransformToParent.scaleX = mTransformHelper.scaleX
//        mStopTransformToParent.scaleY = mTransformHelper.scaleY
//        mStopTransformToParent.rotationInRadians = mTransformHelper.rotationInRadians
//
////        Log.d("xyz", "------------------------")
////        Log.d("xyz", "pinch: dx=%.3f, dy=%.3f; dScale=%.3f, delta degrees=%.3f".format(
////            dx, dy, dScale, Math.toDegrees(dRadians.toDouble())))
////        Log.d("xyz", "pinch: x=%.3f, y=%.3f; scale=%.3f, degrees=%.3f".format(
////            mStopTransformToParent.translationX,
////            mStopTransformToParent.translationY,
////            mStopTransformToParent.scaleX,
////            Math.toDegrees(mStopTransformToParent.rotationInRadians.toDouble())))
//
//        mCanvasView!!.setTransform(mStopTransformToParent.copy())
//    }
//
//    override fun onPinchFling(event: MyMotionEvent,
//                              touchingObject: Any?,
//                              touchContext: Any?) {
//    }
//
//    override fun onPinchEnd(event: MyMotionEvent,
//                            touchingObject: Any?,
//                            touchContext: Any?,
//                            startPointersInCanvas: Array<out PointF>,
//                            stopPointersInCanvas: Array<out PointF>) {
//    }

    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun holdStartTransform() {
        mStartMatrixToParent.reset()
        mStartMatrixToParent.set(mCanvasView!!.getTransformMatrix())
        mStopMatrixToParent.reset()
        mStopMatrixToParent.set(mStartMatrixToParent)
    }

    private fun convertPointToParentWorld(point: PointF): PointF {
        mPointerMap[0] = point.x
        mPointerMap[1] = point.y
        mCanvasView!!.convertPointToParentWorld(mPointerMap)

        return PointF(mPointerMap[0], mPointerMap[1])
    }
}
