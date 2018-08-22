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

package com.paper.view.editPanel

import android.content.Context
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.widget.Toast
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding2.view.RxView
import com.paper.R
import com.paper.domain.event.CanvasEvent
import com.paper.domain.event.UpdatePenSizeEvent
import com.paper.domain.vm.PaperMenuWidget
import com.paper.model.ModelConst
import com.paper.model.Rect
import com.paper.observables.SeekBarChangeObservable
import com.paper.view.IWidgetView
import com.paper.view.canvas.IPaperContext
import com.paper.view.canvas.ViewPortIndicatorView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

/**
 * The editing panel for the paper editor. See [R.layout.view_paper_edit_panel] for layout.
 */
class PaperEditPanelView : ConstraintLayout,
                           IWidgetView<PaperMenuWidget> {

    private val mDisposables = CompositeDisposable()

    constructor(context: Context) : this(context, null)
    constructor(context: Context,
                attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context, R.layout.view_paper_edit_panel, this)

        mToolListView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        mToolListView.adapter = mToolListViewController.adapter

        mColorTicketsView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        mColorTicketsView.adapter = mColorTicketsViewController.adapter
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        if (isInEditMode) {
            val oneDp = resources.getDimension(R.dimen.one_dp)
            val height = (200f * oneDp).toInt()
            val heightMode = MeasureSpec.getMode(heightSpec)
            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(height, heightMode))
        } else {
            super.onMeasure(widthSpec, heightSpec)
        }
    }

    private lateinit var mWidget: PaperMenuWidget

    override fun bindWidget(widget: PaperMenuWidget) {
        ensureMainThread()
        ensureNoLeakingSubscription()

        mWidget = widget

        // For item click event handling
        mToolListViewController.setWidget(widget)
        mColorTicketsViewController.setWidget(widget)

        // Edit tool, e.g. eraser, pen, scissor, ...
        mDisposables.add(
            widget.onUpdateEditToolList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    // Update view
                    mToolListViewController.setData(event)
                })
        mDisposables.add(
            widget.onChooseUnsupportedEditTool()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Toast.makeText(context, R.string.msg_under_construction, Toast.LENGTH_SHORT).show()
                })

        // Pen colors
        mDisposables.add(
            widget.onUpdatePenColorList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    val color = event.colorTickets[event.usingIndex]
                    mPenSizeView.showPenColor(color)
                    // Bypass color to external component
                    mPenColorSignal.onNext(color)

                    // Update view
                    mColorTicketsViewController.setData(event)
                })

        // Pen size
//        mDisposables.add(
//            widget.setPenSize(
//                SeekBarChangeObservable(mPenSizeView)
//                    .filter { it.fromUser }
//                    .map { event ->
//                        val t = event.progress.toFloat() / 100f
//                        val penSize = (1f - t) * ModelConst.MIN_PEN_SIZE + t * ModelConst.MAX_PEN_SIZE
//
//                        UpdatePenSizeEvent(lifecycle = event.lifecycle,
//                                           size = penSize)
//                    }
//                    .doOnNext {
//                        // Bypass event to external component, e.g. another
//                        // size previewer
//                        mPenSizeSignal.onNext(it)
//                    }))
        // Pen size initialization
        mDisposables.add(
            widget.onUpdatePenSize()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { penSize ->
                    // t = (x - x0) / (x1 - x0)
                    val t = (penSize - ModelConst.MIN_PEN_SIZE) / (ModelConst.MAX_PEN_SIZE - ModelConst.MIN_PEN_SIZE)
                    val progress = (100f * t).toInt()

                    mPenSizeView.progress = progress
                })
    }

    override fun unbindWidget() {
        mDisposables.clear()
    }

    private fun ensureMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalThreadStateException("Not in MAIN thread")
        }
    }

    private fun ensureNoLeakingSubscription() {
        if (mDisposables.size() > 0) throw IllegalStateException(
            "Already start to a widget")
    }

    // Canvas context /////////////////////////////////////////////////////////

    private var mPaperContext: IPaperContext? = null

    fun setCanvasContext(paperContext: IPaperContext) {
        mPaperContext = paperContext
    }

    // View port //////////////////////////////////////////////////////////////

    private val mViewPortIndicatorView by lazy { findViewById<ViewPortIndicatorView>(R.id.view_port_indicator) }

    fun setCanvasAndViewPort(canvas: Rect,
                             viewPort: Rect) {
        mViewPortIndicatorView.setCanvasAndViewPort(canvas, viewPort)
    }

    fun onUpdateViewPortPosition(): Observable<CanvasEvent> {
        return mViewPortIndicatorView.onUpdateViewPortPosition()
    }

    // Edit tool //////////////////////////////////////////////////////////////

    // Tools sub-view
    private val mToolListView by lazy { findViewById<RecyclerView>(R.id.list_tools) }
    private val mToolListViewController by lazy {
        ToolListEpoxyController(imageLoader = Glide.with(context))
    }

    // Pen color & pen size ///////////////////////////////////////////////////

    // The color list view and view controller
    private val mColorTicketsView by lazy { findViewById<RecyclerView>(R.id.list_color_tickets) }
    private val mColorTicketsViewController by lazy {
        ColorTicketListEpoxyController(imageLoader = Glide.with(context))
    }

    private val mPenColorSignal = PublishSubject.create<Int>()
    fun onUpdatePenColor(): Observable<Int> {
        return mPenColorSignal
    }

    private val mPenSizeView by lazy { findViewById<PenSizeSeekBar>(R.id.slider_stroke_size) }

    private val mPenSizeSignal = PublishSubject.create<UpdatePenSizeEvent>()
    fun onUpdatePenSize(): Observable<UpdatePenSizeEvent> {
        return mPenSizeSignal
    }

    // Import & export ////////////////////////////////////////////////////////

    fun onClickImport(): Observable<Any> {
        return RxView.clicks(findViewById(R.id.btn_add_from_library))
    }

    fun onClickExport(): Observable<Any> {
        return RxView.clicks(findViewById(R.id.btn_save_as_photo))
    }

    // Other //////////////////////////////////////////////////////////////////

    override fun toString(): String {
        return javaClass.simpleName
    }
}
