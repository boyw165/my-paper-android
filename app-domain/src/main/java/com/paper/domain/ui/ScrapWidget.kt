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

import android.location.SettingInjectorService
import com.cardinalblue.gesture.rx.GestureObservable
import com.paper.domain.DomainConst
import com.paper.domain.store.WhiteboardStore
import com.paper.domain.ui.manipulator.DragManipulator
import com.paper.model.Frame
import com.paper.model.ISchedulers
import com.paper.model.Scrap
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import org.koin.core.KoinContext
import java.util.*
import java.util.concurrent.atomic.AtomicReference

open class ScrapWidget(protected val scrap: Scrap,
                       protected val schedulers: ISchedulers)
    : IWidget {

    protected val lock = Any()

    protected val staticDisposableBag = CompositeDisposable()

    override fun start(): Observable<Boolean> {
        return autoStop {
            scrap.observeFrame()
                .subscribe { frame ->
                    // Clear displacement
                    synchronized(lock) {
                        frameDisplacement.set(DomainConst.EMPTY_FRAME_DISPLACEMENT)
                    }
                    // Signal out
                    frameSignal.onNext(frame)
                }
                .addTo(staticDisposableBag)
        }
    }

    override fun stop() {
        synchronized(lock) {
            staticDisposableBag.clear()
        }
    }

    open fun getID(): UUID {
        return scrap.getID()
    }

    // Frame //////////////////////////////////////////////////////////////////

    private val frameDisplacement = AtomicReference(DomainConst.EMPTY_FRAME_DISPLACEMENT)
    private val frameSignal = PublishSubject.create<Frame>().toSerialized()

    open fun getFrame(): Frame {
        return synchronized(lock) {
            val actual = scrap.getFrame()
            val displacement = frameDisplacement.get()
            actual.add(displacement)
        }
    }

    open fun setFrameDisplacement(displacement: Frame) {
        synchronized(lock) {
            frameDisplacement.set(displacement)

            // Signal out
            val actual = scrap.getFrame()
            frameSignal.onNext(actual.add(displacement))
        }
    }

//    val store: WhiteboardStore by inject {
//
//    }

    open fun handleTouchSequence(gestureSequence: Observable<GestureObservable>) {
//        gestureSequence
//            .observeOn(schedulers.main())
//            .flatMapCompletable(DragManipulator(scrapWidget = this@ScrapWidget,
//                                                whiteboardStore =,
//                                                undoWidget =,
//                                                schedulers = schedulers), true)
//            .subscribe()
//            .addTo(staticDisposableBag)
    }

    fun observeFrame(): Observable<Frame> {
        return frameSignal
    }

    // Busy ///////////////////////////////////////////////////////////////////

    private val dirtyFlag = ScrapDirtyFlag(0)

    fun markBusy() {
        dirtyFlag.markDirty(DomainConst.BUSY)
    }

    fun markNotBusy() {
        dirtyFlag.markNotDirty(DomainConst.BUSY)
    }

    fun observeBusy(): Observable<Boolean> {
        return dirtyFlag
            .onUpdate()
            .map { event ->
                event.flag != 0
            }
    }
}
