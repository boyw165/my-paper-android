package com.paper.editor

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
    }

    override fun onActionEnd(event: MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
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

        // TODO: Create the temporary model for view to observe.
    }

    override fun onDrag(event: MyMotionEvent,
                        touchingObject: Any?,
                        touchingContext: Any?,
                        startPointerInCanvas: PointF,
                        stopPointerInCanvas: PointF) {
        val dx = stopPointerInCanvas.x - startPointerInCanvas.x
        val dy = stopPointerInCanvas.y - startPointerInCanvas.y

        mStopTransform.translationX += dx
        mStopTransform.translationY += dy

        Log.d("xyz", "start x=%.3f, y=%.3f; stop x=%.3f, y=%.3f".format(
            mStartTransform.translationX, mStartTransform.translationY,
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
        // TODO: Commit the temporary model to persistent model.
    }

    override fun onPinchBegin(event: MyMotionEvent,
                              touchingObject: Any?,
                              touchContext: Any?,
                              startPointers: Array<out PointF>) {
    }

    override fun onPinch(event: MyMotionEvent,
                         touchingObject: Any?,
                         touchContext: Any?,
                         startPointersInCanvas: Array<out PointF>,
                         stopPointersInCanvas: Array<out PointF>) {
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
}
