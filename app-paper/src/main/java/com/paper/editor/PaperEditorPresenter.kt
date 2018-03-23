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

package com.paper.editor

import android.graphics.Bitmap
import android.os.Environment
import com.paper.event.ProgressEvent
import com.paper.protocol.IPresenter
import com.paper.shared.model.PaperConsts
import com.paper.shared.model.repository.protocol.IPaperModelRepo
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PaperEditorPresenter(private val mPaperController: PaperController,
                           private val mPaperRepo: IPaperModelRepo,
                           private val mUiScheduler: Scheduler,
                           private val mWorkerScheduler: Scheduler)
    : IPresenter<PaperEditorContract.View> {

    private var mPaperId = PaperConsts.TEMP_ID

    // Editor view.
    private var mView: PaperEditorContract.View? = null

    // Progress signal.
    private val mUpdateProgressSignal = PublishSubject.create<ProgressEvent>()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposables = CompositeDisposable()

    override fun bindViewOnCreate(view: PaperEditorContract.View) {
        mView = view

        // Close button.
        mDisposablesOnCreate.add(
            mView!!.onClickCloseButton()
                .debounce(150, TimeUnit.MILLISECONDS)
                .take(1)
                .observeOn(mWorkerScheduler)
                .switchMap {
                    commitPaperById(mPaperId)
                    return@switchMap Observable.just(0)
                }
                .observeOn(mUiScheduler)
                .subscribe {
                    mView?.close()
                })

        // Draw toggle button.
        mDisposablesOnCreate.add(
            mView!!.onClickDrawButton()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(mUiScheduler)
                .subscribe { checked ->
                    // TODO: Configure paper controller to drawing mode.
                })
    }

    override fun unbindViewOnDestroy() {
        // Paper controller.
        mPaperController.unbindView()

        mDisposablesOnCreate.clear()
        mDisposables.clear()

        mView = null
    }

    override fun onResume() {
        // DO NOTHING
    }

    override fun onPause() {
        // DO NOTHING
    }

    fun loadPaperById(id: Long) {
        mPaperId = id

        // Inflate paper model.
        mDisposables.add(
            mPaperRepo
                .getPaperById(id)
                .toObservable()
                // TODO: Support progress event.
                .observeOn(mUiScheduler)
                .subscribe { paper ->
                    // Inflate model where it would create the corresponding
                    // scrap-controllers.
                    mPaperController.initScrapControllers(paper)

                    // Bind view.
                    mView?.getCanvasView()?.let { canvasView ->
                        mPaperController.bindView(canvasView)
                    }
                })
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun commitPaperById(id: Long) {
        val paper = mPaperController.getPaper()

        mView?.getCanvasView()?.let { canvasView ->
            val dir = File("${Environment.getExternalStorageDirectory()}/paper")
            if (!dir.exists()) {
                dir.mkdir()
            }

            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val bmp = canvasView.takeSnapshot()
            val bmpFile = File("${Environment.getExternalStorageDirectory()}/paper",
                               "$ts.jpg")

            FileOutputStream(bmpFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            paper.thumbnailWidth = bmp.width
            paper.thumbnailHeight = bmp.height
            paper.thumbnailPath = bmpFile.canonicalPath
        }

        mPaperRepo.putPaperById(id, paper)
    }
}
