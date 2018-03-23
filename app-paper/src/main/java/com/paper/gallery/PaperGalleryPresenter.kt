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

package com.paper.gallery

import android.Manifest
import com.paper.event.ProgressEvent
import com.paper.shared.model.PaperConsts
import com.paper.shared.model.repository.protocol.IPaperModelRepo
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class PaperGalleryPresenter(private val mPermission: RxPermissions,
                            private val mRepo: IPaperModelRepo,
                            private val mUiScheduler: Scheduler,
                            private val mWorkerScheduler: Scheduler) {

    private var mView: PaperGalleryContract.View? = null
    private var mNavigator: PaperGalleryContract.Navigator? = null

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    fun bindViewOnCreate(view: PaperGalleryContract.View,
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
//                        .switchMap {
//                            mRepo.newTempPaper("")
//                                .toObservable()
//                                .map { Pair(it.id, ProgressEvent.stop(100)) }
//                                .startWith(Pair(PaperConsts.TEMP_ID, ProgressEvent.start()))
//                        }
                }
                .observeOn(mUiScheduler)
                .subscribe {
                    navigator.navigateToPaperEditor(PaperConsts.TEMP_ID)
                    view.hideProgressBar()
                })
        // Button of existing paper.
        mDisposablesOnCreate.add(
            view.onClickPaper()
                .observeOn(mUiScheduler)
                .subscribe { id ->
                    navigator.navigateToPaperEditor(id)
                    view.hideProgressBar()
                })
        // Button of delete all papers.
        mDisposablesOnCreate.add(
            view.onClickDeleteAllPapers()
                .switchMap {
                    requestPermissions()
                        .switchMap {
                            mRepo.deleteAllPapers()
                        }
//                        .switchMap {
//                            mRepo.newTempPaper("")
//                                .toObservable()
//                                .map { Pair(it.id, ProgressEvent.stop(100)) }
//                                .startWith(Pair(PaperConsts.TEMP_ID, ProgressEvent.start()))
//                        }
                }
                .observeOn(mUiScheduler)
                .subscribe {
                    view.hideProgressBar()
                })
    }

    fun unbindViewOnDestroy() {
        mDisposablesOnCreate.clear()

        mView = null
        mNavigator = null
    }

    fun onResume() {
        mDisposablesOnResume.add(
            requestPermissions()
                .switchMap {
                    mRepo.getPaperSnapshotList()
                }
                .observeOn(mUiScheduler)
                .subscribe { papers ->
                    mView?.showPaperThumbnails(papers)
                })
    }

    fun onPause() {
        mDisposablesOnResume.clear()

        // Force to hide the progress-bar.
        mView?.hideProgressBar()
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
