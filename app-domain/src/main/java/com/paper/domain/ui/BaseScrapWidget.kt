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
import com.paper.model.Frame
import com.paper.model.IScrap
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import java.util.*

open class BaseScrapWidget(protected val scrap: IScrap,
                           protected val schedulers: ISchedulerProvider)
    : IWidget {

    protected val lock = Any()

    private val frameDisposableBag = CompositeDisposable()
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
            frameDisposableBag.clear()
        }
    }

    fun getID(): UUID {
        return scrap.getID()
    }

    // Frame //////////////////////////////////////////////////////////////////

    private val frameSignal = PublishSubject.create<Frame>().toSerialized()

    fun getFrame(): Frame {
        synchronized(lock) {
            return scrap.getFrame()
        }
    }

    fun observeFrame(): Observable<Frame> {
        return frameSignal
    }

    fun handleFrameDisplacement(src: Observable<Frame>) {
        frameDisposableBag.clear()

        // Begin of the change
        src.firstElement()
            .subscribe { displacement ->
                // model frame + displacement
                frameSignal.onNext(getFrame().add(displacement))
            }
            .addTo(frameDisposableBag)

        // Doing some change
        src.skip(1)
            .skipLast(1)
            .subscribe { displacement ->
                // model frame + displacement
                frameSignal.onNext(getFrame().add(displacement))
            }
            .addTo(frameDisposableBag)

        // End of the change
        src.lastElement()
            .subscribe { displacement ->
                synchronized(lock) {
                    // model frame + displacement
                    frameSignal.onNext(getFrame().add(displacement))

                    // Commit to model
                    val current = scrap.getFrame()
                    scrap.setFrame(current.add(displacement))
                }
            }
            .addTo(frameDisposableBag)
    }
}
