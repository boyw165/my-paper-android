// Copyright Sep 2018-present Paper
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

import com.paper.domain.ui_event.UndoAvailabilityEvent
import com.paper.model.IPaper
import com.paper.model.ISchedulers
import com.paper.model.repository.EditorOperation
import com.paper.model.repository.IOperationRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.useful.dirtyflag.DirtyFlag
import java.util.*

class UndoManager(private val repository: IOperationRepository,
                  private val schedulers: ISchedulers)
    : IWidget {

    companion object {
        const val BUSY = 1
    }

    private val lock = Any()

    private val disposables = CompositeDisposable()

    override fun start(): Observable<Boolean> {
        return autoStop {
            repository.prepare()
                .andThen {
                    putOperationSignal
                        .flatMap { op ->
                            repository.push(op)
                                .doOnComplete {
                                    capacitySignal.onNext(
                                        UndoAvailabilityEvent(canUndo = undoKeys.size > 0,
                                                              canRedo = redoKeys.size > 0))
                                }
                                .toObservable<Any>()
                        }
                }
                .subscribe()
                .addTo(disposables)
        }
    }

    override fun stop() {
        disposables.clear()
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val busySignal = DirtyFlag()

    /**
     * A busy state of this widget.
     */
    fun observeBusy(): Observable<Boolean> {
        return busySignal
            .onUpdate()
            .map { event ->
                event.flag != 0
            }
    }

    // Undo & redo ////////////////////////////////////////////////////////////

    private val undoKeys = Stack<EditorOperation>()
    private val redoKeys = Stack<EditorOperation>()
    private val capacitySignal = PublishSubject.create<UndoAvailabilityEvent>()

    val undoSize: Int
        get() = synchronized(lock) { undoKeys.size }

    val redoSize: Int
        get() = synchronized(lock) { redoKeys.size }

    private val putOperationSignal = PublishSubject.create<EditorOperation>().toSerialized()

    fun putOperation(operation: EditorOperation) {
        putOperationSignal.onNext(operation)
    }

    fun undo(paper: IPaper): Completable {
        return Completable.fromSingle(
            repository.pop()
                .doOnSubscribe {
                    // Mark busy
                    busySignal.markDirty(BUSY)
                }
                .observeOn(schedulers.main())
                .doOnSuccess { operation ->
                    operation.undo(paper)

                    capacitySignal.onNext(
                        UndoAvailabilityEvent(canUndo = undoKeys.size > 0,
                                              canRedo = redoKeys.size > 0))

                    // Mark not busy
                    busySignal.markNotDirty(BUSY)
                })
    }

    fun redo(paper: IPaper): Completable {
        return Completable.fromSingle(
            repository.pop()
                .doOnSubscribe {
                    // Mark busy
                    busySignal.markDirty(BUSY)
                }
                .observeOn(schedulers.main())
                .doOnSuccess { operation ->
                    operation.redo(paper)

                    capacitySignal.onNext(
                        UndoAvailabilityEvent(canUndo = undoKeys.size > 0,
                                              canRedo = redoKeys.size > 0))

                    // Mark not busy
                    busySignal.markNotDirty(BUSY)
                })
    }

    fun observeUndoCapacity(): Observable<UndoAvailabilityEvent> {
        return capacitySignal
    }
}
