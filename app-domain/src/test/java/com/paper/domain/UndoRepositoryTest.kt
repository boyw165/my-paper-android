// Copyright May 2018-present Paper
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

import com.paper.domain.ui.UndoRepository
import com.paper.domain.ui.operation.AddScrapOperation
import com.paper.model.IPaper
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner.Silent::class)
class UndoRepositoryTest : BaseDomainTest() {

    @Test
    fun `put operation and see one record`() {
        val tester = UndoRepository(fileDir = File("/tmp"),
                                    schedulers = mockSchedulers)

        // Setup
        tester.start().test().assertSubscribed()

        // Add one particular stroke
        tester.putOperation(AddScrapOperation())

        // Make sure the stream moves
        moveScheduler()

        // Must see one record!
        Assert.assertEquals(1, tester.undoSize)
    }

    @Test
    fun `put operation and undo, should see no record`() {
        val mockPaper = Mockito.mock(IPaper::class.java)

        val tester = UndoRepository(fileDir = File("/tmp"),
                                    schedulers = mockSchedulers)

        val disposables = CompositeDisposable()

        // Setup
        tester.start().subscribe()

        // Add one particular stroke
        tester.putOperation(AddScrapOperation())

        // Make sure the stream moves
        moveScheduler()

        // Undo immediately
        tester.undo(mockPaper)
            .subscribe()
            .addTo(disposables)

        // Make sure the stream moves
        moveScheduler()

        // Must see one record!
        Assert.assertEquals(1, tester.redoSize)

        disposables.clear()
    }
}
