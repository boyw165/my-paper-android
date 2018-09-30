// Copyright Sep 2018-present TAI-CHUN, WANG
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

package com.paper.model.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.paper.model.ISchedulers
import com.paper.model.ModelConst
import io.reactivex.Single
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class BitmapFileRepo(private val bmpCacheDir: File,
                     private val schedulers: ISchedulers)
    : IBitmapRepository {

    private val bitmapJournal = HashMap<Int, File>()

    override fun putBitmap(key: Int, bmp: Bitmap): Single<File> {
        return Single
            .fromCallable {
                if (!bmpCacheDir.exists()) {
                    bmpCacheDir.mkdir()
                }

                // TODO: Use LruCache?
                val bmpFile = File(bmpCacheDir, "$key.png")

                FileOutputStream(bmpFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // TODO: Save the journal file somewhere
                bitmapJournal[key] = bmpFile

                println("${ModelConst.TAG}: put Bitmap to cache (key=$key, file=$bmpFile")

                return@fromCallable bmpFile
            }
            .subscribeOn(schedulers.db())
    }

    override fun getBitmap(key: Int): Single<Bitmap> {
        val file = bitmapJournal[key] ?: File(bmpCacheDir, "$key.png")

        return Single
                .fromCallable {
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        throw FileNotFoundException("cannot find the Bitmap")
                    }
                }
                .subscribeOn(schedulers.db())
    }
}
