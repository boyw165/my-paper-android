// Copyright Apr 2018-present boyw165@gmail.com
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

import com.paper.domain.event.ProgressEvent
import com.paper.domain.widget.LoadPaperAndBindModel
import com.paper.domain.widget.canvas.IPaperWidget
import com.paper.model.PaperModel
import com.paper.model.repository.protocol.IPaperModelRepo
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class LoadPaperAndBindModelTest {

    @Test
    fun getFalseIfErrorFromRepo() {
        val mockedRepo = Mockito.mock(IPaperModelRepo::class.java)
        Mockito
            .`when`(mockedRepo.getPaperById(Mockito.anyLong()))
            .thenReturn(Single.error(RuntimeException("Mocked repo!")))
        val mockedWidget = Mockito.mock(IPaperWidget::class.java)

        val testScheduler = TestScheduler()

        val progressSignal = PublishSubject.create<ProgressEvent>()
        val testProgressObserver = progressSignal.test()

        val testMainObserver = LoadPaperAndBindModel(
            paperID = 0,
            paperWidget = mockedWidget,
            paperRepo = mockedRepo,
            updateProgressSignal = progressSignal,
            uiScheduler = testScheduler)
            .test()

        // Must got observable of false without any uncaught exception
        testScheduler.triggerActions()
        testMainObserver.assertValue(false)

        // Must see two ProgressEvent events, where first one is 0 progress and
        // second one is 100 progress
        testProgressObserver.assertValueCount(2)
    }

    @Test
    fun widgetShouldBindModelIfRepoWorks() {
        val mockedModel = PaperModel()
        val mockedRepo = Mockito.mock(IPaperModelRepo::class.java)
        Mockito
            .`when`(mockedRepo.getPaperById(Mockito.anyLong()))
            .thenReturn(Single.just(mockedModel))
        val mockedWidget = Mockito.mock(IPaperWidget::class.java)

        val testScheduler = TestScheduler()

        val progressSignal = PublishSubject.create<ProgressEvent>()
        val testProgressObserver = progressSignal.test()

        val testMainObserver = LoadPaperAndBindModel(
            paperID = 0,
            paperWidget = mockedWidget,
            paperRepo = mockedRepo,
            updateProgressSignal = progressSignal,
            uiScheduler = testScheduler)
            .test()

        // Must got observable of true without any uncaught exception
        testScheduler.triggerActions()
        testMainObserver.assertValue(true)

        // Must see that widget binds with model
        Mockito.verify(mockedWidget).bindModel(mockedModel)
        // Must see two ProgressEvent events, where first one is 0 progress and
        // second one is 100 progress
        testProgressObserver.assertValueCount(2)
        // Must see that widget unbinds model if the subscription is disposed
        testMainObserver.dispose()
        Mockito.verify(mockedWidget).unbindModel()
    }
}
