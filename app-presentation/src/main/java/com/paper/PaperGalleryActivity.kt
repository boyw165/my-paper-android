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
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import com.dant.centersnapreyclerview.SnappingRecyclerView
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdListener
import com.facebook.ads.NativeAd
import com.jakewharton.rxbinding2.view.RxView
import com.paper.domain.useCase.DeletePaper
import com.paper.model.IPaper
import com.paper.model.IPaperRepoProvider
import com.paper.model.IPreferenceServiceProvider
import com.paper.model.ModelConst
import com.paper.model.event.IntProgressEvent
import com.paper.view.HorizontalCentricItemDecoration
import com.paper.view.gallery.*
import com.paper.view.gallery.GalleryViewModelBundle.Type.NativeAds
import com.paper.view.gallery.GalleryViewModelBundle.Type.Thumbnail
import com.tbruyelle.rxpermissions2.RxPermissions
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
    private val mSavedPaperIdSignal = PublishSubject.create<Long>().toSerialized()

    private val mProgressBar: AlertDialog by lazy {
        AlertDialog.Builder(this@PaperGalleryActivity)
            .setCancelable(false)
            .create()
    }

    // Paper thumbnail list view and controller.
    private val mGalleryView by lazy { findViewById<SnappingRecyclerView>(R.id.gallery_item_list) }
    private val mGalleryViewController by lazy {
        GalleryItemEpoxyController()
    }

    private val mPaperSnapshots = mutableListOf<IPaper>()

    private val mRepo by lazy { (application as IPaperRepoProvider).getPaperRepo() }
    private val mPrefs by lazy { (application as IPreferenceServiceProvider).preference }
    private val mPermissions by lazy { RxPermissions(this) }

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<IntProgressEvent>()
    // Error signal
    private val mErrorSignal = PublishSubject.create<Throwable>()

    // Disposables
    private val mDisposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_paper_gallery)

        // Paper thumbnail list view.
        mGalleryView.adapter = mGalleryViewController.adapter
        mGalleryView.addItemDecoration(HorizontalCentricItemDecoration())
        mGalleryView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(view: RecyclerView,
                                              newState: Int) {
                super.onScrollStateChanged(view, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = view.layoutManager as LinearLayoutManager
                    val first = layoutManager.findFirstCompletelyVisibleItemPosition()
                    val last = layoutManager.findLastCompletelyVisibleItemPosition()
                    val position = (first + last) / 2
                    val id = mGalleryViewController
                                 .getPaperFromAdapterPosition(position)
                                 ?.getId() ?: ModelConst.INVALID_ID

                    if (id != ModelConst.INVALID_ID) {
                        mSavedPaperIdSignal.onNext(id)
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        // Paper thumbnail list view.
        // Break the reference to the Epoxy controller's adapter so that the
        // context reference would be recycled.
        mGalleryView.adapter = null
    }

    override fun onResume() {
        super.onResume()

        val savedIDSrc = mPrefs
            .getLong(ModelConst.PREFS_BROWSE_PAPER_ID, ModelConst.INVALID_ID)
            .publish()

        // Click settings button
        mDisposables.add(
            onClickSettings()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Toast.makeText(this@PaperGalleryActivity,
                                   R.string.msg_under_construction,
                                   Toast.LENGTH_SHORT).show()
                })

        // Button of new paper.
        mDisposables.add(
            onClickNewPaper()
                .switchMap {
                    requestPermissions()
                        .observeOn(Schedulers.io())
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
                    mSavedPaperIdSignal.onNext(id)
                    openPaperInEditor(id)
                })
        // Button of delete paper.
        mDisposables.add(
            onClickDeletePaper()
                .flatMap { savedIDSrc }
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap { toDeletePaperID ->
                    if (toDeletePaperID != ModelConst.TEMP_ID &&
                        toDeletePaperID != ModelConst.INVALID_ID) {
                        requestPermissions()
                            .observeOn(Schedulers.io())
                            .switchMap {
                                DeletePaper(paperID = toDeletePaperID,
                                            paperRepo = mRepo,
                                            errorSignal = mErrorSignal)
                                    .toObservable()
                            }
                    } else {
                        Observable.never()
                    }
                }
                .subscribe())

        // Browse papers.
        mDisposables.add(
            mSavedPaperIdSignal
                .debounce(350, TimeUnit.MILLISECONDS)
                .flatMap { savedID ->
                    mPrefs.putLong(ModelConst.PREFS_BROWSE_PAPER_ID, savedID)
                        .toObservable()
                }
                .subscribe())

        // Load papers, create buttons, and ADs
        mDisposables.add(
            Observables
                .combineLatest(
                    getGalleryItems(),
                    savedIDSrc)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (items, savedID) ->
                    showGalleryItems(items)
                    scrollToPaper(savedID)
                })

        // Activate saved ID source
        mDisposables.add(savedIDSrc.connect())
    }

    override fun onPause() {
        super.onPause()

        mDisposables.clear()
    }

    private fun getGalleryItems(): Observable<List<GalleryViewModel>> {
        val defaultItem = listOf<GalleryViewModel>(CreatePaperViewModel(mOnClickNewPaperSignal))
        return Observable
            .merge(
                loadPapers()
                    // To bundle so that the following reducer knows what packs
                    // to update.
                    .map { items ->
                        GalleryViewModelBundle(type = Thumbnail,
                                               items = items)
                    },
                loadAds()
                    // To bundle so that the following reducer knows what packs
                    // to update.
                    .map { items ->
                        GalleryViewModelBundle(type = NativeAds,
                                               items = items)
                    })
            .scan(defaultItem) { old, newViewModelBundle ->
                // Create item
                val createItems = old.filter { item ->
                    item is CreatePaperViewModel
                }

                // Paper thumbnails
                val paperThumbItems = if (newViewModelBundle.type == Thumbnail) {
                    newViewModelBundle.items
                } else {
                    old.filter { item ->
                        item is PaperThumbViewModel
                    }
                }

                // ADs items (if paper exists)
                val adsItems = if (newViewModelBundle.type == NativeAds) {
                    newViewModelBundle.items
                } else {
                    old.filter { item ->
                        item is NativeAdsViewModel
                    }
                }

                // Recompose the item list
                val combined = mutableListOf<GalleryViewModel>()
                combined.addAll(createItems)
                combined.addAll(paperThumbItems)
                combined.addAll(adsItems)

                return@scan combined
            }
    }

    private fun loadPapers(): Observable<List<GalleryViewModel>> {
        return requestPermissions()
            .observeOn(Schedulers.io())
            .switchMap {
                // Note: Any database update will emit new result
                mRepo.getPapers(isSnapshot = true)
                    .doOnNext { papers ->
                        // Hold the paper snapshots.
                        mPaperSnapshots.clear()
                        mPaperSnapshots.addAll(papers)
                    }
                    // Transform model to view-model
                    .map { papers ->
                        papers.map { paper ->
                            PaperThumbViewModel(paper = paper,
                                                clickSignal = mOnClickPaperSignal)
                        } as List<GalleryViewModel>
                    }
            }
    }

    private val mFbNativeAdsSignal = PublishSubject.create<List<GalleryViewModel>>()

    private fun loadAds(): Observable<out List<GalleryViewModel>> {
        val ads = NativeAd(this, getString(R.string.facebook_native_ads_placement_id))
        ads.setAdListener(object : AdListener {

            override fun onAdClicked(ad: Ad) {
                mOnClickNativeAdsSignal.onNext(0)
            }

            override fun onError(ad: Ad, err: AdError) {
                println("${AppConst.TAG}: fail to load Facebook native ADs, err=$err")
            }

            override fun onAdLoaded(ad: Ad) {
                mFbNativeAdsSignal.onNext(listOf(
                    NativeAdsViewModel(ads = ads,
                                       clickSignal = mOnClickNativeAdsSignal)))
            }

            override fun onLoggingImpression(ad: Ad) {
                println("${AppConst.TAG}: logging Facebook native ADs impression")
            }
        })
        ads.loadAd()

        return mFbNativeAdsSignal
    }

    private fun showGalleryItems(items: List<GalleryViewModel>) {
        mGalleryViewController.setData(items)
    }

    private fun scrollToPaper(savedID: Long) {
        // FIXME: without the delay the behavior would be strange
        mGalleryView.post {
            val actualPosition = mGalleryViewController.getAdapterPositionByPaperID(savedID)
            if (actualPosition >= 0) {
                mGalleryView.smoothScrollToPosition(actualPosition)
            }
        }
    }

    private fun setDeleteButtonVisibility(visible: Boolean) {
        mBtnDelPaper.visibility = if (visible) View.VISIBLE else View.GONE
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

    private val mOnClickPaperSignal = PublishSubject.create<Long>()
    private val mOnClickNewPaperSignal = PublishSubject.create<Any>()
    private val mOnClickNativeAdsSignal = PublishSubject.create<Any>()

    private fun onClickPaper(): Observable<Long> {
        return mOnClickPaperSignal
            .debounce(150, TimeUnit.MILLISECONDS)
            .throttleFirst(1000, TimeUnit.MILLISECONDS)
    }

    private fun onClickNewPaper(): Observable<Any> {
        return Observable
            .merge(RxView.clicks(mBtnNewPaper),
                   mOnClickNewPaperSignal)
            .debounce(150, TimeUnit.MILLISECONDS)
            .throttleFirst(1000, TimeUnit.MILLISECONDS)
    }

    private fun onClickDeletePaper(): Observable<Any> {
        return RxView
            .clicks(mBtnDelPaper)
            .debounce(150, TimeUnit.MILLISECONDS)
            .throttleFirst(150, TimeUnit.MILLISECONDS)
    }

    private fun onClickSettings(): Observable<Any> {
        return RxView
            .clicks(mBtnSettings)
            .debounce(150, TimeUnit.MILLISECONDS)
            .throttleFirst(1000, TimeUnit.MILLISECONDS)
    }

    private fun openPaperInEditor(id: Long) {
        startActivity(Intent(this@PaperGalleryActivity,
                             PaperEditorActivity::class.java)
                          .putExtra(AppConst.PARAMS_PAPER_ID, id)
                          .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    private fun showError(err: Throwable) {
        if (BuildConfig.DEBUG) {
            Toast.makeText(this@PaperGalleryActivity,
                           err.toString(),
                           Toast.LENGTH_SHORT).show()
        }
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
