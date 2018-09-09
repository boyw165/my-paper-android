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
import com.paper.domain.ui.ICanvasOperationRepoProvider
import com.paper.domain.ui.UndoWidget
import com.paper.model.ISchedulers
import com.paper.domain.ui.WhiteboardEditorWidget
import com.paper.model.*
import com.paper.model.event.IntProgressEvent
import com.paper.model.event.TimedCounterEvent
import com.paper.model.repository.CommonPenPrefsRepoFileImpl
import com.paper.observables.BooleanDialogSingle
import com.paper.useCase.BindViewWithWidget
import com.paper.view.canvas.PaperCanvasView
import com.paper.view.editPanel.PaperEditPanelView
import com.paper.view.editPanel.PenSizePreview
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class PaperEditorActivity : AppCompatActivity() {

    private val mCanvasView by lazy {
        val field = findViewById<PaperCanvasView>(R.id.paper_canvas)
        field.injectBitmapRepository((application as IBitmapRepoProvider).getBitmapRepo())
        field
    }
    private val mMenuView by lazy { findViewById<PaperEditPanelView>(R.id.edit_panel) }
    private val mMenuPenSizeView by lazy { findViewById<PenSizePreview>(R.id.edit_panel_pen_size_preview) }

    private val mProgressDialog by lazy {
        AlertDialog.Builder(this@PaperEditorActivity)
            .setView(R.layout.dialog_saving_progress)
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

    // Toolbar as a button for opening the DEBUG menu.
    private val mBtnToolbar by lazy { findViewById<View>(R.id.btn_toolbar) }

    // Back button and signal.
    private val mBtnClose by lazy { findViewById<View>(R.id.btn_close) }
    private val mClickSysBackSignal = PublishSubject.create<Any>().toSerialized()

    // Undo & undo buttons
    private val mBtnUndo by lazy { findViewById<View>(R.id.btn_undo) }
    private val mBtnRedo by lazy { findViewById<View>(R.id.btn_redo) }

    // Delete button
    private val mBtnDelete by lazy { findViewById<View>(R.id.btn_delete) }

    private val mUiScheduler = AndroidSchedulers.mainThread()
    private val mWorkerScheduler = Schedulers.io()

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<IntProgressEvent>().toSerialized()
    // Error signal
    private val mErrorSignal = PublishSubject.create<Throwable>().toSerialized()

    private val mPrefs by lazy { (application as IPreferenceServiceProvider).preference }
    private val mPresenter by lazy {
        WhiteboardEditorWidget(
            paperRepo = (application as IWhiteboardRepoProvider).getWhiteboardRepo(),
            undoWidget = UndoWidget(undoRepo =,
                                    redoRepo =,
                                    schedulers = this@PaperEditorActivity),
            penPrefs = CommonPenPrefsRepoFileImpl(getExternalFilesDir(packageName)),
            caughtErrorSignal = mErrorSignal,
            schedulers = (application as ISchedulers))
    }

    // Disposables
    private val mDisposables = CompositeDisposable()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_paper_editor)

        // The window for showing the custom view has the minimum width
        // Ref: http://developerarea.tumblr.com/post/139280308210/how-to-set-dialogfragments-width-and-height
        mProgressDialog.context.theme.applyStyle(R.style.AppTheme_Dialog_Alert_NoMinWidth, true)

        // TODO: Use subject and flatMap
        mCanvasView.addCanvasEventSource(mMenuView.onUpdateViewPortPosition())

        initDebug()

        // Progress
        Observable.merge(
            mUpdateProgressSignal,
            mPresenter.onUpdateProgress())
            .observeOn(mUiScheduler)
            .subscribe { event ->
                when {
                    event.justStart -> showIndeterminateProgressDialog()
                    event.justStop -> hideIndeterminateProgressDialog()
                }
            }
            .addTo(mDisposables)

        // Close button.
        onClickCloseButton()
            .switchMap { _ ->
                mCanvasView
                    .writeThumbFileToBitmapRepository()
                    .toObservable()
                    .doOnSubscribe { mUpdateProgressSignal.onNext(IntProgressEvent.start(0)) }
                    .doOnNext { mUpdateProgressSignal.onNext(IntProgressEvent.stop(100)) }
                    .doOnComplete { close() }
                    .doOnError {
                        mErrorSignal.onNext(it)
                        close()
                    }
                    .observeOn(mUiScheduler)
                    .flatMap { (file, width, height) ->
                        mPresenter.requestStop(file, width, height)
                    }
                    .observeOn(mUiScheduler)
            }
            .observeOn(mUiScheduler)
            .subscribe()
            .addTo(mDisposables)

        // Export button
        mMenuView.onClickExport()
            .switchMap { _ ->
                mCanvasView
                    .writeFileToSystemMediaStore()
                    .toObservable()
                    .doOnSubscribe {
                        mUpdateProgressSignal.onNext(IntProgressEvent.start())
                    }
                    .doOnComplete {
                        mUpdateProgressSignal.onNext(IntProgressEvent.stop())
                    }
                    .doOnError {
                        mUpdateProgressSignal.onNext(IntProgressEvent.stop())
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Toast.makeText(this@PaperEditorActivity,
                               resources.getString(R.string.msg_export_successfully),
                               Toast.LENGTH_SHORT)
                    .show()
            }
            .addTo(mDisposables)

        // View port : boundary
        mCanvasView
            .onDrawViewPort()
            .observeOn(mUiScheduler)
            .subscribe { event ->
                mMenuView.setCanvasAndViewPort(
                    event.canvas,
                    event.viewPort)
            }
            .addTo(mDisposables)

        // Pen size: the menu view needs to know the view-port scale so that it
        // gets the right pen size observed in
        mMenuView.setCanvasContext(mCanvasView)
        mMenuPenSizeView.setCanvasContext(mCanvasView)

        // Pen size preview
        mMenuPenSizeView
            .updatePenSize(sizeSrc = mMenuView.onUpdatePenSize(),
                           colorSrc = mMenuView.onUpdatePenColor())
            .addTo(mDisposables)

        // Undo & undo buttons
        mPresenter.handleUndo(RxView.clicks(mBtnUndo))
        mPresenter.handleRedo(RxView.clicks(mBtnRedo))
        mPresenter.observeUndoAvailability()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                mBtnUndo.isEnabled = event.canUndo
                mBtnRedo.isEnabled = event.canRedo
            }
            .addTo(mDisposables)

        // Delete button
        RxView.clicks(mBtnDelete)
            .observeOn(mUiScheduler)
            .switchMap {
                val builder = AlertDialog.Builder(this@PaperEditorActivity)
                    .setTitle(R.string.doodle_clear_title)
                    .setMessage(R.string.doodle_clear_message)
                    .setCancelable(true)

                BooleanDialogSingle(
                    builder = builder,
                    positiveButtonString = resources.getString(R.string.doodle_clear_ok),
                    negativeButtonString = resources.getString(R.string.doodle_clear_cancel))
                    .toObservable()
                    .observeOn(mUiScheduler)
                    .flatMap { doIt ->
                        if (doIt) {
                            mPresenter.eraseCanvas()
                        } else {
                            Observable.never()
                        }
                    }
            }
            .subscribe()
            .addTo(mDisposables)
        // TODO: Refactor to below
//        mPresenter
//            .handleDelete(RxView.clicks(mBtnDelete))
//            .addTo(disposableBag)

        // Load whiteboard and establish the binding
        // FIXME: save-restore
        val paperIdSrc = if (savedState == null) {
            Observable.just(intent.getLongExtra(AppConst.PARAMS_PAPER_ID, ModelConst.TEMP_ID))
        } else {
            Observable.just(intent.getLongExtra(AppConst.PARAMS_PAPER_ID, ModelConst.TEMP_ID))
        }
        paperIdSrc
            .switchMap { paperID ->
                mPresenter.start(paperID)
                    .flatMap { sources ->
                        initWidgets(sources)
                    }
            }
            .subscribe()
            .addTo(mDisposables)
    }

    override fun onDestroy() {
        super.onDestroy()

        mPresenter.stop()

        mDisposables.clear()

        // Force to hide the progress-bar.
        hideIndeterminateProgressDialog()
        // Force to hide the error dialog.
        mErrorThenFinishDialog.dismiss()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            mClickSysBackSignal.onNext(0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

//        outState.putLong()
    }

    private fun initWidgets(sources: WhiteboardEditorWidget.OnStart): Observable<Any> {
        return Observable.merge(
            sources.onCanvasWidgetReady
                .flatMap { widget ->
                    BindViewWithWidget(view = mCanvasView,
                                       widget = widget,
                                       caughtErrorSignal = mErrorSignal)
                },
            sources.onMenuWidgetReady
                .flatMap { widget ->
                    BindViewWithWidget(view = mMenuView,
                                       widget = widget,
                                       caughtErrorSignal = mErrorSignal)
                })
    }

    private fun initDebug() {
        // Error hub
        mDisposables.add(
            mErrorSignal
                .observeOn(mUiScheduler)
                .subscribe { error ->
                    if (BuildConfig.DEBUG) {
                        showErrorAlert(error)
                    }
                })

        // Debug menu (triggered by tapping on tool-bar quickly for several times)
        mDisposables.add(
            RxView.clicks(mBtnToolbar)
                .filter { BuildConfig.DEBUG }
                .map { TimedCounterEvent(timeInMs = System.currentTimeMillis(),
                                         count = 0) }
                .scan { last, event ->
                    if (event.timeInMs - last.timeInMs < 1000L) {
                        val accumulation = last.count + 1
                        val remaining = 5 - accumulation

                        if (remaining in 0..3) {
                            Toast.makeText(this@PaperEditorActivity,
                                           "$remaining ${getString(R.string.tap_to_show_debug_prefs)}",
                                           Toast.LENGTH_SHORT).show()
                        }

                        event.copy(count = accumulation)
                    } else {
                        event.copy(count = 0)
                    }
                }
                .filter { event -> event.count > 5 }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    try {
                        supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fullscreen_frame,
                                     DebugPreferenceFragment())
                            .addToBackStack(null)
                            .commit()
                    } catch (ignored: Throwable) {
                        finish()
                    }
                })
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

    private fun showIndeterminateProgressDialog() {
        if (!mProgressDialog.isShowing) {
            mProgressDialog.show()
        }
    }

    private fun hideIndeterminateProgressDialog() {
        mProgressDialog.dismiss()
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
