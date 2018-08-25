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
import com.paper.domain.ui_event.*
import com.paper.model.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
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
    private val mScrapWidgets = ConcurrentHashMap<UUID, BaseScrapWidget>()

    // Debug
    private val mDebugSignal = PublishSubject.create<String>()

    override fun start() {
        ensureNoLeakedBinding()

        val paper = mPaper!!

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

                    addScrap(widget)
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

    override fun onInitCanvasSize(): Single<Pair<Float, Float>> {
        val paper = mPaper!!
        return Single.just(paper.getSize())
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

    // Scrap manipulation /////////////////////////////////////////////////////

    private val mUpdateScrapSignal = PublishSubject.create<UpdateScrapEvent>().toSerialized()

    override fun onUpdateCanvas(): Observable<UpdateScrapEvent> {
        return mUpdateScrapSignal
    }

    override fun handleDomainEvent(event: CanvasDomainEvent,
                                   ifOutputOperation: Boolean) {
        when (event) {
            is AddScrapEvent -> addScrap(event.scrap as BaseScrapWidget)
            is RemoveScrapEvent -> removeScrap(event.scrap as BaseScrapWidget)
            is RemoveAllScrapsEvent -> removeAllScraps()
            is FocusScrapEvent -> focusScrapWidget(event.scrapID)

            is StartSketchEvent -> startSketch(event.x, event.y)
            is DoSketchEvent -> doSketch(event.x, event.y)
            is StopSketchEvent -> stopSketch()
        }
    }

    private fun addScrap(scrap: BaseScrapWidget) {
        synchronized(mLock) {
            mScrapWidgets[scrap.getID()] = scrap

            // Signal out
            mUpdateScrapSignal.onNext(AddScrapEvent(scrap))
        }
    }

    private fun removeScrap(scrap: BaseScrapWidget) {
        synchronized(mLock) {
            mScrapWidgets.remove(scrap.getID())

            // Clear focus
            if (mFocusScrapWidget == scrap) {
                mFocusScrapWidget = null
            }

            // Signal out
            mUpdateScrapSignal.onNext(RemoveScrapEvent(scrap))
        }
    }

    private fun removeAllScraps() {
        synchronized(mLock) {
            // Prepare remove events
            val events = mutableListOf<RemoveScrapEvent>()
            mScrapWidgets.forEach { (_, widget) ->
                events.add(RemoveScrapEvent(widget))
            }

            // Remove all
            mScrapWidgets.clear()

            // Signal out
            mUpdateScrapSignal.onNext(GroupUpdateScrapEvent(events))
        }
    }

    private fun focusScrapWidget(id: UUID): IBaseScrapWidget? {
        synchronized(mLock) {
            val widget = mScrapWidgets[id]!!
            mFocusScrapWidget = widget

            // Signal out
            mUpdateScrapSignal.onNext(FocusScrapEvent(widget.getID()))

            return mFocusScrapWidget
        }
    }

    // Drawing ////////////////////////////////////////////////////////////////

    private fun startSketch(x: Float, y: Float) {
        synchronized(mLock) {
            val widget = mFocusScrapWidget!! as SVGScrapWidget

            // TODO: Determine style
            widget.moveTo(x, y)
        }
    }

    private fun doSketch(x: Float, y: Float) {
        synchronized(mLock) {
            val widget = mFocusScrapWidget!! as SVGScrapWidget
            val frame = widget.getFrame()
            val nx = x - frame.x
            val ny = y - frame.y

            // TODO: Use cubic line?
            widget.cubicTo(nx, ny,
                           nx, ny,
                           nx, ny)
        }
    }

    private fun stopSketch() {
        synchronized(mLock) {
            val widget = mFocusScrapWidget!! as SVGScrapWidget

            widget.close()
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
