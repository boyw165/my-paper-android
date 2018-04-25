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

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxPopupMenu
import com.paper.domain.IPaperRepoProvider
import com.paper.domain.ISharedPreferenceService
import com.paper.model.ModelConst
import com.paper.model.PaperModel
import com.paper.presenter.PaperGalleryContract
import com.paper.presenter.PaperGalleryPresenter
import com.paper.view.gallery.PaperThumbnailEpoxyController
import com.tbruyelle.rxpermissions2.RxPermissions
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.transform.Pivot
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

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
    private val mBtnDelPaper by lazy { findViewById<ImageView>(R.id.btn_delete) }
    private val mClickPaperSignal = PublishSubject.create<Long>()
    private val mBrowsePaperSignal = PublishSubject.create<Long>()

    private val mProgressBar: AlertDialog by lazy {
        AlertDialog.Builder(this@PaperGalleryActivity)
            .setCancelable(false)
            .create()
    }

    // Image loader
    private val mImgLoader by lazy { Glide.with(this@PaperGalleryActivity) }

    // Paper thumbnail list view and controller.
    private val mPapersView by lazy { findViewById<DiscreteScrollView>(R.id.paper_list) }
    private val mPapersViewController by lazy {
        PaperThumbnailEpoxyController(mImgLoader)
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
        mPapersView.adapter = mPapersViewController.adapter
        mPapersView.setItemTransformer(
            ScaleTransformer.Builder()
                .setMaxScale(1.0f)
                .setMinScale(1.0f)
                .setPivotX(Pivot.X.CENTER) // CENTER is a default one
                .setPivotY(Pivot.Y.CENTER) // CENTER is a default one
                .build())
//        mPapersView.setSlideOnFling(true)
//        mPapersView.setOverScrollEnabled(true)
//        // Determines how much time it takes to change the item on fling, settle
//        // or smoothScroll
//        mPapersView.setItemTransitionTimeMillis(300)
        mPapersView.addScrollStateChangeListener(object : DiscreteScrollView.ScrollStateChangeListener<RecyclerView.ViewHolder> {

            override fun onScroll(scrollPosition: Float,
                                  currentPosition: Int,
                                  newPosition: Int,
                                  currentHolder: RecyclerView.ViewHolder?,
                                  newCurrent: RecyclerView.ViewHolder?) {
            }

            override fun onScrollEnd(currentItemHolder: RecyclerView.ViewHolder,
                                     adapterPosition: Int) {
                val paper = mPapersViewController.getPaperFromAdapterPosition(adapterPosition)
                // Report the paper ID in the database
                mBrowsePaperSignal.onNext(paper?.id ?: ModelConst.INVALID_ID)
            }

            override fun onScrollStart(currentItemHolder: RecyclerView.ViewHolder,
                                       adapterPosition: Int) {
            }
        })

        // Presenter.
        mPresenter.bindView(
            view = this,
            navigator = this)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Presenter.
        mPresenter.unbind()

        // Paper thumbnail list view.
        // Break the reference to the Epoxy controller's adapter so that the
        // context reference would be recycled.
        mPapersView.adapter = null
    }

    override fun onResume() {
        super.onResume()

        // Presenter.
        mPresenter.resume()
    }

    override fun onPause() {
        super.onPause()

        // Presenter.
        mPresenter.pause()
    }

    override fun showPaperThumbnails(papers: List<PaperModel>) {
        mPapersViewController.setData(papers)
    }

    override fun showPaperThumbnailAt(position: Int) {
        val actualPosition = mPapersViewController.getAdapterPositionFromDataPosition(position)
        if (actualPosition > 0) {
            // FIXME: without the delay the behavior would be strange
            mPapersView.postDelayed({
                mPapersView.scrollToPosition(actualPosition)
            }, 100)
        }
    }

    override fun setDeleteButtonVisibility(visible: Boolean) {
        mBtnDelPaper.visibility = if (visible) View.VISIBLE else View.GONE
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
        return mPapersViewController
            .onClickPaper()
            .throttleFirst(1000, TimeUnit.MILLISECONDS)
    }

    override fun onClickNewPaper(): Observable<Any> {
        return Observable
            .merge(RxView.clicks(mBtnNewPaper),
                   mPapersViewController.onClickNewButton())
            .throttleFirst(1000, TimeUnit.MILLISECONDS)
    }

    override fun onBrowsePaper(): Observable<Long> {
        return mBrowsePaperSignal
    }

    override fun onClickDeletePaper(): Observable<Any> {
        return RxView.clicks(mBtnDelPaper)
    }

    override fun onClickShowExpMenu(): Observable<Any> {
        return RxView.clicks(mBtnSettings)
    }

    override fun onClickExpMenu(): Observable<Int> {
        return RxPopupMenu.itemClicks(mBtnExpMenu)
            .map {
                return@map when (it.itemId) {
                    R.id.exp_rx_cancel -> 0
                    R.id.exp_convex_hull -> 1
                    R.id.exp_event_driven_simulation -> 2
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
                    ExampleOfConvexHullActivity::class.java))
            }
            2 -> {
                startActivity(Intent(
                    this@PaperGalleryActivity,
                    ExampleOfEventDrivenSimulationActivity::class.java))
            }
        }
    }

    override fun navigateToPaperEditor(id: Long) {
        startActivity(Intent(this@PaperGalleryActivity,
                             PaperEditorActivity::class.java)
                          .putExtra(AppConst.PARAMS_PAPER_ID, id)
                          .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun showError(err: Throwable) {
        Toast.makeText(this@PaperGalleryActivity,
                       err.toString(),
                       Toast.LENGTH_SHORT).show()
    }
}
