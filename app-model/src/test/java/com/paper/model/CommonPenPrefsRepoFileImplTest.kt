package com.paper.model

// Copyright Mar 2017-present boyw165@gmail.com
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

import com.paper.model.repository.CommonPenPrefsRepoFileImpl
import com.paper.model.repository.ICommonPenPrefsRepo
import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class CommonPenPrefsRepoFileImplTest {

    @Test
    fun readDefaultColors() {
        val dir = File("json")

        val testScheduler = TestScheduler()
        val tester = CommonPenPrefsRepoFileImpl(
            dir = dir,
            ioScheduler = testScheduler)

        val testObserver = TestObserver<List<Int>>()
        tester.getPenColors()
            .subscribe(testObserver)

        testScheduler.triggerActions()

        testObserver.assertValue { colors ->
            var theSame = true
            colors.forEachIndexed { i, color ->
                theSame = theSame && (color == ICommonPenPrefsRepo.DEFAULT_COLORS[i])
            }
            return@assertValue theSame
        }
    }

    @Test
    fun readDefaultChosenColor() {
        val dir = File("json")

        val testScheduler = TestScheduler()
        val tester = CommonPenPrefsRepoFileImpl(
            dir = dir,
            ioScheduler = testScheduler)

        val testObserver = TestObserver<Int>()
        tester.getChosenPenColor()
            .subscribe(testObserver)

        testScheduler.triggerActions()

        testObserver.assertValue(ICommonPenPrefsRepo.DEFAULT_CHOSEN_COLOR)
    }

    @Test
    fun putColors_willSaveFile() {
        val dir = File("/tmp")

        val testScheduler = TestScheduler()
        val tester = CommonPenPrefsRepoFileImpl(
            dir = dir,
            ioScheduler = testScheduler)

        val testObserver = TestObserver.create<Boolean>()
        Observables
            .combineLatest(
                tester.putPenColors(ICommonPenPrefsRepo.DEFAULT_COLORS)
                    .toObservable(),
                tester.putChosenPenColor(ICommonPenPrefsRepo.DEFAULT_CHOSEN_COLOR)
                    .toObservable(),
                tester.putPenSize(ICommonPenPrefsRepo.DEFAULT_PEN_SIZE)
                    .toObservable())
            .map { it.first && it.second && it.third }
            .subscribe(testObserver)

        testScheduler.advanceTimeBy(1, TimeUnit.DAYS)
        testObserver.assertValue(true)
    }

    @Test
    fun putAndGetAgain_shouldBeTheSame() {
        val dir = File("/tmp")

        val testScheduler = TestScheduler()
        val tester = CommonPenPrefsRepoFileImpl(
            dir = dir,
            ioScheduler = testScheduler)

        val testObserver = TestObserver.create<List<Int>>()
        tester.getPenColors()
            .subscribe(testObserver)

        testScheduler.triggerActions()
        testObserver.assertValue { colors ->
            var theSame = true
            colors.forEachIndexed { i, color ->
                theSame = theSame && (color == ICommonPenPrefsRepo.DEFAULT_COLORS[i])
            }
            return@assertValue theSame
        }
    }
}

