// Copyright Feb 2018-present boyw165@gmail.com
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

package com.paper.presenter

import com.paper.domain.ISharedPreferenceService
import com.paper.domain.event.ProgressEvent
import com.paper.domain.useCase.LoadPaperAndBindModel
import com.paper.domain.useCase.SavePaperToStore
import com.paper.domain.widget.canvas.PaperWidget
import com.paper.model.repository.IPaperRepo
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class PaperEditorPresenter(paperRepo: IPaperRepo,
                           prefs: ISharedPreferenceService,
                           uiScheduler: Scheduler,
                           workerScheduler: Scheduler) {

    private val mPaperRepo = paperRepo
    private val mPrefs = prefs
    private val mUiScheduler = uiScheduler
    private val mWorkerScheduler = workerScheduler

    // Editor view.
    private var mView: PaperEditorContract.View? = null

    private val mPaperWidget by lazy {
        PaperWidget(AndroidSchedulers.mainThread(),
                    Schedulers.io())
    }

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposables = CompositeDisposable()

    fun bindView(view: PaperEditorContract.View,
                 id: Long) {
        mView = view

        val canvasView = view.getCanvasView()
        val editingPanelView = view.getEditingPanelView()

        // Progress
        mDisposablesOnCreate.add(
            mUpdateProgressSignal
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    when {
                        event.justStart -> view.showProgressBar(0)
                        event.justStop -> view.hideProgressBar()
                    }
                })

        // Close button.
        mDisposablesOnCreate.add(
            mView!!.onClickCloseButton()
                .throttleFirst(1000, TimeUnit.MILLISECONDS)
                .switchMap {
                    canvasView
                        .takeSnapshot()
                        .compose(SavePaperToStore(
                            paper = mPaperWidget.getPaper(),
                            paperRepo = mPaperRepo))
                        .toObservable()
//                        .startWith { view.showProgressBar(0) }
//                        .subscribeOn(mUiScheduler)
//                        .observeOn(mUiScheduler)
//                        .doOnNext { view.hideProgressBar() }
                }
                .onErrorResumeNext { err: Throwable ->
//                    err.printStackTrace()

                    mView!!.onClickCloseButton()
                        .throttleFirst(1000, TimeUnit.MILLISECONDS)
                        .switchMap {
                            canvasView
                                .takeSnapshot()
                                .compose(SavePaperToStore(
                                    paper = mPaperWidget.getPaper(),
                                    paperRepo = mPaperRepo))
                                .toObservable()
                            //                        .startWith { view.showProgressBar(0) }
                            //                        .subscribeOn(mUiScheduler)
                            //                        .observeOn(mUiScheduler)
                            //                        .doOnNext { view.hideProgressBar() }
                        }
                }
                .observeOn(mUiScheduler)
                .subscribe {
                    view.close()
                })

        // View port indicator.
        mDisposablesOnCreate.add(
            canvasView
                .onDrawViewPort()
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    editingPanelView.setCanvasAndViewPort(
                        event.canvas,
                        event.viewPort)
                })
        mDisposablesOnCreate.add(
            editingPanelView
                .onUpdateViewPortPosition()
                .observeOn(mUiScheduler)
                .subscribe { position ->
                    canvasView.setViewPortPosition(position.x, position.y)
                })

        // Color, stroke width, and edit tool.
        mDisposablesOnCreate.add(
            editingPanelView
                .onChooseColorTicket()
                .observeOn(mUiScheduler)
                .subscribe { color ->
                    mPaperWidget.handleChoosePenColor(color)
                })
        mDisposablesOnCreate.add(
            editingPanelView
                .onUpdatePenSize()
                .observeOn(mUiScheduler)
                .subscribe { penSize ->
                    mPaperWidget.handleUpdatePenSize(penSize)
                })
        mDisposablesOnCreate.add(
            editingPanelView
                .onChooseEditTool()
                .observeOn(mUiScheduler)
                .subscribe { toolID ->
                    // TODO
                })

        // Undo & redo buttons
        mDisposablesOnCreate.add(
            view.onClickUndoButton()
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
                    view.showWIP()
                })
        mDisposablesOnCreate.add(
            view.onClickRedoButton()
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
                    view.showWIP()
                })

        // Delete button
        mDisposablesOnCreate.add(
            view.onClickDeleteButton()
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
                    view.showWIP()
                })

        // Inflate paper model.
        mDisposablesOnCreate.add(
            LoadPaperAndBindModel(
                paperID = id,
                paperWidget = mPaperWidget,
                paperRepo = mPaperRepo,
                updateProgressSignal = mUpdateProgressSignal,
                uiScheduler = AndroidSchedulers.mainThread())
                .observeOn(mUiScheduler)
                .doOnDispose {
                    // Unbind widget.
                    mView?.getCanvasView()?.unbindWidget()
                }
                .observeOn(mUiScheduler)
                .subscribe { successful ->
                    // Bind view with the widget.
                    if (successful) {
                        view.getCanvasView().bindWidget(mPaperWidget)
                    } else {
                        view.showErrorAlertThenFinish(
                            RuntimeException("Cannot load paper and bind model!"))
                    }
                })
    }

    fun unbindView() {
        mDisposablesOnCreate.clear()
        mDisposables.clear()

        mView = null
    }
}
