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

package com.paper.editor.widget.canvas

import com.paper.editor.data.DrawSVGEvent
import com.paper.shared.model.Point
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import java.util.concurrent.TimeUnit

class ScrapWidget(
    private val mUiScheduler: Scheduler,
    private val mWorkerScheduler: Scheduler)
    : IScrapWidget {

    private lateinit var mModel: ScrapModel

    private val mDrawSVGSignal = PublishSubject.create<DrawSVGEvent>()

    private val mSetTransformSignal = BehaviorSubject.create<TransformModel>()

    private val mCancelSignal = PublishSubject.create<Any>()
    private val mDisposables = CompositeDisposable()

    override fun bindModel(model: ScrapModel) {
        ensureNoLeakedBinding()

        mModel = model

        mSetTransformSignal.onNext(TransformModel(
            translationX = mModel.x,
            translationY = mModel.y,
            scaleX = mModel.scale,
            scaleY = mModel.scale,
            rotationInRadians = mModel.rotationInRadians))
    }

    override fun unbindModel() {
        mCancelSignal.onNext(0)
        mDisposables.clear()
    }

    override fun getId(): UUID {
        return mModel.uuid
    }

    // Drawing ////////////////////////////////////////////////////////////////

    override fun onDrawSVG(): Observable<DrawSVGEvent> {
        // FIXME: It's dangerous that any new draw event is created before this
        // FIXME: initialization finishes.
        Observable
            .timer(1, TimeUnit.MILLISECONDS, mUiScheduler)
            .takeUntil(mCancelSignal)
            .subscribe {
                mModel.sketch.allStrokes.forEach { stroke ->
                    if (stroke.firstPathTuple == stroke.lastPathTuple) {
                        val onlyPath = stroke.lastPathTuple
                        mDrawSVGSignal.onNext(DrawSVGEvent(
                            action = DrawSVGEvent.Action.DOT_AT,
                            point = Point(onlyPath.firstPoint.x,
                                          onlyPath.firstPoint.y)))
                    } else {
                        val lastIndex = stroke.pathTupleList.lastIndex
                        stroke.pathTupleList.forEachIndexed { i, path ->
                            when (i) {
                                0 -> mDrawSVGSignal.onNext(DrawSVGEvent(
                                    action = DrawSVGEvent.Action.MOVE,
                                    point = Point(path.firstPoint.x,
                                                  path.firstPoint.y)))
                                lastIndex -> mDrawSVGSignal.onNext(DrawSVGEvent(
                                    action = DrawSVGEvent.Action.CLOSE,
                                    point = Point(path.firstPoint.x,
                                                  path.firstPoint.y)))
                                else -> mDrawSVGSignal.onNext(DrawSVGEvent(
                                    action = DrawSVGEvent.Action.LINE_TO,
                                    point = Point(path.firstPoint.x,
                                                  path.firstPoint.y)))
                            }
                        }
                    }
                }
            }

        return mDrawSVGSignal
    }

    // Touch //////////////////////////////////////////////////////////////////

    override fun handleTap(x: Float, y: Float) {
        TODO()
    }

    override fun onTransform(): Observable<TransformModel> {
        return mSetTransformSignal
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun ensureNoLeakedBinding() {
        if (mDisposables.size() > 0)
            throw IllegalStateException("Already bind a model")
    }
}
