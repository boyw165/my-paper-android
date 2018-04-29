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

import com.paper.model.IPaperTransformRepo
import com.paper.model.PaperModel
import com.paper.model.transform.AddStrokeTransform
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.*

class PaperTransformWidget(historyRepo: IPaperTransformRepo) : IWidget<PaperModel> {

    private lateinit var mPaper: PaperModel
    private val mOperationRepo = historyRepo

    private val mDisposables = CompositeDisposable()

    override fun bindModel(model: PaperModel) {
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
                .switchMap {
                    val key = UUID.randomUUID()
                    val value = AddStrokeTransform(
                        paper = mPaper)

                    mOperationRepo
                        .putRecord(key, value)
                        .toObservable()
                }
                .subscribe {
                    // DO NOTHING
                })
        mDisposables.add(
            mPaper.onRemoveStroke()
                .switchMap {
                    // TODO
                    Observable.never<Any>()
                }
                .subscribe {
                    // DO NOTHING
                })
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
    private val mUndoCapacitySignal = PublishSubject.create<Int>()
    private val mRedoKeys = Stack<UUID>()
    private val mRedoCapacitySignal = PublishSubject.create<Int>()

    fun onUpdateUndoCapacity(): Observable<Int> {
        return mUndoCapacitySignal
    }

    fun onUpdateRedoCapacity(): Observable<Int> {
        return mRedoCapacitySignal
    }

    fun undo(): Single<Boolean> {
        val key = mUndoKeys.pop()
        mRedoKeys.push(key)

        mUndoCapacitySignal.onNext(mUndoKeys.size)
        mRedoCapacitySignal.onNext(mRedoKeys.size)

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

        mUndoCapacitySignal.onNext(mUndoKeys.size)
        mRedoCapacitySignal.onNext(mRedoKeys.size)

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
