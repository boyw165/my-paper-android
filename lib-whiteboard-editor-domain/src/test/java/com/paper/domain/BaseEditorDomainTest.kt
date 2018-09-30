package com.paper.domain

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paper.domain.ui.*
import com.paper.model.*
import com.paper.model.command.WhiteboardCommand
import com.paper.model.command.WhiteboardCommandJSONTranslator
import com.paper.model.repository.ICommonPenPrefsRepo
import com.paper.model.repository.IUndoRepository
import com.paper.model.repository.IWhiteboardRepository
import com.paper.model.repository.IWhiteboardStore
import com.paper.model.repository.json.FrameJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.repository.json.WhiteboardJSONTranslator
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.useful.ShadowMotionEvent
import io.useful.rx.*
import org.koin.dsl.module.module
import org.mockito.Mockito
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

abstract class BaseEditorDomainTest {

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

    protected val caughtErrorSignal = PublishSubject.create<Throwable>().toSerialized()

    private val testScheduler = TestScheduler()
    protected val mockSchedulers: ISchedulers by lazy {
        val mock = Mockito.mock(ISchedulers::class.java)
        Mockito.`when`(mock.main()).thenReturn(testScheduler)
        Mockito.`when`(mock.computation()).thenReturn(testScheduler)
        Mockito.`when`(mock.io()).thenReturn(testScheduler)
        Mockito.`when`(mock.db()).thenReturn(testScheduler)
        mock
    }

    val mockWhiteboard: Whiteboard
        get() {
            val mock = Whiteboard()
            (1..50).forEach { _ ->
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
        val whiteboard = mockWhiteboard

        Mockito.`when`(field.busy).thenReturn(Observable.just(false))
        Mockito.`when`(field.whiteboard).thenReturn(Single.just(whiteboard))

        field
    }
    protected val mockWhiteboardWidget: IWhiteboardWidget by lazy {
        val field = Mockito.mock(IWhiteboardWidget::class.java)

        Mockito.`when`(field.busy).thenReturn(Observable.just(false))
        Mockito.`when`(field.whiteboardStore).then { mockWhiteboardStore }

        field
    }
    protected val mockUndoRepo: IUndoRepository by lazy {
        val field = Mockito.mock(IUndoRepository::class.java)

        Mockito.`when`(field.prepare()).thenReturn(Completable.complete())
        Mockito.`when`(field.canUndo).thenReturn(Observable.just(false))
        Mockito.`when`(field.canRedo).thenReturn(Observable.just(false))

        field
    }
    protected val mockWhiteboardEditorWidget: IWhiteboardEditorWidget by lazy {
        val field = Mockito.mock(IWhiteboardEditorWidget::class.java)

        Mockito.`when`(field.busy).thenReturn(Observable.just(false))
        Mockito.`when`(field.whiteboardWidget).then { mockWhiteboardWidget }
        Mockito.`when`(field.undoRepo).then { mockUndoRepo }

        field
    }

    protected val mockPenPrefsRepo: ICommonPenPrefsRepo by lazy {
        val field = Mockito.mock(ICommonPenPrefsRepo::class.java)

        Mockito.`when`(field.getPenSize())
            .thenReturn(Observable.just(100f))
        Mockito.`when`(field.getChosenPenColor())
            .thenReturn(Observable.just(Color.GREEN))
        Mockito.`when`(field.getPenColors())
            .thenReturn(Observable.just(listOf(Color.RED,
                                               Color.GREEN,
                                               Color.BLUE)))

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

    private val testModule = module {
        factory { mockSchedulers }
        factory { jsonTranslator }
        factory { mockWhiteboard }
        factory { mockWhiteboardRepo }
        factory { mockWhiteboardStore }
        factory { mockWhiteboardEditorWidget }
        factory<Observable<Throwable>> { caughtErrorSignal }
    }

    open fun setup() {
//        startKoin(listOf(testModule))
        println("init ${mockUndoRepo.javaClass.simpleName}")
        println("init ${mockWhiteboardRepo.javaClass.simpleName}")
        println("init ${mockWhiteboardStore.javaClass.simpleName}")
        println("init ${mockWhiteboardWidget.javaClass.simpleName}")
        println("init ${mockWhiteboardEditorWidget.javaClass.simpleName}")
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

    protected fun createBaseScrapBy(frame: Frame): Scrap {
        return Scrap(frame = frame)
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
        return SketchScrapWidget(scrap = createRandomSketchScrap())
    }

    protected fun createRandomImageScrapWidget(): ImageScrapWidget {
        return ImageScrapWidget(scrap = createRandomImageScrap())
    }

    protected fun createRandomTextScrapWidget(): TextScrapWidget {
        return TextScrapWidget(scrap = createRandomTextScrap())
    }

    // Touch sequence /////////////////////////////////////////////////////////

    protected val mockTouchBegin: Observable<GestureEvent> = Observable.just(TouchBeginEvent(
        rawEvent = ShadowMotionEvent(maskedAction = 0,
                                     downXs = floatArrayOf(0f),
                                     downYs = floatArrayOf(0f),
                                     downFocusX = 0f,
                                     downFocusY = 0f),
        target = null,
        context = null))

    protected val mockTouchEnd: Observable<GestureEvent> = Observable.just(TouchEndEvent(
        rawEvent = ShadowMotionEvent(maskedAction = 0,
                                     downXs = floatArrayOf(0f),
                                     downYs = floatArrayOf(0f),
                                     downFocusX = 0f,
                                     downFocusY = 0f),
        target = null,
        context = null))

    protected val mockDragSequence: Observable<GestureEvent> by lazy {
        Observable.fromArray<GestureEvent>(
            DragEvent(rawEvent = EMPTY_SHADOW_EVENT,
                      target = null,
                      context = null,
                      startPointer = Pair(0f, 0f),
                      stopPointer = Pair(20f, 0f)),
            DragEvent(rawEvent = EMPTY_SHADOW_EVENT,
                      target = null,
                      context = null,
                      startPointer = Pair(0f, 0f),
                      stopPointer = Pair(40f, 0f)),
            DragEvent(rawEvent = EMPTY_SHADOW_EVENT,
                      target = null,
                      context = null,
                      startPointer = Pair(0f, 0f),
                      stopPointer = Pair(60f, 0f)),
            DragEvent(rawEvent = EMPTY_SHADOW_EVENT,
                      target = null,
                      context = null,
                      startPointer = Pair(0f, 0f),
                      stopPointer = Pair(80f, 0f)),
            DragEvent(rawEvent = EMPTY_SHADOW_EVENT,
                      target = null,
                      context = null,
                      startPointer = Pair(0f, 0f),
                      stopPointer = Pair(100f, 0f)))
    }

    protected val mockPinchSequence: Observable<GestureEvent> by lazy {
        Observable.fromArray<GestureEvent>(
            PinchEvent(rawEvent = EMPTY_SHADOW_EVENT,
                       target = null,
                       context = null,
                       startPointers = arrayOf(Pair(-100f, 100f),
                                               Pair(100f, -100f)),
                       stopPointers = arrayOf(Pair(-80f, 80f),
                                              Pair(80f, -80f))),
            PinchEvent(rawEvent = EMPTY_SHADOW_EVENT,
                       target = null,
                       context = null,
                       startPointers = arrayOf(Pair(-100f, 100f),
                                               Pair(100f, -100f)),
                       stopPointers = arrayOf(Pair(-60f, 60f),
                                              Pair(60f, -60f))),
            PinchEvent(rawEvent = EMPTY_SHADOW_EVENT,
                       target = null,
                       context = null,
                       startPointers = arrayOf(Pair(-100f, 100f),
                                               Pair(100f, -100f)),
                       stopPointers = arrayOf(Pair(-40f, 40f),
                                              Pair(40f, -40f))))
    }

    protected val mockDragEndDisplacement by lazy {
        Frame(x = 100f,
              y = 0f)
    }
}
