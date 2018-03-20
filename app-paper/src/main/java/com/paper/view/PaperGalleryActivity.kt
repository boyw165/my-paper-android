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

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxPopupMenu
import com.paper.AppConsts
import com.paper.R
import com.paper.gallery.PaperGalleryContract
import com.paper.gallery.PaperGalleryPresenter
import com.paper.gallery.PaperThumbnailEpoxyController
import com.paper.gallery.PaperThumbnailEpoxyModel
import com.paper.shared.model.PaperModel
import com.paper.shared.model.repository.PaperRepo
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class PaperGalleryActivity : AppCompatActivity(),
                             PaperGalleryContract.View,
                             PaperGalleryContract.Navigator {

    // View.
    private val mBtnExp: View by lazy { findViewById<View>(R.id.btn_other_exp) }
    private val mBtnExpMenu: PopupMenu by lazy {
        val m = PopupMenu(this@PaperGalleryActivity, mBtnExp)
        m.inflate(R.menu.menu_exp)
        m
    }
    private val mBtnNewPaper by lazy { findViewById<ImageView>(R.id.btn_new) }
    private val mBtnDelAllPapers by lazy { findViewById<ImageView>(R.id.btn_delete_all) }
    private val mClickPaperSignal = PublishSubject.create<Long>()

    private val mProgressBar: AlertDialog by lazy {
        AlertDialog.Builder(this@PaperGalleryActivity)
            .setCancelable(false)
            .create()
    }

    // Paper thumbnail list view and controller.
    private val mPapersView by lazy { findViewById<RecyclerView>(R.id.paper_list) }
    private val mPapersController by lazy { PaperThumbnailEpoxyController() }

    private val mPresenter by lazy {
        PaperGalleryPresenter(
            mRxPermissions,
            mPaperRepo,
            AndroidSchedulers.mainThread(),
            Schedulers.io())
    }

    // Permission.
    private val mRxPermissions: RxPermissions by lazy {
        RxPermissions(this@PaperGalleryActivity)
    }

    // Repo.
    // TODO: Inject the repo.
    private val mPaperRepo: PaperRepo by lazy {
        PaperRepo(packageName,
                  contentResolver,
                  externalCacheDir,
                  Schedulers.io())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paper_gallery)

        // Paper thumbnail list view.
        mPapersView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }

    override fun onResume() {
        super.onResume()

        // Paper thumbnail list view.
        mPapersView.adapter = mPapersController.adapter
        // Paper thumbnail list view controller.
        mPapersController.setOnClickPaperThumbnailListener(
            object : PaperThumbnailEpoxyModel.OnClickPaperThumbnailListener {
                override fun onClickPaperThumbnail(id: Long) {
                    mClickPaperSignal.onNext(id)
                }
            })

        mPresenter.bindViewOnCreate(
            view = this,
            navigator = this)
        mPresenter.onResume()
    }

    override fun onPause() {
        super.onPause()

        mPresenter.onPause()
        mPresenter.unbindViewOnDestroy()

        // Force to hide the progress-bar.
        hideProgressBar()

        // Paper thumbnail list view.
        // Break the reference to the Epoxy controller's adapter so that the
        // context reference would be recycled.
        mPapersView.adapter = null
        // Paper thumbnail list view controller.
        mPapersController.setOnClickPaperThumbnailListener(null)
    }

    override fun showPaperThumbnails(papers: List<PaperModel>) {
        mPapersController.cancelPendingModelBuild()
        mPapersController.setData(papers)
    }

    override fun showExpMenu() {
        mBtnExpMenu.show()
    }

    override fun showProgressBar() {
        mProgressBar.setMessage(getString(R.string.loading))
        mProgressBar.show()
    }

    override fun hideProgressBar() {
        mProgressBar.dismiss()
    }

    override fun showErrorAlert(error: Throwable) {
        TODO("not implemented")
    }

    override fun onClickPaper(): Observable<Long> {
        return mClickPaperSignal
    }

    override fun onClickNewPaper(): Observable<Any> {
        return RxView.clicks(mBtnNewPaper)
    }

    override fun onClickDeleteAllPapers(): Observable<Any> {
        return RxView.clicks(mBtnDelAllPapers)
    }

    override fun onClickShowExpMenu(): Observable<Any> {
        return RxView.clicks(mBtnExp)
    }

    override fun onClickExpMenu(): Observable<Int> {
        return RxPopupMenu.itemClicks(mBtnExpMenu)
            .map {
                return@map when (it.itemId) {
                    R.id.exp_rx_cancel -> 0
                    R.id.exp_navigation_framework -> 1
                    R.id.exp_convex_hull -> 2
                    R.id.exp_event_driven_simulation -> 3
                    else -> -1
                }
            }
    }

    override fun navigateToExpById(id: Int) {
        when (id) {
            0 -> {
                startActivity(Intent(
                    this@PaperGalleryActivity,
                    ExampleOfRxCancelActivity::class.java))
            }
            1 -> {
                startActivity(Intent(
                    this@PaperGalleryActivity,
                    ExampleOfCiceroneActivity1::class.java))
            }
            2 -> {
                startActivity(Intent(
                    this@PaperGalleryActivity,
                    ExampleOfConvexHullActivity::class.java))
            }
            3 -> {
                startActivity(Intent(
                    this@PaperGalleryActivity,
                    ExampleOfEventDrivenSimulationActivity::class.java))
            }
        }
    }

    override fun navigateToPaperEditor(id: Long) {
        startActivity(Intent(this@PaperGalleryActivity,
                             PaperEditorActivity::class.java)
                          .putExtra(AppConsts.PARAMS_PAPER_ID, id))
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun showError(err: Throwable) {
        Toast.makeText(this@PaperGalleryActivity,
                       err.toString(),
                       Toast.LENGTH_SHORT).show()
    }
}
