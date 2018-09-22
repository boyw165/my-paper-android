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

package com.paper.domain.store

import com.paper.domain.DomainConst
import com.paper.model.ISchedulers
import com.paper.model.Whiteboard
import com.paper.model.command.WhiteboardCommand
import com.paper.model.repository.IWhiteboardRepository
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import io.useful.dirtyflag.DirtyFlag

class WhiteboardStore(private val whiteboardID: Long,
                      private val whiteboardRepo: IWhiteboardRepository,
                      private val schedulers: ISchedulers)
    : IWhiteboardStore {

    override val whiteboard: Single<Whiteboard> get() = cacheWhiteboard.hide()
    private val cacheWhiteboard = SingleSubject.create<Whiteboard>()

    private val disposableBag = CompositeDisposable()

    override fun start() {
        // First document initialization
        whiteboardRepo
            .getBoardById(whiteboardID)
            .doOnSubscribe {
                dirtyFlag.markDirty(DomainConst.BUSY)
            }
            .observeOn(schedulers.main())
            .subscribe { document ->
                dirtyFlag.markNotDirty(DomainConst.BUSY)
                cacheWhiteboard.onSuccess(document)
            }
            .addTo(disposableBag)

        // Incoming command
        Observables.combineLatest(cacheWhiteboard.toObservable(),
                                  commandDooSignal)
            .observeOn(schedulers.main())
            .subscribe { (document, command) ->
                command.doo(document)
            }
            .addTo(disposableBag)
        Observables.combineLatest(cacheWhiteboard.toObservable(),
                                  commandUndoSignal)
            .observeOn(schedulers.main())
            .subscribe { (document, command) ->
                command.undo(document)
            }
            .addTo(disposableBag)

        println("${javaClass.simpleName} starts")
    }

    override fun stop() {
        println("${javaClass.simpleName} stops")

        disposableBag.clear()
    }

    private val commandDooSignal = PublishSubject.create<WhiteboardCommand>().toSerialized()
    private val commandUndoSignal = PublishSubject.create<WhiteboardCommand>().toSerialized()

    override fun offerCommandDoo(command: WhiteboardCommand) {
        commandDooSignal.onNext(command)
    }

    override fun offerCommandUndo(command: WhiteboardCommand) {
        commandUndoSignal.onNext(command)
    }

    private val dirtyFlag = DirtyFlag(0)

    override val busy: Observable<Boolean> get() {
        return dirtyFlag.updated()
            .map { event ->
                event.flag != 0
            }
    }
}
