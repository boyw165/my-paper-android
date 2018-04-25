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

package com.paper.view.canvas

import android.graphics.*
import android.os.Looper
import android.view.MotionEvent
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.IAllGesturesListener
import com.paper.AppConst
import com.paper.domain.event.DrawSVGEvent
import com.paper.domain.util.TransformUtils
import com.paper.domain.widget.editor.IScrapWidget
import com.paper.model.Point
import com.paper.model.TransformModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.*

open class ScrapWidgetView : IScrapWidgetView {

    private lateinit var mWidget: IScrapWidget

    private var mContext: IPaperContext? = null
    private var mParent: IParentWidgetView? = null
    private val mChildren = mutableListOf<ScrapWidgetView>()

    // Sketch
    private val mDrawables = mutableListOf<SVGDrawable>()

    // Rendering properties.
    private var mX = Float.NaN
    private var mY = Float.NaN
    private var mScale = Float.NaN
    private var mRotationInRadians = Float.NaN
    private var mIsCacheDirty: Boolean = true
    private val mScrapBound = RectF(0f, 0f, 0f, 0f)
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

    private val mSharpeningMatrix = Matrix()
    private val mSharpeningMatrixInverse = Matrix()

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

        println("${AppConst.TAG}: Bind scrap widget \"View\" with a scrap \"Widget\"")
    }

    override fun unbindWidget() {
        // Clear the reference!
        mContext = null
        mParent = null

        mDisposables.clear()

        println("${AppConst.TAG}: Unbind scrap widget \"View\" from a scrap \"Widget\"")
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

    override fun dispatchDraw(canvas: Canvas,
                              previousXforms: Stack<Matrix>,
                              ifSharpenDrawing: Boolean) {
        val count = canvas.save()

        computeMatrix()

        // Draw itself
        if (ifSharpenDrawing) {
            // Concatenate all the previous transform
            mSharpeningMatrix.reset()
            previousXforms.forEach { m ->
                mSharpeningMatrix.preConcat(m)
            }
            mSharpeningMatrix.preConcat(mMatrix)
            mSharpeningMatrix.invert(mSharpeningMatrixInverse)

            mDrawables.forEach { d ->
                d.onDraw(canvas, mSharpeningMatrix)
            }
        } else {
            canvas.concat(mMatrix)
            mDrawables.forEach { d ->
                d.onDraw(canvas)
            }
        }

        // Then children
        previousXforms.push(mMatrix)
        mChildren.forEach {
            it.dispatchDraw(canvas,
                            previousXforms,
                            ifSharpenDrawing)
        }
        previousXforms.pop()

        canvas.restoreToCount(count)
    }

    private fun onDrawSVG(event: DrawSVGEvent) {
        val nx = event.point.x
        val ny = event.point.y
        val (x, y) = mContext!!.mapM2V(nx, ny)

        when (event.action) {
            DrawSVGEvent.Action.MOVE -> {
                val d = SVGDrawable(context = mContext!!,
                                    penColor = event.penColor,
                                    penSize = event.penSize)
                d.moveTo(Point(x, y, event.point.time))

                mDrawables.add(d)
            }
            DrawSVGEvent.Action.LINE_TO -> {
                val d = mDrawables.last()
                d.lineTo(Point(x, y, event.point.time))
            }
            DrawSVGEvent.Action.CLOSE -> {
                val d = mDrawables.last()
                d.close()
            }
            DrawSVGEvent.Action.CLEAR_ALL -> {
                val d = mDrawables.last()
                d.clear()
                mDrawables.clear()
            }
            else -> {
                // NOT SUPPORT
            }
        }

        mParent?.invalidate()
    }

    private fun onUpdateTransform(xform: TransformModel) {
        mX = xform.translationX
        mY = xform.translationY
        mScale = xform.scaleX
        mRotationInRadians = xform.rotationInRadians

        mIsMatrixDirty = true

        // Trigger the invalidation will lead to dispatchDraw and onDraw
        // functions
        invalidate()
    }

    private fun invalidate() {
        mParent?.invalidate()
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

