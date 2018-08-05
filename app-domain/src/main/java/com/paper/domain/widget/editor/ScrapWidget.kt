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

package com.paper.domain.widget.editor

import com.paper.domain.DomainConst
import com.paper.domain.event.CanvasEvent
import com.paper.domain.useCase.TranslateSketchToSVG
import com.paper.model.Point
import com.paper.model.Scrap
import com.paper.model.Transform
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*

class ScrapWidget(
    private val uiScheduler: Scheduler,
    private val workerScheduler: Scheduler)
    : IScrapWidget {

    private lateinit var mModel: Scrap

    private val mCancelSignal = PublishSubject.create<Any>()
    private val mDisposables = CompositeDisposable()

    override fun bindModel(model: Scrap) {
        ensureNoLeakedBinding()

        mModel = model

        mPositionSignal.onNext(Point(model.x, model.y))

        mSetTransformSignal.onNext(Transform(
            translationX = mModel.x,
            translationY = mModel.y,
            scaleX = mModel.scale,
            scaleY = mModel.scale,
            rotationInRadians = mModel.rotationInRadians))

        println("${DomainConst.TAG}: Bind scrap \"Widget\" with \"Model\"")
    }

    override fun unbindModel() {
        mCancelSignal.onNext(0)
        mDisposables.clear()

        println("${DomainConst.TAG}: Unbind scrap \"Widget\" from \"Model\"")
    }

    override fun getId(): UUID {
        return mModel.uuid
    }

    private val mPositionSignal = BehaviorSubject.create<Point>().toSerialized()

    override fun onSetPosition(): Observable<Point> {
        return mPositionSignal
    }

    // Drawing ////////////////////////////////////////////////////////////////

    private val mDrawSVGSignal = PublishSubject.create<CanvasEvent>().toSerialized()

    override fun onDrawSVG(): Observable<CanvasEvent> {
        return Observable
            .merge(
                mDrawSVGSignal,
                // For the first time subscription, send events one by one!
                TranslateSketchToSVG(mModel.sketch)
                    .subscribeOn(workerScheduler))
    }

    // Touch //////////////////////////////////////////////////////////////////

    private val mSetTransformSignal = BehaviorSubject.create<Transform>().toSerialized()

    override fun onTransform(): Observable<Transform> {
        return mSetTransformSignal
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun ensureNoLeakedBinding() {
        if (mDisposables.size() > 0)
            throw IllegalStateException("Already bind a model")
    }
}
