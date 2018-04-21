// Copyright Mar 2018-present boyw165@gmail.com
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

package com.paper.domain.widget.editPanel

import android.util.Log
import com.paper.domain.DomainConst
import com.paper.domain.event.UpdateColorTicketsEvent
import com.paper.domain.event.UpdateEditingToolsEvent
import com.paper.model.repository.ICommonPenPrefsRepo
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class PaperEditPanelWidget(
    penColorRepo: ICommonPenPrefsRepo,
    uiScheduler: Scheduler,
    workerScheduler: Scheduler) {

    private val mCommonPenPrefsRepo = penColorRepo

    private val mUiScheduler = uiScheduler
    private val mWorkerScheduler = workerScheduler

    private val mCancelSignal = PublishSubject.create<Any>()
    private val mDisposables = CompositeDisposable()

    // Lifecycle //////////////////////////////////////////////////////////////

    fun start() {
        ensureNoLeakingSubscription()

        // Prepare initial tools and select the pen by default.
        val tools = getEditToolIDs()
        mToolIndex = tools.indexOf(EditingToolFactory.TOOL_PEN)
        mEditingTools.onNext(UpdateEditingToolsEvent(
            toolIDs = tools,
            usingIndex = mToolIndex))

        // Prepare initial color tickets
        mDisposables.add(
            Observables
                .combineLatest(
                    mCommonPenPrefsRepo.getPenColors(),
                    mCommonPenPrefsRepo.getChosenPenColor())
                .subscribe { (colors, chosenColor) ->
                    val index = colors.indexOf(chosenColor)

                    // Export the signal as onUpdatePenColorList()
                    mColorTicketsSignal.onNext(UpdateColorTicketsEvent(
                        colorTickets = colors,
                        usingIndex = index))
                })

        // Prepare initial pen size
        mDisposables.add(
            mCommonPenPrefsRepo
                .getPenSize()
                .subscribe { penSize ->
                    // Export the signal as onUpdatePenSize()
                    mPenSizeSignal.onNext(penSize)
                })
    }

    fun stop() {
        mDisposables.clear()
    }

    private fun ensureNoLeakingSubscription() {
        if (mDisposables.size() > 0) {
            throw IllegalStateException("Already started!")
        }
    }

    fun handleChoosePrimaryFunction(id: Int) {
        TODO("not implemented")
    }

    // Edit tool //////////////////////////////////////////////////////////////

    private var mToolIndex = -1
    private val mEditingTools = BehaviorSubject.create<UpdateEditingToolsEvent>()

    private val mUnsupportedToolMsg = PublishSubject.create<Any>()

    fun handleClickTool(toolID: Int) {
        val toolIDs = getEditToolIDs()
        val usingIndex = if (toolID != EditingToolFactory.TOOL_PEN) {
            mUnsupportedToolMsg.onNext(0)
            toolIDs.indexOf(EditingToolFactory.TOOL_PEN)
        } else {
            toolIDs.indexOf(toolID)
        }

        mEditingTools.onNext(UpdateEditingToolsEvent(
            toolIDs = toolIDs,
            usingIndex = usingIndex))
    }

    fun onUpdateEditToolList(): Observable<UpdateEditingToolsEvent> {
        return mEditingTools
    }

    fun onChooseUnsupportedEditTool(): Observable<Any> {
        return mUnsupportedToolMsg
    }

    private fun getEditToolIDs(): List<Int> {
        return listOf(
            EditingToolFactory.TOOL_ERASER,
            EditingToolFactory.TOOL_PEN,
            EditingToolFactory.TOOL_SCISSOR)
    }

    // Pen Color & size //////////////////////////////////////////////////////

    private val mColorTicketsSignal = PublishSubject.create<UpdateColorTicketsEvent>()

    fun handleClickColor(color: Int) {
        Log.d(DomainConst.TAG, "choose color=${Integer.toHexString(color)}")
        mCancelSignal.onNext(0)

        mDisposables.add(
            mCommonPenPrefsRepo
                .putChosenPenColor(color)
                .toObservable()
                .takeUntil(mCancelSignal)
                .subscribe())
    }

    fun onUpdatePenColorList(): Observable<UpdateColorTicketsEvent> {
        return mColorTicketsSignal
    }

    private val mPenSizeSignal = PublishSubject.create<Float>()

    fun handleChangePenSize(size: Float) {
        Log.d(DomainConst.TAG, "change pen size=$size")
        mCancelSignal.onNext(0)

        mDisposables.add(
            mCommonPenPrefsRepo
                .putPenSize(size)
                .toObservable()
                .takeUntil(mCancelSignal)
                .subscribe())
    }

    fun onUpdatePenSize(): Observable<Float> {
        return mPenSizeSignal
    }
}
