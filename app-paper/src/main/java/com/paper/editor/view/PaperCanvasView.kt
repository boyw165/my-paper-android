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
import android.graphics.Color
import android.graphics.Matrix
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.cardinalblue.gesture.GestureDetector
import com.paper.R
import com.paper.editor.ITouchConfig
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils
import java.lang.UnsupportedOperationException
import java.util.*

class PaperCanvasView : FrameLayout,
                        ITouchConfig,
                        ICanvasView {

    // Views.
    private val mViewLookupTable = hashMapOf<Long, View>()

    // Listeners.
    private var mListener: IScrapLifecycleListener? = null

    // Gesture.
    private val mTransformHelper: TransformUtils = TransformUtils()
    private val mGestureDetector: GestureDetector by lazy {
        GestureDetector(context,
                        getTouchSlop(),
                        getTapSlop(),
                        getMinFlingVec(),
                        getMaxFlingVec())
    }

    constructor(context: Context?) : super(context)

    constructor(context: Context?,
                attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Changing the pivot to left-top at the beginning.
        // Note: Changing the pivot will update the rendering matrix, where it
        // is like making the parent see the child in a different angles.
        pivotX = 0f
        pivotY = 0f

        // Giving a background would make onDraw() able to be called.
        setBackgroundColor(Color.TRANSPARENT)

        // TEST
        ViewCompat.setElevation(this, 12f)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Touch config ///////////////////////////////////////////////////////////

    override fun getTouchSlop(): Float {
        return resources.getDimension(R.dimen.touch_slop)
    }

    override fun getTapSlop(): Float {
        return resources.getDimension(R.dimen.tap_slop)
    }

    override fun getMinFlingVec(): Float {
        return resources.getDimension(R.dimen.fling_min_vec)
    }

    override fun getMaxFlingVec(): Float {
        return resources.getDimension(R.dimen.fling_max_vec)
    }

    ///////////////////////////////////////////////////////////////////////////
    // ICanvasView ////////////////////////////////////////////////////////////

    override fun getTransform(): TransformModel {
        return TransformModel(translationX = translationX,
                              translationY = translationY,
                              scaleX = scaleX,
                              scaleY = scaleY,
                              rotationInRadians = Math.toRadians(
                                  rotation.toDouble()).toFloat())
    }

    override fun getTransformMatrix(): Matrix {
        return matrix
    }

    override fun getGestureDetector(): GestureDetector {
        return mGestureDetector
    }

    override fun setTransform(transform: TransformModel) {
        TODO("not implemented")
    }

    override fun setTransformPivot(px: Float, py: Float) {
        pivotX = px
        pivotY = py
    }

    override fun convertPointToParentWorld(point: FloatArray) {
        TODO("not implemented")
    }

    override fun setScrapLifecycleListener(listener: IScrapLifecycleListener?) {
        mListener = listener
    }

    // TODO: Separate the application domain and business domain model.
    override fun addViewBy(scrap: ScrapModel) {
        val id = scrap.id
        when {
            scrap.sketch != null -> {
                val scrapView = ScrapView(context)
                scrapView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)

                // TODO: Initialize the view by model?
                // TODO: Separate the application domain and business domain model.
                scrapView.setModel(scrap)

                // Add view.
                addView(scrapView)
                // Dispatch the adding event so that the scrap-controller is
                // aware of the scrap-view.
                onAttachToCanvas(scrapView)

                // Add to lookup table.
                mViewLookupTable[id] = scrapView

                postInvalidate()
            }
            else -> {
                // TODO: Support more types of scrap
                throw UnsupportedOperationException("Unrecognized scrap model")
            }
        }
    }

    override fun removeViewBy(id: Long) {
        val view = (mViewLookupTable[id] ?: throw NoSuchElementException(
            "No view with ID=%d".format(id)))
        val scrapView = view as IScrapView

        // Remove view.
        removeView(view)
        // Dispatch the removing event so that the scrap-controller becomes
        // unaware of the scrap-view.
        onDetachFromCanvas(scrapView)

        // Remove view from the lookup table.
        mViewLookupTable.remove(view.getScrapId())
    }

    override fun removeAllViews() {
        mViewLookupTable.values.forEach {
            val scrapView = it as IScrapView
            removeViewBy(scrapView.getScrapId())
        }
    }

    override fun onAttachToCanvas(view: IScrapView) {
        mListener?.onAttachToCanvas(view)
    }

    override fun onDetachFromCanvas(view: IScrapView) {
        mListener?.onDetachFromCanvas(view)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////
}
