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

package com.paper.editor.widget

import android.graphics.PointF
import android.util.Log
import com.paper.AppConst
import com.paper.editor.data.DrawSVGEvent
import com.paper.editor.data.DrawSVGEvent.Action.*
import com.paper.editor.data.GestureRecord
import com.paper.editor.data.Size
import com.paper.shared.model.PaperModel
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.sketch.PathTuple
import com.paper.shared.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.*
import kotlin.NoSuchElementException

class PaperWidget(private val mUiScheduler: Scheduler,
                  private val mWorkerScheduler: Scheduler)
    : IPaperWidget {

    // Model
    private lateinit var mModel: PaperModel
    private val mModelDisposables = CompositeDisposable()

    // Scrap controllers
    private val mScrapWidgets = hashMapOf<UUID, IScrapWidget>()
    private val mAddWidgetSignal = PublishSubject.create<IScrapWidget>()
    private val mRemoveWidgetSignal = PublishSubject.create<IScrapWidget>()

    // Gesture detector
    private val mGestureHistory = mutableListOf<GestureRecord>()

    // Drawing
    private val mStrokes = mutableListOf<SketchStroke>()
    private val mDrawSVGSignal = PublishSubject.create<DrawSVGEvent>()

    override fun bindModel(model: PaperModel) {
        ensureNoLeakedBinding()

        // Hold reference.
        mModel = model

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

        Log.d(AppConst.TAG, "bind to a model(w=${model.width}, h=${model.height})")
    }

    override fun unbindModel() {
        mModelDisposables.clear()

        Log.d(AppConst.TAG, "unbind from the model")
    }

    override fun onSetCanvasSize(): Observable<Size> {
        return Observable.just(Size(mModel.width, mModel.height))
    }

    // Gesture ////////////////////////////////////////////////////////////////

    override fun handleActionBegin() {
    }

    override fun handleActionEnd() {
    }

    override fun handleTap(x: Float, y: Float) {
        addScrapAtPosition((mModel.width * Math.random()).toFloat(),
                           (mModel.height * Math.random()).toFloat())
    }

    // Add & Remove Scrap /////////////////////////////////////////////////////

    override fun onAddScrapWidget(): Observable<IScrapWidget> {
        return mAddWidgetSignal
    }

    override fun onRemoveScrapWidget(): Observable<IScrapWidget> {
        return mRemoveWidgetSignal
    }

    // Drawing ////////////////////////////////////////////////////////////////

    override fun handleDragBegin(x: Float,
                                 y: Float) {
        val stroke = SketchStroke(
            color = 0,
            isEraser = false,
            width = 1f)
        stroke.addPathTuple(PathTuple(x, y))

        mStrokes.add(stroke)

        // Notify the observer
        mDrawSVGSignal.onNext(DrawSVGEvent(action = MOVE,
                                           point = PointF(x, y)))
    }

    override fun handleDrag(x: Float,
                            y: Float) {
        val stroke = mStrokes[mStrokes.size - 1]

        stroke.addPathTuple(PathTuple(x, y))

        // Notify the observer
        mDrawSVGSignal.onNext(DrawSVGEvent(action = LINE_TO,
                                           point = PointF(x, y)))
    }

    override fun handleDragEnd(x: Float,
                               y: Float) {
        // TODO: Set a timer and create a Scrap when time is up.

        // Notify the observer
        mDrawSVGSignal.onNext(DrawSVGEvent(action = CLOSE,
                                           point = PointF(x, y)))
    }

    override fun onDrawSVG(): Observable<DrawSVGEvent> {
        return mDrawSVGSignal
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
