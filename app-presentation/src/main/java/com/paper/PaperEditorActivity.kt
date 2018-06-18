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
import com.paper.domain.widget.editor.PaperEditorWidget
import com.paper.model.*
import com.paper.model.event.ProgressEvent
import com.paper.model.event.TimedCounterEvent
import com.paper.model.repository.CommonPenPrefsRepoFileImpl
import com.paper.observables.BooleanDialogSingle
import com.paper.useCase.BindViewWithWidget
import com.paper.view.canvas.PaperCanvasView
import com.paper.view.editPanel.PaperEditPanelView
import com.paper.view.editPanel.PenSizePreview
import io.reactivex.Observable
import io.reactivex.Single
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
    private val mMenuView by lazy { findViewById<PaperEditPanelView>(R.id.edit_panel) }
    private val mMenuPenSizeView by lazy { findViewById<PenSizePreview>(R.id.edit_panel_pen_size_preview) }

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

    // Toolbar as a button for opening the DEBUG menu.
    private val mBtnToolbar by lazy { findViewById<View>(R.id.btn_toolbar) }

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

    private val mPrefs by lazy { (application as IPreferenceServiceProvider).preference }
    private val mWidget by lazy {
        PaperEditorWidget(
            paperRepo = (application as IPaperRepoProvider).getPaperRepo(),
            paperTransformRepo = (application as IPaperTransformRepoProvider).getPaperTransformRepo(),
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

        // TODO: Use subject and flatMap
        mCanvasView.addCanvasEventSource(mMenuView.onUpdateViewPortPosition())

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
                .switchMap {
                    mCanvasView
                        .writeThumbFile()
                        .toObservable()
                        .doOnSubscribe { mUpdateProgressSignal.onNext(ProgressEvent.start(0)) }
                        .doOnNext { mUpdateProgressSignal.onNext(ProgressEvent.stop(100)) }
                        .observeOn(mUiScheduler)
                        .flatMap { (file, width, height) ->
                            mWidget.requestStop(file, width, height)
                        }
                        .observeOn(mUiScheduler)
                        .doOnComplete { close() }
                }
                .observeOn(mUiScheduler)
                .subscribe())

        // View port indicator
        mDisposables.add(
            mCanvasView
                .onDrawViewPort()
                .observeOn(mUiScheduler)
                .subscribe { event ->
                    mMenuView.setCanvasAndViewPort(
                        event.canvas,
                        event.viewPort)
                })

        // Pen size preview
        mDisposables.add(
            mMenuPenSizeView
                .updatePenSize(sizeSrc = mMenuView.onUpdatePenSize(),
                               colorSrc = mMenuView.onUpdatePenColor()))

        // Undo & redo buttons
        mWidget.addUndoSignal(RxView.clicks(mBtnUndo))
        mWidget.addRedoSignal(RxView.clicks(mBtnRedo))
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
                                mWidget.eraseCanvas()
                            } else {
                                Observable.never()
                            }
                        }
                }
                .subscribe())

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
                    BindViewWithWidget(view = mMenuView,
                                       widget = widget,
                                       caughtErrorSignal = mErrorSignal)
                }
                .subscribe())

        // Start
        val paperIdSrc = if (savedState == null) {
            Single.just(intent.getLongExtra(AppConst.PARAMS_PAPER_ID, ModelConst.TEMP_ID))
        } else {
            mPrefs
                .getLong(ModelConst.PREFS_BROWSE_PAPER_ID, ModelConst.TEMP_ID)
                .first(ModelConst.TEMP_ID)
        }
        mDisposables.add(
            paperIdSrc
                .subscribe { paperID ->
                    mWidget.start(paperID)
                })
    }

    override fun onDestroy() {
        super.onDestroy()

        mWidget.stop()

        mDisposables.clear()

        // Force to hide the progress-bar.
        hideProgressBar()
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
        mProgressBar.setMessage("${getString(R.string.processing)}...")

        if (!mProgressBar.isShowing) {
            mProgressBar.show()
        }
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
