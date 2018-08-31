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
import com.paper.model.IPaper
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

class UndoRepository(private val fileDir: File,
                     private val schedulers: ISchedulerProvider)
    : ICanvasOperationHistoryRepository {

    companion object {

        const val BUSY = 1
    }

    private val lock = Any()

    private val disposables = CompositeDisposable()

    override fun start(): Observable<Boolean> {
        return autoStop {
            ensureNoLeakedBinding()

            putOperationSignal
                .flatMap { _ ->
                    val value = AddScrapOperation()

                    putOperationImpl(value)
                        .doOnComplete {
                            capacitySignal.onNext(
                                UndoRedoAvailabilityEvent(canUndo = undoKeys.size > 0,
                                                          canRedo = redoKeys.size > 0))
                        }
                        .toObservable<Any>()
                }
                .subscribe()
                .addTo(disposables)
        }
    }

    override fun stop() {
        disposables.clear()
    }

    fun inject(paper: IPaper) {
        TODO("not implemented")
    }

    private fun ensureNoLeakedBinding() {
        if (disposables.size() > 0)
            throw IllegalStateException("Already start a model")
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val busySignal = DirtyFlag()

    /**
     * A busy state of this widget.
     */
    override fun onBusy(): Observable<Boolean> {
        return busySignal
            .onUpdate()
            .map { event ->
                event.flag != 0
            }
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    private val undoKeys = Stack<ICanvasOperation>()
    private val redoKeys = Stack<ICanvasOperation>()
    private val capacitySignal = PublishSubject.create<UndoRedoAvailabilityEvent>()

    val undoSize: Int
        get() = synchronized(lock) { undoKeys.size }

    val redoSize: Int
        get() = synchronized(lock) { redoKeys.size }

    private val putOperationSignal = PublishSubject.create<ICanvasOperation>().toSerialized()

    override fun putOperation(operation: ICanvasOperation) {
        putOperationSignal.onNext(operation)
    }

    private fun putOperationImpl(operation: ICanvasOperation): Completable {
        return Completable
            .fromCallable {
                synchronized(lock) {
                    println("${ModelConst.TAG}: put $operation to transformation repo (file impl)")

                    undoKeys.push(operation)
                }
            }
            .subscribeOn(schedulers.db())
            .doOnSubscribe {
                // Mark busy
                busySignal.markDirty(BUSY)
            }
            .doOnComplete {
                // Mark not busy
                busySignal.markNotDirty(BUSY)
            }
    }

    override fun eraseAll() {
        undoKeys.clear()
        redoKeys.clear()

        capacitySignal.onNext(
            UndoRedoAvailabilityEvent(canUndo = undoKeys.size > 0,
                                      canRedo = redoKeys.size > 0))
    }

    override fun undo(paper: IPaper): Single<Boolean> {
        return doUndo()
            .observeOn(schedulers.main())
            .flatMap { operation ->
                operation.undo(paper)

                capacitySignal.onNext(
                    UndoRedoAvailabilityEvent(canUndo = undoKeys.size > 0,
                                              canRedo = redoKeys.size > 0))

                Single.just(true)
            }
    }

    override fun redo(paper: IPaper): Single<Boolean> {
        return doRedo()
            .observeOn(schedulers.main())
            .flatMap { operation ->
                operation.redo(paper)

                capacitySignal.onNext(
                    UndoRedoAvailabilityEvent(canUndo = undoKeys.size > 0,
                                              canRedo = redoKeys.size > 0))

                Single.just(true)
            }
    }

    private fun doUndo(): Single<ICanvasOperation> {
        return Single
            .fromCallable {
                synchronized(lock) {
                    val operation = undoKeys.pop()
                    redoKeys.push(operation)

                    println("${ModelConst.TAG}: get $operation from transformation repo (file impl)")

                    operation
                }
            }
            .subscribeOn(schedulers.db())
            .doOnSubscribe {
                // Mark busy
                busySignal.markDirty(BUSY)
            }
            .doOnSuccess {
                // Mark not busy
                busySignal.markNotDirty(BUSY)
            }
    }

    private fun doRedo(): Single<ICanvasOperation> {
        return Single
            .fromCallable {
                synchronized(lock) {
                    val operation = redoKeys.pop()
                    undoKeys.push(operation)

                    println("${ModelConst.TAG}: get $operation from transformation repo (file impl)")

                    operation
                }
            }
            .subscribeOn(schedulers.db())
            .doOnSubscribe {
                // Mark busy
                busySignal.markDirty(BUSY)
            }
            .doOnSuccess {
                // Mark not busy
                busySignal.markNotDirty(BUSY)
            }
    }

    fun onUpdateUndoRedoCapacity(): Observable<UndoRedoAvailabilityEvent> {
        return capacitySignal
    }

    override fun toString(): String {
        return javaClass.simpleName
    }
}
