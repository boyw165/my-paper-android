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

import android.view.ViewConfiguration

/**
 * The context gives the global editor settings and the ability to map point
 * from one coordinate to other coordinate.
 */
interface IPaperContext {

    // Rendering //////////////////////////////////////////////////////////////

    fun getOneDp(): Float

    fun getMinStrokeWidth(): Float

    fun getMaxStrokeWidth(): Float

    /**
     * Map the point observed in the Model world to the View world.
     */
    fun mapM2V(x: Float, y: Float): FloatArray

    // Gesture ////////////////////////////////////////////////////////////////

    fun getViewConfiguration(): ViewConfiguration

    fun getTouchSlop(): Float

    fun getTapSlop(): Float

    fun getMinFlingVec(): Float

    fun getMaxFlingVec(): Float
}
