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

import com.paper.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*

class PaperAutoSaveImpl(
    // The SQLite ID.
    id: Long = ModelConst.TEMP_ID,
    // The global ID.
    uuid: UUID = UUID.randomUUID(),
    createdAt: Long = 0L)
    : IPaper {

    private val mLock = Any()

    // General ////////////////////////////////////////////////////////////////

    private val mID = id

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
        mWidth = width
    }

    override fun setHeight(height: Float) {
        mHeight = height
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
        mThumbnail = file
    }

    override fun setThumbnailWidth(width: Int) {
        mThumbnailWidth = width
    }

    override fun setThumbnailHeight(height: Int) {
        mThumbnailHeight = height
    }

    // Sketch & strokes ///////////////////////////////////////////////////////

    private val mSketch = mutableListOf<SketchStroke>()
    private val mAddStrokeSignal = PublishSubject.create<SketchStroke>()
    private val mRemoveStrokeSignal = PublishSubject.create<SketchStroke>()

    override fun getSketch(): List<SketchStroke> {
        return synchronized(mLock) {
            mSketch.toList()
        }
    }

    override fun pushStroke(stroke: SketchStroke) {
        synchronized(mLock) {
            mSketch.add(stroke)
            mAddStrokeSignal.onNext(stroke)
        }
    }

    override fun popStroke(): SketchStroke {
        val stroke = mSketch.removeAt(mSketch.lastIndex)
        mRemoveStrokeSignal.onNext(stroke)
        return stroke
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

    private var mScraps = mutableListOf<ScrapModel>()
    private val mAddScrapSignal = PublishSubject.create<ScrapModel>()
    private val mRemoveScrapSignal = PublishSubject.create<ScrapModel>()

    override fun getScraps(): List<ScrapModel> {
        return synchronized(mLock) {
            // Must clone the list in case concurrent modification
            mScraps.toList()
        }
    }

    override fun addScrap(scrap: ScrapModel) {
        synchronized(mLock) {
            mScraps.add(scrap)
            mAddScrapSignal.onNext(scrap)
        }
    }

    override fun removeScrap(scrap: ScrapModel) {
        synchronized(mLock) {
            mScraps.remove(scrap)
            mRemoveScrapSignal.onNext(scrap)
        }
    }

    override fun onAddScrap(replayAll: Boolean): Observable<ScrapModel> {
        return if (replayAll) {
            Observable.merge(
                Observable.fromIterable(getScraps()),
                mAddScrapSignal)
        } else {
            mAddScrapSignal
        }
    }

    override fun onRemoveScrap(): Observable<ScrapModel> {
        return mRemoveScrapSignal
    }
}
