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
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxPopupMenu
import com.paper.AppConst
import com.paper.R
import com.paper.gallery.PaperGalleryContract
import com.paper.gallery.PaperGalleryPresenter
import com.paper.gallery.view.IOnClickPaperThumbnailListener
import com.paper.gallery.view.PaperThumbnailEpoxyController
import com.paper.protocol.IPaperRepoProvider
import com.paper.protocol.ISharedPreferenceService
import com.paper.shared.model.PaperModel
import com.tbruyelle.rxpermissions2.RxPermissions
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.transform.Pivot
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class PaperGalleryActivity : AppCompatActivity(),
                             PaperGalleryContract.View,
                             PaperGalleryContract.Navigator {
    // Settings view
    private val mBtnSettings: View by lazy { findViewById<View>(R.id.btn_settings) }

    // Experiment views.
    private val mBtnExpMenu: PopupMenu by lazy {
        val m = PopupMenu(this@PaperGalleryActivity, mBtnSettings)
        m.inflate(R.menu.menu_exp)
        m
    }

    // Paper views and signals.
    private val mBtnNewPaper by lazy { findViewById<ImageView>(R.id.btn_new) }
    private val mBtnDelAllPapers by lazy { findViewById<ImageView>(R.id.btn_delete) }
    private val mClickPaperSignal = PublishSubject.create<Long>()
    private val mBrowsePositionSignal = PublishSubject.create<Int>()

    private val mProgressBar: AlertDialog by lazy {
        AlertDialog.Builder(this@PaperGalleryActivity)
            .setCancelable(false)
            .create()
    }

    // Paper thumbnail list view and controller.
    private val mPapersView by lazy { findViewById<DiscreteScrollView>(R.id.paper_list) }
    private val mPapersController by lazy {
        PaperThumbnailEpoxyController(Glide.with(this@PaperGalleryActivity))
    }

    // Presenter.
    private val mPresenter by lazy {
        PaperGalleryPresenter(
            RxPermissions(this@PaperGalleryActivity),
            (application as IPaperRepoProvider).getRepo(),
            (application as ISharedPreferenceService),
            AndroidSchedulers.mainThread(),
            Schedulers.io())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paper_gallery)

        // Paper thumbnail list view.
        mPapersView.adapter = mPapersController.adapter
        mPapersView.setItemTransformer(
            ScaleTransformer.Builder()
                .setMaxScale(1.05f)
                .setMinScale(0.8f)
                .setPivotX(Pivot.X.CENTER) // CENTER is a default one
                .setPivotY(Pivot.Y.CENTER) // CENTER is a default one
                .build())
        mPapersView.setSlideOnFling(true)
        mPapersView.setOverScrollEnabled(true)
        // Determines how much time it takes to change the item on fling, settle
        // or smoothScroll
        mPapersView.setItemTransitionTimeMillis(300)
        mPapersView.addScrollListener { scrollPosition,
                                        currentPosition,
                                        newPosition,
                                        currentHolder,
                                        newCurrent ->
            if (currentPosition != newPosition) {
                mBrowsePositionSignal.onNext(newPosition)
            }
        }

        // Presenter.
        mPresenter.bindViewOnCreate(
            view = this,
            navigator = this)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Presenter.
        mPresenter.unbindViewOnDestroy()

        // Paper thumbnail list view.
        // Break the reference to the Epoxy controller's adapter so that the
        // context reference would be recycled.
        mPapersView.adapter = null
    }

    override fun onResume() {
        super.onResume()

        //        // Paper thumbnail list view.
        //        mPapersView.adapter = mPapersController.adapter
        // Paper thumbnail list view controller.
        mPapersController.setOnClickPaperThumbnailListener(
            object : IOnClickPaperThumbnailListener {
                override fun onClickPaperThumbnail(id: Long) {
                    mClickPaperSignal.onNext(id)
                }
            })
        mPresenter.onResume()
    }

    override fun onPause() {
        super.onPause()

        // Presenter.
        mPresenter.onPause()

        // Paper thumbnail list view controller.
        mPapersController.setOnClickPaperThumbnailListener(null)
    }

    override fun setPaperThumbnailAspectRatio(ratio: Float) {
        mPapersController.setThumbnailAspectRatio(ratio)
    }

    override fun showPaperThumbnails(papers: List<PaperModel>) {
        mPapersController.setData(papers)
    }

    override fun showPaperThumbnailAt(position: Int) {
        if (position > 0 &&
            mPapersView.adapter.itemCount > position) {
            mPapersView.post { mPapersView.smoothScrollToPosition(position) }
        }
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

    override fun onBrowsePaper(): Observable<Int> {
        return mBrowsePositionSignal
    }

    override fun onClickNewPaper(): Observable<Any> {
        return RxView.clicks(mBtnNewPaper)
    }

    override fun onClickDeleteAllPapers(): Observable<Any> {
        return RxView.clicks(mBtnDelAllPapers)
    }

    override fun onClickShowExpMenu(): Observable<Any> {
        return RxView.clicks(mBtnSettings)
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
                          .putExtra(AppConst.PARAMS_PAPER_ID, id))
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun showError(err: Throwable) {
        Toast.makeText(this@PaperGalleryActivity,
                       err.toString(),
                       Toast.LENGTH_SHORT).show()
    }
}
