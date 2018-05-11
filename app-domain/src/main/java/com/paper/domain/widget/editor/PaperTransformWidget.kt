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

package com.paper.domain.widget.editor

import com.paper.model.IPaper
import com.paper.model.IPaperTransformRepo
import com.paper.model.transform.AddStrokeTransform
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.*

class PaperTransformWidget(historyRepo: IPaperTransformRepo,
                           uiScheduler: Scheduler,
                           ioScheduler: Scheduler)
    : IWidget<IPaper> {

    private lateinit var mPaper: IPaper
    private val mOperationRepo = historyRepo

    private val mUiScheduler = uiScheduler
    private val mIoScheduler = ioScheduler

    private val mDisposables = CompositeDisposable()

    override fun bindModel(model: IPaper) {
        ensureNoLeakedBinding()

        mPaper = model

        bindPaperImpl()
    }

    override fun unbindModel() {
        unbindPaperImpl()
    }

    private fun bindPaperImpl() {
        mDisposables.add(
            mPaper.onAddStroke(replayAll = false)
                .observeOn(mUiScheduler)
                .switchMap {
                    val key = UUID.randomUUID()
                    val value = AddStrokeTransform(
                        paper = mPaper)

                    mUndoKeys.push(key)

                    mOperationRepo
                        .putRecord(key, value)
                        .doOnSuccess {
                            mUndoCapacitySignal.onNext(Pair(mUndoKeys.size, mRedoKeys.size))
                        }
                        .toObservable()
                }
                .subscribe())
        mDisposables.add(
            mPaper.onRemoveStroke()
                .observeOn(mUiScheduler)
                .switchMap {
                    // TODO
                    Observable.never<Any>()
                }
                .subscribe())
    }

    private fun unbindPaperImpl() {
        mDisposables.clear()
    }

    private fun ensureNoLeakedBinding() {
        if (mDisposables.size() > 0)
            throw IllegalStateException("Already bind a model")
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    private val mUndoKeys = Stack<UUID>()
    private val mRedoKeys = Stack<UUID>()
    private val mUndoCapacitySignal = PublishSubject.create<Pair<Int, Int>>()

    fun onUpdateUndoRedoCapacity(): Observable<Pair<Int, Int>> {
        return mUndoCapacitySignal
    }

    fun undo(): Single<Boolean> {
        val key = mUndoKeys.pop()
        mRedoKeys.push(key)

        mUndoCapacitySignal.onNext(Pair(mUndoKeys.size, mRedoKeys.size))

        return Single
            .just(key)
            .flatMap { k ->
                mOperationRepo
                    .getRecord(k)
                    .flatMap { transform ->
                        unbindModel()
                        transform.undo()
                        bindPaperImpl()

                        Single.just(true)
                    }
            }
            .onErrorReturnItem(false)
    }

    fun redo(): Single<Boolean> {
        val key = mRedoKeys.pop()
        mUndoKeys.push(key)

        mUndoCapacitySignal.onNext(Pair(mUndoKeys.size, mRedoKeys.size))

        return Single
            .just(key)
            .flatMap { k ->
                mOperationRepo
                    .getRecord(k)
                    .flatMap { transform ->
                        unbindModel()
                        transform.redo()
                        bindPaperImpl()

                        Single.just(true)
                    }
            }
            .onErrorReturnItem(false)
    }

    override fun toString(): String {
        return javaClass.simpleName
    }
}
