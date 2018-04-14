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

package com.paper.editor

import android.graphics.Bitmap
import android.os.Environment
import com.paper.editor.widget.canvas.PaperWidget
import com.paper.event.ProgressEvent
import com.paper.shared.model.PaperConsts
import com.paper.shared.model.PaperModel
import com.paper.shared.model.repository.protocol.IPaperModelRepo
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PaperEditorPresenter(private val mPaperRepo: IPaperModelRepo,
                           private val mUiScheduler: Scheduler,
                           private val mWorkerScheduler: Scheduler) {

    private var mPaperId = PaperConsts.TEMP_ID

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

    fun bindViewOnCreate(view: PaperEditorContract.View) {
        mView = view

        // Close button.
        mDisposablesOnCreate.add(
            mView!!.onClickCloseButton()
                .debounce(150, TimeUnit.MILLISECONDS)
                .take(1)
//                .map { mPaperWidget.getPaper() }
//                .switchMap { paper -> updateThumbnail(paper) }
//                .switchMap { paper -> commitPaper(paper) }
                .observeOn(mUiScheduler)
                .subscribe {
                    mView?.close()
                })

        // View port indicator.
        val canvasView = view.getCanvasView()
        val editingPanelView = view.getEditingPanelView()
        mDisposablesOnCreate.add(
            canvasView
                .onDrawViewPort()
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    editingPanelView.setCanvasAndViewPort(
                        event.canvas,
                        event.viewPort)
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
    }

    fun unbindViewOnDestroy() {
        // Unbind widget.
        mView?.getCanvasView()?.unbindWidget()
        // Unbind model.
        mPaperWidget.unbindModel()

        mDisposablesOnCreate.clear()
        mDisposables.clear()

        mView = null
    }

    fun loadPaperById(id: Long) {
        mPaperId = id

        // Inflate paper model.
        mDisposables.add(
            mPaperRepo
                .getPaperById(id)
                .toObservable()
                // TODO: Support progress event.
                .observeOn(mUiScheduler)
                .subscribe { paperM ->
                    // Bind main widget to the model.
                    mPaperWidget.bindModel(paperM)

                    // And then bind view with the widget.
                    mView?.getCanvasView()?.bindWidget(mPaperWidget)
                })
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun updateThumbnail(paper: PaperModel): Observable<PaperModel> {
        return Observable
            .fromCallable {
                mView?.getCanvasView()?.let { canvasView ->
                    val dir = File("${Environment.getExternalStorageDirectory()}/paper")
                    if (!dir.exists()) {
                        dir.mkdir()
                    }

                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
                    val bmp = canvasView.takeSnapshot()
                    val bmpFile = File("${Environment.getExternalStorageDirectory()}/paper",
                                       "$ts.jpg")

                    FileOutputStream(bmpFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }

                    paper.thumbnailWidth = bmp.width
                    paper.thumbnailHeight = bmp.height
                    paper.thumbnailPath = bmpFile.canonicalPath
                }

                return@fromCallable paper
            }
            .subscribeOn(mWorkerScheduler)
    }

    private fun commitPaper(paper: PaperModel): Observable<PaperModel> {
        return Observable
            .fromCallable {
                paper.modifiedAt = getCurrentTime()
                mPaperRepo.putPaperById(paper.id, paper)
                return@fromCallable paper
            }
    }

    private fun getCurrentTime(): Long = System.currentTimeMillis() / 1000
}
