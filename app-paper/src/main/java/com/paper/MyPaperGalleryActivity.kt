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
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.cardinalblue.lib.doodle.model.UiModel
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxPopupMenu
import com.paper.shared.model.repository.PaperRepo
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MyPaperGalleryActivity : AppCompatActivity() {

    // View.
    private val mBtnExp: View by lazy { findViewById<View>(R.id.btn_other_exp) }
    private val mBtnExpMenu: PopupMenu by lazy {
        val m = PopupMenu(this@MyPaperGalleryActivity, mBtnExp)
        m.inflate(R.menu.menu_exp)
        m
    }
    private val mBtnNewPaper: TextView by lazy { findViewById<TextView>(R.id.btn_new) }
//    private val mBtnList: TextView by lazy { findViewById<TextView>(R.id.btn_list) }
//    private val mText: TextView by lazy { findViewById<TextView>(R.id.text_message) }
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

    private val mDisposablesOnCreate: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_paper_gallery)

        // Show how many papers in the database...
//        mDisposablesOnCreate.add(
//            RxView.clicks(mBtnList)
//                .debounce(150, TimeUnit.MILLISECONDS)
//                .flatMap {
//                    mPaperRepo
//                        .getPaperSnapshotList()
//                        // TODO: Refactoring.
//                        .compose { upstream ->
//                            upstream
//                                .map { anything -> UiModel.succeed(anything) }
//                                .startWithArray(UiModel.inProgress(null))
//                        }
//                }
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { vm ->
//                    when {
//                        vm.isSuccessful -> {
//                            hideProgressBar()
//
//                            val papers = vm.bundle
//                            mText.text = String.format(
//                                Locale.ENGLISH,
//                                "There are %d papers in your gallery",
//                                papers.size)
//
//                        }
//                        vm.isInProgress -> {
//                            showProgressBar()
//                        }
//                        else -> {
//                            hideProgressBar()
//                            showError(vm.error)
//                        }
//                    }
//                })

        // Exp menu button.
        mDisposablesOnCreate.add(
            RxView.clicks(mBtnExp)
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    mBtnExpMenu.show()
                })

        // Exp menu.
        mDisposablesOnCreate.add(
            RxPopupMenu.itemClicks(mBtnExpMenu)
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { item ->
                    when (item.itemId) {
                        R.id.rx_cancel_ex -> {
                            startActivity(Intent(
                                this@MyPaperGalleryActivity,
                                ExampleOfRxCancelActivity::class.java))
                        }
                        R.id.navigation_ex -> {
                            startActivity(Intent(
                                this@MyPaperGalleryActivity,
                                ExampleOfFlow1Page1Activity::class.java))
                        }
                        R.id.convex_hull_ex -> {
                            startActivity(Intent(
                                this@MyPaperGalleryActivity,
                                ExampleOfConvexHullActivity::class.java))
                        }
                    }
                })

        // Create a new paper...
        mDisposablesOnCreate.add(
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

        mDisposablesOnCreate.clear()
    }

    fun showProgressBar() {
        mProgressBar.setMessage(getString(R.string.loading))
        mProgressBar.show()
    }

    fun showProgressBar(msg: String) {
        mProgressBar.setMessage(msg)
        mProgressBar.show()
    }

    fun hideProgressBar() {
        mProgressBar.hide()
    }

    fun updateProgress(progress: Int) {
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
