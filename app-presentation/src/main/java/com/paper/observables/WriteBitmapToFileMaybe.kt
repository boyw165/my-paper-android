// Copyright Jul 2018-present Paper
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

package com.paper.observables

import android.graphics.Bitmap
import io.reactivex.Maybe
import io.reactivex.MaybeObserver
import io.reactivex.android.MainThreadDisposable
import java.io.File
import java.io.FileOutputStream

/**
 * Maybe of writing a Bitmap to a file.
 */
class WriteBitmapToFileMaybe(private val inputBitmap: Bitmap,
                             private val outputFile: File,
                             private val recycleBitmap: Boolean)
    : Maybe<File>() {

    override fun subscribeActual(observer: MaybeObserver<in File>) {
        val disposable = Disposable()

        observer.onSubscribe(disposable)

        try {
            val parentDir = File(outputFile.parent)
            if (!parentDir.exists()) parentDir.mkdirs()

            // Write Bitmap to system public folder
            FileOutputStream(outputFile).use { out ->
                inputBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Recycle memory
            if (recycleBitmap) {
                inputBitmap.recycle()
            }

            if (!disposable.isDisposed) {
                observer.onSuccess(outputFile)
            }
        } catch (err: Throwable) {
            // Delete incomplete file
            if (outputFile.exists()) outputFile.delete()

            observer.onComplete()
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private class Disposable : MainThreadDisposable() {

        override fun onDispose() {
            // DO NOTHING
        }
    }
}
