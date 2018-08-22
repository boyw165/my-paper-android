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

import com.paper.domain.useCase.StartWidgetAutoStopObservable
import com.paper.domain.vm.PaperHistoryWidget
import com.paper.model.BasePaper
import com.paper.model.operation.AddScrapOperation
import com.paper.model.repository.PaperCanvasOperationRepoFileLRUImpl
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class PaperHistoryWidgetTest {

    @Test
    fun `put operation and see one record`() {
        val historyRepo = PaperCanvasOperationRepoFileLRUImpl(fileDir = File("/tmp"))

        val testScheduler = TestScheduler()
        val mockSchedulers = Mockito.mock(ISchedulerProvider::class.java)
//        Mockito.`when`(mockSchedulers.main()).thenReturn(testScheduler)
//        Mockito.`when`(mockSchedulers.computation()).thenReturn(testScheduler)
//        Mockito.`when`(mockSchedulers.io()).thenReturn(testScheduler)
//        Mockito.`when`(mockSchedulers.db()).thenReturn(testScheduler)

        val disposables = CompositeDisposable()

        val tester = PaperHistoryWidget(
            historyRepo = historyRepo,
            schedulers = mockSchedulers)

        // Setup
        disposables.add(
            StartWidgetAutoStopObservable(tester)
                .subscribe())

        // Add one particular stroke
        tester.putOperation(AddScrapOperation())
        testScheduler.triggerActions()

        // Must see one record!
        Assert.assertEquals(1, historyRepo.recordSize)

        disposables.clear()
    }

    @Test
    fun undo_shouldSeeNoStrokeInPaper() {
        val paper = BasePaper()
        val historyRepo = PaperCanvasOperationRepoFileLRUImpl(fileDir = File("/tmp"))
        val testScheduler = TestScheduler()
        val mockSchedulers = Mockito.mock(ISchedulerProvider::class.java)
//        Mockito.`when`(mockSchedulers.main()).thenReturn(testScheduler)
//        Mockito.`when`(mockSchedulers.computation()).thenReturn(testScheduler)
//        Mockito.`when`(mockSchedulers.io()).thenReturn(testScheduler)
//        Mockito.`when`(mockSchedulers.db()).thenReturn(testScheduler)

        val disposables = CompositeDisposable()

        val tester = PaperHistoryWidget(
            historyRepo = historyRepo,
            schedulers = mockSchedulers)

        // Setup
        disposables.add(
            StartWidgetAutoStopObservable(tester)
                .subscribe())

        // Add one particular stroke
        tester.putOperation(AddScrapOperation())
        testScheduler.triggerActions()
        // Undo immediately
        disposables.add(
            tester.undo(paper)
                .subscribe())
        testScheduler.triggerActions()

        // Must see one record!
        Assert.assertEquals(1, historyRepo.recordSize)

        disposables.clear()
    }
}
