package com.paper.editor

class PaperCanvasContract private constructor() {

    interface View {

        fun setInterceptTouchEvent(enabled: Boolean)

        fun addScrapView(data: Object): View
    }
}
