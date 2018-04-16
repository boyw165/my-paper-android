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
import com.paper.model.ModelConst
import com.paper.presenter.PaperEditorContract
import com.paper.presenter.PaperEditorPresenter
import com.paper.view.canvas.IPaperWidgetView
import com.paper.view.canvas.PaperWidgetView
import com.paper.view.editPanel.IPaperEditPanelView
import com.paper.view.editPanel.PaperEditPanelView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class PaperEditorActivity : AppCompatActivity(),
                            PaperEditorContract.View {

    // View.
    private val mCanvasView by lazy { findViewById<PaperWidgetView>(R.id.paper_canvas) }
    private val mEditingPanelView by lazy { findViewById<PaperEditPanelView>(R.id.editing_panel) }

    private val mProgressBar by lazy {
        AlertDialog.Builder(this@PaperEditorActivity)
            .setCancelable(false)
            .create()
    }

    // Back button and signal.
    private val mBtnClose: View by lazy { findViewById<View>(R.id.btn_close) }
    private val mClickSysBackSignal = PublishSubject.create<Any>()

    // Undo & redo buttons
    private val mBtnUndo by lazy { findViewById<View>(R.id.btn_undo) }
    private val mBtnRedo by lazy { findViewById<View>(R.id.btn_redo) }

    // Delete button
    private val mBtnDelete by lazy { findViewById<View>(R.id.btn_delete) }

    // Repositories.
    // TODO: Inject the repo.
    private val mPaperRepo by lazy { (application as IPaperRepoProvider).getRepo() }

    // Presenters and controllers.
    private val mEditorPresenter: PaperEditorPresenter by lazy {
        PaperEditorPresenter(mPaperRepo,
                             AndroidSchedulers.mainThread(),
                             Schedulers.single())
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_paper_editor)

        val paperId = intent.getLongExtra(AppConst.PARAMS_PAPER_ID, ModelConst.TEMP_ID)

        // Presenter.
        mEditorPresenter.bindView(this, paperId)
    }

    override fun onDestroy() {
        super.onDestroy()

        mEditorPresenter.unbindView()

        // Force to hide the progress-bar.
        hideProgressBar()
    }

    override fun onBackPressed() {
        mClickSysBackSignal.onNext(0)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Editor view ////////////////////////////////////////////////////////////

    override fun getCanvasView(): IPaperWidgetView {
        return mCanvasView
    }

    override fun getEditingPanelView(): IPaperEditPanelView {
        return mEditingPanelView
    }

    override fun close() {
        finish()
    }

    override fun onClickCloseButton(): Observable<Any> {
        return Observable.merge(mClickSysBackSignal,
                                RxView.clicks(mBtnClose))
    }

    override fun onClickUndoButton(): Observable<Any> {
        return RxView.clicks(mBtnUndo)
    }

    override fun onClickRedoButton(): Observable<Any> {
        return RxView.clicks(mBtnRedo)
    }

    override fun onClickDeleteButton(): Observable<Any> {
        return RxView.clicks(mBtnDelete)
    }

    override fun onClickMenu(): Observable<Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showProgressBar(progress: Int) {
        if (!mProgressBar.isShowing) {
            mProgressBar.show()
        }

        mProgressBar.setMessage(
            "%s: %d".format(getString(R.string.loading), progress))
    }

    override fun hideProgressBar() {
        mProgressBar.dismiss()
    }

    override fun showWIP() {
        Toast.makeText(this, R.string.msg_under_construction, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorAlert(error: Throwable) {
        Toast.makeText(this@PaperEditorActivity,
                       error.toString(),
                       Toast.LENGTH_SHORT).show()
    }
}
