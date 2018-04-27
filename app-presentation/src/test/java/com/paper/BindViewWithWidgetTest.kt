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

package com.paper

import com.paper.useCase.BindViewWithWidget
import com.paper.view.IWidgetView
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BindViewWithWidgetTest {

    @Test
    fun shouldSeeBindWhenSubscribes() {
        val mockView = Mockito.mock(TypedView::class.java)

        val tester = BindViewWithWidget(view = mockView,
                                        widget = 0)
        val testObserver = tester.test()

        // Must see complete
        testObserver.assertComplete()
        // Must see bindWidget call!
        Mockito.verify(mockView).bindWidget(Mockito.anyInt())
    }

    @Test
    fun shouldSeeUnbindWhenDisposes() {
        val mockView = Mockito.mock(TypedView::class.java)

        val tester = BindViewWithWidget(view = mockView,
                                        widget = 0)
        val testObserver = tester.test()

        testObserver.dispose()

        // Must see bindWidget call!
        Mockito.verify(mockView).unbindWidget()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    interface TypedView : IWidgetView<Int>
}
