package com.paper.editor

import io.reactivex.Observable

class PaperEditorContract private constructor() {

    interface View {

        fun showProgressBar(progress: Int)

        fun hideProgressBar()

        fun showErrorAlert(error: Throwable)

        fun close()

        fun onClickCloseButton(): Observable<Any>

        fun onClickDrawButton(): Observable<Boolean>

        fun onClickMenu(): Observable<Any>
    }
}
