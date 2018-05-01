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

class PaperModel(
    // The SQLite ID.
    val id: Long = ModelConst.TEMP_ID,
    // The global ID.
    val uuid: UUID = UUID.randomUUID(),
    val createdAt: Long = 0L) {

    private val mLock = Any()

    var modifiedAt: Long = 0L

    // By default is landscape A4, 210 x 297 units.
    var width: Float = 297f
    var height: Float = 210f

    var thumbnailPath: File? = null
    var thumbnailWidth: Int = 0
    var thumbnailHeight: Int = 0

    var caption: String = ""

    // Sketch & strokes ///////////////////////////////////////////////////////

    private val mSketch = mutableListOf<SketchStroke>()

    val sketch: List<SketchStroke>
        get() {
            return synchronized(mLock) {
                mSketch.toList()
            }
        }

    fun pushStroke(stroke: SketchStroke) {
        synchronized(mLock) {
            mSketch.add(stroke)
            mAddStrokeSignal.onNext(stroke)
        }
    }

    fun popStroke() {
        synchronized(mLock) {
            val stroke = mSketch.removeAt(mSketch.lastIndex)
            mRemoveStrokeSignal.onNext(stroke)
        }
    }

    private val mAddStrokeSignal = PublishSubject.create<SketchStroke>()
    private val mRemoveStrokeSignal = PublishSubject.create<SketchStroke>()

    fun onAddStroke(replayAll: Boolean = true): Observable<SketchStroke> {
        return if (replayAll) {
            Observable.merge(
                Observable.fromIterable(sketch),
                mAddStrokeSignal)
        } else {
            mAddStrokeSignal
        }
    }

    fun onRemoveStroke(): Observable<SketchStroke> {
        return mRemoveStrokeSignal
    }

    // Scraps /////////////////////////////////////////////////////////////////

    private var mScraps = mutableListOf<ScrapModel>()
    private val mAddScrapSignal = PublishSubject.create<ScrapModel>()
    private val mRemoveScrapSignal = PublishSubject.create<ScrapModel>()

    // Must clone the list in case concurrent modification
    val scraps: List<ScrapModel>
        get() {
            return synchronized(mLock) {
                mScraps.toList()
            }
        }

    fun addScrap(scrap: ScrapModel) {
        synchronized(mLock) {
            mScraps.add(scrap)
            mAddScrapSignal.onNext(scrap)
        }
    }

    fun removeScrap(scrap: ScrapModel) {
        synchronized(mLock) {
            mScraps.remove(scrap)
            mRemoveScrapSignal.onNext(scrap)
        }
    }

    fun onAddScrap(replayAll: Boolean = true): Observable<ScrapModel> {
        return if (replayAll) {
            Observable.merge(
                Observable.fromIterable(scraps),
                mAddScrapSignal)
        } else {
            mAddScrapSignal
        }
    }

    fun onRemoveScrap(): Observable<ScrapModel> {
        return mRemoveScrapSignal
    }
}
