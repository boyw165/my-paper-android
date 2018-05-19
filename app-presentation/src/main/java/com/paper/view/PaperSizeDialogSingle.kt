// Copyright May 2018-present Paper
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

package com.paper.view

import android.support.v4.app.FragmentManager
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.MainThreadDisposable

/**
 * Encapsulate the dialog fragment to a [Single] that emits paper width and height.
 */
class PaperSizeDialogSingle(dialog: PaperSizeDialogFragment,
                            fragmentManager: FragmentManager)
    : Single<Pair<Float, Float>>() {

    private val mDialog = dialog
    private val mFragmentManager = fragmentManager

    override fun subscribeActual(observer: SingleObserver<in Pair<Float, Float>>) {
        val d = AutoDismissDisposable(mDialog, observer)
        observer.onSubscribe(d)

        mDialog.setOnClickOkListener(d)
        mDialog.show(mFragmentManager, PaperSizeDialogFragment::class.java.simpleName)
    }

    class AutoDismissDisposable(private val dialog: PaperSizeDialogFragment,
                                private val observer: SingleObserver<in Pair<Float, Float>>)
        : MainThreadDisposable(),
          PaperSizeDialogFragment.OnClickOkListener {

        override fun onDispose() {
            dialog.dismiss()
        }

        override fun onClickOk(paperWidth: Float,
                               paperHeight: Float) {
            observer.onSuccess(Pair(paperWidth, paperHeight))
        }
    }
}
