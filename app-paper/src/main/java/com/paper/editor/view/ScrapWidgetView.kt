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

import android.content.Context
import android.graphics.*
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.IAllGesturesListener
import com.paper.AppConst
import com.paper.R
import com.paper.editor.widget.IScrapWidget
import com.paper.util.TransformUtils
import io.reactivex.disposables.CompositeDisposable

open class ScrapWidgetView(private val mContext: Context,
                           private val mScaleFromModelToView: Float)
    : IScrapWidgetView,
      GestureEventMapper.Mapper {

    private lateinit var mWidget: IScrapWidget
    private val mModelDisposables = CompositeDisposable()

    private val mChildren = mutableListOf<ScrapWidgetView>()

    // Rendering properties.
    private var mIsCacheDirty: Boolean = true
    private val mScrapBound = RectF(0f, 0f,
                                    mScaleFromModelToView * 100f,
                                    mScaleFromModelToView * 100f)
    private val mSketchPath = Path()
    private val mSketchPaint = Paint()
    private val mSketchMinWidth: Float by lazy { mContext.resources.getDimension(R.dimen.sketch_min_stroke_width) }
    private val mSketchMaxWidth: Float by lazy { mContext.resources.getDimension(R.dimen.sketch_max_stroke_width) }

    // Transform
    private val mToParentMatrix = Matrix()
    private val mToParentMatrixInverse = Matrix()
    private val mTransformHelper: TransformUtils = TransformUtils()

    // Gesture.
    private var mIfHandleEvent = false
    private val mGestureDetector: GestureDetector by lazy {
        GestureDetector(Looper.getMainLooper(),
                        ViewConfiguration.get(mContext),
                        mContext.resources.getDimension(R.dimen.touch_slop),
                        mContext.resources.getDimension(R.dimen.tap_slop),
                        mContext.resources.getDimension(R.dimen.fling_min_vec),
                        mContext.resources.getDimension(R.dimen.fling_max_vec))
    }
    private val mEventNormalizationHelper by lazy {
        GestureEventMapper()
    }

    init {
        // Map the events to the canvas world and apply normalization function.
        mGestureDetector.tapGestureListener = mEventNormalizationHelper
        mGestureDetector.dragGestureListener = mEventNormalizationHelper
        mGestureDetector.pinchGestureListener = mEventNormalizationHelper
    }

//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//
//        // Enable helper of converting the event from this coordinate to canvas
//        // coordinate.
//        mEventNormalizationHelper.setNumberMapper(this@ScrapWidgetView)
//
//        // Giving a background would make onDraw() able to be called.
//        setBackgroundColor(Color.TRANSPARENT)
//
//        invalidateRenderingCache()
//    }

    override fun dispatchDraw(canvas: Canvas) {
        val count = canvas.save()
        canvas.concat(mToParentMatrix)

        // Draw itself
        onDraw(canvas)

        // Then children
        mChildren.forEach { it.dispatchDraw(canvas) }

        canvas.restoreToCount(count)
    }

    override fun dispatchTouch(event: MotionEvent) {
        TODO()
    }

    ///////////////////////////////////////////////////////////////////////////
    // IScrapWidgetView ///////////////////////////////////////////////////////

    protected fun onDraw(canvas: Canvas) {
        mSketchPaint.style = Paint.Style.STROKE
        mSketchPaint.color = Color.BLUE
        mSketchPaint.strokeWidth = mSketchMinWidth

        //        validateRenderingCache()

        // TEST: Improve performance.
        canvas.clipRect(mScrapBound)

        // Boundary.
        canvas.drawRect(mScrapBound, mSketchPaint)
        //        // Sketch.
        //        canvas.drawPath(mSketchPath, mSketchPaint)
    }

    protected fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.getX(0)
        val y = event.getY(0)

        // If the canvas doesn't handle the touch, bubble up the event.
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mIfHandleEvent = mScrapBound.contains(x, y)
            }
        }

        return if (mIfHandleEvent) {
            mGestureDetector.onTouchEvent(event, null, null)
            true
        } else {
            false
        }
    }

    override fun bindWidget(widget: IScrapWidget) {
        mWidget = widget

        mModelDisposables.add(
            mWidget.onTransform()
                .subscribe { xform ->
                    val scaledModelX = mScaleFromModelToView * xform.translationX
                    val scaledModelY = mScaleFromModelToView * xform.translationY

                    mToParentMatrix.reset()
                    mToParentMatrix.postScale(xform.scaleX, xform.scaleY)
                    mToParentMatrix.postRotate(Math.toDegrees(xform.rotationInRadians.toDouble()).toFloat())
                    mToParentMatrix.postTranslate(scaledModelX, scaledModelY)

                    Log.d(AppConst.TAG, "x=${xform.translationX}, y=${xform.translationY}")
                    Log.d(AppConst.TAG, "x=$scaledModelX, y=$scaledModelY")

                    mToParentMatrix.invert(mToParentMatrixInverse)

//                    invalidate()
                })

//        // Update view transform according to model.
//        setTransform(TransformModel(
//            translationX = mWidget.x,
//            translationY = mWidget.y,
//            scaleX = mWidget.scale,
//            scaleY = mWidget.scale,
//            rotationInRadians = mWidget.rotationInRadians))
//
//        invalidateRenderingCache()
//
//        invalidate()
//        if (!isLayoutRequested) {
//            requestLayout()
//        }
    }

    override fun unbindWidget() {
        mModelDisposables.clear()
    }

    override fun addChild(child: IScrapWidgetView) {
        val childView = child as ScrapWidgetView
        mChildren.add(childView)
    }

    override fun removeChild(child: IScrapWidgetView) {
        val childView = child as ScrapWidgetView
        mChildren.remove(childView)
    }

    override fun setGestureListener(listener: IAllGesturesListener?) {
        mEventNormalizationHelper.setGestureListener(listener)
    }

//    fun setViewToModelScale(scale: Float) {
//        mViewToModelScale = scale
//
//        // Also update the event normalization helper.
//        mEventNormalizationHelper.setScaleFactors(
//            mViewToModelScale, mViewToModelScale)
//    }

    // Transform //////////////////////////////////////////////////////////////

    override fun map(nums: FloatArray) {
        // TODO()
//        matrix.mapPoints(nums)
    }

    override fun invertMap(nums: FloatArray) {
        // TODO()
    }

    //
//    override fun setTransform(transform: TransformModel) {
//        scaleX = transform.scaleX
//        scaleY = transform.scaleY
//        rotation = Math.toDegrees(transform.rotationInRadians.toDouble()).toFloat()
//        translationX = mEventNormalizationHelper.invertScale(transform.translationX)
//        translationY = mEventNormalizationHelper.invertScaleY(transform.translationY)
//    }

    // Rendering //////////////////////////////////////////////////////////////

//    override fun invalidateRenderingCache() {
//        // Mark the rendering cache is dirty and later validateRenderingCache()
//        // would update the necessary properties for rendering and touching.
//        mIsCacheDirty = true
//    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

//    private fun validateRenderingCache() {
//        if (!mIsCacheDirty) return
//
//        // Boundary.
//        rebuildSketchBound()
//
//        // Path for sketch.
//        rebuildSketchPath()
//
//        mIsCacheDirty = false
//    }
//
//    private fun rebuildSketchBound() {
//        var left = Float.POSITIVE_INFINITY
//        var top = Float.POSITIVE_INFINITY
//        var right = Float.NEGATIVE_INFINITY
//        var bottom = Float.NEGATIVE_INFINITY
//        mWidget.sketch?.let { sketch ->
//            sketch.allStrokes.forEach { stroke ->
//                stroke.pathTupleList.forEach { tuple ->
//                    left = Math.min(left, tuple.firstPoint.x)
//                    top = Math.min(top, tuple.firstPoint.y)
//                    right = Math.max(right, tuple.firstPoint.x)
//                    bottom = Math.max(bottom, tuple.firstPoint.y)
//                }
//            }
//            mScrapBound.set(left,
//                            top,
//                            right,
//                            bottom)
//        }
//
//        // To view world.
//        mScrapBound.set(mScrapBound.left / mViewToModelScale,
//                        mScrapBound.top / mViewToModelScale,
//                        mScrapBound.right / mViewToModelScale,
//                        mScrapBound.bottom / mViewToModelScale)
//    }
//
//    private fun rebuildSketchPath() {
//        mWidget.sketch?.let { sketch ->
//            mSketchPath.reset()
//
//            sketch.allStrokes.forEach { stroke ->
//                stroke.pathTupleList.forEachIndexed { i, tuple ->
//                    val x = tuple.firstPoint.x / mViewToModelScale
//                    val y = tuple.firstPoint.y / mViewToModelScale
//
//                    when (i) {
//                        0 -> {
//                            mSketchPath.moveTo(x, y)
//                        }
//                        else -> {
//                            mSketchPath.lineTo(x, y)
//                        }
//                    }
//                }
//            }
//        }
//    }
}

