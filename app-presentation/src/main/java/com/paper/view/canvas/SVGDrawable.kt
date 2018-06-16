// Copyright Apr 2018-present Paper
//
// Author: boyw165@gmail.com
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
import com.paper.AppConst
import com.paper.BuildConfig
import com.paper.domain.interpolator.HermiteCubicSplineInterpolator
import com.paper.domain.interpolator.ISplineInterpolator
import com.paper.model.DirtyFlag
import com.paper.model.DirtyType
import com.paper.model.Point
import java.util.*

class SVGDrawable(val id: UUID,
                  context: IPaperContext,
                  points: List<Point> = emptyList(),
                  penColor: Int = 0,
                  penSize: Float = 1f,
                  porterDuffMode: PorterDuffXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)) {

    private val mContext = context
    private val mPenColor = penColor
    private val mPenSize = penSize

    private var mConsumedCount = 0
    private val mPointList = mutableListOf<Point>()
    /**
     * The spline interpolation list, where the size is equal to
     * [mPointList]'s size - 1.
     */
    private val mSplineList = mutableListOf<ISplineInterpolator>()
    private val mStrokePaint = Paint()
    private val mDebugStrokePaint = Paint()

    // Dirty flag
    private val mDirtyFlag = DirtyFlag()

    // Bezier
    private var mBasedWidth: Float = 0f
    private var mMinWidth: Float = 0f
    private var mMaxWidth: Float = 0f
    private var mVelocityFilterWeight: Float = 0.toFloat()
    private val mPath = Path()

    init {
        mBasedWidth = getPaintSize(mPenSize)
        mMinWidth = 1f * mBasedWidth
        mMaxWidth = 2.5f * mBasedWidth
        mVelocityFilterWeight = 0.9f

        mStrokePaint.strokeWidth = mBasedWidth
        mStrokePaint.color = mPenColor
        mStrokePaint.style = Paint.Style.FILL_AND_STROKE
        mStrokePaint.strokeCap = Paint.Cap.ROUND
        mStrokePaint.xfermode = porterDuffMode

        mDebugStrokePaint.strokeWidth = 3f * context.getOneDp()
        mDebugStrokePaint.color = Color.GREEN
        mDebugStrokePaint.style = Paint.Style.FILL_AND_STROKE
        mDebugStrokePaint.strokeCap = Paint.Cap.ROUND

        // Initialize the points
        points.forEachIndexed { i, p ->
            when (i) {
                0 -> moveTo(p)
                points.lastIndex -> close()
                else -> lineTo(p)
            }
        }
    }

    private fun getPaintSize(penSize: Float): Float {
        synchronized(this) {
            // See convex-set, https://en.wikipedia.org/wiki/Convex_set
            return (1 - penSize) * mContext.getMinStrokeWidth() +
                   penSize * mContext.getMaxStrokeWidth()
        }
    }

    // Drawing ////////////////////////////////////////////////////////////////

    fun clear() {
        synchronized(this) {
            mPointList.clear()
            mSplineList.clear()

            mConsumedCount = 0
            mDirtyFlag.markDirty(DirtyType.HASH)
        }
    }

    fun moveTo(point: Point) {
        synchronized(this) {
            addSpline(point)
            mDirtyFlag.markDirty(DirtyType.HASH)
        }
    }

    fun lineTo(point: Point) {
        synchronized(this) {
            addSpline(point)
            mDirtyFlag.markDirty(DirtyType.HASH)
        }
    }

    fun close() {
        synchronized(this) {
            // DO NOTHING
        }
    }

    /**
     * Check if this Drawable still gets something not drew.
     */
    fun isSomethingToDraw(): Boolean {
        synchronized(this) {
            return mConsumedCount < mPointList.size
        }
    }

    /**
     * Mark nothing need to be drew of the Drawable.
     */
    fun markAllDrew() {
        synchronized(this) {
            mConsumedCount = mPointList.size
        }
    }

    /**
     * Mark the entire drawable is not drew.
     */
    fun markUndrew() {
        synchronized(this) {
            mConsumedCount = 0
        }
    }

    /**
     * @return true if there is canvas update; false no canvas update.
     */
    fun draw(canvas: Canvas,
             startOver: Boolean = false) {
        synchronized(this) {
            println("${AppConst.TAG}: point size=${mPointList.size}, spline size=${mSplineList.size}, consumed count=$mConsumedCount")
            val startIndex = if (startOver) 0 else mConsumedCount


            // Only draw those points not consumed
            for (i in startIndex..mPointList.lastIndex) {
                if (i == 0) {
                    val p = mPointList[i]
                    canvas.drawPoint(p.x, p.y, mStrokePaint)
                } else {
                    val spline = mSplineList[i - 1]

                    // Interpolation
                    for (progress in 0..100) {
                        val percentage = progress.toFloat() / 100.0
                        val x = (1.0 - percentage) * spline.start.x + percentage * spline.end.x
                        canvas.drawPoint(x.toFloat(), spline.f(x).toFloat(), mStrokePaint)
                    }
                }
            }

            // End point for debugging
            if (BuildConfig.DEBUG) {
                mPointList.forEach { p ->
                    canvas.drawPoint(p.x, p.y, mDebugStrokePaint)
                }
            }
        }
    }

    private fun addSpline(point: Point) {
        mPointList.add(point)

        if (mPointList.size > 1) {
            val i = mPointList.lastIndex
            val previous = mPointList[i - 1]
            val current = mPointList[i]
            val spline = if (mPointList.size == 2) {
                val slope = previous.slopeTo(current).toDouble()

                HermiteCubicSplineInterpolator(
                    start = previous,
                    startSlope = slope,
                    end = current,
                    endSlope = slope)
            } else {
                val startSlope = mSplineList[i - 2].endSlope
                val endSlope = previous.slopeTo(current).toDouble()

                HermiteCubicSplineInterpolator(
                    start = previous,
                    startSlope = startSlope,
                    end = current,
                    endSlope = endSlope)
            }

            mSplineList.add(spline)
        }

        mDirtyFlag.markDirty(DirtyType.HASH)
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        synchronized(this) {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SVGDrawable

            if (mPenColor != other.mPenColor) return false
            if (mPenSize != other.mPenSize) return false
            if (mPointList != other.mPointList) return false

            return true
        }
    }

    private var mHashCode = 0

    override fun hashCode(): Int {
        synchronized(this) {
            if (mDirtyFlag.isDirty(DirtyType.HASH)) {
                mHashCode = mPenColor.hashCode()
                mHashCode = 31 * mHashCode + mPenSize.hashCode()
                mHashCode = 31 * mHashCode + mPointList.hashCode()

                mDirtyFlag.markNotDirty(DirtyType.HASH)
            }

            return mHashCode
        }
    }
}
