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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paper.model.command.WhiteboardCommand
import com.paper.model.command.WhiteboardCommandJSONTranslator
import com.paper.model.repository.json.FrameJSONTranslator
import com.paper.model.repository.json.WhiteboardJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.sketch.VectorGraphics
import io.reactivex.schedulers.TestScheduler
import org.mockito.Mockito
import java.lang.IllegalStateException
import java.net.URL
import java.util.*

abstract class BaseModelTest {

    private val testScheduler = TestScheduler()
    protected val mockSchedulers: ISchedulers by lazy {
        val mock = Mockito.mock(ISchedulers::class.java)
        Mockito.`when`(mock.main()).thenReturn(testScheduler)
        Mockito.`when`(mock.computation()).thenReturn(testScheduler)
        Mockito.`when`(mock.io()).thenReturn(testScheduler)
        Mockito.`when`(mock.db()).thenReturn(testScheduler)
        mock
    }

    protected fun moveScheduler() {
        testScheduler.triggerActions()
    }

    protected val jsonTranslator: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Whiteboard::class.java,
                                 WhiteboardJSONTranslator())
            .registerTypeAdapter(Scrap::class.java,
                                 ScrapJSONTranslator())
            .registerTypeAdapter(Frame::class.java,
                                 FrameJSONTranslator())
            .registerTypeAdapter(VectorGraphics::class.java,
                                 VectorGraphicsJSONTranslator())
            .registerTypeAdapter(WhiteboardCommand::class.java,
                                 WhiteboardCommandJSONTranslator())
            .create()
    }

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

    protected fun createRandomSVG(): VectorGraphics {
        return VectorGraphics(tupleList = mutableListOf(LinearPointTuple(0f, 0f),
                                                        CubicPointTuple(10f, 10f, 10f, 10f, 20f, 0f),
                                                        CubicPointTuple(-30f, -30f, -30f, -30f, 40f, 0f),
                                                        CubicPointTuple(50f, 50f, 50f, 50f, 40f, 20f)))
    }

    protected fun createRandomSVGScrap(): SketchScrap {
        return SketchScrap(frame = createRandomFrame(),
                           svg = createRandomSVG())
    }

    protected fun createRandomImageScrap(): ImageScrap {
        return ImageScrap(frame = createRandomFrame(),
                          imageURL = URL("http://foo.com/foo.png"))
    }

    protected fun createRandomTextScrap(): TextScrap {
        return TextScrap(frame = createRandomFrame(),
                         text = "foo")
    }

    protected fun createRandomScrap(): Scrap {
        val random = rand(0, 2)

        return when (random) {
            0 -> createRandomSVGScrap()
            1 -> createRandomImageScrap()
            2 -> createRandomTextScrap()
            else -> throw IllegalStateException()
        }
    }
}
