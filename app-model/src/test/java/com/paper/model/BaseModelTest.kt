// Copyright Sep 2018-present Paper
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

package com.paper.model

import java.lang.IllegalStateException
import java.net.URL
import java.util.*

abstract class BaseModelTest {

    private val random = Random()

    protected fun rand(from: Int, to: Int) : Int {
        return random.nextInt(to - from) + from
    }

    protected fun rand(from: Float) : Float {
        return random.nextFloat() + from
    }

    protected fun createRandomFrame(): Frame {
        val scale = rand(1, 5).toFloat()
        return Frame(x = rand(0f),
                     y = rand(0f),
                     width = rand(0, 500).toFloat(),
                     height = rand(0, 500).toFloat(),
                     scaleX = scale,
                     scaleY = scale,
                     rotationInDegrees = rand(0, 360).toFloat(),
                     z = rand(0, 1000))
    }

    protected fun createRandomSVGScrap(): ISVGScrap {
        return SVGScrap(frame = createRandomFrame())
    }

    protected fun createRandomImageScrap(): IImageScrap {
        return ImageScrap(frame = createRandomFrame(),
                          imageURL = URL("http://foo.com/foo.png"))
    }

    protected fun createRandomTextScrap(): ITextScrap {
        return TextScrap(frame = createRandomFrame(),
                         text = "foo")
    }

    protected fun createRandomScrap(): IScrap {
        val random = rand(0, 2)

        return when (random) {
            0 -> createRandomSVGScrap()
            1 -> createRandomImageScrap()
            2 -> createRandomTextScrap()
            else -> throw IllegalStateException()
        }
    }
}
