package com.paper.domain

import com.paper.model.*
import com.paper.model.repository.IPaperRepo
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Before
import org.mockito.Mockito
import java.util.concurrent.TimeUnit

abstract class BaseTest {

    companion object {

        const val SHORT_TIMEOUT = 60L
        const val NORMAL_TIMEOUT = 180L
        const val LONG_TIMEOUT = 360L
        const val DEFINITE_LONG_ENOUGH_TIMEOUT = 1000000L
    }

    protected val disposableBag = CompositeDisposable()

    protected val caughtErrorSignal = PublishSubject.create<Throwable>().toSerialized()

    protected val testScheduler = TestScheduler()
    protected val mockSchedulers: ISchedulerProvider by lazy {
        val mock = Mockito.mock(ISchedulerProvider::class.java)
        Mockito.`when`(mock.main()).thenReturn(testScheduler)
        Mockito.`when`(mock.computation()).thenReturn(testScheduler)
        Mockito.`when`(mock.io()).thenReturn(testScheduler)
        Mockito.`when`(mock.db()).thenReturn(testScheduler)
        mock
    }

    protected val mockPaper: IPaper by lazy {
        val mock = BasePaper()
        mock.addScrap(SVGScrap(mutableFrame = Frame(2f, 3f),
                               graphicsList = mutableListOf(VectorGraphics(tupleList = mutableListOf(LinearPointTuple(3f, 4f))))))
        mock
    }

    protected val mockPaperRepo: IPaperRepo by lazy {
        val mock = Mockito.mock(IPaperRepo::class.java)
        Mockito.`when`(mock.getPaperById(Mockito.anyLong()))
            .thenReturn(
                Single.just(mockPaper)
                    .delay(SHORT_TIMEOUT, TimeUnit.MILLISECONDS, testScheduler))
        mock
    }

    @Before
    fun setup() {
        // DO NOTHING
    }

    @After
    fun stop() {
        disposableBag.clear()
    }
}
