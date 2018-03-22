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
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.cardinalblue.gesture.GestureDetector
import com.paper.AppConsts
import com.paper.R
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils
import java.util.*

open class ScrapView : FrameLayout,
                       IScrapView,
                       GestureEventNormalizationHelper.ToCanvasWorldConverter {

    private lateinit var mModel: ScrapModel

    private var mViewToModelScale: Float = 1f

    // Rendering properties.
    private var mIsCacheDirty: Boolean = true
    private val mScrapBound = RectF()
    private val mSketchPath = Path()
    private val mSketchPaint = Paint()
    private val mSketchMinWidth: Float by lazy { resources.getDimension(R.dimen.sketch_min_stroke_width) }
    private val mSketchMaxWidth: Float by lazy { resources.getDimension(R.dimen.sketch_max_stroke_width) }

    // Gesture.
    private var mIfHandleEvent = false
    private val mTransformHelper: TransformUtils = TransformUtils()
    private val mGestureDetector: GestureDetector by lazy {
        GestureDetector(Looper.getMainLooper(),
                        ViewConfiguration.get(context),
                        resources.getDimension(R.dimen.touch_slop),
                        resources.getDimension(R.dimen.tap_slop),
                        resources.getDimension(R.dimen.fling_min_vec),
                        resources.getDimension(R.dimen.fling_max_vec))
    }
    private val mEventNormalizationHelper by lazy {
        GestureEventNormalizationHelper(this@ScrapView)
    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?,
                attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        // Map the events to the canvas world and apply normalization function.
        mGestureDetector.tapGestureListener = mEventNormalizationHelper
        mGestureDetector.dragGestureListener = mEventNormalizationHelper
        mGestureDetector.pinchGestureListener = mEventNormalizationHelper

        // Changing the pivot to left-top at the beginning.
        // Note: Changing the pivot will update the rendering matrix, where it
        // is like making the parent see the child in a different angles.
        pivotX = 0f
        pivotY = 0f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Giving a background would make onDraw() able to be called.
        setBackgroundColor(Color.TRANSPARENT)

        invalidateRenderingCache()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        Log.d(AppConsts.TAG, "ScrapView # onMeasure()")
    }

    override fun onLayout(changed: Boolean,
                          left: Int,
                          top: Int,
                          right: Int,
                          bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        Log.d(AppConsts.TAG, "ScrapView # onLayout(changed=$changed)")
    }

    override fun onDraw(canvas: Canvas) {
        mSketchPaint.style = Paint.Style.STROKE
        mSketchPaint.color = Color.BLUE
        mSketchPaint.strokeWidth = mSketchMinWidth * 3

        validateRenderingCache()

        // TEST: Improve performance.
        canvas.clipRect(mScrapBound)

        // Boundary.
        canvas.drawRect(mScrapBound, mSketchPaint)
        // Sketch.
        canvas.drawPath(mSketchPath, mSketchPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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

    ///////////////////////////////////////////////////////////////////////////
    // IScrapView /////////////////////////////////////////////////////////////

    override fun setModel(model: ScrapModel) {
        mModel = model

        // Update view transform according to model.
        setTransform(TransformModel(
            translationX = mModel.x,
            translationY = mModel.y,
            scaleX = mModel.scale,
            scaleY = mModel.scale,
            rotationInRadians = mModel.rotationInRadians))

        invalidateRenderingCache()

        invalidate()
        if (!isLayoutRequested) {
            requestLayout()
        }
    }

    override fun getScrapId(): UUID {
        return mModel.uuid
    }

    // Gesture ////////////////////////////////////////////////////////////////

    override fun setGestureListener(listener: SimpleGestureListener?) {
        mEventNormalizationHelper.setGestureListener(listener)
    }

    // Transform //////////////////////////////////////////////////////////////

    override fun mapToCanvasWorld(nums: FloatArray) {
        matrix.mapPoints(nums)
    }

    override fun setTransform(transform: TransformModel) {
        scaleX = transform.scaleX
        scaleY = transform.scaleY
        rotation = Math.toDegrees(transform.rotationInRadians.toDouble()).toFloat()
        translationX = mEventNormalizationHelper.inverseNormalizationToX(transform.translationX)
        translationY = mEventNormalizationHelper.inverseNormalizationToY(transform.translationY)
    }

    // Rendering //////////////////////////////////////////////////////////////

    override fun invalidateRenderingCache() {
        // Mark the rendering cache is dirty and later validateRenderingCache()
        // would update the necessary properties for rendering and touching.
        mIsCacheDirty = true
    }

    // Parent /////////////////////////////////////////////////////////////////

    fun setViewToModelScale(scale: Float) {
        mViewToModelScale = scale

        // Also update the event normalization helper.
        mEventNormalizationHelper.setNormalizationFactors(
            mViewToModelScale, mViewToModelScale)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun validateRenderingCache() {
        if (!mIsCacheDirty) return

        // Boundary.
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = Float.MIN_VALUE
        var bottom = Float.MIN_VALUE
        mModel.sketch?.let { sketch ->
            sketch.allStrokes.forEach { stroke ->
                stroke.allPathTuple.forEach { tuple ->
                    left = Math.min(left, tuple.firstPoint.x)
                    top = Math.min(top, tuple.firstPoint.y)
                    right = Math.max(right, tuple.firstPoint.x)
                    bottom = Math.max(bottom, tuple.firstPoint.y)
                }
            }
            mScrapBound.set(left * width,
                            top * height,
                            right * width,
                            bottom * height)
        }

        // Path for sketch.
        rebuildSketchPath()

        mIsCacheDirty = false
    }

    private fun rebuildSketchPath() {
        mModel.sketch?.let { sketch ->
            mSketchPath.reset()

            sketch.allStrokes.forEach { stroke ->
                stroke.allPathTuple.forEachIndexed { i, tuple ->
                    val x = tuple.firstPoint.x * width
                    val y = tuple.firstPoint.y * height

                    when (i) {
                        0 -> {
                            mSketchPath.moveTo(x, y)
                        }
                        else -> {
                            mSketchPath.lineTo(x, y)
                        }
                    }
                }
            }
        }
    }
}

