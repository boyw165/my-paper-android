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

package com.paper.domain.useCase

import android.graphics.Bitmap
import com.paper.domain.DomainConst
import com.paper.domain.ISharedPreferenceService
import com.paper.model.PaperModel
import com.paper.model.repository.IPaperRepo
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.SingleTransformer

class SavePaperToStore(paper: PaperModel,
                       paperRepo: IPaperRepo,
                       prefs: ISharedPreferenceService,
                       errorSignal: Observer<Throwable>? = null)
    : SingleTransformer<Bitmap, Boolean> {

    private val mPaper = paper
    private val mPaperRepo = paperRepo
    private val mPrefs = prefs

    private val mErrorSignal = errorSignal

    override fun apply(upstream: Single<Bitmap>): SingleSource<Boolean> {
        return upstream
            .flatMap { bmp ->
                mPaperRepo
                    .putBitmap(bmp)
                    .flatMap { bmpFile ->
                        mPaper.thumbnailPath = bmpFile
                        mPaper.thumbnailWidth = bmp.width
                        mPaper.thumbnailHeight = bmp.height

                        mPaperRepo.putPaperById(mPaper.id, mPaper)
                    }
                    .map { event ->
                        if (event.successful) {
                            mPrefs.putLong(DomainConst.PREFS_BROWSE_PAPER_ID, event.id)
                        }

                        return@map event.successful
                    }
            }
            .doOnError { err ->
                mErrorSignal?.onNext(err)
            }
            .onErrorReturnItem(false)
    }
}
