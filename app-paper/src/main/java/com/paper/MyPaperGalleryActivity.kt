// Copyright (c) 2016-present boyw165
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.paper

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.my.core.protocol.IProgressBarView
import com.my.reactive.uiModel.UiModel
import com.paper.shared.model.repository.PaperRepo
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class MyPaperGalleryActivity : AppCompatActivity(),
                               IProgressBarView {
    // View.
    private val mBtnNewPaper: TextView by lazy { findViewById(R.id.btn_new) as TextView }
    private val mBtnList: TextView by lazy { findViewById(R.id.btn_list) as TextView }
    private val mText: TextView by lazy { findViewById(R.id.text_message) as TextView }
    private val mProgressBar: AlertDialog by lazy {
        AlertDialog.Builder(this@MyPaperGalleryActivity)
            .setCancelable(false)
            .create()
    }

    // Permission.
    private val mRxPermissions: RxPermissions by lazy {
        RxPermissions(this@MyPaperGalleryActivity)
    }

    // Repo.
    // TODO: Inject the repo.
    private val mPaperRepo: PaperRepo by lazy {
        PaperRepo(packageName,
                  contentResolver,
                  externalCacheDir,
                  Schedulers.io())
    }

    private val mDisposables1: CompositeDisposable = CompositeDisposable()
    private var mDisposables2: CompositeDisposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_paper_gallery)

        mDisposables1.addAll(
            // Show how many papers in the database...
            RxView.clicks(mBtnList)
                .debounce(150, TimeUnit.MILLISECONDS)
                .flatMap {
                    mPaperRepo
                        .getPaperSnapshotList()
                        // TODO: Refactoring.
                        .compose { upstream ->
                            upstream
                                .map { anything -> UiModel.succeed(anything) }
                                .startWithArray(UiModel.inProgress(null))
                        }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm ->
                    when {
                        vm.isSuccessful -> {
                            hideProgressBar()

                            val papers = vm.bundle
                            mText.text = String.format(
                                Locale.ENGLISH,
                                "There are %d papers in your gallery",
                                papers.size)

                        }
                        vm.isInProgress -> {
                            showProgressBar()
                        }
                        else -> {
                            hideProgressBar()
                            showError(vm.error)
                        }
                    }
                },
            // Create a new paper...
            RxView.clicks(mBtnNewPaper)
                .debounce(150, TimeUnit.MILLISECONDS)
                // Grant permissions.
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap {
                    mRxPermissions
                        .request(Manifest.permission.READ_EXTERNAL_STORAGE,
                                 Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                // TODO: MVP's Presenter's transformer.
                // Insert a temp paper to the repository.
                .flatMap { granted ->
                    if (granted) {
                        mPaperRepo
                            // TODO: Presenter's responsibility to assign the caption.
                            .newTempPaper("New Paper")
                            // TODO: Refactoring.
                            .compose { upstream ->
                                upstream
                                    .map { anything -> UiModel.succeed(anything) }
                                    .startWithArray(UiModel.inProgress(null))
                            }
                    } else {
                        throw RuntimeException("Permissions are not granted.")
                    }
                }
                .onErrorReturn { err -> UiModel.failed(err) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm ->
                    when {
                        vm.isInProgress -> {
                            showProgressBar()
                        }
                        vm.isSuccessful -> {
                            hideProgressBar()
                            navigateToPaperEditor()
                        }
                        else -> {
                            hideProgressBar()
                            showError(vm.error)
                        }
                    }
                })
    }

    override fun onDestroy() {
        super.onDestroy()

        // Force to hide the progress-bar.
        hideProgressBar()

        mDisposables1.clear()
    }

    override fun onResume() {
        super.onResume()

        mDisposables2 = CompositeDisposable()
        // TODO: Refresh the list with difference comparison.
    }

    override fun onPause() {
        super.onPause()

        mDisposables2?.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun showProgressBar() {
        mProgressBar.setMessage(getString(R.string.loading))
        mProgressBar.show()
    }

    override fun showProgressBar(msg: String) {
        mProgressBar.setMessage(msg)
        mProgressBar.show()
    }

    override fun hideProgressBar() {
        mProgressBar.hide()
    }

    override fun updateProgress(progress: Int) {
        TODO("not implemented")
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun showError(err: Throwable) {
        Toast.makeText(this@MyPaperGalleryActivity,
                       err.toString(),
                       Toast.LENGTH_SHORT).show()
    }

    // TODO: MVP's View method.
    private fun navigateToPaperEditor() {
        startActivity(Intent(this, PaperEditorActivity::class.java))
    }
}
