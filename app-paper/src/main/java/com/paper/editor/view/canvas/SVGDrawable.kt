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

package com.paper.editor.view.canvas

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.paper.editor.bezier.Bezier
import com.paper.editor.bezier.ControlTimedPoints
import com.paper.editor.bezier.TimedPoint
import com.paper.shared.model.Point
import java.util.ArrayList

class SVGDrawable(oneDp: Float) {

    private val mOneDp = oneDp

    private val mPath = Path()
    private val mStrokePoint = mutableListOf<Point>()
    private val mStrokeWidth = mutableListOf<Float>()
    private val mStrokePaint = Paint()

    // Bezier
    private var mPoints: MutableList<TimedPoint> = mutableListOf()
    private val mControlTimedPointsCached = ControlTimedPoints()
    private val mPointsCache = ArrayList<TimedPoint>()
    private val mBezierCached = Bezier()
    private var mVelocityFilterWeight: Float = 0.toFloat()
    private var mLastWidth: Float = 0.toFloat()
    private var mLastVelocity: Float = 0.toFloat()
    private var mMinWidth: Float = 0f
    private var mMaxWidth: Float = 0f

    init {
        mStrokePaint.strokeWidth = mOneDp
        mStrokePaint.color = Color.BLACK
        mStrokePaint.style = Paint.Style.STROKE
        mStrokePaint.strokeCap = Paint.Cap.ROUND

        mMinWidth = 3f * oneDp
        mMaxWidth = 9f * oneDp
        mVelocityFilterWeight = 0.9f
    }

    fun clear() {
        TODO()
    }

    fun moveTo(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        addPoint(getNewPoint(x, y))
    }

    fun lineTo(x: Float, y: Float) {
        addPoint(getNewPoint(x, y))
    }

    fun close() {
        // DO NOTHING
    }

    fun onDraw(canvas: Canvas) {
        mStrokePoint.forEachIndexed { index, point ->
            mStrokePaint.strokeWidth = mStrokeWidth[index]
            canvas.drawPoint(point.x, point.y, mStrokePaint)
        }
    }

    // Start of Bezier functions
    private fun addPoint(newPoint: TimedPoint) {
        mPoints.add(newPoint)

        val pointsCount = mPoints.size
        if (pointsCount > 3) {

            var tmp = calculateCurveControlPoints(mPoints[0], mPoints[1], mPoints[2])
            val c2 = tmp.c2
            recyclePoint(tmp.c1)

            tmp = calculateCurveControlPoints(mPoints[1], mPoints[2], mPoints[3])
            val c3 = tmp.c1
            recyclePoint(tmp.c2)

            val curve = mBezierCached.set(mPoints[1], c2, c3, mPoints[2])

            val startPoint = curve.startPoint
            val endPoint = curve.endPoint

            var velocity = endPoint.velocityFrom(startPoint)
            velocity = if (java.lang.Float.isNaN(velocity)) 0.0f else velocity

            velocity = mVelocityFilterWeight * velocity + (1 - mVelocityFilterWeight) * mLastVelocity

            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            val newWidth = strokeWidth(velocity)

            // The Bezier's width starts out as last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end mPoints.
            addBezier(curve, mLastWidth, newWidth)

            mLastVelocity = velocity
            mLastWidth = newWidth

            // Remove the first element from the list,
            // so that we always have no more than 4 mPoints in mPoints array.
            recyclePoint(mPoints.removeAt(0))

            recyclePoint(c2)
            recyclePoint(c3)

        } else if (pointsCount == 1) {
            // To reduce the initial lag make it work with 3 mPoints
            // by duplicating the first point
            val firstPoint = mPoints[0]
            mPoints.add(getNewPoint(firstPoint.x, firstPoint.y))
        }
    }

    private fun calculateCurveControlPoints(s1: TimedPoint, s2: TimedPoint, s3: TimedPoint): ControlTimedPoints {
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

        return mControlTimedPointsCached.set(getNewPoint(m1X + tx, m1Y + ty), getNewPoint(m2X + tx, m2Y + ty))
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
            mStrokePoint.add(Point(x,y))

            i++
        }
    }

    private fun recyclePoint(point: TimedPoint) {
        mPointsCache.add(point)
    }

    private fun strokeWidth(velocity: Float): Float {
        return Math.max(mMaxWidth / (velocity + 1), mMinWidth.toFloat())
    }

    private fun getNewPoint(x: Float, y: Float): TimedPoint {
        val mCacheSize = mPointsCache.size
        val timedPoint: TimedPoint
        timedPoint = if (mCacheSize == 0) {
            // Cache is empty, create a new point
            TimedPoint()
        } else {
            // Get point from cache
            mPointsCache.removeAt(mCacheSize - 1)
        }

        return timedPoint.set(x, y)
    }
}
