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

package com.paper

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.paper.domain.IPaperRepoProvider
import com.paper.domain.ISharedPreferenceService
import com.paper.domain.event.ProgressEvent
import com.paper.domain.useCase.LoadPaperAndBindModel
import com.paper.domain.useCase.SavePaperToStore
import com.paper.model.ModelConst
import com.paper.useCase.BindViewWithWidget
import com.paper.view.canvas.PaperWidgetView
import com.paper.view.editPanel.PaperEditPanelView
import com.paper.view.editor.PaperEditorWidget
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class PaperEditorActivity : AppCompatActivity() {

    private val mCanvasView by lazy { findViewById<PaperWidgetView>(R.id.paper_canvas) }
    private val mEditPanelView by lazy { findViewById<PaperEditPanelView>(R.id.edit_panel) }

    private val mProgressBar by lazy {
        AlertDialog.Builder(this@PaperEditorActivity)
            .setCancelable(false)
            .create()
    }

    private val mErrorThenFinishDialog by lazy {
        AlertDialog.Builder(this@PaperEditorActivity)
            .setCancelable(false)
            .setTitle(R.string.alert_title)
            .setPositiveButton(R.string.close) { _, _ -> finish() }
            .create()
    }

    // Back button and signal.
    private val mBtnClose by lazy { findViewById<View>(R.id.btn_close) }
    private val mClickSysBackSignal = PublishSubject.create<Any>()

    // Undo & redo buttons
    private val mBtnUndo by lazy { findViewById<View>(R.id.btn_undo) }
    private val mBtnRedo by lazy { findViewById<View>(R.id.btn_redo) }

    // Delete button
    private val mBtnDelete by lazy { findViewById<View>(R.id.btn_delete) }

    // Repositories.
    // TODO: Inject the repo.
    private val mPaperRepo by lazy { (application as IPaperRepoProvider).getRepo() }

    private val mPrefs by lazy { application as ISharedPreferenceService }
    private val mUiScheduler = AndroidSchedulers.mainThread()
    private val mWorkerScheduler = Schedulers.io()

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()
    // Error signal
    private val mErrorSignal = PublishSubject.create<Throwable>()

    private val mWidget by lazy {
        PaperEditorWidget(
            paperRepo = (application as IPaperRepoProvider).getRepo(),
            uiScheduler = AndroidSchedulers.mainThread(),
            ioScheduler = Schedulers.io())
    }

    // Disposables
    private val mDisposables = CompositeDisposable()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_paper_editor)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Force to hide the progress-bar.
        hideProgressBar()
        // Force to hide the error dialog.
        mErrorThenFinishDialog.dismiss()
    }

    override fun onResume() {
        super.onResume()

        // Progress
        mDisposables.add(
            mUpdateProgressSignal
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    when {
                        event.justStart -> showProgressBar(0)
                        event.justStop -> hideProgressBar()
                    }
                })

        // Close button.
        mDisposables.add(
            onClickCloseButton()
                .throttleFirst(1000, TimeUnit.MILLISECONDS)
                .switchMap {
                    mCanvasView
                        .takeSnapshot()
                        .compose(SavePaperToStore(
                            paper = mPaperWidget.getPaper(),
                            paperRepo = mPaperRepo,
                            prefs = mPrefs,
                            errorSignal = mErrorSignal))
                        .toObservable()
//                        .startWith { showProgressBar(0) }
//                        .subscribeOn(mUiScheduler)
//                        .observeOn(mUiScheduler)
//                        .doOnNext { hideProgressBar() }
                }
                .observeOn(mUiScheduler)
                .subscribe {
                    close()
                })

        // View port indicator.
        mDisposables.add(
            mCanvasView
                .onDrawViewPort()
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    mEditPanelView.setCanvasAndViewPort(
                        event.canvas,
                        event.viewPort)
                })
        mDisposables.add(
            mEditPanelView
                .onUpdateViewPortPosition()
                .observeOn(mUiScheduler)
                .subscribe { position ->
                    mCanvasView.setViewPortPosition(position.x, position.y)
                })

        // Color, stroke width, and edit tool.
        mDisposables.add(
            mEditPanelView
                .onChooseColorTicket()
                .observeOn(mUiScheduler)
                .subscribe { color ->
                    mPaperWidget.handleChoosePenColor(color)
                })
        mDisposables.add(
            mEditPanelView
                .onUpdatePenSize()
                .observeOn(mUiScheduler)
                .subscribe { penSize ->
                    mPaperWidget.handleUpdatePenSize(penSize)
                })
        mDisposables.add(
            mEditPanelView
                .onChooseEditTool()
                .observeOn(mUiScheduler)
                .subscribe { toolID ->
                    // TODO
                })

        // Undo & redo buttons
        mDisposables.add(
            onClickUndoButton()
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
                    showWIP()
                })
        mDisposables.add(
            onClickRedoButton()
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
                    showWIP()
                })

        // Delete button
        mDisposables.add(
            onClickDeleteButton()
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
                    showWIP()
                })

        // Inflate paper model.
        mDisposables.add(
            LoadPaperAndBindModel(
                paperID = paperID,
                paperWidget = mPaperWidget,
                paperRepo = mPaperRepo,
                updateProgressSignal = mUpdateProgressSignal,
                uiScheduler = AndroidSchedulers.mainThread())
                .observeOn(mUiScheduler)
                .doOnDispose {
                    // Unbind widget.
                    mCanvasView.unbindWidget()
                }
                .observeOn(mUiScheduler)
                .subscribe { successful ->
                    // Bind view with the widget.
                    if (successful) {
                        mCanvasView.bindWidget(mPaperWidget)
                    } else {
                        showErrorAlertThenFinish(
                            RuntimeException("Cannot load paper and bind model!"))
                    }
                })

        // Bind sub-view with the sub-widget if the widget is ready!
        mDisposables.add(
            mWidget.onPaperWidgetReady()
                .switchMap { widget ->
                    BindViewWithWidget(view = mCanvasView,
                                       widget = widget)
                }
                .subscribe())

        val paperID = intent.getLongExtra(AppConst.PARAMS_PAPER_ID, ModelConst.TEMP_ID)
        mWidget.start(paperID)
    }

    override fun onPause() {
        super.onPause()

        mWidget.stop()

        mDisposables.clear()
    }

    override fun onBackPressed() {
        mClickSysBackSignal.onNext(0)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Editor view ////////////////////////////////////////////////////////////

    private fun close() {
        finish()
    }

    private fun onClickCloseButton(): Observable<Any> {
        return Observable.merge(mClickSysBackSignal,
                                RxView.clicks(mBtnClose))
    }

    private fun onClickUndoButton(): Observable<Any> {
        return RxView.clicks(mBtnUndo)
    }

    private fun onClickRedoButton(): Observable<Any> {
        return RxView.clicks(mBtnRedo)
    }

    private fun onClickDeleteButton(): Observable<Any> {
        return RxView.clicks(mBtnDelete)
    }

    private fun onClickMenu(): Observable<Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun showProgressBar(progress: Int) {
        if (!mProgressBar.isShowing) {
            mProgressBar.show()
        }

        mProgressBar.setMessage(
            "%s: %d".format(getString(R.string.loading), progress))
    }

    private fun hideProgressBar() {
        mProgressBar.dismiss()
    }

    private fun showWIP() {
        Toast.makeText(this, R.string.msg_under_construction, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorAlert(error: Throwable) {
        Toast.makeText(this@PaperEditorActivity,
                       error.toString(),
                       Toast.LENGTH_SHORT).show()
    }

    private fun showErrorAlertThenFinish(error: Throwable) {
        mErrorThenFinishDialog.setMessage(error.toString())
        mErrorThenFinishDialog.show()
    }
}
