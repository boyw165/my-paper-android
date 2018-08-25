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

package com.paper.domain.ui

import com.paper.domain.ISchedulerProvider
import com.paper.domain.ui_event.UndoRedoAvailabilityEvent
import com.paper.domain.ui.operation.AddScrapOperation
import com.paper.model.ModelConst
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.useful.dirtyflag.DirtyFlag
import java.io.File
import java.util.*

class CanvasOperationHistoryRepository(private val fileDir: File,
                                       private val schedulers: ISchedulerProvider)
    : ICanvasOperationHistoryRepository {

    companion object {

        const val BUSY = 1
    }

    private val mLock = Any()

    private val mDisposables = CompositeDisposable()

    override fun start() {
        ensureNoLeakedBinding()

        mPutOperationSignal
            .flatMap { _ ->
                val value = AddScrapOperation()

                putOperationImpl(value)
                    .doOnComplete {
                        mUndoCapacitySignal.onNext(
                            UndoRedoAvailabilityEvent(canUndo = mUndoKeys.size > 0,
                                                      canRedo = mRedoKeys.size > 0))
                    }
                    .toObservable<Any>()
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

    private val mUndoKeys = Stack<ICanvasOperation>()
    private val mRedoKeys = Stack<ICanvasOperation>()
    private val mUndoCapacitySignal = PublishSubject.create<UndoRedoAvailabilityEvent>()

    val undoSize: Int
        get() = synchronized(mLock) { mUndoKeys.size }

    val redoSize: Int
        get() = synchronized(mLock) { mRedoKeys.size }

    private val mPutOperationSignal = PublishSubject.create<ICanvasOperation>().toSerialized()

    override fun putOperation(operation: ICanvasOperation) {
        mPutOperationSignal.onNext(operation)
    }

    private fun putOperationImpl(operation: ICanvasOperation): Completable {
        return Completable
            .fromCallable {
                synchronized(mLock) {
                    println("${ModelConst.TAG}: put $operation to transformation repo (file impl)")

                    mUndoKeys.push(operation)

                    true
                }
            }
            .subscribeOn(schedulers.db())
            .doOnSubscribe {
                // Mark busy
                mBusySignal.markDirty(BUSY)
            }
            .doOnComplete {
                // Mark not busy
                mBusySignal.markNotDirty(BUSY)
            }
    }

    override fun eraseAll() {
        mUndoKeys.clear()
        mRedoKeys.clear()

        mUndoCapacitySignal.onNext(
            UndoRedoAvailabilityEvent(canUndo = mUndoKeys.size > 0,
                                      canRedo = mRedoKeys.size > 0))
    }

    override fun undo(paper: ICanvasWidget): Single<Boolean> {
        return doUndo()
            .observeOn(schedulers.main())
            .flatMap { operation ->
                operation.undo(paper)

                mUndoCapacitySignal.onNext(
                    UndoRedoAvailabilityEvent(canUndo = mUndoKeys.size > 0,
                                              canRedo = mRedoKeys.size > 0))

                Single.just(true)
            }
    }

    override fun redo(paper: ICanvasWidget): Single<Boolean> {
        return doRedo()
            .observeOn(schedulers.main())
            .flatMap { operation ->
                operation.redo(paper)

                mUndoCapacitySignal.onNext(
                    UndoRedoAvailabilityEvent(canUndo = mUndoKeys.size > 0,
                                              canRedo = mRedoKeys.size > 0))

                Single.just(true)
            }
    }

    private fun doUndo(): Single<ICanvasOperation> {
        return Single
            .fromCallable {
                synchronized(mLock) {
                    val operation = mUndoKeys.pop()
                    mRedoKeys.push(operation)

                    println("${ModelConst.TAG}: get $operation from transformation repo (file impl)")

                    operation
                }
            }
            .subscribeOn(schedulers.db())
            .doOnSubscribe {
                // Mark busy
                mBusySignal.markDirty(BUSY)
            }
            .doOnSuccess {
                // Mark not busy
                mBusySignal.markNotDirty(BUSY)
            }
    }

    private fun doRedo(): Single<ICanvasOperation> {
        return Single
            .fromCallable {
                synchronized(mLock) {
                    val operation = mRedoKeys.pop()
                    mUndoKeys.push(operation)

                    println("${ModelConst.TAG}: get $operation from transformation repo (file impl)")

                    operation
                }
            }
            .subscribeOn(schedulers.db())
            .doOnSubscribe {
                // Mark busy
                mBusySignal.markDirty(BUSY)
            }
            .doOnSuccess {
                // Mark not busy
                mBusySignal.markNotDirty(BUSY)
            }
    }

    fun onUpdateUndoRedoCapacity(): Observable<UndoRedoAvailabilityEvent> {
        return mUndoCapacitySignal
    }

    override fun toString(): String {
        return javaClass.simpleName
    }
}
