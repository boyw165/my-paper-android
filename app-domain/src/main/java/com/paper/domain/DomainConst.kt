// Copyright Mar 2018-present boyw165@gmail.com
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

package com.paper.domain

import com.paper.model.Frame

object DomainConst {

    // Common /////////////////////////////////////////////////////////////////

    @JvmStatic
    val TAG = "whiteboard domain"

    @JvmStatic
    val BUSY = 1.shl(0)

    // Whiteboard /////////////////////////////////////////////////////////////

    @JvmStatic
    val EMPTY_FRAME_DISPLACEMENT = Frame(x = 0f,
                                         y = 0f,
                                         width = 0f,
                                         height = 0f,
                                         scaleX = 0f,
                                         scaleY = 0f,
                                         rotationInDegrees = 0f,
                                         z = 0)

    @JvmStatic
    val BASE_THUMBNAIL_WIDTH = 640f
    @JvmStatic
    val BASE_THUMBNAIL_HEIGHT = 480f

    @JvmStatic
    val BASE_HD_WIDTH = 1920f
    @JvmStatic
    val BASE_HD_HEIGHT = 1080f

    @JvmStatic
    val VIEW_PORT_MIN_SCALE = 32f

    @JvmStatic
    val COLLECT_PATH_WINDOW_MS = 66L
    @JvmStatic
    val COLLECT_STROKES_TIMEOUT_MS = 850L
}
