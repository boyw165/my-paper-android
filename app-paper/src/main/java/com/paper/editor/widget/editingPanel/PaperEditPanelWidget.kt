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

package com.paper.editor.widget.editingPanel

import com.paper.editor.data.UpdateEditingToolsEvent
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class PaperEditPanelWidget(
    private val mUiScheduler: Scheduler,
    private val mWorkerScheduler: Scheduler) {

    // Editing tool
    private var mToolIndex = -1
    private val mEditingTools = BehaviorSubject.create<UpdateEditingToolsEvent>()

    private val mUnsupportedToolMsg = PublishSubject.create<Any>()

    private val mDisposables = CompositeDisposable()

    // Lifecycle //////////////////////////////////////////////////////////////

    fun handleStart() {
        ensureNoLeakingSubscription()

        // Prepare initial tools and select the pen by default.
        val tools = getToolIDs()
        mToolIndex = tools.indexOf(EditingToolFactory.TOOL_PEN)
        mEditingTools.onNext(UpdateEditingToolsEvent(
            toolIDs = tools,
            usingIndex = mToolIndex))
    }

    fun handleStop() {
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

    // Editing tool ///////////////////////////////////////////////////////////

    fun handleClickTool(toolID: Int) {
        val toolIDs = getToolIDs()
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

    fun onUpdateEditingToolList(): Observable<UpdateEditingToolsEvent> {
        return mEditingTools
    }

    fun onChooseUnsupportedTool(): Observable<Any> {
        return mUnsupportedToolMsg
    }

    private fun getToolIDs(): List<Int> {
        return mutableListOf(
            EditingToolFactory.TOOL_ERASER,
            EditingToolFactory.TOOL_PEN,
            EditingToolFactory.TOOL_SCISSOR)
    }
}
