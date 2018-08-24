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

package com.paper.domain.ui

import com.paper.domain.ISchedulerProvider
import com.paper.domain.data.DrawingMode
import com.paper.domain.ui.operation.AddScrapOperation
import com.paper.domain.ui.operation.RemoveScrapOperation
import com.paper.domain.event.AddScrapWidgetEvent
import com.paper.domain.event.FocusScrapWidgetEvent
import com.paper.domain.event.RemoveScrapWidgetEvent
import com.paper.domain.event.UpdateScrapWidgetEvent
import com.paper.model.*
import com.paper.model.sketch.SVGStyle
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class CanvasWidget(private val schedulers: ISchedulerProvider)
    : ICanvasWidget {

    private val mLock = Any()

    private var mPaper: IPaper? = null

    private val mDisposables = CompositeDisposable()

    // Focus scrap controller
    @Volatile
    private var mFocusScrapWidget: IBaseScrapWidget? = null
    // Scrap controllers
    private val mScrapWidgets = ConcurrentHashMap<UUID, IBaseScrapWidget>()

    // Debug
    private val mDebugSignal = PublishSubject.create<String>()

    override fun start() {
        ensureNoLeakedBinding()

        val paper = mPaper!!

        // Canvas size
        mCanvasSizeSignal.onNext(paper.getSize())

        inflateScrapWidgets(paper)
    }

    private fun inflateScrapWidgets(paper: IPaper) {
        val scraps = paper.getScraps()
        scraps.forEach { scrap ->
            when (scrap) {
                is ISVGScrap -> {
                    val widget = SVGScrapWidget(
                        scrap = scrap,
                        schedulers = schedulers)

                    addScrapWidget(widget)
                }
                else -> TODO()
            }
        }
    }

    override fun stop() {
        mDisposables.clear()
    }

    override fun setModel(paper: IPaper) {
        if (mPaper != null) throw IllegalAccessException("Already set model")

        mPaper = paper
    }

    override fun toPaper(): IPaper {
        synchronized(mLock) {
            return mPaper!!
        }
    }

    private fun ensureNoLeakedBinding() {
        if (mDisposables.size() > 0)
            throw IllegalStateException("Already start a model")
    }

    // Debug //////////////////////////////////////////////////////////////////

    override fun onPrintDebugMessage(): Observable<String> {
        return mDebugSignal
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val mDirtyFlag = CanvasDirtyFlag()

    fun onBusy(): Observable<Boolean> {
        return mDirtyFlag
            .onUpdate()
            .map { event ->
                // Ready iff flag is zero
                event.flag == 0
            }
    }

    // Add & Remove Scrap /////////////////////////////////////////////////////

    private val mUpdateScrapSignal = PublishSubject.create<UpdateScrapWidgetEvent>().toSerialized()

    override fun getFocusScrap(): IBaseScrapWidget? {
        synchronized(mLock) {
            return mFocusScrapWidget
        }
    }

    override fun addScrapWidgetAndSetFocus(scrapWidget: IBaseScrapWidget) {
        addScrapWidget(scrapWidget)

        synchronized(mLock) {
            mFocusScrapWidget = scrapWidget

            // Signal out
            mUpdateScrapSignal.onNext(FocusScrapWidgetEvent(scrapWidget))
        }
    }

    override fun addScrapWidget(scrapWidget: IBaseScrapWidget) {
        synchronized(mLock) {
            mScrapWidgets[scrapWidget.getID()] = scrapWidget

            // Signal out
            mUpdateScrapSignal.onNext(AddScrapWidgetEvent(scrapWidget))
            // TODO: ADD operation holds immutable scrap?
            mOperationSignal.onNext(AddScrapOperation())
        }
    }

    override fun removeScrapWidget(scrapWidget: IBaseScrapWidget) {
        synchronized(mLock) {
            mScrapWidgets.remove(scrapWidget.getID())

            // Clear focus
            if (mFocusScrapWidget == scrapWidget) {
                mFocusScrapWidget = null
            }

            // Signal out
            mUpdateScrapSignal.onNext(RemoveScrapWidgetEvent(scrapWidget))
            // TODO: REMOVE operation holds immutable scrap?
            mOperationSignal.onNext(RemoveScrapOperation())
        }
    }

    override fun onUpdateScrap(): Observable<UpdateScrapWidgetEvent> {
        return mUpdateScrapSignal
    }

    override fun eraseCanvas() {
        synchronized(mLock) {
            TODO("not implemented")
        }
    }

    private val mCanvasSizeSignal = BehaviorSubject.create<Pair<Float, Float>>().toSerialized()

    override fun onUpdateCanvasSize(): Observable<Pair<Float, Float>> {
        return mCanvasSizeSignal
    }

    // Drawing ////////////////////////////////////////////////////////////////

    override fun startSketch(x: Float, y: Float) {
        synchronized(mLock) {
            val widget = SVGScrapWidget(
                scrap = SVGScrap(mutableFrame = Frame(x, y)),
                schedulers = schedulers)
            // TODO: Define the style
            widget.moveTo(x = 0f, y = 0f,
                          style = setOf(SVGStyle.Stroke(color = Color.RED,
                                                        size = 0.1f,
                                                        closed = false)))
            addScrapWidgetAndSetFocus(widget)
        }
    }

    override fun sketchTo(x: Float, y: Float) {
        synchronized(mLock) {
            val widget = mFocusScrapWidget!! as ISVGScrapWidget
            val frame = widget.getFrame()
            val startX = frame.x
            val startY = frame.y

            // TODO: Use cubic line?
            widget.lineTo(x - startX, y - startY)

            TODO()
        }
    }

    override fun closeSketch() {
        synchronized(mLock) {
            val widget = mFocusScrapWidget!! as ISVGScrapWidget

            TODO()
        }
    }

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

    override fun setDrawingMode(mode: DrawingMode) {
        synchronized(mLock) {
            TODO()
        }
    }

    override fun setChosenPenColor(color: Int) {
        synchronized(mLock) {
            mPenColor = color
        }
    }

    override fun setViewPortScale(scale: Float) {
        synchronized(mLock) {
            mViewPortScale = scale
        }
    }

    override fun setPenSize(size: Float) {
        synchronized(mLock) {
            mPenSize = size
        }
    }

    // Operation & undo/redo //////////////////////////////////////////////////

    private val mOperationSignal = PublishSubject.create<ICanvasOperation>().toSerialized()

    override fun onUpdateCanvasOperation(): Observable<ICanvasOperation> {
        return mOperationSignal
    }

    // Equality ///////////////////////////////////////////////////////////////

    override fun toString(): String {
        return mPaper?.let { paper ->
            "${javaClass.simpleName}{\n" +
            "id=${paper.getID()}, uuid=${paper.getUUID()}\n" +
            "scraps=${paper.getScraps().size}\n" +
            "}"
        } ?: "${javaClass.simpleName}{no model}"
    }
}
