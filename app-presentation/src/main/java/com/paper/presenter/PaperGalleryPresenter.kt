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

package com.paper.presenter

import android.Manifest
import com.paper.domain.DomainConst
import com.paper.domain.event.ProgressEvent
import com.paper.domain.ISharedPreferenceService
import com.paper.domain.useCase.DeletePaper
import com.paper.model.ModelConst
import com.paper.model.PaperModel
import com.paper.model.repository.IPaperRepo
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class PaperGalleryPresenter(private val mPermission: RxPermissions,
                            private val mRepo: IPaperRepo,
                            private val mPrefs: ISharedPreferenceService,
                            private val mUiScheduler: Scheduler,
                            private val mWorkerScheduler: Scheduler) {

    private var mView: PaperGalleryContract.View? = null
    private var mNavigator: PaperGalleryContract.Navigator? = null

    private val mPaperSnapshots = mutableListOf<PaperModel>()

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()
    // Error signal
    private val mErrorSignal = PublishSubject.create<Throwable>()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    fun bindView(view: PaperGalleryContract.View,
                 navigator: PaperGalleryContract.Navigator) {
        mView = view
        mNavigator = navigator

        // Exp menu button.
        mDisposablesOnCreate.add(
            view.onClickShowExpMenu()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(mUiScheduler)
                .subscribe {
                    view.showExpMenu()
                })

        // Exp menu.
        mDisposablesOnCreate.add(
            view.onClickExpMenu()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(mUiScheduler)
                .subscribe { id ->
                    navigator.navigateToExpById(id)
                })

        // Button of new paper.
        mDisposablesOnCreate.add(
            view.onClickNewPaper()
                .switchMap {
                    requestPermissions()
                }
                .observeOn(mUiScheduler)
                .subscribe {
                    mPrefs.putLong(DomainConst.PREFS_BROWSE_PAPER_ID, ModelConst.TEMP_ID)

                    navigator.navigateToPaperEditor(ModelConst.TEMP_ID)
                })
        // Button of existing paper.
        mDisposablesOnCreate.add(
            view.onClickPaper()
                .observeOn(mUiScheduler)
                .subscribe { id ->
                    navigator.navigateToPaperEditor(id)
                    view.hideProgressBar()
                })
        // Button of delete paper.
        mDisposablesOnCreate.add(
            view.onClickDeletePaper()
                .map {
                    val toDeletePaperID = mPrefs.getLong(DomainConst.PREFS_BROWSE_PAPER_ID,
                                                         ModelConst.INVALID_ID)
                    val toDeletePaperPosition = mPaperSnapshots.indexOfFirst { it.id == toDeletePaperID }
                    val newPaperPosition = toDeletePaperPosition - 1
                    val newPaperID = if (newPaperPosition >= 0) {
                        mPaperSnapshots[newPaperPosition].id
                    } else {
                        ModelConst.INVALID_ID
                    }

                    // Save new paper ID.
                    mPrefs.putLong(DomainConst.PREFS_BROWSE_PAPER_ID, newPaperID)

                    return@map toDeletePaperID
                }
                .switchMap { toDeletePaperID ->
                    requestPermissions()
                        .switchMap {
                            DeletePaper(paperID = toDeletePaperID,
                                        paperRepo = mRepo,
                                        errorSignal = mErrorSignal)
                                .toObservable()
                        }
                }
                .observeOn(mUiScheduler)
                .subscribe {
                    view.hideProgressBar()

//                    val id = mPrefs.getLong(DomainConst.PREFS_BROWSE_PAPER_ID, ModelConst.INVALID_ID)
//                    val position = papers.indexOfFirst { it.id == id }
//                    val currentPosition = mPrefs.getInt(PREFS_BROWSE_PAPER_POSITION, -1)
//                    if(currentPosition <= 0) {
//                        mPrefs.putInt(PREFS_BROWSE_PAPER_POSITION, 0)
//                    } else {
//                        mPrefs.putInt(PREFS_BROWSE_PAPER_POSITION, currentPosition - 1)
//                    }
                })
        // Browse papers.
        mDisposablesOnCreate.add(
            view.onBrowsePaper()
                .observeOn(mUiScheduler)
                .subscribe { id ->

                    mPrefs.putLong(DomainConst.PREFS_BROWSE_PAPER_ID, id)

                    if (id == ModelConst.INVALID_ID) {
                        view.setDeleteButtonVisibility(false)
                    } else {
                        view.setDeleteButtonVisibility(true)
                    }
                })
    }

    fun unbind() {
        mDisposablesOnCreate.clear()

        mView = null
        mNavigator = null
    }

    fun resume() {
        mDisposablesOnResume.add(
            requestPermissions()
                .switchMap {
                    // Note: Any database update will emit new result
                    mRepo.getPapers(isSnapshot = true)
                }
                .observeOn(mUiScheduler)
                .subscribe { papers ->
                    // Hold the paper snapshots.
                    mPaperSnapshots.clear()
                    mPaperSnapshots.addAll(papers)

                    mView?.let { view ->
                        val id = mPrefs.getLong(DomainConst.PREFS_BROWSE_PAPER_ID, ModelConst.INVALID_ID)
                        val position = papers.indexOfFirst { it.id == id }

                        if (position > 0 && position <= papers.size) {
                            view.setDeleteButtonVisibility(true)
                        } else {
                            view.setDeleteButtonVisibility(false)
                        }

                        view.showPaperThumbnails(papers)
                        view.showPaperThumbnailAt(position)
                    }
                })
    }

    fun pause() {
        mDisposablesOnResume.clear()

        // Force to hide the progress-bar.
        mView?.hideProgressBar()

        // Clear snapshots reference.
        mPaperSnapshots.clear()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun requestPermissions(): Observable<Boolean> {
        return mPermission
            .request(Manifest.permission.READ_EXTERNAL_STORAGE,
                     Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // TODO: Properly handle it, like showing permission explanation
            // TODO: page if it is denied.
            .filter { it }
    }
}
