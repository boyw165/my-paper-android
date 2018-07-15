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

import android.media.MediaScannerConnection
import android.net.Uri
import com.paper.services.IContextProvider
import io.reactivex.Maybe
import io.reactivex.MaybeObserver
import io.reactivex.android.MainThreadDisposable
import java.io.File

/**
 * Maybe of adding a file to system media store.
 */
class AddFileToMediaStoreMaybe(private val contextProvider: IContextProvider,
                               private val file: File)
    : Maybe<Uri>() {

    override fun subscribeActual(observer: MaybeObserver<in Uri>) {
        val listenerDisposable = Disposable(observer)

        observer.onSubscribe(listenerDisposable)

        contextProvider.context?.let { context ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null, listenerDisposable)
        } ?: observer.onComplete()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private class Disposable internal constructor(
        internal val observer: MaybeObserver<in Uri>)
        : MainThreadDisposable(),
          MediaScannerConnection.OnScanCompletedListener {

        override fun onDispose() {
            // DO NOTHING
        }

        override fun onScanCompleted(path: String, uri: Uri) {
            if (isDisposed) return

            observer.onSuccess(uri)
        }
    }
}
