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

package com.paper.view

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.paper.AppConst
import com.paper.R
import com.paper.editor.PaperEditorContract
import com.paper.editor.PaperEditorPresenter
import com.paper.editor.view.EditingPanelView
import com.paper.editor.view.IEditingPanelView
import com.paper.editor.view.IPaperWidgetView
import com.paper.editor.view.PaperWidgetView
import com.paper.protocol.IPaperRepoProvider
import com.paper.shared.model.PaperConsts
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class PaperEditorActivity : AppCompatActivity(),
                            PaperEditorContract.View {

    // View.
    private val mCanvasView by lazy { findViewById<PaperWidgetView>(R.id.paper_canvas) }
    private val mEditingPanelView by lazy { findViewById<EditingPanelView>(R.id.editing_panel) }
    private val mProgressBar by lazy {
        AlertDialog.Builder(this@PaperEditorActivity)
            .setCancelable(false)
            .create()
    }

    // Back button and signal.
    private val mBtnClose: View by lazy { findViewById<View>(R.id.btn_close) }
    private val mClickSysBackSignal = PublishSubject.create<Any>()

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

        val paperId = intent.getLongExtra(AppConst.PARAMS_PAPER_ID, PaperConsts.TEMP_ID)

        // Presenter.
        mEditorPresenter.bindViewOnCreate(this)

        // Load paper.
        mEditorPresenter.loadPaperById(paperId)
    }

    override fun onDestroy() {
        super.onDestroy()

        mEditorPresenter.unbindViewOnDestroy()

//        // Force to hide the progress-bar.
//        hideProgressBar()
    }

    override fun onBackPressed() {
        mClickSysBackSignal.onNext(0)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Editor view ////////////////////////////////////////////////////////////

    override fun getCanvasView(): IPaperWidgetView {
        return mCanvasView
    }

    override fun getEditingPanelView(): IEditingPanelView {
        return mEditingPanelView
    }

    override fun close() {
        finish()
    }

    override fun onClickCloseButton(): Observable<Any> {
        return Observable.merge(mClickSysBackSignal,
                                RxView.clicks(mBtnClose))
    }

    override fun onClickDrawButton(): Observable<Boolean> {
        TODO()
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
        mProgressBar.hide()
    }

    override fun showErrorAlert(error: Throwable) {
        Toast.makeText(this@PaperEditorActivity,
                       error.toString(),
                       Toast.LENGTH_SHORT).show()
    }

    private fun navigateToSketchEditor(width: Int, height: Int) {
        // FIXME: Workaround of creating a new paper model and navigate to the
        // FIXME: sketch editor immediately.
        //        startActivityForResult(
        //            Intent(this, SketchEditorActivity::class.java)
        //                // Pass a sketch width and height.
        //                .putExtra(SketchEditorActivity.PARAMS_SKETCH_WIDTH, width)
        //                .putExtra(SketchEditorActivity.PARAMS_SKETCH_HEIGHT, height)
        //                // Pass a sketch background.
        //                //                .putExtra(SketchEditorActivity.PARAMS_BACKGROUND_FILE, background)
        //                // Remembering brush color and stroke width.
        //                //                .putExtra(SketchEditorActivity.PARAMS_REMEMBERING_BRUSH_COLOR, brushColor)
        //                //                .putExtra(SketchEditorActivity.PARAMS_REMEMBERING_BRUSH_SIZE, brushSize)
        //                // Ask the editor enter fullscreen mode.
        //                .putExtra(SketchEditorActivity.PARAMS_FULLSCREEN_MODE, false)
        //                // DEBUG mode.
        //                .putExtra(SketchEditorActivity.PARAMS_DEBUG_MODE, true),
        //            0)
    }
}
