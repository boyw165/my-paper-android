package com.paper.editor

import android.graphics.Matrix
import android.graphics.PointF
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.IGestureListener
import com.cardinalblue.gesture.MyMotionEvent
import com.paper.protocol.IContextProvider
import com.paper.protocol.IPresenter
import com.paper.shared.model.TransformModel
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

class PaperController(contextProvider: IContextProvider,
                      config: PaperCanvasContract.Config,
                      uiScheduler: Scheduler,
                      workerScheduler: Scheduler)
    : IPresenter<PaperCanvasContract.BaseView>,
      IGestureListener {

    // Given.
    private val mContextProvider: IContextProvider = contextProvider
    private val mConfig: PaperCanvasContract.Config = config
    private val mUiScheduler: Scheduler = uiScheduler
    private val mWorkerScheduler: Scheduler = workerScheduler
    private var mView: PaperCanvasContract.BaseView? = null

    // Gesture detector.
    private val mPointerMap: FloatArray = floatArrayOf(0f, 0f)
    private val mStartMatrixToParent: Matrix = Matrix()
    private val mStopMatrixToParent: Matrix = Matrix()
    private val mStopTransformToParent: TransformModel = TransformModel(0f, 0f, 1f, 1f, 0f)
    private val mGestureDetector: GestureDetector by lazy {
        GestureDetector(mContextProvider.getContext(),
                        this,
                        mConfig.getTouchSlop(),
                        mConfig.getTapSlop(),
                        mConfig.getMinFlingVec(),
                        mConfig.getMaxFlingVec())
    }
    private val mTransformHelper: TwoDTransformUtils = TwoDTransformUtils()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    override fun bindViewOnCreate(view: PaperCanvasContract.BaseView) {
        mView = view

        // Set the gesture detector.
        mView!!.setGestureDetector(mGestureDetector)

        //        mDisposablesOnCreate.add()
    }

    override fun unBindViewOnDestroy() {
        mDisposablesOnCreate.clear()

        // Unset the gesture detector.
        mView!!.setGestureDetector(null)

        mView = null
    }

    override fun onResume() {
        //        mDisposablesOnResume.add()
    }

    override fun onPause() {
        mDisposablesOnResume.clear()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gesture handling ///////////////////////////////////////////////////////

    override fun onActionBegin(event: MyMotionEvent,
                               touchingObject: Any?,
                               touchingContext: Any?) {
        mView!!.setTransformPivot(0f, 0f)
    }

    override fun onActionEnd(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
        // TODO: Commit the temporary model to persistent model.
    }

    override fun onSingleTap(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
    }

    override fun onDoubleTap(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
    }

    override fun onLongTap(event: MyMotionEvent,
                           touchingObject: Any?,
                           touchingContext: Any?) {
    }

    override fun onLongPress(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
    }

    override fun onMoreTap(event: MyMotionEvent,
                           touchingObject: Any?,
                           touchingContext: Any?,
                           tapCount: Int) {
    }

    override fun onDragBegin(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
        // Get the current transform from the view.
        holdStartTransform()

//        Log.d("xyz", "drag start: tx=%.3f, ty=%.3f, scaleX=%.3f, scaleY=%.3f, rotation in degrees=%.3f".format(
//            TwoDTransformUtils.getTranslationX(mStartMatrixToParent),
//            TwoDTransformUtils.getTranslationY(mStartMatrixToParent),
//            TwoDTransformUtils.getScaleX(mStartMatrixToParent),
//            TwoDTransformUtils.getScaleY(mStartMatrixToParent),
//            TwoDTransformUtils.getRotationInDegrees(mStartMatrixToParent)))

        // TODO: Create the temporary model for view to observe.
    }

    override fun onDrag(event: MyMotionEvent,
                        touchingObject: Any?,
                        touchingContext: Any?,
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

//        Log.d("xyz", "------------------------")
//        Log.d("xyz", "start=%s, stop=%s".format(startPointer, stopPointer))
//        Log.d("xyz", "drag: child(dx=%.3f, dy=%.3f), parent(dx=%.3f, dy=%.3f)".format(
//            stopPointer.x - startPointer.x, stopPointer.y - startPointer.y,
//            delta.x, delta.y))
//        Log.d("xyz", "drag: x=%.3f, y=%.3f".format(
//            mStopTransformToParent.translationX, mStopTransformToParent.translationY))

        mView!!.setTransform(mStopTransformToParent.copy())
    }

    override fun onDragFling(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchContext: Any?,
                             startPointerInCanvas: PointF,
                             stopPointerInCanvas: PointF,
                             velocityX: Float,
                             velocityY: Float) {
    }

    override fun onDragEnd(event: MyMotionEvent,
                           touchingObject: Any?,
                           touchingContext: Any?,
                           startPointerInCanvas: PointF,
                           stopPointerInCanvas: PointF) {
    }

    override fun onPinchBegin(event: MyMotionEvent,
                              touchingObject: Any?,
                              touchContext: Any?,
                              startPointers: Array<out PointF>) {
        // Get the current transform from the view.
        holdStartTransform()

//        Log.d("xyz", "pinch start: tx=%.3f, ty=%.3f, scaleX=%.3f, scaleY=%.3f, rotation in degrees=%.3f".format(
//            TwoDTransformUtils.getTranslationX(mStartMatrixToParent),
//            TwoDTransformUtils.getTranslationY(mStartMatrixToParent),
//            TwoDTransformUtils.getScaleX(mStartMatrixToParent),
//            TwoDTransformUtils.getScaleY(mStartMatrixToParent),
//            TwoDTransformUtils.getRotationInDegrees(mStartMatrixToParent)))
    }

    override fun onPinch(event: MyMotionEvent,
                         touchingObject: Any?,
                         touchContext: Any?,
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
        val transform = PointerUtils2.getTransformFromPointers(
            startPointersInParent, stopPointersInParent)

        val dx = transform[PointerUtils2.DELTA_X]
        val dy = transform[PointerUtils2.DELTA_Y]
        val dScale = transform[PointerUtils2.DELTA_SCALE_X]
        val dRadians = transform[PointerUtils2.DELTA_RADIANS]
        val pivotX = transform[PointerUtils2.PIVOT_X]
        val pivotY = transform[PointerUtils2.PIVOT_Y]

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

//        Log.d("xyz", "------------------------")
//        Log.d("xyz", "pinch: dx=%.3f, dy=%.3f; dScale=%.3f, delta degrees=%.3f".format(
//            dx, dy, dScale, Math.toDegrees(dRadians.toDouble())))
//        Log.d("xyz", "pinch: x=%.3f, y=%.3f; scale=%.3f, degrees=%.3f".format(
//            mStopTransformToParent.translationX,
//            mStopTransformToParent.translationY,
//            mStopTransformToParent.scaleX,
//            Math.toDegrees(mStopTransformToParent.rotationInRadians.toDouble())))

        mView!!.setTransform(mStopTransformToParent.copy())
    }

    override fun onPinchFling(event: MyMotionEvent,
                              touchingObject: Any?,
                              touchContext: Any?) {
    }

    override fun onPinchEnd(event: MyMotionEvent,
                            touchingObject: Any?,
                            touchContext: Any?,
                            startPointersInCanvas: Array<out PointF>,
                            stopPointersInCanvas: Array<out PointF>) {
    }

    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun holdStartTransform() {
        mStartMatrixToParent.reset()
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
