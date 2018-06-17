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

package com.paper.view.gallery

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.paper.R
import com.paper.model.IPreferenceService
import com.paper.model.IPreferenceServiceProvider
import com.paper.model.ModelConst
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class PaperSizeDialogFragment : DialogFragment() {

    private val mDialogView by lazy {
        // Pass null as the parent view because its going in the dialog layout
        activity!!.layoutInflater.inflate(R.layout.dialog_choose_paper_size, null)
    }

    /**
     * The list of set of item view, paper width, and paper height.
     */
    private val mSizeOption by lazy {
        listOf(Triple(mDialogView.findViewById<View>(R.id.item_a_four_landscape),
                      ModelConst.SIZE_OF_A_FOUR_LANDSCAPE.first,
                      ModelConst.SIZE_OF_A_FOUR_LANDSCAPE.second),
               Triple(mDialogView.findViewById<View>(R.id.item_a_four_portrait),
                      ModelConst.SIZE_OF_A_FOUR_PORTRAIT.first,
                      ModelConst.SIZE_OF_A_FOUR_PORTRAIT.second),
               Triple(mDialogView.findViewById<View>(R.id.item_a_four_square),
                      ModelConst.SIZE_OF_A_FOUR_SQUARE.first,
                      ModelConst.SIZE_OF_A_FOUR_SQUARE.second))
    }
    private var mSizeOptionSelectionIndex = 0

    private val mPrefs by lazy { (activity!!.application as IPreferenceServiceProvider).preference }
    private val mPrefsKeyOfPaperSizeOption = "saved_paper_size_option_index"

    private val mDisposables = CompositeDisposable()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Load saved preference
        mDisposables.add(
            mPrefs.getInt(mPrefsKeyOfPaperSizeOption, 0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { i ->
                    mSizeOptionSelectionIndex = if (i < 0 || i >= mSizeOption.size) 0 else i

                    updateItemViews(mSizeOptionSelectionIndex)
                })

        // Click listener
        mSizeOption.forEachIndexed { i, (view, _, _) ->
            mDisposables.add(
                RxView.clicks(view)
                    .switchMap {
                        mSizeOptionSelectionIndex = i
                        updateItemViews(mSizeOptionSelectionIndex)

                        Single
                            .fromCallable {
                                mPrefs.putInt(mPrefsKeyOfPaperSizeOption, i)
                            }
                            .subscribeOn(Schedulers.io())
                            .toObservable()
                    }
                    .subscribe())
        }

        val dialogBuilder = AlertDialog.Builder(context!!)
            .setTitle(R.string.title_of_choose_paper_size)
            .setView(mDialogView)
//            .setView(R.layout.dialog_choose_paper_size)
            .setPositiveButton(getString(R.string.yes)) { dialogInterface, _ ->
                val (_, w, h) = mSizeOption[mSizeOptionSelectionIndex]
                mOnClickOkListener?.onClickOk(w, h)

                dialogInterface.dismiss()
            }
            .setNegativeButton(getString(R.string.no)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setCancelable(true)

        return dialogBuilder.create()
    }

    override fun onDestroy() {
        super.onDestroy()

        mDisposables.clear()
    }

    private fun updateItemViews(selectionIndex: Int) {
        mSizeOption.forEachIndexed { i, (v, _, _) ->
            val checkView = v.findViewById<View>(R.id.option_checkbox)
            checkView.visibility = if (i == selectionIndex) View.VISIBLE else View.INVISIBLE
        }
    }

    private var mOnClickOkListener: OnClickOkListener? = null

    fun setOnClickOkListener(listener: OnClickOkListener?) {
        mOnClickOkListener = listener
    }

    // Listener ///////////////////////////////////////////////////////////////

    interface OnClickOkListener {

        fun onClickOk(paperWidth: Float, paperHeight: Float)
    }
}
