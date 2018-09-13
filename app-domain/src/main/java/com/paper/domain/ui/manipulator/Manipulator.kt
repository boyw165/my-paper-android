// Copyright Aug 2018-present CardinalBlue
//
// Author: boy@cardinalblue.com
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

package com.paper.domain.ui.manipulator

import com.cardinalblue.gesture.rx.GestureEvent
import com.paper.domain.DomainConst
import com.paper.model.ISchedulers
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import java.util.concurrent.atomic.AtomicBoolean

abstract class Manipulator(protected val schedulers: ISchedulers)
    : ObservableTransformer<GestureEvent, WhiteboardCommand> {

    protected val disposableBag = CompositeDisposable()

    final override fun apply(touchSequence: Observable<GestureEvent>): ObservableSource<WhiteboardCommand> {
        return Observable.create<WhiteboardCommand> { emitter ->
            // Enable auto stop when downstream asks terminating
            emitter.setCancellable {
                disposableBag.dispose()
            }

            val handleIt = AtomicBoolean(false)
            val sharedTouchSequence = touchSequence.publish()

            // Start
            sharedTouchSequence
                .firstElement()
                .observeOn(schedulers.main())
                .subscribe { event ->
                    handleIt.set(isMyBusiness(event))

                    if (handleIt.get()) {
                        println("${DomainConst.TAG}: ${javaClass.simpleName} starts")
                        onFirst(event)
                    } else {
                        emitter.onComplete()
                    }
                }
                .addTo(disposableBag)

            // Rest except first and last
            sharedTouchSequence
                .skip(1)
                .skipLast(1)
                .observeOn(schedulers.main())
                .subscribe { event ->
                    if (handleIt.get()) {
                        onInBetweenFirstAndLast(event)
                    }
                }
                .addTo(disposableBag)

            sharedTouchSequence
                .lastElement()
                .observeOn(schedulers.main())
                .subscribe { event ->
                    if (handleIt.get()) {
                        val c = onLast(event)
                        println("${DomainConst.TAG}: ${javaClass.simpleName} stops")

                        emitter.onNext(c)
                        emitter.onComplete()
                    }
                }
                .addTo(disposableBag)

            // Activate the upstream
            sharedTouchSequence
                .connect()
                .addTo(disposableBag)
        }
    }

    protected abstract fun isMyBusiness(event: GestureEvent): Boolean

    protected abstract fun onFirst(event: GestureEvent)

    protected abstract fun onInBetweenFirstAndLast(event: GestureEvent)

    protected abstract fun onLast(event: GestureEvent): WhiteboardCommand
}
