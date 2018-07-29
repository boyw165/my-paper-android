// Copyright Jul 2018-present Paper
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

import com.paper.model.event.DirtyEvent
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class DirtyFlagTest {

    @Test
    fun equality() {
        Assert.assertEquals(DirtyEvent(flag = 0,
                                       changedType = 0),
                            DirtyEvent(flag = 0,
                                       changedType = 0))
        Assert.assertNotEquals(DirtyEvent(flag = DirtyType.HASH,
                                          changedType = 0),
                               DirtyEvent(flag = 0,
                                          changedType = 0))
    }

    @Test
    fun dirtyHash() {
        val tester = DirtyFlag(flag = DirtyType.HASH)

        Assert.assertTrue(tester.isDirty(DirtyType.HASH))

        tester.markNotDirty(DirtyType.HASH)

        Assert.assertFalse(tester.isDirty(DirtyType.HASH))
    }

    @Test
    fun dirtyOther() {
        val tester = DirtyFlag(flag = DirtyType.PATH)

        Assert.assertFalse(tester.isDirty(DirtyType.HASH))
    }

    @Test
    fun dirtyObservable() {
        val tester = DirtyFlag(flag = 0)
        val testObserver = tester
            .onUpdate()
            .test()

        tester.markDirty(DirtyType.HASH)
        tester.markNotDirty(DirtyType.HASH)

        testObserver.assertValues(DirtyEvent(flag = DirtyType.HASH,
                                             changedType = DirtyType.HASH),
                                  DirtyEvent(flag = 0,
                                             changedType = DirtyType.HASH))
    }

    @Test
    fun dirtyObservableByTypes() {
        val tester = DirtyFlag(flag = 0)
        val testObserver = tester
            .onUpdate(DirtyType.HASH,
                      DirtyType.PATH)
            .test()

        tester.markDirty(DirtyType.TRANSFORM)
        tester.markDirty(DirtyType.HASH)
        tester.markDirty(DirtyType.PATH)

        testObserver.assertValues(DirtyEvent(flag = DirtyType.HASH,
                                             changedType = DirtyType.HASH),
                                  DirtyEvent(flag = DirtyType.HASH.or(DirtyType.PATH),
                                             changedType = DirtyType.PATH))
    }

    @Test
    fun dirtyAsyncObservable() {
        val tester = DirtyFlag(flag = 0)
        val testScheduler = TestScheduler()
        val testObserver = tester
            .onUpdate()
            .observeOn(testScheduler)
            .test()

        tester.markDirty(DirtyType.HASH)
        tester.markNotDirty(DirtyType.HASH)
        testScheduler.triggerActions()

        testObserver.assertValues(DirtyEvent(flag = DirtyType.HASH,
                                             changedType = DirtyType.HASH),
                                  DirtyEvent(flag = 0,
                                             changedType = DirtyType.HASH))
    }
}

