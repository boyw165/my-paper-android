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
import com.paper.domain.IBitmapRepoProvider
import com.paper.domain.IPaperRepoProvider
import com.paper.domain.IPaperTransformRepoProvider
import com.paper.domain.ISharedPreferenceService
import com.paper.domain.event.ProgressEvent
import com.paper.domain.widget.editor.PaperEditorWidget
import com.paper.model.ModelConst
import com.paper.model.repository.CommonPenPrefsRepoFileImpl
import com.paper.useCase.BindViewWithWidget
import com.paper.view.canvas.PaperCanvasView
import com.paper.view.editPanel.PaperEditPanelView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class PaperEditorActivity : AppCompatActivity() {

    private val mCanvasView by lazy {
        val field = findViewById<PaperCanvasView>(R.id.paper_canvas)
        field.setBitmapRepo((application as IBitmapRepoProvider).getBitmapRepo())
        field
    }
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

    private val mUiScheduler = AndroidSchedulers.mainThread()
    private val mWorkerScheduler = Schedulers.io()

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()
    // Error signal
    private val mErrorSignal = PublishSubject.create<Throwable>()

    private val mWidget by lazy {
        PaperEditorWidget(
            paperRepo = (application as IPaperRepoProvider).getPaperRepo(),
            paperTransformRepo = (application as IPaperTransformRepoProvider).getPaperTransformRepo(),
            sharedPrefs = application as ISharedPreferenceService,
            penPrefs = CommonPenPrefsRepoFileImpl(getExternalFilesDir(packageName)),
            caughtErrorSignal = mErrorSignal,
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
            mWidget.onUpdateProgress()
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    when {
                        event.justStart -> showProgressBar(0)
                        event.justStop -> hideProgressBar()
                    }
                })

        // Error
        mDisposables.add(
            mErrorSignal
                .observeOn(mUiScheduler)
                .subscribe { error ->
                    if (BuildConfig.DEBUG) {
                        showErrorAlert(error)
                    }
                })

        // Close button.
        mDisposables.add(
            onClickCloseButton()
                .observeOn(mUiScheduler)
                .subscribe {
                    close()
                })

        // View port indicator
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

        // Undo & redo buttons
        mDisposables.add(
            RxView.clicks(mBtnUndo)
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
                    showWIP()
//                    mWidget.handleUndo()
                })
        mDisposables.add(
            RxView.clicks(mBtnRedo)
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
                    showWIP()
//                    mWidget.handleRedo()
                })
        mDisposables.add(
            mWidget.onGetUndoRedoEvent()
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    mBtnUndo.isEnabled = event.canUndo
                    mBtnRedo.isEnabled = event.canRedo
                })

        // Delete button
        mDisposables.add(
            RxView.clicks(mBtnDelete)
                .observeOn(mUiScheduler)
                .subscribe {
                    // TODO
                    showWIP()
                })

        // Bind sub-view with the sub-widget if the widget is ready!
        mDisposables.add(
            mWidget.onCanvasWidgetReady()
                .observeOn(mUiScheduler)
                .switchMap { widget ->
                    BindViewWithWidget(view = mCanvasView,
                                       widget = widget,
                                       caughtErrorSignal = mErrorSignal)
                }
                .subscribe())
        mDisposables.add(
            mWidget.onEditPanelWidgetReady()
                .observeOn(mUiScheduler)
                .switchMap { widget ->
                    BindViewWithWidget(view = mEditPanelView,
                                       widget = widget,
                                       caughtErrorSignal = mErrorSignal)
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
                       Toast.LENGTH_LONG).show()
    }

    private fun showErrorAlertThenFinish(error: Throwable) {
        mErrorThenFinishDialog.setMessage(error.toString())
        mErrorThenFinishDialog.show()
    }
}
