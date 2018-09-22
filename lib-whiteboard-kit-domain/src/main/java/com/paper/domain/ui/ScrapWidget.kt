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

import com.paper.domain.DomainConst
import com.paper.domain.ui.manipulator.IUserTouchManipulator
import com.paper.model.Frame
import com.paper.model.IBundle
import com.paper.model.Scrap
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.useful.rx.GestureEvent
import java.util.*
import java.util.concurrent.atomic.AtomicReference

open class ScrapWidget(protected val scrap: Scrap)
    : IWidget {

    protected val lock = Any()

    // User touch
    var userTouchManipulator: IUserTouchManipulator? = null
        get() {
            throw IllegalAccessException()
        }
        set(value) {
            synchronized(lock) {
                field = value
            }
        }
    private val gestureSequenceSignal = PublishSubject.create<Observable<Observable<GestureEvent>>>()
        .toSerialized()

    protected val staticDisposableBag = CompositeDisposable()

    override fun start() {
        // Frame
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

        // User touch
        gestureSequenceSignal
            .switchMapCompletable { gestureSequence ->
                println("${DomainConst.TAG}: A new gesture sequence is given")

                synchronized(lock) {
                    userTouchManipulator?.apply(gestureSequence) ?:
                    Completable.complete()
                }
            }
            .subscribe {
                println("${DomainConst.TAG}: The gesture sequence is finished")
            }
            .addTo(staticDisposableBag)
    }

    override fun stop() {
        staticDisposableBag.clear()
    }

    override fun saveStates(bundle: IBundle) {
        // DO NOTHING
    }

    override fun restoreStates(bundle: IBundle) {
        // DO NOTHING
    }

    open fun getID(): UUID {
        return scrap.getID()
    }

    // Frame //////////////////////////////////////////////////////////////////

    private val frameDisplacement = AtomicReference(DomainConst.EMPTY_FRAME_DISPLACEMENT)
    private val frameSignal = BehaviorSubject.create<Frame>().toSerialized()

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

    val frame: Observable<Frame> = frameSignal.hide()

    fun handleUserTouch(gestureSequence: Observable<Observable<GestureEvent>>) {
        gestureSequenceSignal.onNext(gestureSequence)
    }

    // Busy ///////////////////////////////////////////////////////////////////

    private val dirtyFlag = ScrapDirtyFlag(0)

    fun markBusy() {
        dirtyFlag.markDirty(DomainConst.BUSY)
    }

    fun markNotBusy() {
        dirtyFlag.markNotDirty(DomainConst.BUSY)
    }

    val busy: Observable<Boolean> get() {
        return dirtyFlag
            .updated()
            .map { event ->
                event.flag != 0
            }
    }
}
