package com.paper.domain

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paper.domain.store.IWhiteboardStore
import com.paper.domain.ui.ImageScrapWidget
import com.paper.domain.ui.ScrapWidget
import com.paper.domain.ui.SketchScrapWidget
import com.paper.domain.ui.TextScrapWidget
import com.paper.model.*
import com.paper.model.command.WhiteboardCommand
import com.paper.model.command.WhiteboardCommandJSONTranslator
import com.paper.model.repository.IWhiteboardRepository
import com.paper.model.repository.json.FrameJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.repository.json.WhiteboardJSONTranslator
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.TestScheduler
import io.useful.ShadowMotionEvent
import org.mockito.Mockito
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

abstract class BaseWhiteboardKitDomainTest {

    companion object {

        @JvmStatic
        val SHORT_TIMEOUT = 300L
        @JvmStatic
        val NORMAL_TIMEOUT = 600L
        @JvmStatic
        val LONG_TIMEOUT = 1200L
        @JvmStatic
        val DEFINITELY_LONG_ENOUGH_TIMEOUT = 1000000L

        @JvmStatic
        val EMPTY_SHADOW_EVENT = ShadowMotionEvent(maskedAction = 0,
                                                   downFocusX = 0f,
                                                   downFocusY = 0f,
                                                   downXs = floatArrayOf(0f),
                                                   downYs = floatArrayOf(0f))

        @JvmStatic
        val RANDOM_WHITEBOARD_ID = 0L

        @JvmStatic
        val DEFAULT_FRAME = Frame()
    }

    protected val disposableBag = CompositeDisposable()

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

    val mockWhiteboard: Whiteboard get() {
        val mock = Whiteboard()
        (1..50).forEach {
            mock.addScrap(createRandomScrap())
        }
        return mock
    }

    protected val mockWhiteboardRepo: IWhiteboardRepository by lazy {
        val field = Mockito.mock(IWhiteboardRepository::class.java)

        Mockito.`when`(field.getBoardById(Mockito.anyLong()))
            .thenReturn(
                Single.just(mockWhiteboard)
                    .delay(SHORT_TIMEOUT, TimeUnit.MILLISECONDS, testScheduler))

        field
    }
    protected val mockWhiteboardStore: IWhiteboardStore by lazy {
        val field = Mockito.mock(IWhiteboardStore::class.java)

        Mockito.`when`(field.busy).thenReturn(Observable.just(false))
        Mockito.`when`(field.whiteboard).thenReturn(Single.just(mockWhiteboard))

        field
    }

    protected val jsonTranslator: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Whiteboard::class.java,
                                 WhiteboardJSONTranslator())
            .registerTypeAdapter(Scrap::class.java,
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
        // DO NOTHING
    }

    open fun clean() {
        disposableBag.clear()
    }

    protected fun moveScheduler() {
        testScheduler.advanceTimeBy(DEFINITELY_LONG_ENOUGH_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    private val random = Random()

    protected fun rand(from: Int, to: Int): Int {
        return random.nextInt(to - from) + from
    }

    protected fun rand(from: Float): Float {
        return random.nextFloat() + from
    }

    // Scrap //////////////////////////////////////////////////////////////////

    protected fun createRandomFrame(): Frame {
        val scale = rand(1, 5).toFloat()
        return Frame(x = rand(0, 500).toFloat(),
                     y = rand(0, 500).toFloat(),
                     width = rand(0, 500).toFloat(),
                     height = rand(0, 500).toFloat(),
                     scaleX = scale,
                     scaleY = scale,
                     rotationInDegrees = rand(0, 360).toFloat(),
                     z = rand(0, 1000))
    }

    protected fun createRandomSVG(): VectorGraphics {
        return VectorGraphics(tupleList = mutableListOf(LinearPointTuple(0f, 0f),
                                                        CubicPointTuple(10f, 10f, 10f, 10f, 20f, 0f),
                                                        CubicPointTuple(-30f, -30f, -30f, -30f, 40f, 0f),
                                                        CubicPointTuple(50f, 50f, 50f, 50f, 40f, 20f)))
    }

    protected fun createRandomScrap(): Scrap {
        val random = rand(0, 2)

        return when (random) {
            0 -> createRandomSketchScrap()
            1 -> createRandomImageScrap()
            2 -> createRandomTextScrap()
            else -> createRandomSketchScrap()
        }
    }

    protected fun createRandomSketchScrap(): SketchScrap {
        return SketchScrap(frame = createRandomFrame(),
                           svg = createRandomSVG())
    }

    protected fun createRandomImageScrap(): ImageScrap {
        return ImageScrap(frame = createRandomFrame(),
                          imageURL = URL("http://foo.com/foo.png"))
    }

    protected fun createRandomTextScrap(): TextScrap {
        return TextScrap(frame = createRandomFrame(),
                         text = "foo")
    }

    // Widget /////////////////////////////////////////////////////////////////

    protected fun createRandomScrapWidget(): ScrapWidget {
        val random = rand(0, 2)

        return when (random) {
            0 -> createRandomSketchScrapWidget()
            1 -> createRandomImageScrapWidget()
            2 -> createRandomTextScrapWidget()
            else -> throw IllegalStateException()
        }
    }

    protected fun createRandomSketchScrapWidget(): SketchScrapWidget {
        return SketchScrapWidget(scrap = createRandomSketchScrap(),
                                 schedulers = mockSchedulers)
    }

    protected fun createRandomImageScrapWidget(): ImageScrapWidget {
        return ImageScrapWidget(scrap = createRandomImageScrap(),
                                schedulers = mockSchedulers)
    }

    protected fun createRandomTextScrapWidget(): TextScrapWidget {
        return TextScrapWidget(scrap = createRandomTextScrap(),
                               schedulers = mockSchedulers)
    }
}
