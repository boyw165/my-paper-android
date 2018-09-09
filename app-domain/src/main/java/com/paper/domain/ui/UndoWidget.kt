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
import com.paper.model.ISchedulers
import com.paper.model.command.WhiteboardCommand
import com.paper.model.repository.ICommandRepository
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.useful.dirtyflag.DirtyFlag

class UndoWidget(private val undoRepo: ICommandRepository,
                 private val redoRepo: ICommandRepository,
                 private val schedulers: ISchedulers)
    : IUndoWidget {

    companion object {
        const val BUSY = 1
    }

    private val lock = Any()

    private val disposables = CompositeDisposable()

    override fun start(): Observable<Boolean> {
        return autoStop {
            val preparation = Singles
                .zip(undoRepo.prepare(),
                     redoRepo.prepare())
                .cache()

            preparation
                .subscribe { (undoSize, redoSize) ->
                    notifyUndoAvailability(undoSize, redoSize)
                }
                .addTo(disposables)
            preparation
                .flatMapObservable {
                    putOperationSignal
                        .flatMap { op ->
                            Singles.zip(undoRepo.push(op),
                                        redoRepo.deleteAll().toSingleDefault(true))
                                .doOnSuccess { (undoSize, _) ->
                                    notifyUndoAvailability(undoSize, 0)
                                }
                                .toObservable()
                        }
                }
                .subscribe()
                .addTo(disposables)
        }
    }

    override fun stop() {
        disposables.clear()
    }

    private fun notifyUndoAvailability(undoSize: Int, redoSize: Int) {
        capacitySignal.onNext(
            UndoAvailabilityEvent(canUndo = undoSize > 0,
                                  canRedo = redoSize > 0))
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val busySignal = DirtyFlag()

    /**
     * A busy state of this widget.
     */
    override fun observeBusy(): Observable<Boolean> {
        return busySignal
            .onUpdate()
            .map { event ->
                event.flag != 0
            }
    }

    // Undo & undo ////////////////////////////////////////////////////////////

    private val capacitySignal = PublishSubject.create<UndoAvailabilityEvent>()
    private val putOperationSignal = PublishSubject.create<WhiteboardCommand>().toSerialized()

    override fun putOperation(command: WhiteboardCommand) {
        putOperationSignal.onNext(command)
    }

    override fun undo(): Single<WhiteboardCommand> {
        return undoRepo.pop()
            .doOnSubscribe(onStartUndoOrRedo)
            .observeOn(schedulers.main())
            .flatMap { (undoSize, command) ->
                // Push command to the undo repository
                Singles.zip(Single.just(undoSize), // undo size
                            redoRepo.push(command), // undo size
                            Single.just(command)) // command
            }
            .doOnSuccess(onSuccessUndoOrRedo)
            .map { (_, _, command) ->
                command
            }
    }

    override fun redo(): Single<WhiteboardCommand> {
        return redoRepo.pop()
            .doOnSubscribe(onStartUndoOrRedo)
            .observeOn(schedulers.main())
            .flatMap { (redoSize, command) ->
                // Push command to the doo repository
                Singles.zip(Single.just(redoSize), // undo size
                            undoRepo.push(command), // undo size
                            Single.just(command)) // command
            }
            .doOnSuccess(onSuccessUndoOrRedo)
            .map { (_, _, command) ->
                command
            }
    }

    override fun observeUndoCapacity(): Observable<UndoAvailabilityEvent> {
        return capacitySignal
    }

    private val onStartUndoOrRedo: Consumer<in Disposable> = Consumer {
        // Mark busy
        busySignal.markDirty(BUSY)
    }

    private val onSuccessUndoOrRedo: Consumer<Triple<Int, Int, WhiteboardCommand>> = Consumer { (undoSize, redoSize, _) ->
        notifyUndoAvailability(undoSize, redoSize)

        // Mark not busy
        busySignal.markNotDirty(BUSY)
    }
}
