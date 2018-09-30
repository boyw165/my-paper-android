// Copyright Sep 2018-present Whiteboard
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
import com.paper.model.Whiteboard
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import io.useful.dirtyflag.DirtyFlag

class WhiteboardStore(private val boardID: Long,
                      private val boardLocalRepo: IWhiteboardRepository,
                      private val undoRepository: IUndoRepository,
                      private val schedulers: ISchedulers)
    : IWhiteboardStore {

    private val whiteboardSignal = SingleSubject.create<Whiteboard>()
    override var whiteboard: Single<Whiteboard> = whiteboardSignal.hide()

    private val disposableBag = CompositeDisposable()

    // TODO: Deprecate this and move the offering methods to repository
    override fun loadBoard() {
        inflateBoard()
        setupCommandOffer()

        println("${javaClass.simpleName} connects")
    }

    override fun unloadBoard() {
        println("${javaClass.simpleName} disconnects")

        disposableBag.clear()

        whiteboard = Single.never()
    }

    private fun inflateBoard() {
        boardLocalRepo
            .getBoardById(boardID)
            .doOnSubscribe {
                // Mark busy
                dirtyFlag.markDirty(1)
            }
            .subscribe { board ->
                // Mark available
                dirtyFlag.markNotDirty(1)

                whiteboardSignal.onSuccess(board)
            }
            .addTo(disposableBag)
    }

    private val commandDooSignal = PublishSubject.create<WhiteboardCommand>().toSerialized()
    private val commandUndoSignal = PublishSubject.create<WhiteboardCommand>().toSerialized()

    private fun setupCommandOffer() {
        commandDooSignal
            .observeOn(schedulers.main())
            .flatMap { command ->
                // Mark busy
                dirtyFlag.markDirty(1)

                // Transform command for collaboration
                Observables.combineLatest(
                    transform(command).toObservable(),
                    whiteboard.toObservable())
            }
            .observeOn(schedulers.main())
            .flatMapCompletable { (command, board) ->
                // Execute "doo" (idempotent operation)
                command.doo(board)

                undoRepository
                    .offerCommand(command)
            }
            .observeOn(schedulers.main())
            .subscribe {
                // Mark available
                dirtyFlag.markNotDirty(1)
            }
            .addTo(disposableBag)
        Observables.combineLatest(commandUndoSignal,
                                  whiteboard.toObservable())
            .observeOn(schedulers.main())
            .subscribe { (command, board) ->
                command.undo(board)
            }
            .addTo(disposableBag)
    }

    override fun offerCommandDoo(command: WhiteboardCommand) {
        commandDooSignal.onNext(command)
    }

    override fun offerCommandUndo(command: WhiteboardCommand) {
        commandUndoSignal.onNext(command)
    }

    private fun transform(input: WhiteboardCommand): Single<WhiteboardCommand> {
        // TODO: Reference OT for collaboration
        // TODO: https://en.wikipedia.org/wiki/Operational_transformation
        return Single
            .just(input)
            .subscribeOn(schedulers.io())
    }

    // Busy ///////////////////////////////////////////////////////////////////

    private val dirtyFlag = DirtyFlag()

    override val busy: Observable<Boolean>
        get() = dirtyFlag
            .updated()
            .map { event ->
                event.flag != 0
            }
            .observeOn(schedulers.main())
}
