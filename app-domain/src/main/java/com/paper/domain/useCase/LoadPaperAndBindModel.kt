// Copyright Apr 2018-present Paper
//
// Author: boyw165@gmail.com
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

package com.paper.domain.useCase

import com.paper.domain.event.ProgressEvent
import com.paper.domain.widget.editor.IPaperWidget
import com.paper.model.repository.IPaperRepo
import io.reactivex.*
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.Subject

// .------.
// |      | ----> true or false
// |  ob  |
// |      | ----> ProgressEvent
// '------'

/**
 * An observable to load paper from DB and then bind widget with the model.
 * True if the model is successfully loaded and binding is done. False means
 * neither the model loading nor binding works.
 *
 * There is also a side-effect that it sends ProgressEvent through the given
 * progress signal.
 */
class LoadPaperAndBindModel(paperID: Long,
                            paperWidget: IPaperWidget,
                            paperRepo: IPaperRepo,
                            updateProgressSignal: Subject<ProgressEvent>,
                            uiScheduler: Scheduler)
    : Single<Boolean>() {

    private val mPaperID = paperID

    private val mPaperWidget = paperWidget
    private val mPaperRepo = paperRepo

    private val mUpdateProgressSignal = updateProgressSignal

    private val mUiScheduler = uiScheduler

    override fun subscribeActual(observer: SingleObserver<in Boolean>) {
        val actualSrc = mPaperRepo
            .getPaperById(mPaperID)
            .toObservable()
            .publish()
        val actualDisposable = actualSrc
            .observeOn(mUiScheduler)
            .subscribe(
                { paper ->
                    // Bind widget with data.
                    mPaperWidget.bindModel(paper)

                    observer.onSuccess(true)

                    mUpdateProgressSignal.onNext(ProgressEvent.stop())
                }, { err ->
                    observer.onError(err)

                    mUpdateProgressSignal.onNext(ProgressEvent.stop())
                })

        val d = InnerDisposable(widget = mPaperWidget,
                                actualDisposable = actualDisposable)
        observer.onSubscribe(d)

        if (!d.isDisposed) {
            // Right after the subscription, send a START progress event.
            mUpdateProgressSignal.onNext(ProgressEvent.start())
        }

        if (!d.isDisposed) {
            // Then start the graph
            actualSrc.connect()
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    internal class InnerDisposable(widget: IPaperWidget,
                                   actualDisposable: Disposable) : Disposable {

        private var mDisposed = false

        private val mPaperWidget = widget
        private val mActualDisposable = actualDisposable

        override fun isDisposed(): Boolean {
            return mDisposed
        }

        override fun dispose() {
            mActualDisposable.dispose()

            mPaperWidget.unbindModel()

            mDisposed = true
        }
    }
}
