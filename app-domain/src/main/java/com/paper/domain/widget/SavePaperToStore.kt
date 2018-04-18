// Copyright Apr 2018-present boyw165@gmail.com
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

package com.paper.domain.widget

import android.graphics.Bitmap
import android.os.Environment
import com.paper.domain.widget.canvas.IPaperWidget
import com.paper.model.repository.IPaperRepo
import io.reactivex.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SavePaperToStore(paperWidget: IPaperWidget,
                       paperRepo: IPaperRepo,
                       ioScheduler: Scheduler)
    : SingleTransformer<Bitmap, Boolean> {

    private val mPaperWidget = paperWidget
    private val mPaperRepo = paperRepo

    private val mIoScheduler = ioScheduler

    override fun apply(upstream: Single<Bitmap>): SingleSource<Boolean> {
        return upstream
            .flatMap { bmp ->
                applyImpl(bmp)
            }
    }

    private fun applyImpl(bmp: Bitmap): Single<Boolean> {
        return Single
            .fromCallable {
                val dir = File("${Environment.getExternalStorageDirectory()}/paper")
                if (!dir.exists()) {
                    dir.mkdir()
                }

                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
                val bmpFile = File("${Environment.getExternalStorageDirectory()}/paper",
                                   "$ts.jpg")

                FileOutputStream(bmpFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }

                mPaperWidget.handleSetThumbnail(bmpFile, bmp.width, bmp.height)

                return@fromCallable mPaperWidget.getPaper()
            }
            .subscribeOn(mIoScheduler)
            .flatMap { paper ->
                mPaperRepo.putPaperById(paper.id, paper)
            }
    }
}
