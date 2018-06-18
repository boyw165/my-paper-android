// Copyright Apr 2017-present CardinalBlue
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

package com.paper.observables

import android.os.Looper
import android.widget.SeekBar

import com.paper.domain.event.ProgressBarEvent

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

/**
 * An observable wrapper for [SeekBar]. It emits the [ProgressBarEvent] to
 * downstream to indicate the progress of the seek-bar, also the start-doing-stop
 * touch information.
 */
class SeekBarChangeObservable(view: SeekBar)
    : Observable<ProgressBarEvent>() {

    private val mView = view

    override fun subscribeActual(observer: Observer<in ProgressBarEvent>) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            observer.onError(IllegalStateException(
                "Expected to be called on the main thread but was " +
                Thread.currentThread().name))
        }

        val listener = Listener(mView, observer)
        mView.setOnSeekBarChangeListener(listener)
        observer.onSubscribe(listener)
    }

    // Clazz //////////////////////////////////////////////////////////////////

    internal class Listener(view: SeekBar,
                            private val observer: Observer<in ProgressBarEvent>)
        : MainThreadDisposable(),
          SeekBar.OnSeekBarChangeListener {

        private val mView = view

        override fun onProgressChanged(seekBar: SeekBar,
                                       progress: Int,
                                       fromUser: Boolean) {

            if (!isDisposed) {
                observer.onNext(ProgressBarEvent.doing(progress = progress,
                                                                              fromUser = fromUser))
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            if (!isDisposed) {
                observer.onNext(ProgressBarEvent.start(fromUser = true))
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            if (!isDisposed) {
                observer.onNext(ProgressBarEvent.stop(fromUser = true))
            }
        }

        override fun onDispose() {
            mView.setOnSeekBarChangeListener(null)
        }
    }
}
