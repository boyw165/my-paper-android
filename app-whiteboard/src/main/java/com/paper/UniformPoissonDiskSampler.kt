// Copyright Feb 2016-present boyw165@gmail.com
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

package com.paper

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF

import java.util.ArrayList

/**
 * An algorithm to populate large worlds with objects is to simply place objects
 * on a grid, or randomly.
 *
 * The basic idea is:
 * 1) Generate points around existing points and check whether they can be added
 * so that they won't disturb the minimum distance requirement.
 * 2) A grid is used to perform fast lookups of points.
 * 3) Two lists keep track of points that being generated, and those that needs
 * processing.
 *
 * Author: boy@cardinalblue.com, prada@cardinalblue.com
 *
 * Reference:
 * - http://bl.ocks.org/mbostock/19168c663618b7f07158
 * - http://devmag.org.za/2009/05/03/poisson-disk-sampling/
 */
class UniformPoissonDiskSampler(private val mCanvasRect: RectF,
                                private val mMinDist: Double,
                                private val mInitPoint: PointF) {

    private val mCellSize: Double
    private val mGridWidth: Int
    private val mGridHeight: Int

    /**
     * List of points that being processed.
     */
    private val mProcessList: MutableList<PointF>
    /**
     * List of points that being generated.
     */
    private val mPointList: MutableList<PointF>
    /**
     * Grid is used to perform fast lookups of points.
     */
    private val mGridList: Array<Array<PointF?>>

    init {
        if (mCanvasRect.width() <= mMinDist || mCanvasRect.height() <= mMinDist) {
            throw IllegalArgumentException(
                "The given minimum distance is bigger than width/height of given canvas size.")
        }
        if (mInitPoint.x < mCanvasRect.left || mInitPoint.x > mCanvasRect.right ||
            mInitPoint.y < mCanvasRect.top || mInitPoint.y > mCanvasRect.bottom) {
            throw IllegalArgumentException("Init point is outside the canvas boundary.")
        }

        mCellSize = mMinDist / Math.sqrt(2.0)
        mGridWidth = (mCanvasRect.width() / mCellSize + 1).toInt()
        mGridHeight = (mCanvasRect.height() / mCellSize + 1).toInt()
        mProcessList = ArrayList()
        mPointList = ArrayList()
        mGridList = Array(mGridWidth, { arrayOfNulls<PointF?>(mGridHeight) })

        // Empty grids.
        for (i in 0 until mGridWidth) {
            for (j in 0 until mGridHeight) {
                mGridList[i][j] = null
            }
        }
    }

    /**
     * Generate a candidate point.
     */
    fun sample(): PointF? {
        // Generate first point.
        if (mPointList.size == 0) {
            val firstPoint = mInitPoint
            val gridIndex = pointToGrid(firstPoint)

            mGridList[gridIndex.x][gridIndex.y] = firstPoint
            mProcessList.add(firstPoint)
            mPointList.add(firstPoint)

            return firstPoint
        }

        // Find the candidate point around the points in active list.
        while (!mProcessList.isEmpty()) {
            val centerIndex = Math.floor(mProcessList.size * Math.random()).toInt()
            val center = mProcessList[centerIndex]

            // Find valid new point.
            for (i in 0 until MAX_TRY_TIMES) {
                // TODO: Can Math.random() return non-duplicate result?
                val radius = mMinDist + mMinDist * Math.random()
                val angle = 2.0 * Math.PI * Math.random()
                val newX = radius * Math.sin(angle)
                val newY = radius * Math.cos(angle)
                val newPoint = PointF(center.x + newX.toFloat(),
                                      center.y + newY.toFloat())
                val gridIndex = pointToGrid(newPoint)

                if (mCanvasRect.left <= newPoint.x && newPoint.x <= mCanvasRect.right &&
                    mCanvasRect.top <= newPoint.y && newPoint.y <= mCanvasRect.bottom) {
                    var isOk = true

                    // +---+---+---+---+---+---+---+
                    // |   |   |   |   |   |   |   |
                    // +---+---+---+---+---+---+---+
                    // |   |   |///|///|///|   |   |
                    // +---+---+---+---+---+---+---+
                    // |   |///|///|///|///|///|   |
                    // +---+---+---+---+---+---+---+
                    // |   |///|///| C |///|///|   |
                    // +---+---+---+---+---+---+---+
                    // |   |///|///|///|///|///|   |
                    // +---+---+---+---+---+---+---+
                    // |   |   |///|///|///|   |   |
                    // +---+---+---+---+---+---+---+
                    // |   |   |   |   |   |   |   |
                    // +---+---+---+---+---+---+---+
                    // Check the points in the region (if any) around a center point, C. Make sure they don't
                    // disturb the minimum distance requirement.
                    for (j in Math.max(0, gridIndex.x - 2) until Math.min(mGridWidth, gridIndex.x + 3)) {
                        for (k in Math.max(0, gridIndex.y - 2) until Math.min(mGridHeight, gridIndex.y + 3)) {
                            val gridPoint = mGridList[j][k]

                            if (gridPoint != null && distanceBetween(gridPoint, newPoint) < mMinDist) {
                                isOk = false
                            }
                        }

                        if (!isOk) break
                    }

                    if (isOk) {
                        mGridList[gridIndex.x][gridIndex.y] = newPoint

                        mProcessList.add(newPoint)
                        mPointList.add(newPoint)

                        return newPoint
                    }
                }
            }

            mProcessList.removeAt(centerIndex)
        }

        return null
    }

    /**
     * Find out all the points populated in the world.
     */
    fun sampleAll(): List<PointF> {
        for (i in 0..2) {
            while (sample() != null);
        }

        return mPointList
    }

    /**
     * Clear the sample result.
     */
    fun clear() {
        mProcessList.clear()
        mPointList.clear()

        // Empty grids.
        for (i in 0 until mGridWidth) {
            for (j in 0 until mGridHeight) {
                mGridList[i][j] = null
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private ////////////////////////////////////////////////////////////////

    /**
     * Find the position of a given point in the grid.
     *
     * @param pt The given point that being to lookup in the grid list.
     */
    private fun pointToGrid(pt: PointF): Point {
        return Point(((pt.x - mCanvasRect.left) / mCellSize).toInt(),
                     ((pt.y - mCanvasRect.top) / mCellSize).toInt())
    }

    companion object {
        /**
         * Maximum number of samples before rejection
         */
        private val MAX_TRY_TIMES = 30

//        /**
//         * Policy for generating the initial point.
//         */
//        val RANDOM_INIT_POINT = 0x1001
//        val CENTER_INIT_POINT = 0x1002
//        val SPECIFY_INIT_POINT = 0x1003

        fun distanceBetween(pointA: PointF, pointB: PointF): Double {
            return Math.sqrt(Math.pow((pointA.x - pointB.x).toDouble(), 2.0) + Math.pow((pointA.y - pointB.y).toDouble(), 2.0))
        }
    }
}
