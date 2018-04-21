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

package com.paper.model.observables

import com.paper.model.ModelConst
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.io.File
import java.io.FileWriter

/**
 * Write the [String] to a file piece by piece, where this operation is stoppable.
 * Be aware of that, unexpected stop will corrupt the content of file.
 */
class WriteStringToFileObservable(file: File,
                                  txt: String) : Observable<Int>() {

    private val mFile = file
    private val mStringWriteToFile = txt

    override fun subscribeActual(observer: Observer<in Int>) {
        val writer = FileWriter(mFile)

        val d = WriteDisposable()

        observer.onSubscribe(d)
        observer.onNext(0)

        // Return if it's disposed
        if (d.isDisposed) return

        if (!mFile.exists()) {
            mFile.createNewFile()
        }

        // Return if it's disposed
        if (d.isDisposed) return

        // TODO: Write file piece by piece
        writer.use {
            it.write(mStringWriteToFile)
        }
        observer.onNext(100)
        observer.onComplete()

        println("${ModelConst.TAG}: Successfully write txt to disk, thread=${Thread.currentThread()}")
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    internal class WriteDisposable : Disposable {

        @Volatile
        private var mDisposed: Boolean = false

        override fun isDisposed(): Boolean {
            return mDisposed
        }

        override fun dispose() {
            // TODO: Terminate the writing

            mDisposed = true
        }
    }
}
