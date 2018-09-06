package com.paper.domain

import com.cardinalblue.gesture.ShadowMotionEvent
import com.cardinalblue.gesture.rx.DragBeginEvent
import com.cardinalblue.gesture.rx.DragDoingEvent
import com.cardinalblue.gesture.rx.DragEndEvent
import com.cardinalblue.gesture.rx.GestureEvent
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paper.domain.ui.SVGScrapWidget
import com.paper.model.*
import com.paper.model.command.WhiteboardCommand
import com.paper.model.command.WhiteboardCommandJSONTranslator
import com.paper.model.repository.IPaperRepo
import com.paper.model.repository.json.FrameJSONTranslator
import com.paper.model.repository.json.PaperJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.mockito.Mock
import org.mockito.Mockito
import java.lang.IllegalStateException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

abstract class BaseDomainTest {

    companion object {

        const val SHORT_TIMEOUT = 300L
        const val NORMAL_TIMEOUT = 600L
        const val LONG_TIMEOUT = 1200L
        const val DEFINITELY_LONG_ENOUGH_TIMEOUT = 1000000L

        @JvmStatic
        val EMPTY_SHADOW_EVENT = ShadowMotionEvent(maskedAction = 0,
                                                   downFocusX = 0f,
                                                   downFocusY = 0f,
                                                   downXs = floatArrayOf(0f),
                                                   downYs = floatArrayOf(0f))

        @JvmStatic
        val DEFAULT_FRAME = Frame()
    }

    protected val disposableBag = CompositeDisposable()

    protected val caughtErrorSignal = PublishSubject.create<Throwable>().toSerialized()

    private val testScheduler = TestScheduler()
    protected val mockSchedulers: ISchedulers by lazy {
        val mock = Mockito.mock(ISchedulers::class.java)
        Mockito.`when`(mock.main()).thenReturn(testScheduler)
        Mockito.`when`(mock.ui()).thenReturn(testScheduler)
        Mockito.`when`(mock.computation()).thenReturn(testScheduler)
        Mockito.`when`(mock.io()).thenReturn(testScheduler)
        Mockito.`when`(mock.db()).thenReturn(testScheduler)
        mock
    }

    val mockPaper: IPaper by lazy {
        val mock = BasePaper()
        (1..50).forEach {
            mock.addScrap(createRandomScrap())
        }
        mock
    }

    @Mock
    lateinit var mockPaperRepo: IPaperRepo

    protected val jsonTranslator: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(BasePaper::class.java,
                                 PaperJSONTranslator())
            .registerTypeAdapter(BaseScrap::class.java,
                                 ScrapJSONTranslator())
            .registerTypeAdapter(Frame::class.java,
                                 FrameJSONTranslator())
            .registerTypeAdapter(VectorGraphics::class.java,
                                 VectorGraphicsJSONTranslator())
            .registerTypeAdapter(WhiteboardCommand::class.java,
                                 WhiteboardCommandJSONTranslator())
            .create()
    }

    open fun setup() {
        Mockito.`when`(mockPaperRepo.getPaperById(Mockito.anyLong()))
            .thenReturn(
                Single.just(mockPaper)
                    .delay(SHORT_TIMEOUT, TimeUnit.MILLISECONDS, testScheduler))
    }

    open fun clean() {
        disposableBag.clear()
    }

    protected fun moveScheduler() {
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    // Scrap or widget ////////////////////////////////////////////////////////

    private val random = Random()

    protected fun rand(from: Int, to: Int): Int {
        return random.nextInt(to - from) + from
    }

    protected fun rand(from: Float): Float {
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

    protected fun createBaseScrapBy(frame: Frame): BaseScrap {
        return BaseScrap(frame = frame)
    }

    protected fun createRandomSVGScrap(): SVGScrap {
        return SVGScrap(frame = createRandomFrame())
    }

    protected fun createRandomSVGScrapWidget(): SVGScrapWidget {
        return SVGScrapWidget(scrap = createRandomSVGScrap(),
                              newSVGPenStyle = Observable.empty(),
                              schedulers = mockSchedulers)
    }

    protected fun createMockSVGScrapWidget(): SVGScrapWidget {
        val mock = Mockito.mock(SVGScrapWidget::class.java)

        Mockito.`when`(mock.getID()).thenReturn(UUID.randomUUID())
        Mockito.`when`(mock.getFrame()).thenReturn(DEFAULT_FRAME)

        return mock
    }

    protected fun createRandomImageScrap(): ImageScrap {
        return ImageScrap(frame = createRandomFrame(),
                          imageURL = URL("http://foo.com/foo.png"))
    }

    protected fun createRandomTextScrap(): TextScrap {
        return TextScrap(frame = createRandomFrame(),
                         text = "foo")
    }

    protected fun createRandomScrap(): BaseScrap {
        val random = rand(0, 2)

        return when (random) {
            0 -> createRandomSVGScrap()
            1 -> createRandomImageScrap()
            2 -> createRandomTextScrap()
            else -> throw IllegalStateException()
        }
    }

    // Touch sequence /////////////////////////////////////////////////////////

    protected val mockDragSequence: Observable<GestureEvent> by lazy {
        Observable.fromArray<GestureEvent>(
            DragBeginEvent(rawEvent = EMPTY_SHADOW_EVENT,
                           target = null,
                           context = null,
                           startPointer = Pair(0f, 0f)),
            DragDoingEvent(rawEvent = EMPTY_SHADOW_EVENT,
                           target = null,
                           context = null,
                           startPointer = Pair(0f, 0f),
                           stopPointer = Pair(20f, 0f)),
            DragDoingEvent(rawEvent = EMPTY_SHADOW_EVENT,
                           target = null,
                           context = null,
                           startPointer = Pair(0f, 0f),
                           stopPointer = Pair(40f, 0f)),
            DragDoingEvent(rawEvent = EMPTY_SHADOW_EVENT,
                           target = null,
                           context = null,
                           startPointer = Pair(0f, 0f),
                           stopPointer = Pair(60f, 0f)),
            DragDoingEvent(rawEvent = EMPTY_SHADOW_EVENT,
                           target = null,
                           context = null,
                           startPointer = Pair(0f, 0f),
                           stopPointer = Pair(80f, 0f)),
            DragEndEvent(rawEvent = EMPTY_SHADOW_EVENT,
                         target = null,
                         context = null,
                         startPointer = Pair(0f, 0f),
                         stopPointer = Pair(100f, 0f)))
    }

    protected val mockDragEndDisplacement by lazy {
        Frame(x = 100f,
              y = 0f)
    }
}
