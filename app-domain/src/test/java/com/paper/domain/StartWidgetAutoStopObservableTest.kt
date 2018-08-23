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

import com.paper.domain.action.StartWidgetAutoStopObservable
import com.paper.domain.vm.IWidget
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StartWidgetAutoStopObservableTest {

    @Test
    fun `subscribe observable should see widget start`() {
        val mockWidget = Mockito.mock(TypedWidget::class.java)

        val tester = StartWidgetAutoStopObservable(mockWidget)
        val testObserver = tester.test()

        // Must see true
        testObserver.assertValue(true)
        // Must see start call!
        Mockito.verify(mockWidget).start()

        testObserver.dispose()
    }

    @Test
    fun `dispose observable should see widget stop`() {
        val mockWidget = Mockito.mock(TypedWidget::class.java)

        val tester = StartWidgetAutoStopObservable(mockWidget)
        val testObserver = tester.test()

        testObserver.dispose()

        // Must see stop call!
        Mockito.verify(mockWidget).stop()
    }

    @Test
    fun `subscribe observable in nested graph should see widget start`() {
        val mockWidget = Mockito.mock(TypedWidget::class.java)

        val tester = StartWidgetAutoStopObservable(mockWidget)
        // Put the tester in a random nested RX graph
        val testObserver = Observable
            .just(0)
            .switchMap { _ ->
                Observable
                    .just(1)
                    .switchMap {
                        tester
                    }
            }
            .test()

        testObserver.dispose()

        // Must see stop call!
        Mockito.verify(mockWidget).start()
    }

    @Test
    fun `dispose observable in nested graph should see widget stop`() {
        val mockWidget = Mockito.mock(TypedWidget::class.java)

        val tester = StartWidgetAutoStopObservable(mockWidget)
        // Put the tester in a random nested RX graph
        val disposables = CompositeDisposable()
        disposables.add(
            Observable
                .just(0)
                .switchMap { _ ->
                    Observable
                        .just(1)
                        .switchMap {
                            tester
                        }
                }
                .subscribe())

        disposables.clear()

        // Must see stop call!
        Mockito.verify(mockWidget).stop()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    interface TypedWidget : IWidget
}
