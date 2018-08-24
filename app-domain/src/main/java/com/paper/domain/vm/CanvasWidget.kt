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

package com.paper.domain.vm

import com.paper.domain.ISchedulerProvider
import com.paper.domain.data.DrawingMode
import com.paper.domain.vm.operation.AddScrapOperation
import com.paper.domain.vm.operation.RemoveScrapOperation
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
    private var mFocusScrapWidget: IScrap? = null
    // Scrap controllers
    private val mScrapWidgets = ConcurrentHashMap<UUID, IBaseScrapWidget>()

    // Debug
    private val mDebugSignal = PublishSubject.create<String>()

    override fun start() {
        ensureNoLeakedBinding()

        val paper = mPaper!!

        // Canvas size
        mCanvasSizeSignal.onNext(paper.getSize())
    }

    override fun stop() {
        mDisposables.clear()
    }

    override fun setModel(paper: IPaper) {
        if (mPaper != null) throw IllegalAccessException("Already set model")

        mPaper = paper
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

    private val mUpdateScrapSignal = PublishSubject.create<UpdateScrapEvent>().toSerialized()

    override fun getFocusScrap(): IScrap? {
        synchronized(mLock) {
            return mFocusScrapWidget
        }
    }

    override fun addScrapAndSetFocus(scrap: IScrap) {
        addScrap(scrap)

        synchronized(mLock) {
            mFocusScrapWidget = scrap

            // Signal out
            mUpdateScrapSignal.onNext(FocusScrapEvent(scrap))
        }
    }

    override fun removeScrapAndClearFocus(scrap: IScrap) {
        removeScrap(scrap)

        synchronized(mLock) {
            mFocusScrapWidget = null
        }
    }

    override fun addScrap(scrap: IScrap) {
        scrap as IBaseScrapWidget

        synchronized(mLock) {
            mScrapWidgets[scrap.getId()] = scrap

            // Signal out
            mUpdateScrapSignal.onNext(AddScrapEvent(scrap))
            // TODO: ADD operation holds immutable scrap?
            mOperationSignal.onNext(AddScrapOperation())
        }
    }

    override fun removeScrap(scrap: IScrap) {
        scrap as IBaseScrapWidget

        synchronized(mLock) {
            mScrapWidgets.remove(scrap.getId())

            // Signal out
            mUpdateScrapSignal.onNext(RemoveScrapEvent(scrap))
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

    override fun toPaper(): IPaper {
        synchronized(mLock) {
            return mPaper!!
        }
    }

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
