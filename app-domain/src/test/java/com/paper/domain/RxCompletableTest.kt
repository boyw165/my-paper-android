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

import io.reactivex.Completable
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class RxCompletableTest : BaseTest() {

    @Test
    fun `auto dispose test`() {
        var seeDisposed = false
        val tester = Completable
            .create { downstream ->
                downstream.setCancellable {
                    seeDisposed = true
                }
            }

        val testObserver = tester.test()
        testObserver.dispose()

        Assert.assertTrue(seeDisposed)
    }

    @Test
    fun `no completion test`() {
        val tester = Completable
            .create { _ ->
                // DO NOTHING
            }

        val testObserver = tester.test()

        testObserver.assertNotComplete()
    }

    @Test
    fun `completion test`() {
        val tester = Completable
            .create { downstream ->
                if (downstream.isDisposed) return@create

                downstream.onComplete()
            }

        val testObserver = tester.test()

        testObserver.assertComplete()
        testObserver.assertTerminated()
        Assert.assertFalse(testObserver.isDisposed)
    }
}
