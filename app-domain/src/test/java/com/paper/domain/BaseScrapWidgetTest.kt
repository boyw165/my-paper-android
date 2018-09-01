// Copyright Aug 2018-present Paper
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

package com.paper.domain

import com.paper.domain.ui.BaseScrapWidget
import com.paper.model.Frame
import io.reactivex.Observable
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class BaseScrapWidgetTest : BaseDomainTest() {

    companion object {

        val DefaultFrame = Frame(x = 100f,
                                 y = 100f,
                                 width = 100f,
                                 height = 100f,
                                 scaleX = 1f,
                                 scaleY = 1f,
                                 rotationInDegrees = 0f,
                                 z = 0)
    }

    @Test
    fun `update frame with a sequence of displacement`() {
        val candidate = BaseScrapWidget(scrap = createBaseScrapBy(DefaultFrame),
                                        schedulers = mockSchedulers)
        // Start widget
        candidate.start().test().assertSubscribed()

        val frameObserverTester = candidate.observeFrame().test()

        // Move toward to positive direction of the x axis
        candidate.handleFrameDisplacement(
            Observable.fromArray(Frame(x = 1f,
                                       y = 1f),
                                 Frame(x = 2f,
                                       y = 1f),
                                 Frame(x = 3f,
                                       y = 1f),
                                 Frame(x = 4f,
                                       y = 1f)))

        // Make sure the stream moves
        moveScheduler()

        // The observer must see the sequence
        frameObserverTester.assertValues(
            DefaultFrame.add(Frame(x = 1f,
                                   y = 1f)),
            DefaultFrame.add(Frame(x = 2f,
                                   y = 1f)),
            DefaultFrame.add(Frame(x = 3f,
                                   y = 1f)),
            DefaultFrame.add(Frame(x = 4f,
                                   y = 1f)),
            DefaultFrame.add(Frame(x = 4f,
                                   y = 1f)))
    }
}
