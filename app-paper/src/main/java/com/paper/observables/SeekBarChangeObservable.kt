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

import com.paper.event.ProgressEvent

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

/**
 * An observable wrapper for [SeekBar]. It emits the [ProgressEvent] to
 * downstream to indicate the progress of the seek-bar, also the start-doing-stop
 * touch information.
 */
class SeekBarChangeObservable(view: SeekBar,
                              byUserOnly: Boolean = false)
    : Observable<ProgressEvent>() {

    private val mView = view
    private val mByUserOnly = byUserOnly

    override fun subscribeActual(observer: Observer<in ProgressEvent>) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            observer.onError(IllegalStateException(
                "Expected to be called on the main thread but was " +
                Thread.currentThread().name))
        }

        val listener = Listener(mView, mByUserOnly, observer)
        mView.setOnSeekBarChangeListener(listener)
        observer.onSubscribe(listener)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    internal class Listener(view: SeekBar,
                            byUserOnly: Boolean,
                            private val observer: Observer<in ProgressEvent>)
        : MainThreadDisposable(),
          SeekBar.OnSeekBarChangeListener {

        private val mView = view
        private val mByUserOnly = byUserOnly

        override fun onProgressChanged(seekBar: SeekBar,
                                       progress: Int,
                                       fromUser: Boolean) {

            if (!isDisposed) {
                if (mByUserOnly) {
                    if (mByUserOnly == fromUser) {
                        observer.onNext(ProgressEvent.doing(progress))
                    }
                } else {
                    observer.onNext(ProgressEvent.doing(progress))
                }
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            if (!isDisposed) {
                observer.onNext(ProgressEvent.start(mView.progress))
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            if (!isDisposed) {
                observer.onNext(ProgressEvent.stop(mView.progress))
            }
        }

        override fun onDispose() {
            mView.setOnSeekBarChangeListener(null)
        }
    }
}
