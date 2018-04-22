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

package com.paper.domain.widget.canvas

import com.paper.domain.DomainConst
import com.paper.domain.data.GestureRecord
import com.paper.domain.event.DrawSVGEvent
import com.paper.domain.event.DrawSVGEvent.Action.*
import com.paper.model.PaperModel
import com.paper.model.Point
import com.paper.model.Rect
import com.paper.model.ScrapModel
import com.paper.model.sketch.PathTuple
import com.paper.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException

class PaperWidget(private val mUiScheduler: Scheduler,
                  private val mWorkerScheduler: Scheduler)
    : IPaperWidget {

    // Model
    private lateinit var mModel: PaperModel
    private val mModelDisposables = CompositeDisposable()

    // Global canceller
    private val mCancelSignal = PublishSubject.create<Any>()

    // Scrap controllers
    private val mScrapWidgets = hashMapOf<UUID, IScrapWidget>()

    // Gesture
    private val mGestureHistory = mutableListOf<GestureRecord>()

    // Debug
    private val mDebugSignal = PublishSubject.create<String>()

    override fun bindModel(model: PaperModel) {
        ensureNoLeakedBinding()

        // Hold reference.
        mModel = model

        mSetCanvasSize.onNext(Rect(0f, 0f, mModel.width, mModel.height))

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
                    val widget = mScrapWidgets[scrapM.uuid] ?: throw NoSuchElementException("Cannot find the widget")

                    widget.unbindModel()

                    // Signal the removing event.
                    mRemoveWidgetSignal.onNext(widget)
                })

        println("${DomainConst.TAG}: Bind paper \"Widget\" to a paper model(w=${model.width}, h=${model.height})")
    }

    override fun unbindModel() {
        mModelDisposables.clear()

        println("${DomainConst.TAG}: Unbind paper \"Widget\" from the paper model")
    }

    // Save ///////////////////////////////////////////////////////////////////

    override fun handleSetThumbnail(file: File,
                                    width: Int,
                                    height: Int) {
        mModel.thumbnailPath = file
        mModel.thumbnailWidth = width
        mModel.thumbnailHeight = height
    }

    override fun getPaper(): PaperModel {
        return mModel
    }

    // Add & Remove Stroke ////////////////////////////////////////////////////




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

    override fun handleChoosePenColor(color: Int) {
        mPenColor = color
    }

    override fun handleUpdatePenSize(size: Float) {
        mPenSize = size
    }

    override fun handleDragBegin(x: Float,
                                 y: Float) {
        // Clear all the delayed execution.
        mCancelSignal.onNext(0)

        // Create a new stroke and hold it in the temporary pool.
        mTmpStroke = SketchStroke(
            color = mPenColor,
            isEraser = false,
            width = mPenSize)
        mTmpStroke.addPathTuple(PathTuple(x, y))

        mLineToSignal.onNext(Point(x, y))

        mLineToSignal
            // FIXME: The window filter would make the SVGDrawable laggy.
            .throttleFirst(DomainConst.COLLECT_PATH_WINDOW_MS,
                           TimeUnit.MILLISECONDS, mUiScheduler)
            .takeUntil(mCancelDrawingSignal)
            .observeOn(mUiScheduler)
            .subscribe { p ->
                mTmpStroke.addPathTuple(PathTuple(p.x, p.y, p.time))

                // Notify the observer
                mDrawSVGSignal.onNext(DrawSVGEvent(action = LINE_TO,
                                                   point = Point(p.x, p.y, p.time)))
            }

        // Notify the observer
        mDrawSVGSignal.onNext(DrawSVGEvent(action = MOVE,
                                           point = Point(x, y),
                                           penColor = mPenColor,
                                           penSize = mPenSize))
    }

    override fun handleDrag(x: Float,
                            y: Float) {
        mLineToSignal.onNext(Point(x, y))
    }

    override fun handleDragEnd(x: Float,
                               y: Float) {
        // Brutally stop the drawing filter.
        mCancelDrawingSignal.onNext(0)

        mTmpStroke.addPathTuple(PathTuple(x, y))
        mModel.addStrokeToSketch(mTmpStroke)

        // Notify the observer
        mDrawSVGSignal.onNext(DrawSVGEvent(action = CLOSE))
    }

    private val mSetCanvasSize = BehaviorSubject.create<Rect>()

    override fun onSetCanvasSize(): Observable<Rect> {
        return mSetCanvasSize
    }

    override fun onDrawSVG(): Observable<DrawSVGEvent> {
        return mDrawSVGSignal
    }

    // Gesture ////////////////////////////////////////////////////////////////

    override fun handleActionBegin() {
    }

    override fun handleActionEnd() {
        // Brutally stop the drawing filter.
        mCancelDrawingSignal.onNext(0)
    }

    override fun handleTap(x: Float, y: Float) {
        // Draw a DOT!!!
        mTmpStroke = SketchStroke(
            color = 0,
            isEraser = false,
            width = 1f)
        mTmpStroke.addPathTuple(PathTuple(x, y))

        mModel.addStrokeToSketch(mTmpStroke)

        // Notify the observer
        mDrawSVGSignal.onNext(DrawSVGEvent(action = MOVE,
                                           point = Point(x, y),
                                           penColor = mPenColor,
                                           penSize = mPenSize))
        mDrawSVGSignal.onNext(DrawSVGEvent(action = CLOSE))
    }

    // Debug //////////////////////////////////////////////////////////////////

    override fun onPrintDebugMessage(): Observable<String> {
        return mDebugSignal
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun ensureNoLeakedBinding() {
        if (mModelDisposables.size() > 0)
            throw IllegalStateException("Already bind a model")
    }

    private fun addScrapAtPosition(x: Float,
                                   y: Float) {
        val scrap = ScrapModel(UUID.randomUUID())
        scrap.x = x
        scrap.y = y

        mModel.addScrap(scrap)
    }
}
