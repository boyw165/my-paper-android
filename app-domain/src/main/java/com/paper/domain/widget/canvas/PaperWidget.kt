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

import android.util.Log
import com.paper.domain.DomainConst
import com.paper.domain.event.DrawSVGEvent
import com.paper.domain.event.DrawSVGEvent.Action.*
import com.paper.domain.data.GestureRecord
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
    private val mAddWidgetSignal = PublishSubject.create<IScrapWidget>()
    private val mRemoveWidgetSignal = PublishSubject.create<IScrapWidget>()

    // Gesture
    private val mGestureHistory = mutableListOf<GestureRecord>()

    // Debug
    private val mDebugSignal = PublishSubject.create<String>()

    override fun bindModel(model: PaperModel) {
        ensureNoLeakedBinding()

        // Hold reference.
        mModel = model

        mSetCanvasSize.onNext(Rect(0f, 0f, mModel.width, mModel.height))

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

        Log.d(DomainConst.TAG, "Bind to a model(w=${model.width}, h=${model.height})")
    }

    override fun unbindModel() {
        mModelDisposables.clear()

        Log.d(DomainConst.TAG, "Unbind from the model")
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

    // Gesture ////////////////////////////////////////////////////////////////

    override fun handleActionBegin() {
    }

    override fun handleActionEnd() {
        // Brutally stop the drawing filter.
        mCancelDrawingSignal.onNext(0)
    }

    override fun handleTap(x: Float, y: Float) {
        // Draw a DOT!!!
        val w = (Math.random()).toFloat()
        val h = (Math.random()).toFloat()
        val stroke = SketchStroke(
            color = 0,
            isEraser = false,
            width = 1f)
        stroke.addPathTuple(PathTuple(x, y))
        // Add to stroke collection
        mTmpStrokes.add(stroke)

        collectStrokesAndCreateScrap()
    }

    // Add & Remove Scrap /////////////////////////////////////////////////////

    override fun onAddScrapWidget(): Observable<IScrapWidget> {
        return mAddWidgetSignal
    }

    override fun onRemoveScrapWidget(): Observable<IScrapWidget> {
        return mRemoveWidgetSignal
    }

    // Drawing ////////////////////////////////////////////////////////////////

    /**
     * Temporary ...
     */
    private val mTmpStrokes = mutableListOf<SketchStroke>()
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
        val stroke = SketchStroke(
            color = mPenColor,
            isEraser = false,
            width = mPenSize)
        stroke.addPathTuple(PathTuple(x, y))
        mTmpStrokes.add(stroke)

        mLineToSignal.onNext(Point(x, y))

        mLineToSignal
            // FIXME: The window filter would make the SVGDrawable laggy.
            .throttleFirst(DomainConst.COLLECT_PATH_WINDOW_MS,
                           TimeUnit.MILLISECONDS, mUiScheduler)
            .takeUntil(mCancelDrawingSignal)
            .observeOn(mUiScheduler)
            .subscribe { p ->
                stroke.addPathTuple(PathTuple(p.x, p.y, p.time))

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

        val stroke = mTmpStrokes.last()
        stroke.addPathTuple(PathTuple(x, y))

        // Notify the observer
        mDrawSVGSignal.onNext(DrawSVGEvent(action = CLOSE))

        // Set a timer and create a Scrap when time is up.
        Observable
            .timer(DomainConst.COLLECT_STROKES_TIMEOUT_MS,
                   TimeUnit.MILLISECONDS,
                   mUiScheduler)
            .takeUntil(mCancelSignal)
            .subscribe {
                collectStrokesAndCreateScrap()
            }
    }

    private fun collectStrokesAndCreateScrap() {
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY

        mTmpStrokes.forEach { stroke ->
            val bound = stroke.bound

            left = Math.min(left, bound.left)
            top = Math.min(top, bound.top)
            right = Math.max(right, bound.right)
            bottom = Math.max(bottom, bound.bottom)
        }

        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f

        mTmpStrokes.forEach { stroke ->
            stroke.offset(-cx, -cy)
        }

        val scrapM = ScrapModel()
        scrapM.x = cx
        scrapM.y = cy
        scrapM.sketch.addAllStroke(mTmpStrokes)

        // Clear widget hold strokes
        mTmpStrokes.clear()

        // Add to Model (will trigger bound View to react)
        mModel.addScrap(scrapM)

        // Notify view to clear strokes
        mDrawSVGSignal.onNext(DrawSVGEvent(
            action = CLEAR_ALL))
    }

    private val mSetCanvasSize = BehaviorSubject.create<Rect>()

    override fun onSetCanvasSize(): Observable<Rect> {
        return mSetCanvasSize
    }

    override fun onDrawSVG(): Observable<DrawSVGEvent> {
        return mDrawSVGSignal
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
