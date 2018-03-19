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
import com.paper.shared.model.repository.PaperRepo
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class PaperGalleryPresenter(private val mPermission: RxPermissions,
                            private val mRepo: PaperRepo,
                            private val mUiScheduler: Scheduler,
                            private val mWorkerScheduler: Scheduler) {

    private var mView: PaperGalleryContract.View? = null
    private var mNavigator: PaperGalleryContract.Navigator? = null

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposables = CompositeDisposable()

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
                    mPermission
                        .request(Manifest.permission.READ_EXTERNAL_STORAGE,
                                 Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .filter { it }
                        .switchMap {
                            mRepo.newTempPaper("")
                                .toObservable()
                                .map { Pair(it.id, ProgressEvent.stop(100)) }
                                .startWith(Pair(PaperConsts.INVALID_ID, ProgressEvent.start()))
                        }
                }
                .observeOn(mUiScheduler)
                .subscribe { (id, event) ->
                    when {
                        event.justStart -> view.showProgressBar()
                        event.justStop -> {
                            navigator.navigateToPaperEditor(id)
                            view.hideProgressBar()
                        }
                    }
                })

        //        // Create a new paper...
        //        mDisposablesOnCreate.add(
        //            RxView.clicks(mBtnNewPaper)
        //                .debounce(150, TimeUnit.MILLISECONDS)
        //                // Grant permissions.
        //                .observeOn(mUiScheduler)
        //                .flatMap {
        //                    mRxPermissions
        //                        .request(Manifest.permission.READ_EXTERNAL_STORAGE,
        //                                 Manifest.permission.WRITE_EXTERNAL_STORAGE)
        //                }
        //                // TODO: MVP's Presenter's transformer.
        //                // Insert a temp paper to the repository.
        //                .flatMap { granted ->
        //                    if (granted) {
        //                        mPaperRepo
        //                            // TODO: Presenter's responsibility to assign the caption.
        //                            .newTempPaper("New Paper")
        //                            // TODO: Refactoring.
        //                            .compose { upstream ->
        //                                upstream
        //                                    .map { anything -> UiModel.succeed(anything) }
        //                                    .startWithArray(UiModel.inProgress(null))
        //                            }
        //                    } else {
        //                        throw RuntimeException("Permissions are not granted.")
        //                    }
        //                }
        //                .onErrorReturn { err -> UiModel.failed(err) }
        //                .observeOn(mUiScheduler)
        //                .subscribe { vm ->
        //                    when {
        //                        vm.isInProgress -> {
        //                            showProgressBar()
        //                        }
        //                        vm.isSuccessful -> {
        //                            hideProgressBar()
        //                            navigateToPaperEditor()
        //                        }
        //                        else -> {
        //                            hideProgressBar()
        //                            showError(vm.error)
        //                        }
        //                    }
        //                })
    }

    fun unbindViewOnDestroy() {
        mDisposablesOnCreate.clear()
        mDisposables.clear()

        mView = null
        mNavigator = null
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////
}
