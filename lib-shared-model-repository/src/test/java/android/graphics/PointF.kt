/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.graphics

/**
 * Mocked [android.graphics.PointF].
 */
class PointF constructor(
    var x: Float = 0f,
    var y: Float = 0f) {

    constructor(p: PointF) : this(p.x, p.y)

    /**
     * Set the point's x and y coordinates
     */
    operator fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    /**
     * Set the point's x and y coordinates to the coordinates of p
     */
    fun set(p: PointF) {
        this.x = p.x
        this.y = p.y
    }

    fun negate() {
        x = -x
        y = -y
    }

    fun offset(dx: Float, dy: Float) {
        x += dx
        y += dy
    }

    override fun toString(): String {
        return "PointF($x, $y)"
    }

    /**
     * Return the euclidian distance from (0,0) to the point
     */
    fun length(): Float {
        return length(x, y)
    }

    companion object {

        /**
         * Returns the euclidian distance from (0,0) to (x,y)
         */
        fun length(x: Float, y: Float): Float {
            return Math.hypot(x.toDouble(), y.toDouble()).toFloat()
        }
    }
}
