// Copyright Jun 2018-present CardinalBlue
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

package com.paper.model.observables

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableHelper
import io.reactivex.internal.queue.SpscLinkedArrayQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * The Observable that emits the items emitted by the [src] Observable when a
 * second ObservableSource, [whenSrc], emits an true; It buffers all them items
 * emitted by the [src] when [whenSrc] emits an false.
 */
open class TakeWhenObservable<T>(private val src: ObservableSource<T>,
                                 private val whenSrc: ObservableSource<Boolean>,
                                 private val bufferSize: Int = Flowable.bufferSize())
    : Observable<T>() {

    override fun subscribeActual(observer: Observer<in T>) {
        val coordinator = Coordinator(src = src,
                                      whenSrc = whenSrc,
                                      actualObserver = observer,
                                      bufferSize = bufferSize)
        coordinator.subscribe()
    }

    internal class Coordinator<T>(val src: ObservableSource<T>,
                                  val whenSrc: ObservableSource<Boolean>,
                                  val actualObserver: Observer<in T>,
                                  val bufferSize: Int)
        : AtomicBoolean(false),
          Disposable {

        @Volatile
        private var canDrain = false
        private val queue = SpscLinkedArrayQueue<T>(bufferSize)

        private val srcObserver = SrcObserver(parent = this)
        private val whenObserver = WhenObserver(parent = this)

        fun subscribe() {
            actualObserver.onSubscribe(this)

            src.subscribe(srcObserver)
            whenSrc.subscribe(whenObserver)
        }

        fun bufferIt(t: T) {
            queue.offer(t)
            // Drop item when size is over the buffer size
            while (queue.size() > bufferSize) {
                queue.poll()
            }
        }

        fun canDrain(t: Boolean) {
            canDrain = t
        }

        fun drain() {
            synchronized(this) {
                while (!isDisposed &&
                       canDrain &&
                       !srcObserver.done &&
                       !queue.isEmpty) {
                    val t = queue.poll()!!
                    this.actualObserver.onNext(t)
                }
            }
        }

        fun onComplete() {
            if (!srcObserver.done) {
                actualObserver.onComplete()
            }
        }

        fun onError(err: Throwable) {
            actualObserver.onError(err)
        }

        override fun isDisposed(): Boolean {
            return get()
        }

        override fun dispose() {
            // Mark disposed!
            set(true)

            srcObserver.dispose()
            whenObserver.dispose()

            synchronized(this) {
                queue.clear()
            }
        }
    }

    internal class SrcObserver<T>(val parent: Coordinator<in T>)
        : Observer<T> {

        @Volatile
        var done = false

        val disposable = AtomicReference<Disposable>()

        override fun onComplete() {
            parent.onComplete()
            done = true
        }

        override fun onSubscribe(d: Disposable) {
            DisposableHelper.setOnce(disposable, d)
        }

        override fun onNext(t: T) {
            if (!done) {
                parent.bufferIt(t)
                parent.drain()
            }
        }

        override fun onError(e: Throwable) {
            if (!done) {
                parent.onError(e)
            }
        }

        fun dispose() {
            DisposableHelper.dispose(disposable)
        }
    }

    internal class WhenObserver<T>(val parent: Coordinator<in T>)
        : Observer<Boolean> {

        @Volatile
        var done = false

        val disposable = AtomicReference<Disposable>()

        override fun onComplete() {
            // Close the throttle permanently
            parent.canDrain(false)
        }

        override fun onSubscribe(d: Disposable) {
            DisposableHelper.setOnce(disposable, d)
        }

        override fun onNext(t: Boolean) {
            if (!done) {
                parent.canDrain(t)
                parent.drain()
            }
        }

        override fun onError(e: Throwable) {
            if (!done) {
                parent.onError(e)
            }
        }

        fun dispose() {
            DisposableHelper.dispose(disposable)
        }
    }
}
