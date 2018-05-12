// Copyright Apr 2018-present boyw165@gmail.com
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

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.paper.AppConst
import com.paper.domain.data.Bezier
import com.paper.model.Point

class SVGDrawable(context: IPaperContext,
                  penColor: Int = 0,
                  penSize: Float = 1f) {

    private val mContext = context
    private val mPenColor = penColor
    private val mPenSize = penSize

    private val mPointMap = FloatArray(2)

    private var mConsumedPointCount = 0
    private val mStrokePoint = mutableListOf<Point>()
    private val mStrokePointTransformed = mutableListOf<Point>()
    private val mStrokeWidth = mutableListOf<Float>()
    private val mStrokePaint = Paint()

    // Bezier
    private var mCachedPoints: MutableList<Point> = mutableListOf()
    private val mBezierCached = Bezier()
    private var mVelocityFilterWeight: Float = 0.toFloat()
    private var mLastWidth: Float = 0.toFloat()
    private var mLastVelocity: Float = 0.toFloat()
    private var mMinWidth: Float = 0f
    private var mMaxWidth: Float = 0f

    init {
        val paintSize = getPaintSize(mPenSize)

        mStrokePaint.strokeWidth = paintSize
        mStrokePaint.color = mPenColor
        mStrokePaint.style = Paint.Style.FILL_AND_STROKE
        mStrokePaint.strokeCap = Paint.Cap.ROUND

        println("${AppConst.TAG}: SVGDrawable(size=$mPenSize, color=#${Integer.toHexString(mPenColor)})")

        mMinWidth = 1f * paintSize
        mMaxWidth = 2.5f * paintSize
        mVelocityFilterWeight = 0.9f
    }

    private fun getPaintSize(penSize: Float): Float {
        return (1 - penSize) * mContext.getMinStrokeWidth() +
               penSize * mContext.getMaxStrokeWidth()
    }

    // Drawing ////////////////////////////////////////////////////////////////

    fun clear() {
        mCachedPoints.clear()

        mStrokePoint.clear()
        mStrokeWidth.clear()
        mStrokePointTransformed.clear()
    }

    fun moveTo(point: Point) {
//        // Try #1: simple points
//        mStrokePoint.add(Point(x, y))
//        mStrokePointTransformed.add(Point(x, y))

        // Try #2: Bezier points
//        addPoint(getNewPoint(x, y))

        // Try #3
        addPoint(point)
    }

    fun lineTo(point: Point) {
//        // Try #1: simple points
//        mStrokePoint.add(Point(x, y))
//        mStrokePointTransformed.add(Point(x, y))

        // Try #2: Bezier points
//        addPoint(getNewPoint(x, y))

        // Try #3
        addPoint(point)
    }

    fun close() {
        // DO NOTHING
    }

    fun onDraw(canvas: Canvas,
               transform: Matrix? = null) {
        if (transform != null) {
            mStrokePointTransformed.forEachIndexed { i, point ->
                mPointMap[0] = mStrokePoint[i].x
                mPointMap[1] = mStrokePoint[i].y
                transform.mapPoints(mPointMap)
                point.x = mPointMap[0]
                point.y = mPointMap[1]
            }

            mStrokePointTransformed.forEachIndexed { i, point ->
                mStrokePaint.strokeWidth = mStrokeWidth[i]
                canvas.drawPoint(point.x, point.y, mStrokePaint)
            }
        } else {
            // Only draw those points not consumed
            val newPoints = mStrokePoint.subList(mConsumedPointCount, mStrokePoint.size)
            newPoints.forEachIndexed { i, point ->
                mStrokePaint.strokeWidth = mStrokeWidth[i + mConsumedPointCount]
                canvas.drawPoint(point.x, point.y, mStrokePaint)
            }

            // Update consumed number
            mConsumedPointCount = mStrokePoint.size
        }
    }

    // Start of Bezier functions
    private fun addPoint(newPoint: Point) {
        mCachedPoints.add(newPoint)

        val pointsCount = mCachedPoints.size
        if (pointsCount > 3) {
            val (_, c2) = calculateCurveControlPoints(mCachedPoints[0], mCachedPoints[1], mCachedPoints[2])
            val (c3, _) = calculateCurveControlPoints(mCachedPoints[1], mCachedPoints[2], mCachedPoints[3])

            val curve = mBezierCached.set(mCachedPoints[1], c2, c3, mCachedPoints[2])

            val startPoint = curve.startPoint
            val endPoint = curve.endPoint

            var velocity = endPoint.velocityFrom(startPoint)
            velocity = if (java.lang.Float.isNaN(velocity)) 0.0f else velocity

            velocity = mVelocityFilterWeight * velocity + (1 - mVelocityFilterWeight) * mLastVelocity

            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            val newWidth = strokeWidthWith(velocity)

            // The Bezier's width starts out as last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end mCachedPoints.
            addBezier(curve, mLastWidth, newWidth)

            mLastVelocity = velocity
            mLastWidth = newWidth

            // Remove the first element from the list,
            // so that we always have no more than 4 mCachedPoints in mCachedPoints array.
            mCachedPoints.removeAt(0)
        } else if (pointsCount == 1) {
            // To reduce the initial lag make it work with 3 mCachedPoints
            // by duplicating the first point
            val firstPoint = mCachedPoints[0]
            mCachedPoints.add(firstPoint)
        }
    }

    private fun calculateCurveControlPoints(s1: Point, s2: Point, s3: Point): Pair<Point, Point> {
        val dx1 = s1.x - s2.x
        val dy1 = s1.y - s2.y
        val dx2 = s2.x - s3.x
        val dy2 = s2.y - s3.y

        val m1X = (s1.x + s2.x) / 2.0f
        val m1Y = (s1.y + s2.y) / 2.0f
        val m2X = (s2.x + s3.x) / 2.0f
        val m2Y = (s2.y + s3.y) / 2.0f

        val l1 = Math.sqrt((dx1 * dx1 + dy1 * dy1).toDouble()).toFloat()
        val l2 = Math.sqrt((dx2 * dx2 + dy2 * dy2).toDouble()).toFloat()

        val dxm = m1X - m2X
        val dym = m1Y - m2Y
        var k = l2 / (l1 + l2)
        if (java.lang.Float.isNaN(k)) k = 0.0f
        val cmX = m2X + dxm * k
        val cmY = m2Y + dym * k

        val tx = s2.x - cmX
        val ty = s2.y - cmY

        return Pair(getNewPoint(m1X + tx, m1Y + ty), getNewPoint(m2X + tx, m2Y + ty))
    }

    private fun addBezier(curve: Bezier, startWidth: Float, endWidth: Float) {
        val widthDelta = endWidth - startWidth
        val drawSteps = Math.floor(curve.length().toDouble()).toFloat()

        var i = 0
        while (i < drawSteps) {
            // Calculate the Bezier (x, y) coordinate for this step.
            val t = i.toFloat() / drawSteps
            val tt = t * t
            val ttt = tt * t
            val u = 1 - t
            val uu = u * u
            val uuu = uu * u

            var x = uuu * curve.startPoint.x
            x += 3 * uu * t * curve.control1.x
            x += 3 * u * tt * curve.control2.x
            x += ttt * curve.endPoint.x

            var y = uuu * curve.startPoint.y
            y += 3 * uu * t * curve.control1.y
            y += 3 * u * tt * curve.control2.y
            y += ttt * curve.endPoint.y

            // Set the incremental stroke width and draw.
            mStrokeWidth.add(startWidth + ttt * widthDelta)
            mStrokePoint.add(Point(x, y))
            mStrokePointTransformed.add(Point(x, y))

            i++
        }
    }

    private fun strokeWidthWith(velocity: Float): Float {
        return Math.max(mMaxWidth / (velocity + 1), mMinWidth)
    }

    private fun getNewPoint(x: Float, y: Float): Point {
        return Point(x, y)
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SVGDrawable

        if (mPenColor != other.mPenColor) return false
        if (mPenSize != other.mPenSize) return false
        if (mStrokePoint != other.mStrokePoint) return false
        if (mStrokeWidth != other.mStrokeWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mPenColor
        result = 31 * result + mPenSize.hashCode()
        result = 31 * result + mStrokeWidth.hashCode()

        mStrokePoint.forEach { p ->
            result = 31 * result + p.hashCode()
        }

        return result
    }
}
