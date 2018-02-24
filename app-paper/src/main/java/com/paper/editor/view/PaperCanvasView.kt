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
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.paper.shared.model.PaperModel

class PaperCanvasView : SketchView,
                        ICanvasView {

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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    ///////////////////////////////////////////////////////////////////////////
    // ICanvasView ////////////////////////////////////////////////////////////

    override fun getScrapId(): Long {
        TODO("not implemented")
    }

    override fun setScrapId(id: Long) {
        TODO("not implemented")
    }

    override fun setScrapLifecycleListener(listener: IScrapLifecycleListener?) {
        mListener = listener
    }

    override fun inflateViewBy(model: PaperModel) {
        // Inflate scraps.
        model.scraps.forEach { scrap ->
            when {
                scrap.sketch != null -> {
                    val scrapView = SketchView(context)
                    scrapView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)

                    addScrapView(scrapView)
                }
                else -> {
                    // TODO: Support more types of scrap
                    throw IllegalStateException("Unsupported action")
                }
            }
        }
    }

    override fun deflateView() {
        while (childCount > 0) {
            removeScrapView(getChildAt(0) as IScrapView)
        }
    }

    override fun addScrapView(view: IScrapView) {
        when (view) {
            is View -> {
                addView(view)
                onAttachToCanvas(view)
            }
            else -> throw IllegalArgumentException(
                "Given scrap-view isn't an Android View")
        }
    }

    override fun removeScrapView(view: IScrapView) {
        when (view) {
            is View -> {
                removeView(view)
                onDetachFromCanvas(view)
            }
            else -> throw IllegalArgumentException(
                "Given scrap-view isn't an Android View")
        }
    }

    override fun onAttachToCanvas(view: IScrapView) {
        mListener?.onAttachToCanvas(view)
    }

    override fun onDetachFromCanvas(view: IScrapView) {
        mListener?.onDetachFromCanvas(view)
    }
}
