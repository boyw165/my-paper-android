// Copyright (c) 2017-present boyw165
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.paper.model

import com.paper.model.repository.IPaperRepo
import com.paper.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

class PaperAutoSaveImpl(
    // The SQLite ID.
    id: Long = ModelConst.TEMP_ID,
    // The global ID.
    uuid: UUID = UUID.randomUUID(),
    createdAt: Long = 0L)
    : IPaper,
      NoObfuscation {

    // General ////////////////////////////////////////////////////////////////

    internal var mID = id

    override fun getId(): Long {
        return mID
    }

    private val mUUID = uuid

    override fun getUUID(): UUID {
        return mUUID
    }

    private val mCreatedAt = createdAt

    override fun getCreatedAt(): Long {
        return mCreatedAt
    }

    private var mModifiedAt: Long = 0L

    override fun getModifiedAt(): Long {
        return mModifiedAt
    }

    override fun setModifiedAt(time: Long) {
        mModifiedAt = time
    }

    // By default is landscape A4, 210 x 297 units.
    private var mWidth: Float = 297f
    private var mHeight: Float = 210f

    override fun getWidth(): Float {
        return mWidth
    }

    override fun getHeight(): Float {
        return mHeight
    }

    override fun setWidth(width: Float) {
        mLock.lock()
        mWidth = width
        mLock.unlock()
    }

    override fun setHeight(height: Float) {
        mLock.lock()
        mHeight = height
        mLock.unlock()
    }

    // Caption & tags /////////////////////////////////////////////////////////

    override fun getCaption(): String {
        return ""
    }

    override fun getTags(): List<String> {
        return emptyList()
    }

    // Thumbnail //////////////////////////////////////////////////////////////

    private var mThumbnail: File? = null
    private var mThumbnailWidth: Int = 0
    private var mThumbnailHeight: Int = 0

    override fun getThumbnail(): File? {
        return mThumbnail
    }

    override fun getThumbnailWidth(): Int {
        return mThumbnailWidth
    }

    override fun getThumbnailHeight(): Int {
        return mThumbnailHeight
    }

    override fun setThumbnail(file: File) {
        mLock.lock()
        mThumbnail = file
        mLock.unlock()

        // Request to save file
        requestAutoSave()
    }

    override fun setThumbnailWidth(width: Int) {
        mLock.lock()
        mThumbnailWidth = width
        mLock.unlock()

        // Request to save file
        requestAutoSave()
    }

    override fun setThumbnailHeight(height: Int) {
        mLock.lock()
        mThumbnailHeight = height
        mLock.unlock()

        // Request to save file
        requestAutoSave()
    }

    // Sketch & strokes ///////////////////////////////////////////////////////

    private val mSketch = mutableListOf<SketchStroke>()
    private val mAddStrokeSignal = PublishSubject.create<SketchStroke>()
    private val mRemoveStrokeSignal = PublishSubject.create<SketchStroke>()

    override fun getSketch(): List<SketchStroke> {
        mLock.lock()
        val sketch: List<SketchStroke> = mSketch.toList()
        mLock.unlock()

        return sketch
    }

    override fun pushStroke(stroke: SketchStroke) {
        mLock.lock()
        if (stroke.pointList.isNotEmpty()) {
            mSketch.add(stroke)
        }
        mLock.unlock()

        mAddStrokeSignal.onNext(stroke)

        // Request to save file
        requestAutoSave()
    }

    override fun popStroke(): SketchStroke {
        mLock.lock()
        val stroke = mSketch.removeAt(mSketch.lastIndex)
        mLock.unlock()

        mRemoveStrokeSignal.onNext(stroke)

        // Request to save file
        requestAutoSave()

        return stroke
    }

    override fun removeAllStrokes() {
        mLock.lock()
        val removed = mSketch.toList()
        mSketch.clear()
        mLock.unlock()

        removed.forEach { stroke ->
            mRemoveStrokeSignal.onNext(stroke)
        }

        // Request to save file
        requestAutoSave()
    }

    override fun onAddStroke(replayAll: Boolean): Observable<SketchStroke> {
        return if (replayAll) {
            Observable.merge(
                Observable.fromIterable(getSketch()),
                mAddStrokeSignal)
        } else {
            mAddStrokeSignal
        }
    }

    override fun onRemoveStroke(): Observable<SketchStroke> {
        return mRemoveStrokeSignal
    }

    // Scraps /////////////////////////////////////////////////////////////////

    private var mScraps = mutableListOf<Scrap>()
    private val mAddScrapSignal = PublishSubject.create<Scrap>()
    private val mRemoveScrapSignal = PublishSubject.create<Scrap>()

    override fun getScraps(): List<Scrap> {
        mLock.lock()
        // Must clone the list in case concurrent modification
        val scraps = mScraps.toList()
        mLock.unlock()

        return scraps
    }

    override fun addScrap(scrap: Scrap) {
        mLock.lock()
        mScraps.add(scrap)
        mAddScrapSignal.onNext(scrap)
        mLock.unlock()

        // Request to save file
        requestAutoSave()
    }

    override fun removeScrap(scrap: Scrap) {
        mLock.lock()
        mScraps.remove(scrap)
        mRemoveScrapSignal.onNext(scrap)
        mLock.unlock()

        // Request to save file
        requestAutoSave()
    }

    override fun onAddScrap(replayAll: Boolean): Observable<Scrap> {
        return if (replayAll) {
            Observable.merge(
                Observable.fromIterable(getScraps()),
                mAddScrapSignal)
        } else {
            mAddScrapSignal
        }
    }

    override fun onRemoveScrap(): Observable<Scrap> {
        return mRemoveScrapSignal
    }

    // Auto-save //////////////////////////////////////////////////////////////

    private var mRepo: IPaperRepo? = null

    /**
     * Enable the auto-save function.
     */
    fun setAutoSaveRepo(repo: IPaperRepo) {
        mRepo = repo
    }

    private fun requestAutoSave() {
        mRepo?.putPaper(this@PaperAutoSaveImpl)
    }

    // Thread save ////////////////////////////////////////////////////////////

    private val mLock = ReentrantLock()

    override fun lock() {
        mLock.lock()
    }

    override fun tryLock(): Boolean {
        return mLock.tryLock()
    }

    override fun tryLock(time: Long, unit: TimeUnit?): Boolean {
        return mLock.tryLock(time, unit)
    }

    override fun unlock() {
        mLock.unlock()
    }

    override fun lockInterruptibly() {
        mLock.lockInterruptibly()
    }

    override fun newCondition(): Condition {
        return mLock.newCondition()
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PaperAutoSaveImpl

        if (mID != other.mID) return false
        if (mUUID != other.mUUID) return false
        if (mCreatedAt != other.mCreatedAt) return false
        if (mModifiedAt != other.mModifiedAt) return false
        if (mWidth != other.mWidth) return false
        if (mHeight != other.mHeight) return false
        if (mThumbnail != other.mThumbnail) return false
        if (mThumbnailWidth != other.mThumbnailWidth) return false
        if (mThumbnailHeight != other.mThumbnailHeight) return false
        if (mSketch != other.mSketch) return false
        if (mScraps != other.mScraps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mID.hashCode()
        result = 31 * result + mUUID.hashCode()
        result = 31 * result + mCreatedAt.hashCode()
        result = 31 * result + mModifiedAt.hashCode()
        result = 31 * result + mWidth.hashCode()
        result = 31 * result + mHeight.hashCode()
        result = 31 * result + (mThumbnail?.hashCode() ?: 0)
        result = 31 * result + mThumbnailWidth
        result = 31 * result + mThumbnailHeight
        result = 31 * result + mSketch.hashCode()
        result = 31 * result + mScraps.hashCode()
        return result
    }
}
