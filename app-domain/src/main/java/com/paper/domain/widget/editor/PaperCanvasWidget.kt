// Copyright Mar 2018-present boyw165@gmail.com
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

package com.paper.domain.widget.editor

import com.paper.domain.data.DrawingMode
import com.paper.domain.data.GestureRecord
import com.paper.domain.event.*
import com.paper.domain.useCase.TranslateSketchToSVG
import com.paper.model.IPaper
import com.paper.model.Point
import com.paper.model.Rect
import com.paper.model.Scrap
import com.paper.model.sketch.PenType
import com.paper.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*
import kotlin.NoSuchElementException

class PaperCanvasWidget(uiScheduler: Scheduler,
                        workerScheduler: Scheduler)
    : IPaperCanvasWidget {

    private val mUiScheduler = uiScheduler
    private val mWorkerScheduler = workerScheduler

    // Model
    private var mModel: IPaper? = null
    private val mDisposables = CompositeDisposable()

    // Scrap controllers
    private val mScrapWidgets = hashMapOf<UUID, IScrapWidget>()

    // Global canceller
    private val mCancelSignal = PublishSubject.create<Any>()

    // Gesture
    private val mGestureHistory = mutableListOf<GestureRecord>()

    // Debug
    private val mDebugSignal = PublishSubject.create<String>()

    override fun bindModel(model: IPaper) {
        ensureNoLeakedBinding()

        // Hold reference.
        mModel = model

        // Canvas size
        mCanvasSizeSignal.onNext(Rect(0f, 0f, model.getWidth(), model.getHeight()))

        // Add or remove stroke
        mDisposables.add(
            model.onAddStroke(replayAll = false)
                .observeOn(mUiScheduler)
                .subscribe { stroke ->
                    mDrawSVGSignal.onNext(
                        AddSketchStrokeEvent(strokeID = stroke.id,
                                             points = stroke.pointList,
                                             penColor = stroke.penColor,
                                             penSize = stroke.penSize,
                                             penType = stroke.penType))
                })
        mDisposables.add(
            model.onRemoveStroke()
                .observeOn(mUiScheduler)
                .subscribe { stroke ->
                    mDrawSVGSignal.onNext(RemoveSketchStrokeEvent(strokeID = stroke.id))
                })

        // Add or remove scrap
        mDisposables.add(
            model.onAddScrap()
                .observeOn(mUiScheduler)
                // TODO: Use BindWidgetWithModel observable
                .subscribe { scrapModel ->
                    val widget = ScrapWidget(mUiScheduler,
                                             mWorkerScheduler)
                    mScrapWidgets[scrapModel.uuid] = widget

                    widget.bindModel(scrapModel)

                    // Signal the adding event.
                    mAddWidgetSignal.onNext(widget)
                })
        mDisposables.add(
            model.onRemoveScrap()
                .observeOn(mUiScheduler)
                .subscribe { scrapM ->
                    val widget = mScrapWidgets[scrapM.uuid] ?:
                                 throw NoSuchElementException("Cannot find the widget")

                    widget.unbindModel()

                    // Signal the removing event.
                    mRemoveWidgetSignal.onNext(widget)
                })

        // Busy state
        mDisposables.add(
            mBusyFlagSignal
                .observeOn(mUiScheduler)
                .subscribe { busyFlag ->
                    if (busyFlag == 0) {
                        mBusySignal.onNext(false)
                    } else {
                        mBusySignal.onNext(true)
                    }
                })
    }

    override fun unbindModel() {
        mDisposables.clear()
    }

    private fun ensureNoLeakedBinding() {
        if (mDisposables.size() > 0)
            throw IllegalStateException("Already bind a model")
    }

    private fun hasModelBinding(): Boolean {
        return mModel != null && mDisposables.size() > 0
    }

    // Number of on-going task ////////////////////////////////////////////////

    /**
     * Types of operations making canvas busy.
     * @see [mBusyFlagSignal]
     * @see [mBusySignal]
     */
    enum class BusyFlag(val mask: Int) {
        DRAWING(1.shl(0)),
        THUMBNAIL(1.shl(1))
    }

    private val mBusyFlagSignal = BehaviorSubject.createDefault(0)
    /**
     * There are multiple operations making the canvas widget busy, any of them
     * is still running, then this widget is busy.
     */
    private val mBusySignal = BehaviorSubject.createDefault(false)

    private fun markBusy(which: BusyFlag) {
        val busyFlag = mBusyFlagSignal.value!!
        mBusyFlagSignal.onNext(busyFlag.or(which.mask))
    }

    private fun markNotBusy(which: BusyFlag) {
        val busyFlag = mBusyFlagSignal.value!!
        mBusyFlagSignal.onNext(busyFlag.and(which.mask.inv()))
    }

    fun onBusy(): Observable<Boolean> {
        return mBusySignal
    }

    // Add & Remove Scrap /////////////////////////////////////////////////////

    private val mAddWidgetSignal = PublishSubject.create<IScrapWidget>()
    private val mRemoveWidgetSignal = PublishSubject.create<IScrapWidget>()

    override fun onAddScrapWidget(): Observable<IScrapWidget> {
        return Observable.merge(
            // Must clone the list in case concurrent modification
            Observable.fromIterable(mScrapWidgets.values.toList()),
            mAddWidgetSignal)
    }

    override fun onRemoveScrapWidget(): Observable<IScrapWidget> {
        return mRemoveWidgetSignal
    }

    // Drawing ////////////////////////////////////////////////////////////////

    // TODO: Is the mode necessary?
    private var mDrawingMode = DrawingMode.IDLE

    private lateinit var mTmpStroke: SketchStroke
    /**
     * The signal for the external world to know this widget wants to draw SVG.
     */
    private val mDrawSVGSignal = PublishSubject.create<CanvasEvent>().toSerialized()
    /**
     * The current stroke color.
     */
    private var mPenColor = 0x2C2F3C
    /**
     * The current stroke width, where the value is from 0.0 to 1.0.
     */
    private var mPenSize = 0.2f
    /**
     * The current view-port scale.
     */
    private var mViewPortScale = Float.NaN
    /**
     * The scaled pen size, which is view-port-scale * pen-size.
     */
    private val mScaledPenSize: Float
        get() = mViewPortScale * mPenSize

    override fun setDrawingMode(mode: DrawingMode) {
        mDrawingMode = mode
    }

    override fun setChosenPenColor(color: Int) {
        mPenColor = color
    }

    override fun setViewPortScale(scale: Float) {
        mViewPortScale = scale
    }

    override fun setPenSize(size: Float) {
        mPenSize = size
    }

    override fun eraseCanvas() {
        mModel?.removeAllStrokes()
    }

    override fun setThumbnail(bmpFile: File,
                              bmpWidth: Int,
                              bmpHeight: Int) {
        mModel?.setThumbnail(bmpFile)
        mModel?.setThumbnailWidth(bmpWidth)
        mModel?.setThumbnailHeight(bmpHeight)
    }

    private val mCanvasSizeSignal = BehaviorSubject.create<Rect>()

    override fun onSetCanvasSize(): Observable<Rect> {
        return mCanvasSizeSignal
    }

    override fun onDrawSVG(replayAll: Boolean): Observable<CanvasEvent> {
        return if (hasModelBinding()) {
            val source = if(replayAll) {
                Observable
                    .merge(
                        mDrawSVGSignal,
                        // For the first time subscription, send events one by one!
                        TranslateSketchToSVG(mModel!!.getSketch()))
            } else {
                mDrawSVGSignal
            }

            // Canvas operation would change the busy flag
            source.observeOn(mUiScheduler)
                .doOnNext { event ->
                    when (event) {
                        is StartSketchEvent -> {
                            markBusy(BusyFlag.DRAWING)
                        }
                        is StopSketchEvent -> {
                            markNotBusy(BusyFlag.DRAWING)
                        }
                    }
                }
        } else {
            Observable.never()
        }
    }

    // Gesture ////////////////////////////////////////////////////////////////

    override fun handleTouchBegin() {
    }

    override fun handleTouchEnd() {
    }

    private var mCacheTime = 0L
    private var mCanHandleThisDrag = false

    override fun beingDrawCurve(x: Float,
                                y: Float) {
        mCanHandleThisDrag = hasModelBinding() && mDrawingMode != DrawingMode.IDLE
        if (!mCanHandleThisDrag) return

        // Clear all the delayed execution.
        mCancelSignal.onNext(0)

        // Create a new stroke and hold it in the temporary pool.
        val penType = if (mDrawingMode == DrawingMode.ERASER) {
            PenType.ERASER
        } else {
            PenType.PEN
        }
        mTmpStroke = SketchStroke(
            penType = penType,
            penColor = mPenColor,
            penSize = mScaledPenSize)

        mCacheTime = System.currentTimeMillis()
        val p = Point(x, y, time = 0)

        mTmpStroke.addPath(p)

        // Notify the observer the MOVE action
        mDrawSVGSignal.onNext(StartSketchEvent(
            strokeID = mTmpStroke.id,
            point = p,
            penColor = mTmpStroke.penColor,
            penSize = mTmpStroke.penSize,
            penType = mTmpStroke.penType))
    }

    override fun drawCurveTo(x: Float,
                             y: Float) {
        if (!mCanHandleThisDrag) return

        val time = System.currentTimeMillis()
        val diff = System.currentTimeMillis() - mCacheTime
        mCacheTime = time

        val p = Point(x, y, time = diff)
        mTmpStroke.addPath(p)

        // Notify the observer the LINE_TO action
        mDrawSVGSignal.onNext(OnSketchEvent(strokeID = mTmpStroke.id,
                                            point = p))
    }

    override fun stopDrawCurve() {
        if (!mCanHandleThisDrag) return

        // Notify the observer
        mDrawSVGSignal.onNext(StopSketchEvent())

        // Commit to model
        mModel?.pushStroke(mTmpStroke)
    }

    override fun drawDot(x: Float, y: Float) {
        if (!hasModelBinding()) return

        // Draw a DOT!!!
        // Create a new stroke and hold it in the temporary pool.
        val penType = if (mDrawingMode == DrawingMode.ERASER) {
            PenType.ERASER
        } else {
            PenType.PEN
        }
        mTmpStroke = SketchStroke(
            penType = penType,
            penColor = mPenColor,
            penSize = mScaledPenSize,
            mPointList = mutableListOf(Point(x, y, 0)))
        // Commit to model
        mModel?.pushStroke(mTmpStroke)

        // Notify the observer
        mDrawSVGSignal.onNext(StartSketchEvent(
            strokeID = mTmpStroke.id,
            point = Point(x, y, 0),
            penColor = mTmpStroke.penColor,
            penSize = mTmpStroke.penSize,
            penType = mTmpStroke.penType))
        mDrawSVGSignal.onNext(StopSketchEvent())
    }

    // Debug //////////////////////////////////////////////////////////////////

    override fun onPrintDebugMessage(): Observable<String> {
        return mDebugSignal
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun addScrapAtPosition(x: Float,
                                   y: Float) {
        val scrap = Scrap(UUID.randomUUID())
        scrap.x = x
        scrap.y = y

        mModel?.addScrap(scrap)
    }

    override fun toString(): String {
        return javaClass.simpleName
    }
}
