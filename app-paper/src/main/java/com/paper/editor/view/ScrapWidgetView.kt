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

import android.graphics.*
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.IAllGesturesListener
import com.paper.AppConst
import com.paper.editor.data.DrawSVGEvent
import com.paper.editor.widget.IScrapWidget
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

open class ScrapWidgetView : IScrapWidgetView {

    private lateinit var mWidget: IScrapWidget

    private var mContext: IPaperContext? = null
    private var mParent: IParentWidgetView? = null
    private val mChildren = mutableListOf<ScrapWidgetView>()

    // Rendering properties.
    private var mX = Float.NaN
    private var mY = Float.NaN
    private var mScale = Float.NaN
    private var mRotationInRadians = Float.NaN
    private var mIsCacheDirty: Boolean = true
    private val mScrapBound = RectF(0f, 0f, 0f, 0f)
    private val mStrokePaths = mutableListOf<Path>()
    private val mStrokePaint = Paint()
    private val mDebugPaint = Paint()
    private var mIsMatrixDirty = true
    /**
     * The matrix used for mapping a point from this world to parent world.
     */
    private val mMatrix = Matrix()
    /**
     * The inverse matrix of [mMatrix], which is used for mapping a point from
     * parent world to this world.
     */
    private val mMatrixInverse = Matrix()
    private val mTransformHelper: TransformUtils = TransformUtils()

    // Gesture.
    private var mIfHandleEvent = false
    private val mGestureDetector: GestureDetector by lazy {
        val field = GestureDetector(Looper.getMainLooper(),
                                    mContext!!.getViewConfiguration(),
                                    mContext!!.getTouchSlop(),
                                    mContext!!.getTapSlop(),
                                    mContext!!.getMinFlingVec(),
                                    mContext!!.getMaxFlingVec())
//        field.tapGestureListener = this@ScrapWidgetView
//        field.dragGestureListener = this@ScrapWidgetView
//        field.pinchGestureListener = this@ScrapWidgetView
        field
    }

    private val mDisposables = CompositeDisposable()

    override fun bindWidget(widget: IScrapWidget) {
        mWidget = widget

        mDisposables.add(
            mWidget.onDrawSVG()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    onDrawSVG(event)
                })

        mDisposables.add(
            mWidget.onTransform()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { xform ->
                    onUpdateTransform(xform)
                })
    }

    override fun unbindWidget() {
        // Clear the reference!
        mContext = null
        mParent = null

        mDisposables.clear()
    }

    override fun addChild(child: IScrapWidgetView) {
        val childView = child as ScrapWidgetView
        mChildren.add(childView)
    }

    override fun removeChild(child: IScrapWidgetView) {
        val childView = child as ScrapWidgetView
        mChildren.remove(childView)
    }

    fun setParent(parent: IParentWidgetView) {
        mParent = parent
    }

    fun setPaperContext(context: IPaperContext) {
        mContext = context

        mStrokePaint.style = Paint.Style.STROKE
        mStrokePaint.color = Color.RED
        mStrokePaint.strokeWidth = 3f * mContext!!.getMinStrokeWidth()

        mDebugPaint.style = Paint.Style.STROKE
        mDebugPaint.color = Color.RED
        mDebugPaint.strokeWidth = 3f
    }

    // Draw ///////////////////////////////////////////////////////

    override fun dispatchDraw(canvas: Canvas) {
        val count = canvas.save()

        computeMatrix()
        canvas.concat(mMatrix)

        // Draw itself
        onDraw(canvas)

        // Then children
        mChildren.forEach { it.dispatchDraw(canvas) }

        canvas.restoreToCount(count)
    }

    private fun onDraw(canvas: Canvas) {
//        // TEST: Improve performance.
//        canvas.clipRect(mScrapBound)

        validateRenderingCache()

        // Sketch.
        mStrokePaths.forEach { path ->
            canvas.drawPath(path, mStrokePaint)
        }

        drawCenter(canvas)
    }

    private fun drawCenter(canvas: Canvas) {
        canvas.drawLine(-20f, 0f, 20f, 0f, mDebugPaint)
        canvas.drawLine(0f, -20f, 0f, 20f, mDebugPaint)
    }

    private fun onDrawSVG(event: DrawSVGEvent) {
        val nx = event.point.x
        val ny = event.point.y
        val (x, y) = mContext!!.mapM2V(nx, ny)

        when (event.action) {
            DrawSVGEvent.Action.MOVE -> {
                val path = Path()
                path.moveTo(x, y)

                mStrokePaths.add(path)
            }
            DrawSVGEvent.Action.LINE_TO -> {
                val path = mStrokePaths[mStrokePaths.size - 1]
                path.lineTo(x, y)
            }
            DrawSVGEvent.Action.CLOSE -> {
                // DO NOTHING.
                Log.d(AppConst.TAG, "")
            }
            DrawSVGEvent.Action.CLEAR_ALL -> {
                mStrokePaths.clear()
            }
            else -> {
                // NOT SUPPORT
            }
        }

        mParent?.delayedInvalidate()
    }

    private fun onUpdateTransform(xform: TransformModel) {
        mX = xform.translationX
        mY = xform.translationY
        mScale = xform.scaleX
        mRotationInRadians = xform.rotationInRadians

        mIsMatrixDirty = true

        // Trigger the invalidation will lead to dispatchDraw and onDraw
        // functions
        invalidate(false)
    }

    private fun invalidate(rebuildCache: Boolean) {
        if (rebuildCache) {
            invalidateRenderingCache()
        }

        mParent?.delayedInvalidate()
    }

    private fun computeMatrix() {
        if (mIsMatrixDirty) {
            val (x, y) = mContext!!.mapM2V(mX, mY)

            mMatrix.reset()
            mMatrix.postScale(mScale, mScale)
            mMatrix.postRotate(Math.toDegrees(mRotationInRadians.toDouble()).toFloat())
            mMatrix.postTranslate(x, y)

            mIsMatrixDirty = false
        }
    }

    private fun invalidateRenderingCache() {
        // Mark the rendering cache is dirty and later validateRenderingCache()
        // would update the necessary properties for rendering and touching.
        mIsCacheDirty = true
    }

    private fun validateRenderingCache() {
        if (!mIsCacheDirty) return

//        rebuildSketchBound()
//        rebuildSketchPath()

        mIsCacheDirty = false
    }

    // Touch //////////////////////////////////////////////////////////////////

    override fun dispatchTouch(event: MotionEvent) {
        TODO()
    }

    protected fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    override fun setGestureListener(listener: IAllGesturesListener?) {
        TODO()
    }
}

