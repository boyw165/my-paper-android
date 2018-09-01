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
import org.mockito.Mock
import org.mockito.Mockito
import java.lang.IllegalStateException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

abstract class BaseDomainTest {

    companion object {

        const val SHORT_TIMEOUT = 60L
        const val NORMAL_TIMEOUT = 180L
        const val LONG_TIMEOUT = 360L
        const val DEFINITELY_LONG_ENOUGH_TIMEOUT = 1000000L
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

    val mockPaper: IPaper
        get() {
            val mock = BasePaper()
            mock.addScrap(SVGScrap(frame = Frame(2f, 3f),
                                   graphicsList = mutableListOf(VectorGraphics(tupleList = mutableListOf(LinearPointTuple(3f, 4f))))))
            return mock
        }

    @Mock
    lateinit var mockPaperRepo: IPaperRepo

    @Before
    fun setup() {
        Mockito.`when`(mockPaperRepo.getPaperById(Mockito.anyLong()))
            .thenReturn(
                Single.just(mockPaper)
                    .delay(SHORT_TIMEOUT, TimeUnit.MILLISECONDS, testScheduler))
    }

    @After
    fun stop() {
        disposableBag.clear()
    }

    protected fun moveScheduler() {
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    private val random = Random()

    protected fun rand(from: Int, to: Int) : Int {
        return random.nextInt(to - from) + from
    }

    protected fun rand(from: Float) : Float {
        return random.nextFloat() + from
    }

    protected fun createRandomFrame(): Frame {
        val scale = rand(1, 5).toFloat()
        return Frame(x = rand(0f),
                     y = rand(0f),
                     width = rand(0, 500).toFloat(),
                     height = rand(0, 500).toFloat(),
                     scaleX = scale,
                     scaleY = scale,
                     rotationInDegrees = rand(0, 360).toFloat(),
                     z = rand(0, 1000))
    }

    protected fun createBaseScrapBy(frame: Frame): IScrap {
        return BaseScrap(frame = frame)
    }

    protected fun createRandomSVGScrap(): ISVGScrap {
        return SVGScrap(frame = createRandomFrame())
    }

    protected fun createRandomImageScrap(): IImageScrap {
        return ImageScrap(frame = createRandomFrame(),
                          imageURL = URL("http://foo.com/foo.png"))
    }

    protected fun createRandomTextScrap(): ITextScrap {
        return TextScrap(frame = createRandomFrame(),
                         text = "foo")
    }

    protected fun createRandomScrap(): IScrap {
        val random = rand(0, 2)

        return when (random) {
            0 -> createRandomSVGScrap()
            1 -> createRandomImageScrap()
            2 -> createRandomTextScrap()
            else -> throw IllegalStateException()
        }
    }
}
