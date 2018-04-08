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

        mColorTicketsView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        mColorTicketsView.adapter = mColorTicketsViewController.adapter
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Edit tool, e.g. eraser, pen, scissor, ...
        mDisposables.add(
            mWidget.onUpdateEditToolList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    // Update view
                    mToolListViewController.setData(event)

                    // Notify observer
                    val toolID = event.toolIDs[event.usingIndex]
                    mSelectedEditTool.onNext(toolID)
                })
        mDisposables.add(
            mWidget.onChooseUnsupportedEditTool()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Toast.makeText(context, R.string.msg_under_construction, Toast.LENGTH_SHORT).show()
                })

        // Color tickets
        mDisposables.add(
            mWidget.onUpdateColorTicketList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    // Update view
                    mColorTicketsViewController.setData(event)

                    // Notify observer
                    val color = event.colorTickets[event.usingIndex]
                    mSelectedColorTicket.onNext(color)
                })

        mWidget.handleStart()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        mDisposables.clear()

        mWidget.handleStop()
    }

    // View port indicator ////////////////////////////////////////////////////

    // View port sub-view
    private val mViewPortIndicatorView by lazy { findViewById<ViewPortIndicatorView>(R.id.view_port_indicator) }

    override fun setCanvasAndViewPort(canvas: Rect,
                                      viewPort: Rect) {
        mViewPortIndicatorView.setCanvasAndViewPort(canvas, viewPort)
    }

    // Edit tool //////////////////////////////////////////////////////////////

    // Tools sub-view
    private val mToolListView by lazy { findViewById<RecyclerView>(R.id.list_tools) }
    private val mToolListViewController by lazy {
        ToolListEpoxyController(mWidget = mWidget,
                                mImgLoader = Glide.with(context))
    }
    private val mSelectedEditTool = PublishSubject.create<Int>()

    override fun onChooseEditTool(): Observable<Int> {
        return mSelectedEditTool
    }

    // Color & stroke width ///////////////////////////////////////////////////

    // Color & stroke width
    private val mColorTicketsView by lazy { findViewById<RecyclerView>(R.id.list_color_tickets) }
    private val mColorTicketsViewController by lazy {
        ColorTicketListEpoxyController(mWidget = mWidget,
                                       mImgLoader = Glide.with(context))
    }
    private val mSelectedColorTicket = PublishSubject.create<Int>()
    private val mStrokeWidthView by lazy { findViewById<RecyclerView>(R.id.list_color_tickets) }

    override fun onChooseColorTicket(): Observable<Int> {
        return mSelectedColorTicket
    }

    // Other //////////////////////////////////////////////////////////////////

    private fun ensureNoLeakingSubscriptions() {
        if (mDisposables.size() > 0) {
            throw IllegalStateException("Already bind to a widget")
        }
    }
}
