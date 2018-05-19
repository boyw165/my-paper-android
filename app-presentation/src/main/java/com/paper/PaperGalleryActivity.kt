// Copyright Apr 2018-present Paper
//
// Author: boyw165@gmail.com
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

import android.Manifest
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
import com.paper.domain.event.ProgressEvent
import com.paper.domain.useCase.DeletePaper
import com.paper.model.IPaper
import com.paper.model.ISharedPreferenceService
import com.paper.model.ModelConst
import com.paper.view.PaperSizeDialogFragment
import com.paper.view.PaperSizeDialogSingle
import com.paper.view.gallery.PaperThumbnailEpoxyController
import com.tbruyelle.rxpermissions2.RxPermissions
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.transform.Pivot
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class PaperGalleryActivity : AppCompatActivity() {
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

    private val mPaperSnapshots = mutableListOf<IPaper>()

    private val mRepo by lazy { (application as IPaperRepoProvider).getPaperRepo() }
    private val mPrefs by lazy { application as ISharedPreferenceService }
    private val mPermissions by lazy { RxPermissions(this) }

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()
    // Error signal
    private val mErrorSignal = PublishSubject.create<Throwable>()

    // Disposables
    private val mDisposables = CompositeDisposable()

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
                mBrowsePaperSignal.onNext(paper?.getId() ?: ModelConst.INVALID_ID)
            }

            override fun onScrollStart(currentItemHolder: RecyclerView.ViewHolder,
                                       adapterPosition: Int) {
            }
        })

        // Exp menu button.
        mDisposables.add(
            onClickShowExpMenu()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    showExpMenu()
                })

        // Exp menu.
        mDisposables.add(
            onClickExpMenu()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { id ->
                    navigateToExpById(id)
                })

        // Button of new paper.
        mDisposables.add(
            onClickNewPaper()
                .switchMap {
                    requestPermissions()
                        .observeOn(Schedulers.io())
                        .doOnNext {
                            mPrefs.putLong(ModelConst.PREFS_BROWSE_PAPER_ID, ModelConst.TEMP_ID)
                        }
                        .switchMap {
                            PaperSizeDialogSingle(PaperSizeDialogFragment(),
                                                  supportFragmentManager)
                                .flatMap { (w, h) ->
                                    mRepo.setTmpPaperSize(w, h)
                                }
                                .toObservable()
                        }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { done ->
                    if (!done) return@subscribe
                    openPaperInEditor(ModelConst.TEMP_ID)
                })
        // Button of existing paper.
        mDisposables.add(
            onClickPaper()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { id ->
                    openPaperInEditor(id)
                    hideProgressBar()
                })
        // Button of delete paper.
        mDisposables.add(
            onClickDeletePaper()
                .observeOn(Schedulers.io())
                .map {
                    val toDeletePaperID = mPrefs.getLong(ModelConst.PREFS_BROWSE_PAPER_ID,
                                                         ModelConst.INVALID_ID)
                    val toDeletePaperPosition = mPaperSnapshots.indexOfFirst { it.getId() == toDeletePaperID }
                    val newPaperPosition = if (toDeletePaperPosition + 1 < mPaperSnapshots.size) {
                        toDeletePaperPosition + 1
                    } else {
                        toDeletePaperPosition - 1
                    }
                    val newPaperID = if (newPaperPosition >= 0) {
                        mPaperSnapshots[newPaperPosition].getId()
                    } else {
                        ModelConst.INVALID_ID
                    }

                    // Save new paper ID.
                    mPrefs.putLong(ModelConst.PREFS_BROWSE_PAPER_ID, newPaperID)

                    return@map toDeletePaperID
                }
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap { toDeletePaperID ->
                    requestPermissions()
                        .observeOn(Schedulers.io())
                        .switchMap {
                            DeletePaper(paperID = toDeletePaperID,
                                        paperRepo = mRepo,
                                        errorSignal = mErrorSignal)
                                .toObservable()
                        }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideProgressBar()
                })
        // Browse papers.
        mDisposables.add(
            onBrowsePaper()
                .observeOn(Schedulers.io())
                .doOnNext { id ->
                    mPrefs.putLong(ModelConst.PREFS_BROWSE_PAPER_ID, id)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { id ->
                    if (id == ModelConst.INVALID_ID) {
                        setDeleteButtonVisibility(false)
                    } else {
                        setDeleteButtonVisibility(true)
                    }
                })

        // Load papers
        mDisposables.add(
            Observables
                .combineLatest(
                    requestPermissions()
                        .observeOn(Schedulers.io())
                        .switchMap {
                            // Note: Any database update will emit new result
                            mRepo.getPapers(isSnapshot = true)
                        },
                    Observable
                        .fromCallable {
                            mPrefs.getLong(ModelConst.PREFS_BROWSE_PAPER_ID, ModelConst.INVALID_ID)
                        }
                        .subscribeOn(Schedulers.io()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (papers, savedID) ->
                    // Hold the paper snapshots.
                    mPaperSnapshots.clear()
                    mPaperSnapshots.addAll(papers)

                    val position = papers.indexOfFirst { it.getId() == savedID }

                    if (position >= 0 && position <= papers.size) {
                        setDeleteButtonVisibility(true)
                    } else {
                        setDeleteButtonVisibility(false)
                    }

                    showPaperThumbnails(papers)
                    showPaperThumbnailAt(position)
                })
    }

    override fun onDestroy() {
        super.onDestroy()

        mDisposables.clear()

        // Paper thumbnail list view.
        // Break the reference to the Epoxy controller's adapter so that the
        // context reference would be recycled.
        mPapersView.adapter = null
    }

    private fun showPaperThumbnails(papers: List<IPaper>) {
        mPapersViewController.setData(papers)
    }

    private fun showPaperThumbnailAt(position: Int) {
        // FIXME: without the delay the behavior would be strange
        mPapersView.postDelayed(
            {
                val actualPosition = mPapersViewController.getAdapterPositionFromDataPosition(position)
                if (actualPosition >= 0) {
                    val offset = Math.abs(mPapersView.currentItem - actualPosition)
                    if (offset in 1..2) {
                        mPapersView.smoothScrollToPosition(actualPosition)
                    } else {
                        mPapersView.scrollToPosition(actualPosition)
                    }
                }
            }, 100)
    }

    private fun setDeleteButtonVisibility(visible: Boolean) {
        mBtnDelPaper.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun showExpMenu() {
        mBtnExpMenu.show()
    }

    private fun showProgressBar() {
        mProgressBar.setMessage(getString(R.string.processing))
        mProgressBar.show()
    }

    private fun hideProgressBar() {
        mProgressBar.dismiss()
    }

    private fun showErrorAlert(error: Throwable) {
        TODO("not implemented")
    }

    private fun onClickPaper(): Observable<Long> {
        return mPapersViewController
            .onClickPaper()
            .throttleFirst(1000, TimeUnit.MILLISECONDS)
    }

    private fun onClickNewPaper(): Observable<Any> {
        return Observable
            .merge(RxView.clicks(mBtnNewPaper),
                   mPapersViewController.onClickNewButton())
            .throttleFirst(1000, TimeUnit.MILLISECONDS)
    }

    private fun onBrowsePaper(): Observable<Long> {
        return mBrowsePaperSignal
    }

    private fun onClickDeletePaper(): Observable<Any> {
        return RxView.clicks(mBtnDelPaper)
    }

    private fun onClickShowExpMenu(): Observable<Any> {
        return RxView.clicks(mBtnSettings)
    }

    private fun onClickExpMenu(): Observable<Int> {
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

    private fun navigateToExpById(id: Int) {
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

    private fun openPaperInEditor(id: Long) {
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

    private fun requestPermissions(): Observable<Boolean> {
        return mPermissions
            .request(Manifest.permission.READ_EXTERNAL_STORAGE,
                     Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // TODO: Properly handle it, like showing permission explanation
            // TODO: page if it is denied.
            .filter { it }
    }
}
