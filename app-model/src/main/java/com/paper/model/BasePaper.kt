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
import java.io.File
import java.util.*

// TODO: Make it immutable
open class BasePaper(
    // The SQLite ID.
    id: Long = ModelConst.TEMP_ID,
    // The global ID.
    uuid: UUID = UUID.randomUUID(),
    createdAt: Long = 0L)
    : IPaper,
      NoObfuscation {

    private val mLock = Any()

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
        synchronized(mLock) {
            return mModifiedAt
        }
    }

    override fun setModifiedAt(time: Long) {
        synchronized(mLock) {
            mModifiedAt = time
        }
    }

    private var mWidth: Float = 512f
    private var mHeight: Float = 512f

    override fun getSize(): Pair<Float, Float> {
        synchronized(mLock) {
            return Pair(mWidth, mHeight)
        }
    }

    override fun setSize(size: Pair<Float, Float>) {
        synchronized(mLock) {
            val (width, height) = size
            mWidth = width
            mHeight = height
        }
    }

    private val mViewPort = Rect(0f, 0f, 0f, 0f)

    override fun getViewPort(): Rect {
        synchronized(mLock) {
            return mViewPort.copy()
        }
    }

    override fun setViewPort(rect: Rect) {
        synchronized(mLock) {
            mViewPort.set(rect)
        }
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

    override fun getThumbnail(): File? {
        return mThumbnail
    }

    override fun setThumbnail(file: File) {
        synchronized(mLock) {
            mThumbnail = file
        }

        // Request to save file
        requestAutoSave()
    }

    private var mThumbnailSize = Pair(0f, 0f)

    override fun getThumbnailSize(): Pair<Float, Float> {
        return mThumbnailSize.copy()
    }

    override fun setThumbnailSize(size: Pair<Float, Float>) {
        synchronized(mLock) {
            mThumbnailSize = size.copy()
        }

        // Request to save file
        requestAutoSave()
    }

    // Sketch & strokes ///////////////////////////////////////////////////////

//    private val mSketch = mutableListOf<VectorGraphics>()
//    private val mAddStrokeSignal = PublishSubject.create<VectorGraphics>().toSerialized()
//    private val mRemoveStrokeSignal = PublishSubject.create<VectorGraphics>().toSerialized()
//
//    override fun getSketch(): List<VectorGraphics> {
//        mLock.lock()
//        val sketch: List<VectorGraphics> = mSketch.toList()
//        mLock.unlock()
//
//        return sketch
//    }
//
//    override fun pushStroke(stroke: VectorGraphics) {
//        mLock.lock()
//        if (stroke.getTupleList.isNotEmpty()) {
//            mSketch.add(stroke)
//        }
//        mLock.unlock()
//
//        mAddStrokeSignal.onNext(stroke)
//
//        // Request to save file
//        requestAutoSave()
//    }
//
//    override fun popStroke(): VectorGraphics {
//        mLock.lock()
//        val stroke = mSketch.removeAt(mSketch.lastIndex)
//        mLock.unlock()
//
//        mRemoveStrokeSignal.onNext(stroke)
//
//        // Request to save file
//        requestAutoSave()
//
//        return stroke
//    }
//
//    override fun removeAllStrokes() {
//        mLock.lock()
//        val removed = mSketch.toList()
//        mSketch.clear()
//        mLock.unlock()
//
//        removed.forEach { stroke ->
//            mRemoveStrokeSignal.onNext(stroke)
//        }
//
//        // Request to save file
//        requestAutoSave()
//    }
//
//    override fun onAddStroke(replayAll: Boolean): Observable<VectorGraphics> {
//        return if (replayAll) {
//            Observable.merge(
//                Observable.fromIterable(getSketch()),
//                mAddStrokeSignal)
//        } else {
//            mAddStrokeSignal
//        }
//    }
//
//    override fun onRemoveStroke(): Observable<VectorGraphics> {
//        return mRemoveStrokeSignal
//    }

    // Scraps /////////////////////////////////////////////////////////////////

    private var mScraps = mutableListOf<IScrap>()

    override fun getScraps(): List<IScrap> {
        synchronized(mLock) {
            // Must clone the list in case concurrent modification
            return mScraps.toList()
        }
    }

    override fun addScrap(scrap: IScrap) {
        synchronized(mLock) {
            mScraps.add(scrap)
        }

        // Request to save file
        requestAutoSave()
    }

    override fun removeScrap(scrap: IScrap) {
        synchronized(mLock) {
            mScraps.remove(scrap)
        }

        // Request to save file
        requestAutoSave()
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
        mRepo?.putPaper(this@BasePaper)
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BasePaper

        if (getId() != other.getId()) return false
        if (getUUID() != other.getUUID()) return false
        if (getCreatedAt() != other.mCreatedAt) return false
        if (getModifiedAt() != other.getModifiedAt()) return false
        if (getSize() != other.getSize()) return false
        if (getThumbnail() != other.getThumbnail()) return false
        if (getThumbnailSize() != other.getThumbnailSize()) return false
        if (getScraps() != other.getScraps()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = getId().hashCode()
        result = 31 * result + getUUID().hashCode()
        result = 31 * result + getCreatedAt().hashCode()
        result = 31 * result + getModifiedAt().hashCode()
        result = 31 * result + getSize().hashCode()
        getThumbnail()?.let {
            result = 31 * result + it.hashCode()
        }
        result = 31 * result + getThumbnailSize().hashCode()
        result = 31 * result + getScraps().hashCode()
        return result
    }
}
