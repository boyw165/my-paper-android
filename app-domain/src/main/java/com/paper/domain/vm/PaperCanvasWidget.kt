// Copyright Mar 2018-present boyw165@gmail.com
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

package com.paper.domain.vm

import com.paper.domain.ISchedulerProvider
import com.paper.domain.data.DrawingMode
import com.paper.domain.data.GestureRecord
import com.paper.model.ICanvasOperation
import com.paper.model.IPaper
import com.paper.model.IScrap
import com.paper.model.Rect
import com.paper.model.event.AddScrapEvent
import com.paper.model.event.RemoveScrapEvent
import com.paper.model.event.UpdateScrapEvent
import com.paper.model.operation.AddScrapOperation
import com.paper.model.operation.RemoveScrapOperation
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PaperCanvasWidget(private val schedulers: ISchedulerProvider)
    : IPaperCanvasWidget {

    private val mDisposables = CompositeDisposable()

    // Scrap controllers
    private val mScrapWidgets = ConcurrentHashMap<UUID, IBaseScrapWidget>()

    // Global canceller
    private val mCancelSignal = PublishSubject.create<Any>()

    // Gesture
    private val mGestureHistory = mutableListOf<GestureRecord>()

    // Debug
    private val mDebugSignal = PublishSubject.create<String>()

    override fun start() {
        ensureNoLeakedBinding()

        // Canvas size
        mCanvasSizeSignal.onNext(model.getSize())
    }

    override fun stop() {
        mDisposables.clear()
    }

    private fun ensureNoLeakedBinding() {
        if (mDisposables.size() > 0)
            throw IllegalStateException("Already start a model")
    }

    // Debug //////////////////////////////////////////////////////////////////

    override fun onPrintDebugMessage(): Observable<String> {
        return mDebugSignal
    }

    // Number of on-going task ////////////////////////////////////////////////

    private val mDirtyFlag = CanvasDirtyFlag()

    fun onBusy(): Observable<Boolean> {
        return mDirtyFlag
            .onUpdate()
            .map { event ->
                // Ready iff flag is zero
                event.flag == 0
            }
    }

    // Add & Remove Scrap /////////////////////////////////////////////////////

    private val mUpdateScrapSignal = PublishSubject.create<UpdateScrapEvent>().toSerialized()

    override fun getScraps(): List<IScrap> {
        return mScrapWidgets.toList().map { (_, scrap) ->
            scrap as IScrap
        }
    }

    override fun addScrap(scrap: IScrap) {
        scrap as IBaseScrapWidget

        mScrapWidgets[scrap.getId()] = scrap

        // Signal out
        mUpdateScrapSignal.onNext(AddScrapEvent(scrap))
        // TODO: ADD operation holds immutable scrap?
        mOperationSignal.onNext(AddScrapOperation())
    }

    override fun removeScrap(scrap: IScrap) {
        scrap as IBaseScrapWidget

        // Remove key
        mScrapWidgets.remove(scrap.getId())

        // Signal out
        mUpdateScrapSignal.onNext(RemoveScrapEvent(scrap))
        // TODO: REMOVE operation holds immutable scrap?
        mOperationSignal.onNext(RemoveScrapOperation())
    }

    override fun onUpdateScrap(): Observable<UpdateScrapEvent> {
        return mUpdateScrapSignal
    }

    override fun eraseCanvas() {
        TODO("not implemented")
    }

    private val mCanvasSizeSignal = BehaviorSubject.create<Pair<Float, Float>>()

    override fun onUpdateCanvasSize(): Observable<Pair<Float, Float>> {
        return mCanvasSizeSignal
    }

    // Gesture ////////////////////////////////////////////////////////////////

    override fun handleTouchBegin() {
    }

    override fun handleTouchEnd() {
    }

    // Basic /////////////////////////////////////////////////////////////////

    private var id = 0L
    private lateinit var uuid: UUID

    private var createdAt = 0L
    private var modifiedAt = 0L

    private var width = 0f
    private var height = 0f

    private val viewPort = Rect()

    private var thumbnail: File? = null
    private var thumbnailWidth = 0f
    private var thumbnailHeight = 0f

    private var caption = ""
    private val tags = mutableListOf<String>()

    var model: IPaper
        set(value) {
            this.id = value.getId()
            this.uuid = value.getUUID()

            val (canvasWidth, canvasHeight) = value.getSize()
            this.width = canvasWidth
            this.height = canvasHeight

            val vp = value.getViewPort()
            viewPort.set(vp)

            this.createdAt = value.getCreatedAt()
            this.modifiedAt = value.getModifiedAt()

            this.thumbnail = value.getThumbnail()
            val (thumbWidth, thumbHeight) = value.getThumbnailSize()
            this.thumbnailWidth = thumbWidth
            this.thumbnailHeight = thumbHeight

            this.caption = value.getCaption()

            this.tags.addAll(value.getTags())
        }
        get() = this

    override fun getId(): Long {
        return id
    }

    override fun getUUID(): UUID {
        return uuid
    }

    override fun getSize(): Pair<Float, Float> {
        return Pair(width, height)
    }

    override fun setSize(size: Pair<Float, Float>) {
        width = size.first
        height = size.second
    }

    override fun getCreatedAt(): Long {
        return createdAt
    }

    override fun getModifiedAt(): Long {
        return modifiedAt
    }

    override fun setModifiedAt(time: Long) {
        modifiedAt = time
    }

    override fun getCaption(): String {
        return caption
    }

    override fun getTags(): List<String> {
        return tags
    }

    override fun getThumbnail(): File? {
        return thumbnail
    }

    override fun setThumbnail(file: File) {
        thumbnail = file
    }

    override fun getThumbnailSize(): Pair<Float, Float> {
        return Pair(thumbnailWidth, thumbnailHeight)
    }

    override fun setThumbnailSize(size: Pair<Float, Float>) {
        thumbnailWidth = size.first
        thumbnailHeight = size.second
    }

    override fun getViewPort(): Rect {
        return viewPort.copy()
    }

    override fun setViewPort(rect: Rect) {
        viewPort.set(rect)
    }

    // Drawing ////////////////////////////////////////////////////////////////

    /**
     * The current stroke color.
     */
    private var mPenColor = 0x2C2F3C
    /**
     * The current stroke width, where the value is from 0.0 to 1.0.
     */
    private var mPenSize = 0.2f
    /**
     * The current view-port scale.
     */
    private var mViewPortScale = Float.NaN

    override fun setDrawingMode(mode: DrawingMode) {
        TODO()
    }

    override fun setChosenPenColor(color: Int) {
        mPenColor = color
    }

    override fun setViewPortScale(scale: Float) {
        mViewPortScale = scale
    }

    override fun setPenSize(size: Float) {
        mPenSize = size
    }

    // Operation & undo/redo //////////////////////////////////////////////////

    private val mOperationSignal = PublishSubject.create<ICanvasOperation>().toSerialized()

    override fun onUpdateCanvasOperation(): Observable<ICanvasOperation> {
        return mOperationSignal
    }

    // Equality ///////////////////////////////////////////////////////////////

    override fun toString(): String {
        return "${javaClass.simpleName}{\n" +
               "id=${getId()}, uuid=${getUUID()}\n" +
               "scraps=${getScraps().size}\n" +
               "}"
    }
}
