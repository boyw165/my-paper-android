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

import com.cardinalblue.gesture.rx.GestureEvent
import com.paper.domain.ui.manipulator.DragManipulator
import com.paper.model.BaseScrap
import com.paper.model.Frame
import com.paper.model.ISchedulers
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import java.util.*
import java.util.concurrent.atomic.AtomicReference

open class BaseScrapWidget(protected val scrap: BaseScrap,
                           protected val schedulers: ISchedulers)
    : IWidget {

    companion object {

        @JvmStatic
        val GENERAL_BUSY = 1

        @JvmStatic
        val EMPTY_FRAME_DISPLACEMENT = Frame()
    }

    protected val lock = Any()

    protected val dirtyFlag = ScrapDirtyFlag(0)

    private val touchSequenceDisposableBag = CompositeDisposable()
    protected val staticDisposableBag = CompositeDisposable()

    override fun start(): Observable<Boolean> {
        return autoStop {
            scrap.observeFrame()
                .subscribe { frame ->
                    frameSignal.onNext(frame)
                }
                .addTo(staticDisposableBag)
        }
    }

    override fun stop() {
        synchronized(lock) {
            staticDisposableBag.clear()
            touchSequenceDisposableBag.clear()
        }
    }

    open fun getID(): UUID {
        return scrap.getID()
    }

    // Frame //////////////////////////////////////////////////////////////////

    private val frameDisplacement = AtomicReference(EMPTY_FRAME_DISPLACEMENT)
    private val frameSignal = PublishSubject.create<Frame>().toSerialized()

    open fun getFrame(): Frame {
        return synchronized(lock) {
            val actual = scrap.getFrame()
            val displacement = frameDisplacement.get()
            actual.add(displacement)
        }
    }

    open fun setFrame(frame: Frame) {
        synchronized(lock) {
            frameDisplacement.set(EMPTY_FRAME_DISPLACEMENT)
            scrap.setFrame(frame)
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

    open fun handleTouchSequence(touchSequence: Observable<GestureEvent>) {
        touchSequenceDisposableBag.clear()

        touchSequence
            // TODO: Manipulator coordinator
            .compose(DragManipulator(widget = this))
            .subscribe()
            .addTo(touchSequenceDisposableBag)
    }

    fun observeFrame(): Observable<Frame> {
        return frameSignal
    }

    // Busy ///////////////////////////////////////////////////////////////////

    fun observeBusy(): Observable<Boolean> {
        return dirtyFlag
            .onUpdate()
            .map { event ->
                event.flag != 0
            }
    }
}
