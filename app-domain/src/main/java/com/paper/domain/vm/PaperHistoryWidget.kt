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

package com.paper.domain.vm

import com.paper.domain.ISchedulerProvider
import com.paper.domain.event.UndoRedoEvent
import com.paper.model.ICanvasOperation
import com.paper.model.ICanvasOperationRepo
import com.paper.model.IPaper
import com.paper.model.operation.AddScrapOperation
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.useful.dirtyflag.DirtyFlag
import java.util.*

class PaperHistoryWidget(private val historyRepo: ICanvasOperationRepo,
                         private val schedulers: ISchedulerProvider)
    : IPaperHistoryWidget {

    private val BUSY = 1

    private val mDisposables = CompositeDisposable()

    override fun start() {
        ensureNoLeakedBinding()

        mOperationSignal
            .flatMap { _ ->
                val key = UUID.randomUUID()
                val value = AddScrapOperation()

                mUndoKeys.push(key)

                historyRepo
                    .putRecord(key, value)
                    .doOnSuccess {
                        mUndoCapacitySignal.onNext(
                            UndoRedoEvent(canUndo = mUndoKeys.size > 0,
                                          canRedo = mRedoKeys.size > 0))
                    }
                    .toObservable()
            }
            .subscribe()
            .addTo(mDisposables)
    }

    override fun stop() {
        mDisposables.clear()
    }

    private fun ensureNoLeakedBinding() {
        if (mDisposables.size() > 0)
            throw IllegalStateException("Already start a model")
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val mBusySignal = DirtyFlag()

    /**
     * A busy state of this widget.
     */
    override fun onBusy(): Observable<Boolean> {
        return mBusySignal
            .onUpdate()
            .map { event ->
                event.flag != 0
            }
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    private val mUndoKeys = Stack<UUID>()
    private val mRedoKeys = Stack<UUID>()
    private val mUndoCapacitySignal = PublishSubject.create<UndoRedoEvent>()

    private val mOperationSignal = PublishSubject.create<ICanvasOperation>().toSerialized()

    override fun putOperation(operation: ICanvasOperation) {
        mOperationSignal.onNext(operation)
    }

    override fun eraseAll() {
        mUndoKeys.clear()
        mRedoKeys.clear()

        mUndoCapacitySignal.onNext(
            UndoRedoEvent(canUndo = mUndoKeys.size > 0,
                          canRedo = mRedoKeys.size > 0))
    }

    override fun undo(paper: IPaper): Single<Boolean> {
        val key = mUndoKeys.pop()
        mRedoKeys.push(key)

        mUndoCapacitySignal.onNext(
            UndoRedoEvent(canUndo = mUndoKeys.size > 0,
                          canRedo = mRedoKeys.size > 0))

        return Single
            .just(key)
            .flatMap { k ->
                mBusySignal.markDirty(BUSY)

                historyRepo
                    .getRecord(k)
                    .flatMap { transform ->
                        transform.undo(paper)

                        mBusySignal.markNotDirty(BUSY)

                        Single.just(true)
                    }
            }
            .onErrorReturnItem(false)
    }

    override fun redo(paper: IPaper): Single<Boolean> {
        val key = mRedoKeys.pop()
        mUndoKeys.push(key)

        mUndoCapacitySignal.onNext(
            UndoRedoEvent(canUndo = mUndoKeys.size > 0,
                          canRedo = mRedoKeys.size > 0))

        return Single
            .just(key)
            .flatMap { k ->
                mBusySignal.markDirty(BUSY)

                historyRepo
                    .getRecord(k)
                    .flatMap { transform ->
                        transform.redo(paper)

                        mBusySignal.markNotDirty(BUSY)

                        Single.just(true)
                    }
            }
            .onErrorReturnItem(false)
    }


    fun onUpdateUndoRedoCapacity(): Observable<UndoRedoEvent> {
        return mUndoCapacitySignal
    }

    override fun toString(): String {
        return javaClass.simpleName
    }
}
