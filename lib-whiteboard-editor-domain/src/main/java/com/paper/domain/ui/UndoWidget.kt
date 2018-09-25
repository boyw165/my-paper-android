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

import com.paper.model.IBundle
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
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.useful.dirtyflag.DirtyFlag

class UndoWidget(private val undoRepo: ICommandRepository,
                 private val redoRepo: ICommandRepository,
                 private val schedulers: ISchedulers)
    : IUndoWidget {

    companion object {
        const val BUSY = 1
    }

    private val disposables = CompositeDisposable()

    override fun start() {
        val preparation = Singles
            .zip(undoRepo.prepare(),
                 redoRepo.prepare())
            .cache()

        // Initial undo and redo availability
        preparation
            .subscribe { (undoSize, redoSize) ->
                notifyUndoAvailability(undoSize, redoSize)
            }
            .addTo(disposables)
        preparation
            .flatMapObservable {
                offerCommandSignal
                    .flatMap { command ->
                        Singles.zip(undoRepo.push(command),
                                    redoRepo.deleteAll().toSingleDefault(0))
                            .doOnSubscribe(markBusy)
                            .doOnSuccess(markFree2)
                            .toObservable()
                    }
            }
            .subscribe()
            .addTo(disposables)
    }

    override fun stop() {
        disposables.clear()
    }

//    override fun saveStates(bundle: IBundle) {
//        // DO NOTHING
//    }
//
//    override fun restoreStates(bundle: IBundle) {
//        // DO NOTHING
//    }

    private fun notifyUndoAvailability(undoSize: Int,
                                       redoSize: Int) {
        undoAvailableSignal.onNext(undoSize > 0)
        redoAvailableSignal.onNext(redoSize > 0)
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val busySignal = DirtyFlag()

    /**
     * A busy state of this widget.
     */
    override val busy: Observable<Boolean> get() {
        return busySignal
            .updated()
            .map { event ->
                event.flag != 0
            }
    }

    // Undo & undo ////////////////////////////////////////////////////////////

    private val undoAvailableSignal = BehaviorSubject.createDefault(false).toSerialized()
    private val redoAvailableSignal = BehaviorSubject.createDefault(false).toSerialized()
    private val offerCommandSignal = PublishSubject.create<WhiteboardCommand>().toSerialized()

    override fun offerCommand(command: WhiteboardCommand) {
        offerCommandSignal.onNext(command)
    }

    override fun undo(): Single<WhiteboardCommand> {
        return undoRepo.pop()
            .doOnSubscribe(markBusy)
            .observeOn(schedulers.main())
            .flatMap { (undoSize, command) ->
                // Push command to the undo repository
                Singles.zip(Single.just(undoSize), // undo size
                            redoRepo.push(command), // undo size
                            Single.just(command)) // command
            }
            .doOnSuccess(markFree3)
            .map { (_, _, command) ->
                command
            }
    }

    override fun redo(): Single<WhiteboardCommand> {
        return redoRepo.pop()
            .doOnSubscribe(markBusy)
            .observeOn(schedulers.main())
            .flatMap { (redoSize, command) ->
                // Push command to the doo repository
                Singles.zip(Single.just(redoSize), // undo size
                            undoRepo.push(command), // undo size
                            Single.just(command)) // command
            }
            .doOnSuccess(markFree3)
            .map { (_, _, command) ->
                command
            }
    }

    override val canUndo: Observable<Boolean> get() = undoAvailableSignal.hide()

    override val canRedo: Observable<Boolean> get() = redoAvailableSignal.hide()

    private val markBusy: Consumer<in Disposable> = Consumer {
        // Mark busy
        busySignal.markDirty(BUSY)
    }

    private val markFree2: Consumer<Pair<Int, Int>> = Consumer { (undoSize, redoSize) ->
        notifyUndoAvailability(undoSize, redoSize)

        // Mark not busy
        busySignal.markNotDirty(BUSY)
    }

    private val markFree3: Consumer<Triple<Int, Int, WhiteboardCommand>> = Consumer { (undoSize, redoSize, _) ->
        notifyUndoAvailability(undoSize, redoSize)

        // Mark not busy
        busySignal.markNotDirty(BUSY)
    }
}
