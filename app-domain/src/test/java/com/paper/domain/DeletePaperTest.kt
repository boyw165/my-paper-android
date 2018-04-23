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

import com.paper.domain.useCase.DeletePaper
import com.paper.model.repository.IPaperRepo
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import org.mockito.Mockito

class DeletePaperTest {

    @Test
    fun getExceptionIfErrorFromRepo() {
        val error = RuntimeException("Mocked repo!")
        val mockRepo = Mockito.mock(IPaperRepo::class.java)
        Mockito
            .`when`(mockRepo.deletePaperById(Mockito.anyLong()))
            .thenReturn(Single.error(error))

        val testScheduler = TestScheduler()

        val testMainObserver = DeletePaper(
            paperID = 0,
            paperRepo = mockRepo)
            .test()

        // Must see exception
        testScheduler.triggerActions()
        testMainObserver.assertError(error)
    }

    @Test
    fun getTrueIfRepoWorks() {
        val mockRepo = Mockito.mock(IPaperRepo::class.java)
        Mockito
            .`when`(mockRepo.deletePaperById(Mockito.anyLong()))
            .thenReturn(Single.just(true))

        val testMainObserver = DeletePaper(
            paperID = 0,
            paperRepo = mockRepo)
            .test()

        // Must see exception
        testMainObserver.assertValue(true)
    }
}
