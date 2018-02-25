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
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.cardinalblue.gesture.GestureDetector
import com.paper.R
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel
import java.util.*

class PaperCanvasView : FrameLayout,
                        ICanvasView {

    private val mViewLookupTable = hashMapOf<Long, View>()

    // Listeners.
    private var mListener: IScrapLifecycleListener? = null

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

        // TEST
        //        translationX = -360f
        //        translationY = -360f
        //        scaleX = 1.5f
        //        scaleY = 1.5f
        //        rotation = -50f
        setBackgroundColor(Color.LTGRAY)
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
        TODO("not implemented")
    }

    override fun getTransformMatrix(): Matrix {
        TODO("not implemented")
    }

    override fun getGestureDetector(): GestureDetector {
        TODO("not implemented")
    }

    override fun setTransform(transform: TransformModel) {
        TODO("not implemented")
    }

    override fun setTransformPivot(pivotX: Float, pivotY: Float) {
        TODO("not implemented")
    }

    override fun convertPointToParentWorld(point: FloatArray) {
        TODO("not implemented")
    }

    override fun getScrapId(): Long {
        throw IllegalAccessException("Canvas view doesn't have scrap ID")
    }

    override fun setScrapId(id: Long) {
        throw IllegalAccessException("Canvas view doesn't have scrap ID")
    }

    override fun setScrapLifecycleListener(listener: IScrapLifecycleListener?) {
        mListener = listener
    }

    override fun addViewBy(scrap: ScrapModel) {
        val id = scrap.id
        when {
            scrap.sketch != null -> {
                val scrapView = ScrapView(context)
                scrapView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)

                // Add view.
                addView(scrapView)
                onAttachToCanvas(scrapView)

                // Add to lookup table.
                mViewLookupTable[id] = scrapView
            }
            else -> {
                // TODO: Support more types of scrap
                throw IllegalStateException("Unsupported action")
            }
        }
    }

    override fun removeViewBy(id: Long) {
        val view = (mViewLookupTable[id] ?:
                         throw NoSuchElementException(
                             "No view with ID=%d".format(id)))
        val scrapView = view as IScrapView

        // Remove view.
        removeView(view)
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
