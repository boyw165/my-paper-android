package com.paper.editor

import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
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
    private val mStartMatrix: Matrix = Matrix()
    private val mStopMatrix: Matrix = Matrix()
    private var mStartTransform: TransformModel = TransformModel(0f, 0f, 1f, 1f, 0f)
    private var mStopTransform: TransformModel = TransformModel(0f, 0f, 1f, 1f, 0f)
    private val mGestureDetector: GestureDetector by lazy {
        GestureDetector(mContextProvider.getContext(),
                        this,
                        mConfig.getTouchSlop(),
                        mConfig.getTapSlop(),
                        mConfig.getMinFlingVec(),
                        mConfig.getMaxFlingVec())
    }

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

        Log.d("xyz", "drag start: tx=%.3f, ty=%.3f, scaleX=%.3f, scaleY=%.3f, rotation in degrees=%.3f".format(
            mStartTransform.translationX,
            mStartTransform.translationY,
            mStartTransform.scaleX,
            mStartTransform.scaleY,
            Math.toDegrees(mStartTransform.rotationInRadians.toDouble()).toFloat()))
        Log.d("xyz", "drag start: tx=%.3f, ty=%.3f, scaleX=%.3f, scaleY=%.3f, rotation in degrees=%.3f".format(
            TwoDTransformUtils.getTranslationX(mStartMatrix),
            TwoDTransformUtils.getTranslationY(mStartMatrix),
            TwoDTransformUtils.getScaleX(mStartMatrix),
            TwoDTransformUtils.getScaleY(mStartMatrix),
            TwoDTransformUtils.getRotationInDegrees(mStartMatrix)))

        // TODO: Create the temporary model for view to observe.
    }

    override fun onDrag(event: MyMotionEvent,
                        touchingObject: Any?,
                        touchingContext: Any?,
                        startPointer: PointF,
                        stopPointer: PointF) {
        mPointerMap[0] = startPointer.x
        mPointerMap[1] = startPointer.y

        mView!!.convertPointFromChildToParent(mPointerMap)

        val startXInParent = mPointerMap[0]
        val startYInParent = mPointerMap[1]

        mPointerMap[0] = stopPointer.x
        mPointerMap[1] = stopPointer.y

        mView!!.convertPointFromChildToParent(mPointerMap)

        val stopXInParent = mPointerMap[0]
        val stopYInParent = mPointerMap[1]

        val dxInParent = stopXInParent - startXInParent
        val dyInParent = stopYInParent - startYInParent

        mStopTransform.translationX += dxInParent
        mStopTransform.translationY += dyInParent

        Log.d("xyz", "------------------------")
        Log.d("xyz", "start=%s, stop=%s".format(startPointer, stopPointer))
        Log.d("xyz", "drag: child(dx=%.3f, dy=%.3f), parent(dx=%.3f, dy=%.3f)".format(
            stopPointer.x - startPointer.x, stopPointer.y - startPointer.y,
            dxInParent, dyInParent))
        Log.d("xyz", "drag: x=%.3f, y=%.3f".format(
            mStopTransform.translationX, mStopTransform.translationY))
        mView!!.setTransform(mStopTransform.copy())
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

        Log.d("xyz", "pinch start: tx=%.3f, ty=%.3f, scaleX=%.3f, scaleY=%.3f, degrees=%.3f".format(
            mStartTransform.translationX,
            mStartTransform.translationY,
            mStartTransform.scaleX,
            mStartTransform.scaleY,
            Math.toDegrees(mStartTransform.rotationInRadians.toDouble())))
    }

    override fun onPinch(event: MyMotionEvent,
                         touchingObject: Any?,
                         touchContext: Any?,
                         startPointers: Array<PointF>,
                         stopPointers: Array<PointF>) {
        // Map the coordinates from child world to the parent world.
        val startPointersInParent = Array(startPointers.size, { i ->
            val map = floatArrayOf(startPointers[i].x,
                              startPointers[i].y)
            mView!!.convertPointFromChildToParent(map)
            PointF(map[0], map[1])
        })
        val stopPointersInParent = Array(stopPointers.size, { i ->
            val map = floatArrayOf(stopPointers[i].x,
                                   stopPointers[i].y)
            mView!!.convertPointFromChildToParent(map)
            PointF(map[0], map[1])})

        // Calculate the transformation.
        val transform = PointerUtils2.getTransformFromPointers(
            startPointersInParent, stopPointersInParent)

        val dx = transform[PointerUtils2.DELTA_X]
        val dy = transform[PointerUtils2.DELTA_Y]
        val dScale = transform[PointerUtils2.DELTA_SCALE_X]
        val dRadians = transform[PointerUtils2.DELTA_RADIANS]
        val pivotX = transform[PointerUtils2.PIVOT_X]
        val pivotY = transform[PointerUtils2.PIVOT_Y]

        mStartMatrix.set(mView!!.getTransformMatrix())
        mStopMatrix.set(mStartMatrix)
        mStopMatrix.postScale(dScale, dScale, pivotX, pivotY)
        mStopMatrix.postRotate(Math.toDegrees(dRadians.toDouble()).toFloat(), pivotX, pivotY)
        mStopMatrix.postTranslate(dx, dy)

//        mStopTransform.translationX += dx
//        mStopTransform.translationY += dy
//        mStopTransform.scaleX *= dScale
//        mStopTransform.scaleY *= dScale
//        mStopTransform.rotationInRadians += dRadians

        mStopTransform.translationX = TwoDTransformUtils.getTranslationX(mStopMatrix)
        mStopTransform.translationY = TwoDTransformUtils.getTranslationY(mStopMatrix)
        mStopTransform.scaleX = TwoDTransformUtils.getScaleX(mStopMatrix)
        mStopTransform.scaleY = TwoDTransformUtils.getScaleY(mStopMatrix)
        mStopTransform.rotationInRadians = TwoDTransformUtils.getRotationInRadians(mStopMatrix)

        Log.d("xyz", "------------------------")
        Log.d("xyz", "pinch: dx=%.3f, dy=%.3f; dScale=%.3f, delta degrees=%.3f".format(
            dx, dy, dScale, Math.toDegrees(dRadians.toDouble())))
        Log.d("xyz", "pinch: x=%.3f, y=%.3f; scale=%.3f, degrees=%.3f".format(
            mStopTransform.translationX,
            mStopTransform.translationY,
            mStopTransform.scaleX,
            Math.toDegrees(mStopTransform.rotationInRadians.toDouble())))

        mView!!.setTransform(mStopTransform.copy())
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
        val start = mView!!.getTransform()
        mStartTransform.translationX = start.translationX
        mStartTransform.translationY = start.translationY
        mStartTransform.scaleX = start.scaleX
        mStartTransform.scaleY = start.scaleY
        mStartTransform.rotationInRadians = start.rotationInRadians

        mStopTransform.translationX = start.translationX
        mStopTransform.translationY = start.translationY
        mStopTransform.scaleX = start.scaleX
        mStopTransform.scaleY = start.scaleY
        mStopTransform.rotationInRadians = start.rotationInRadians

        mStartMatrix.reset()
        mStartMatrix.set(mView!!.getTransformMatrix())
    }
}
