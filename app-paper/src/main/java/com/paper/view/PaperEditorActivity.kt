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

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import android.view.View
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxCompoundButton
import com.paper.AppConsts
import com.paper.R
import com.paper.editor.ITouchConfig
import com.paper.editor.PaperController
import com.paper.editor.PaperEditorContract
import com.paper.editor.PaperEditorPresenter
import com.paper.editor.view.ICanvasView
import com.paper.editor.view.PaperCanvasView
import com.paper.protocol.IContextProvider
import com.paper.shared.model.PaperConsts
import com.paper.shared.model.repository.PaperRepo
import com.paper.shared.model.repository.SketchRepo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class PaperEditorActivity : AppCompatActivity(),
                            IContextProvider,
                            ITouchConfig,
                            PaperEditorContract.View {

    // View.
    private val mBtnClose: View by lazy { findViewById<View>(R.id.btn_close) }
    private val mBtnDraw: SwitchCompat by lazy { findViewById<SwitchCompat>(R.id.btn_draw) }
    private val mCanvasView: PaperCanvasView by lazy { findViewById<PaperCanvasView>(R.id.paper_canvas) }
    private val mProgressBar: AlertDialog by lazy {
        AlertDialog.Builder(this@PaperEditorActivity)
            .setCancelable(false)
            .create()
    }

    // Repositories.
    // TODO: Inject the repo.
    private val mPaperRepo: PaperRepo by lazy {
        PaperRepo(packageName,
                  contentResolver,
                  externalCacheDir,
                  Schedulers.io())
    }
    private val mSketchRepo: SketchRepo by lazy {
        SketchRepo(packageName,
                   contentResolver,
                   externalCacheDir,
                   Schedulers.io())
    }

    // Presenters and controllers.
    private val mPaperController: PaperController by lazy {
        PaperController(AndroidSchedulers.mainThread(),
                        Schedulers.io())
    }
    private val mEditorPresenter: PaperEditorPresenter by lazy {
        // TODO: It's not a good design that the presenter knows other presenters.
        PaperEditorPresenter(mPaperController,
                             mPaperRepo,
                             AndroidSchedulers.mainThread(),
                             Schedulers.io())
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_paper_editor)

        val paperId = intent.getLongExtra(AppConsts.PARAMS_PAPER_ID, PaperConsts.INVALID_ID)

        // Presenter.
        mEditorPresenter.bindViewOnCreate(this)

        // Load paper.
        mEditorPresenter.loadPaperById(paperId)

        //        mDisposables1.add(
        //            mPaperRepo
        //                // Get the temporary paper if it exists.
        //                .getTempPaper()
        //                // TODO: New temp sketch.
        //                .flatMap { paper ->
        //                    // FIXME: Create a temporary fullscreen size sketch.
        //                    mSketchRepo.newTempSketch(paper.width, paper.height)
        //                }
        //                // Convert to view-model.
        //                .compose { upstream ->
        //                    upstream
        //                        .map { anything -> UiModel.succeed(anything) }
        ////                        .startWith { UiModel.inProgress(null) }
        //                        .onErrorReturn { err -> UiModel.failed(err) }
        //                }
        //                .observeOn(AndroidSchedulers.mainThread())
        //                .subscribe { vm ->
        //                    when {
        //                        vm.isInProgress -> {
        //                            showProgressBar()
        //                        }
        //                        vm.isSuccessful -> {
        //                            hideProgressBar()
        //
        //                            val sketch = vm.bundle
        //                            navigateToSketchEditor(sketch.width, sketch.height)
        //                        }
        //                        else -> {
        //                            showError(vm.error)
        //                            hideProgressBar()
        //                            finish()
        //                        }
        //                    }
        //                })
    }

    override fun onDestroy() {
        super.onDestroy()

        mPaperController.unbindView()
        mEditorPresenter.unbindViewOnDestroy()

        //        // Force to hide the progress-bar.
        //        hideProgressBar()
    }

    override fun onResume() {
        super.onResume()

        mEditorPresenter.onResume()
    }

    override fun onPause() {
        super.onPause()

        mEditorPresenter.onPause()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Context provider ///////////////////////////////////////////////////////

    override fun getContext(): Context {
        return this
    }

    ///////////////////////////////////////////////////////////////////////////
    // Canvas config //////////////////////////////////////////////////////////

    override fun getTouchSlop(): Float {
        return resources.getDimension(R.dimen.touch_slop)
    }

    override fun getTapSlop(): Float {
        return resources.getDimension(R.dimen.tap_slop)
    }

    override fun getMinFlingVec(): Float {
        return resources.getDimension(R.dimen.fling_min_vec)
    }

    override fun getMaxFlingVec(): Float {
        return resources.getDimension(R.dimen.fling_max_vec)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Editor view ////////////////////////////////////////////////////////////

    override fun getCanvasView(): ICanvasView {
        return mCanvasView
    }

    override fun close() {
//        finish()
    }

    override fun onClickCloseButton(): Observable<Any> {
        return RxView.clicks(mBtnClose)
    }

    override fun onClickDrawButton(): Observable<Boolean> {
        return RxCompoundButton.checkedChanges(mBtnDraw)
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
