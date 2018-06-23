// Copyright Jun 2018-present CardinalBlue
//
// Author: boy@cardinalblue.com
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

import com.paper.model.observables.TakeWhenObservable
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class TakeWhenObservableTest {

    @Test
    fun openThrottle_shouldSeeTheItemsAreSentThen() {
        val src = PublishSubject.create<Int>()
        val whenSrc = PublishSubject.create<Boolean>()

        val tester = TakeWhenObservable(src = src,
                                        whenSrc = whenSrc)
            .test()

        src.onNext(0)
        src.onNext(1)
        src.onNext(2)
        src.onNext(3)
        // Open the throttle
        whenSrc.onNext(true)
        // Close the throttle
        whenSrc.onNext(false)
        src.onNext(4)
        src.onNext(5)
        src.onNext(6)
        src.onNext(7)

        tester.assertValues(0, 1, 2, 3)

        // Open the throttle again!
        whenSrc.onNext(true)

        tester.assertValues(0, 1, 2, 3, 4, 5, 6, 7)
    }

    @Test
    fun disposeIt_shouldNotSeeAnyItemsFired() {
        val src = PublishSubject.create<Int>()
        val whenSrc = PublishSubject.create<Boolean>()

        val tester = TakeWhenObservable(src = src,
                                        whenSrc = whenSrc)
            .test()

        // Open the throttle
        whenSrc.onNext(true)
        tester.dispose()
        src.onNext(0)
        src.onNext(1)
        src.onNext(2)
        src.onNext(3)

        tester.assertNoValues()
    }
}
