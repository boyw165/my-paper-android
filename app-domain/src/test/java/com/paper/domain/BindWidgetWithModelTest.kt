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

package com.paper.domain

import com.paper.domain.useCase.BindWidgetWithModel
import com.paper.domain.widget.editor.IWidget
import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BindWidgetWithModelTest {

    @Test
    fun shouldSeeBindWhenSubscribes() {
        val mockWidget = Mockito.mock(TypedWidget::class.java)

        val tester = BindWidgetWithModel(widget = mockWidget,
                                         model = 0)
        val testObserver = tester.test()

        // Must see complete
        testObserver.assertComplete()
        // Must see bind call!
        Mockito.verify(mockWidget).bindModel(Mockito.anyInt())

        testObserver.dispose()
    }

    @Test
    fun shouldSeeUnbindWhenDisposes() {
        val mockWidget = Mockito.mock(TypedWidget::class.java)

        val tester = BindWidgetWithModel(widget = mockWidget,
                                         model = 0)
        val testObserver = tester.test()

        testObserver.dispose()

        // Must see unbind call!
        Mockito.verify(mockWidget).unbindModel()
    }

    @Test
    fun shouldSeeNestedUnbindWhenDisposes() {
        val mockWidget = Mockito.mock(TypedWidget::class.java)

        val tester = BindWidgetWithModel(widget = mockWidget,
                                         model = 0)
        val testObserver = Single
            .just(0)
            .flatMap {
                Single
                    .just(1)
                    .flatMap {
                        tester
                    }
            }
            .test()

        testObserver.dispose()

        // Must see unbind call!
        Mockito.verify(mockWidget).unbindModel()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    interface TypedWidget : IWidget<Int>
}
