// Copyright Feb 2018-present boyw165@gmail.com
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

package com.paper.editor.view.editingPanel

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.widget.Toast
import com.bumptech.glide.Glide
import com.paper.R
import com.paper.editor.view.canvas.ViewPortIndicatorView
import com.paper.editor.widget.editingPanel.PaperEditPanelWidget
import com.paper.shared.model.Rect
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

/**
 * The editing panel for the paper editor. See [R.layout.view_paper_edit_panel] for layout.
 */
class PaperEditPanelView : ConstraintLayout,
                           IPaperEditPanelView {

    private var mOneDp = 0f

    // View port sub-view
    private val mViewPortIndicatorView by lazy { findViewById<ViewPortIndicatorView>(R.id.view_port_indicator) }

    // Tools sub-view
    private val mToolListView by lazy { findViewById<RecyclerView>(R.id.list_tools) }
    private val mToolListViewController by lazy {
        ToolListEpoxyController(mWidget,
                                Glide.with(context))
    }
    // Tool signal
    private val mColorTicket = PublishSubject.create<Int>()
    private val mEditingTool = PublishSubject.create<Int>()

    // The business login/view-model
    private val mWidget by lazy {
        PaperEditPanelWidget(
            AndroidSchedulers.mainThread(),
            Schedulers.io())
    }
    private val mDisposables = CompositeDisposable()

    constructor(context: Context) : this(context, null)
    constructor(context: Context,
                attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context, R.layout.view_paper_edit_panel, this)

        mOneDp = context.resources.getDimension(R.dimen.one_dp)

        mToolListView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        mToolListView.adapter = mToolListViewController.adapter
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Editing tool
        mDisposables.add(
            mWidget.onUpdateEditingToolList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    // Update view
                    mToolListViewController.setData(event)

                    // Notify observer
                    val toolID = event.toolIDs.indexOf(event.usingIndex)
                    mEditingTool.onNext(toolID)
                })
        mDisposables.add(
            mWidget.onChooseUnsupportedTool()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Toast.makeText(context, R.string.msg_under_construction, Toast.LENGTH_SHORT).show()
                })

        mWidget.handleStart()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        mDisposables.clear()

        mWidget.handleStop()
    }

    override fun setCanvasAndViewPort(canvas: Rect,
                                      viewPort: Rect) {
        mViewPortIndicatorView.setCanvasAndViewPort(canvas, viewPort)
    }

    // Tool ///////////////////////////////////////////////////////////////////

    override fun onChooseColorTicket(): Observable<Int> {
        return mColorTicket
    }

    override fun onChooseEditingTool(): Observable<Int> {
        return mEditingTool
    }

    // Other //////////////////////////////////////////////////////////////////

    private fun ensureNoLeakingSubscriptions() {
        if (mDisposables.size() > 0) {
            throw IllegalStateException("Already bind to a widget")
        }
    }
}
