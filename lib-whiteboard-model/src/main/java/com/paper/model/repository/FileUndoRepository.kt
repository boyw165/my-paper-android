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

package com.paper.model.repository

import com.paper.model.ISchedulers
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class FileUndoRepository(private val undoRepo: ICommandRepository,
                         private val redoRepo: ICommandRepository,
                         private val schedulers: ISchedulers)
    : IUndoRepository {

    private val canUndoSignal = BehaviorSubject.createDefault(false).toSerialized()
    private val canRedoSignal = BehaviorSubject.createDefault(false).toSerialized()
    override val canUndo: Observable<Boolean> get() = canUndoSignal.hide()
    override val canRedo: Observable<Boolean> get() = canRedoSignal.hide()

    override fun prepare(): Completable {
        return Completable.fromSingle(
            Singles.zip(undoRepo.prepare(),
                        redoRepo.prepare())
                .observeOn(schedulers.main())
                .doOnSuccess { (undoSize, redoSize) ->
                    notifyUndoAvailability(undoSize, redoSize)
                })
    }

    private fun notifyUndoAvailability(undoSize: Int,
                                       redoSize: Int) {
//        (canUndo as? Subject<Boolean>)?.onNext(undoSize > 0)
//        (canRedo as? Subject<Boolean>)?.onNext(redoSize > 0)
        canUndoSignal.onNext(undoSize > 0)
        canRedoSignal.onNext(redoSize > 0)
    }

    // Undo & undo ////////////////////////////////////////////////////////////

    override fun offerCommand(command: WhiteboardCommand): Completable {
        return Completable.fromSingle(
            Singles.zip(undoRepo.push(command),
                        redoRepo.deleteAll().toSingleDefault(0))
                .observeOn(schedulers.main())
                .doOnSubscribe {
                    notifyUndoAvailability(0, 0)
                }
                .observeOn(schedulers.main())
                .doOnSuccess { (undoSize, redoSize) ->
                    notifyUndoAvailability(undoSize, redoSize)
                })
    }

    override fun undo(): Single<WhiteboardCommand> {
        return undoRepo.pop()
            .observeOn(schedulers.main())
            .flatMap { (undoSize, command) ->
                // Push command to the undo repository
                Singles.zip(Single.just(undoSize), // undo size
                            redoRepo.push(command), // redo size
                            Single.just(command)) // command
            }
            .doOnSuccess { (undoSize, redoSize, _) ->
                notifyUndoAvailability(undoSize, redoSize)
            }
            .map { (_, _, command) ->
                command
            }
    }

    override fun redo(): Single<WhiteboardCommand> {
        return redoRepo.pop()
            .observeOn(schedulers.main())
            .flatMap { (redoSize, command) ->
                // Push command to the doo repository
                Singles.zip(undoRepo.push(command), // undo size
                            Single.just(redoSize), // redo size
                            Single.just(command)) // command
            }
            .doOnSuccess { (undoSize, redoSize, _) ->
                notifyUndoAvailability(undoSize, redoSize)
            }
            .map { (_, _, command) ->
                command
            }
    }
}
