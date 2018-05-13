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
import com.paper.domain.event.DrawSVGEvent
import com.paper.domain.event.OnSketchEvent
import com.paper.domain.event.StartSketchEvent
import com.paper.domain.event.StopSketchEvent
import com.paper.domain.useCase.TranslateSketchToSVG
import com.paper.model.IPaper
import com.paper.model.Point
import com.paper.model.Rect
import com.paper.model.ScrapModel
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
    private val mModelDisposables = CompositeDisposable()

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
        mSetCanvasSize.onNext(Rect(0f, 0f, model.getWidth(), model.getHeight()))

        // Add or remove scrap
        mModelDisposables.add(
            model.onAddScrap()
                .observeOn(mUiScheduler)
                .subscribe { scrapM ->
                    val widget = ScrapWidget(mUiScheduler,
                                             mWorkerScheduler)
                    mScrapWidgets[scrapM.uuid] = widget

                    widget.bindModel(scrapM)

                    // Signal the adding event.
                    mAddWidgetSignal.onNext(widget)
                })
        mModelDisposables.add(
            model.onRemoveScrap()
                .observeOn(mUiScheduler)
                .subscribe { scrapM ->
                    val widget = mScrapWidgets[scrapM.uuid] ?:
                                 throw NoSuchElementException("Cannot find the widget")

                    widget.unbindModel()

                    // Signal the removing event.
                    mRemoveWidgetSignal.onNext(widget)
                })

        // Thumbnail
        mModelDisposables.add(
            mUpdateBitmapSignal
                .observeOn(mUiScheduler)
                .subscribe { (bmpFile, bmpWidth, bmpHeight) ->
                    mModel?.setThumbnail(bmpFile)
                    mModel?.setThumbnailWidth(bmpWidth)
                    mModel?.setThumbnailHeight(bmpHeight)
                })
    }

    override fun unbindModel() {
        mModelDisposables.clear()
    }

    private fun ensureNoLeakedBinding() {
        if (mModelDisposables.size() > 0)
            throw IllegalStateException("Already bind a model")
    }

    private fun hasModelBinding(): Boolean {
        return mModel != null && mModelDisposables.size() > 0
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

    private var mDrawingMode = DrawingMode.IDLE

    private lateinit var mTmpStroke: SketchStroke
    /**
     * Internal drawing signal for throttling touch event.
     */
    private val mLineToSignal = BehaviorSubject.create<Point>()
    /**
     * Internal signal for cancelling any kinds of drawing stream.
     */
    private val mCancelDrawingSignal = PublishSubject.create<Any>()
    /**
     * The signal for the external world to know this widget wants to draw SVG.
     */
    private val mDrawSVGSignal = PublishSubject.create<DrawSVGEvent>()
    /**
     * The current stroke color.
     */
    private var mPenColor = 0x2C2F3C
    /**
     * The current stroke width, where the value is from 0.0 to 1.0.
     */
    private var mPenSize = 0.2f

    override fun setDrawingMode(mode: DrawingMode) {
        mDrawingMode = mode
    }

    override fun setChosenPenColor(color: Int) {
        mPenColor = color
    }

    override fun setPenSize(size: Float) {
        mPenSize = size
    }

    private var mCanHandleThisDrag = false

    override fun handleDragBegin(x: Float,
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
            penSize = mPenSize)

        val p = Point(x, y)

        mTmpStroke.addPath(p)

        // Notify the observer the MOVE action
        mDrawSVGSignal.onNext(StartSketchEvent(
            point = p,
            penColor = mTmpStroke.penColor,
            penSize = mTmpStroke.penSize,
            penType = mTmpStroke.penType))
    }

    override fun handleDrag(x: Float,
                            y: Float) {
        if (!mCanHandleThisDrag) return

        val p = Point(x, y)

        mTmpStroke.addPath(p)

        // Notify the observer the LINE_TO action
        mDrawSVGSignal.onNext(OnSketchEvent(point = p))
    }

    override fun handleDragEnd(x: Float,
                               y: Float) {
        if (!mCanHandleThisDrag) return

        // Brutally stop the drawing filter.
        mCancelDrawingSignal.onNext(0)

        val p = Point(x, y)

        // Add last point
        mTmpStroke.addPath(p)
        // Commit to model
        mModel?.pushStroke(mTmpStroke)

        // Notify the observer
        mDrawSVGSignal.onNext(StopSketchEvent())
    }

    private val mUpdateBitmapSignal = PublishSubject.create<Triple<File, Int, Int>>()

    override fun setThumbnail(bmpFile: File,
                              bmpWidth: Int,
                              bmpHeight: Int) {
        mUpdateBitmapSignal.onNext(Triple(bmpFile, bmpWidth, bmpHeight))
    }

    private val mSetCanvasSize = BehaviorSubject.create<Rect>()

    override fun onSetCanvasSize(): Observable<Rect> {
        return mSetCanvasSize
    }

    override fun onDrawSVG(replayAll: Boolean): Observable<DrawSVGEvent> {
        return if (hasModelBinding()) {
            if (replayAll) {
                Observable
                    .merge(
                        mDrawSVGSignal,
                        // For the first time subscription, send events one by one!
                        TranslateSketchToSVG(mModel!!.getSketch())
                            .subscribeOn(mWorkerScheduler))
            } else {
                mDrawSVGSignal
            }
        } else {
            Observable.never()
        }
    }

    // Gesture ////////////////////////////////////////////////////////////////

    override fun handleActionBegin() {
    }

    override fun handleActionEnd() {
        // Brutally stop the drawing filter.
        mCancelDrawingSignal.onNext(0)
    }

    override fun handleTap(x: Float, y: Float) {
        if (!hasModelBinding()) return

        // Draw a DOT!!!
        val p = Point(x, y, 0)
        mTmpStroke = SketchStroke(
            penType = PenType.PEN,
            penColor = 0,
            penSize = 1f)
        mTmpStroke.addPath(p)

        // Commit to model
        mModel?.pushStroke(mTmpStroke)

        // Notify the observer
        mDrawSVGSignal.onNext(StartSketchEvent(
            point = p,
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
        val scrap = ScrapModel(UUID.randomUUID())
        scrap.x = x
        scrap.y = y

        mModel?.addScrap(scrap)
    }

    override fun toString(): String {
        return javaClass.simpleName
    }
}
