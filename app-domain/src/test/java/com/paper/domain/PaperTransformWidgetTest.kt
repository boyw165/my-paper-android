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

import com.paper.domain.useCase.BindWidgetWithModel
import com.paper.domain.widget.editor.PaperTransformWidget
import com.paper.model.PaperAutoSaveImpl
import com.paper.model.Point
import com.paper.model.repository.PaperTransformRepoFileImpl
import com.paper.model.sketch.VectorGraphics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class PaperTransformWidgetTest {

    private val mTestStroke1 = VectorGraphics()
    private val mTestStroke2 = VectorGraphics()
    private val mTestStroke3 = VectorGraphics()

    init {
        mTestStroke1.addTuple(Point(x = 1f, y = 1f))

        mTestStroke2.addTuple(Point(x = 1f, y = 1f))
        mTestStroke2.addTuple(Point(x = 2f, y = 2f))

        mTestStroke3.addTuple(Point(x = 1f, y = 1f))
        mTestStroke3.addTuple(Point(x = 2f, y = 2f))
        mTestStroke3.addTuple(Point(x = 3f, y = 3f))
    }

    @Test
    fun addOneStrokeToPaper_TransformRepoShouldRecordIt() {
        val paper = PaperAutoSaveImpl()
        val historyRepo = PaperTransformRepoFileImpl(fileDir = File("/tmp"))
        val testScheduler = TestScheduler()
        val disposables = CompositeDisposable()

        val tester = PaperTransformWidget(
            historyRepo = historyRepo,
            uiScheduler = testScheduler,
            ioScheduler = testScheduler)

        // Setup
        disposables.add(
            BindWidgetWithModel(
                widget = tester,
                model = paper)
                .subscribe())

        // Add one particular stroke
        paper.pushStroke(mTestStroke1)
        testScheduler.triggerActions()

        // Must see one record!
        Assert.assertEquals(1, historyRepo.recordSize)

        disposables.clear()
    }

    @Test
    fun undo_shouldSeeNoStrokeInPaper() {
        val paper = PaperAutoSaveImpl()
        val historyRepo = PaperTransformRepoFileImpl(fileDir = File("/tmp"))
        val testScheduler = TestScheduler()
        val disposables = CompositeDisposable()

        val tester = PaperTransformWidget(
            historyRepo = historyRepo,
            uiScheduler = testScheduler,
            ioScheduler = testScheduler)

        // Setup
        disposables.add(
            BindWidgetWithModel(
                widget = tester,
                model = paper)
                .subscribe())

        // Add one particular stroke
        paper.pushStroke(mTestStroke1)
        testScheduler.triggerActions()
        // Undo immediately
        disposables.add(
            tester.undo()
                .subscribe())
        testScheduler.triggerActions()

        // Must see one record!
        Assert.assertEquals(1, historyRepo.recordSize)

        disposables.clear()
    }
}
