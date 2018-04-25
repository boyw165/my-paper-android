// Copyright Mar 2017-present boyw165@gmail.com
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

package com.paper.model.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.GsonBuilder
import com.paper.model.ModelConst
import com.paper.model.PaperModel
import com.paper.model.ScrapModel
import com.paper.model.event.UpdateDatabaseEvent
import com.paper.model.repository.json.PaperJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.SketchStrokeJSONTranslator
import com.paper.model.repository.sqlite.PaperTable
import com.paper.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PaperRepoSqliteImpl(authority: String,
                          resolver: ContentResolver,
                          fileDir: File,
                          dbIoScheduler: Scheduler) : IPaperRepo {

    private val mAuthority = authority
    private val mResolver = resolver
    private val mFileDir = fileDir
    /**
     * A DB specific scheduler with only one thread in the thread pool.
     */
    private val mDbIoScheduler = dbIoScheduler

    // JSON translator.
    private val mTranslator by lazy {
        GsonBuilder()
            .registerTypeAdapter(PaperModel::class.java,
                                 PaperJSONTranslator())
            .registerTypeAdapter(SketchStroke::class.java,
                                 SketchStrokeJSONTranslator())
            .registerTypeAdapter(ScrapModel::class.java,
                                 ScrapJSONTranslator())
            .create()
    }

    /**
     * Observer observing the database change.
     */
    private val mObserver by lazy {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri) {
                super.onChange(selfChange, uri)

                when (uri.lastPathSegment) {
                    CHANGE_ADD,
                    CHANGE_REMOVE,
                    CHANGE_UPDATE -> getPapersThenFinish(true)
                        .subscribe(
                            { papers ->
                                mPaperListSignal.onNext(papers)
                            },
                            { err ->
                                mPaperListSignal.onError(err)
                            })
                }
            }
        }
    }

    init {
        val uri: Uri = Uri.Builder()
            .scheme("content")
            .authority(mAuthority)
            .path("paper")
            .build()
        mResolver.registerContentObserver(uri, true, mObserver)
    }

    private fun getPapersThenFinish(isSnapshot: Boolean): Single<List<PaperModel>> {
        return Single
            .create { downstream: SingleEmitter<List<PaperModel>> ->
                var cursor: Cursor? = null

                try {
                    val uri: Uri = Uri.Builder()
                        .scheme("content")
                        .authority(mAuthority)
                        .path("paper")
                        .build()
                    // Query content provider.
                    cursor = mResolver.query(
                        uri,
                        // project:
                        arrayOf(PaperTable.COL_ID,
                                PaperTable.COL_UUID,
                                PaperTable.COL_CREATED_AT,
                                PaperTable.COL_MODIFIED_AT,
                                PaperTable.COL_WIDTH,
                                PaperTable.COL_HEIGHT,
                                PaperTable.COL_CAPTION,
                                PaperTable.COL_THUMB_PATH,
                                PaperTable.COL_THUMB_WIDTH,
                                PaperTable.COL_THUMB_HEIGHT),
                        // selection:
                        null,
                        // selection args:
                        null,
                        // sort order:
                        "${PaperTable.COL_CREATED_AT} DESC")

                    // TODO: Refer to anko-sqlite,
                    // TODO: https://github.com/Kotlin/anko/wiki/Anko-SQLite

                    // Translate cursor.
                    val papers = mutableListOf<PaperModel>()
                    if (cursor.moveToFirst() &&
                        !downstream.isDisposed) {
                        do {
                            papers.add(convertCursorToPaper(cursor, !isSnapshot))
                        } while (cursor.moveToNext() &&
                                 !downstream.isDisposed)
                    }

                    if (!downstream.isDisposed) {
                        downstream.onSuccess(papers)
                    }
                } catch (error: InterruptedException) {
                    val i = error.stackTrace.indexOfFirst { trace ->
                        trace.className.contains("paper")
                    }
                    if (i >= 0) {
                        val className = error.stackTrace[i].className
                        val lineNo = error.stackTrace[i].lineNumber
                        Log.d(ModelConst.TAG, "$className L$lineNo get interrupted")
                    }
                } finally {
                    cursor?.close()
                }
            }
            .subscribeOn(mDbIoScheduler)
    }

    private val mPaperListSignal = PublishSubject.create<List<PaperModel>>()

    /**
     * Get paper list.
     *
     * @param isSnapshot Readying the whole paper structure is sometimes
     *                   time-consuming. True to ready part only matter with
     *                   thumbnails; False is fully read.
     */
    override fun getPapers(isSnapshot: Boolean): Observable<List<PaperModel>> {
        return Observable.merge(
            getPapersThenFinish(isSnapshot).toObservable(),
            mPaperListSignal)
    }

    /**
     * Read full structure of the paper by ID.
     */
    override fun getPaperById(id: Long): Single<PaperModel> {
        // FIXME: Workaround.
        return if (id == ModelConst.TEMP_ID) {
            Single
                .fromCallable {
//                    // Sol#1
//                    return@fromCallable mTempFile
//                        .bufferedReader()
//                        .use { reader ->
//                            mTranslator.fromJson(reader, PaperModel::class.java)
//                        }

                    // Sol#2
                    // TODO: Assign default portrait size.
                    val timestamp = getCurrentTime()
                    val newPaper = PaperModel(
                        createdAt = timestamp)
                    newPaper.modifiedAt = timestamp
                    return@fromCallable newPaper
                }
                .subscribeOn(mDbIoScheduler)
        } else {
            Single
                .fromCallable {
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(mAuthority)
                        .path("paper/$id")
                        .build()

                    // Query content provider.
                    val cursor = mResolver.query(
                        uri,
                        // projection:
                        arrayOf(PaperTable.COL_ID,
                                PaperTable.COL_UUID,
                                PaperTable.COL_CREATED_AT,
                                PaperTable.COL_MODIFIED_AT,
                                PaperTable.COL_WIDTH,
                                PaperTable.COL_HEIGHT,
                                PaperTable.COL_CAPTION,
                                PaperTable.COL_THUMB_PATH,
                                PaperTable.COL_THUMB_WIDTH,
                                PaperTable.COL_THUMB_HEIGHT,
                                PaperTable.COL_DATA),
                        // selection:
                        null,
                        // selection args:
                        null,
                        // sort order:
                        null)
                    if (cursor.count == 0) throw IllegalArgumentException("Cannot find paper, id=$id")
                    if (cursor.count > 1) throw IllegalStateException("Multiple paper id=%id conflict")

                    // Translate cursor.
                    cursor.moveToFirst()
                    val paper = convertCursorToPaper(cursor, true)
                    cursor.close()

                    return@fromCallable paper
                }
                .subscribeOn(mDbIoScheduler)
        }
    }

    override fun putPaperById(id: Long,
                              paper: PaperModel): Single<UpdateDatabaseEvent> {
        paper.modifiedAt = getCurrentTime()

        return Single
            .create { emitter: SingleEmitter<UpdateDatabaseEvent> ->
                if (id == ModelConst.TEMP_ID) {
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(mAuthority)
                        .path("paper")
                        .build()
                    if (emitter.isDisposed) return@create

                    try {
                        val newURI = mResolver.insert(uri, convertPaperToValues(paper))
                        if (newURI != null) {
                            if (!emitter.isDisposed) {
                                emitter.onSuccess(UpdateDatabaseEvent(
                                    successful = true,
                                    id = newURI.lastPathSegment.toLong()))
                            }

                            // Notify a addition just happens
                            mResolver.notifyChange(Uri.parse("$newURI/$CHANGE_ADD"), null)
                        } else {
                            emitter.onError(IllegalArgumentException("Null data gets null URL"))
                        }
                    } catch (err: Throwable) {
                        emitter.onError(err)
                    }
                } else {
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(mAuthority)
                        .path("paper/$id")
                        .build()
                    if (emitter.isDisposed) return@create

                    try {
                        if (0 < mResolver.update(uri, convertPaperToValues(paper), null, null)) {
                            if (!emitter.isDisposed) {
                                emitter.onSuccess(UpdateDatabaseEvent(
                                    successful = true,
                                    id = id))
                            }

                            // Notify an update just happens
                            mResolver.notifyChange(Uri.parse("$uri/$CHANGE_UPDATE"), null)
                        } else {
                            emitter.onError(NoSuchElementException("Cannot find paper with id=$id"))
                        }
                    } catch (err: Throwable) {
                        emitter.onError(err)
                    }
                }
            }
            .subscribeOn(mDbIoScheduler)
    }

    override fun duplicatePaperById(id: Long): Observable<PaperModel> {
        TODO("not implemented")
    }

    override fun deletePaperById(id: Long): Single<UpdateDatabaseEvent> {
        return Single
            .create { emitter: SingleEmitter<UpdateDatabaseEvent> ->
                val uri = Uri.Builder()
                    .scheme("content")
                    .authority(mAuthority)
                    .path("paper/$id")
                    .build()
                if (emitter.isDisposed) return@create

                try {
                    if (0 < mResolver.delete(uri, null, null)) {
                        if (!emitter.isDisposed) {
                            emitter.onSuccess(UpdateDatabaseEvent(
                                successful = true,
                                id = id))
                        }

                        // Notify a deletion just happens
                        mResolver.notifyChange(Uri.parse("$uri/$CHANGE_REMOVE"), null)
                    } else {
                        emitter.onError(NoSuchElementException("Cannot delete paper with id=$id"))
                    }
                } catch (err: Throwable) {
                    emitter.onError(err)
                }
            }
            .subscribeOn(mDbIoScheduler)
    }

    override fun putBitmap(bmp: Bitmap): Single<File> {
        return Single
            .fromCallable {
                // TODO: Use LruCache?
                if (!mFileDir.exists()) {
                    mFileDir.mkdir()
                }

                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
                val bmpFile = File(mFileDir, "$ts.jpg")

                FileOutputStream(bmpFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }

                return@fromCallable bmpFile
            }
            .subscribeOn(mDbIoScheduler)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun getCurrentTime(): Long = System.currentTimeMillis() / 1000

    private fun convertPaperToValues(paper: PaperModel): ContentValues {
        val values = ContentValues()

        values.put(PaperTable.COL_UUID, paper.uuid.toString())
        values.put(PaperTable.COL_CREATED_AT, paper.createdAt)
        values.put(PaperTable.COL_MODIFIED_AT, paper.modifiedAt)
        values.put(PaperTable.COL_WIDTH, paper.width)
        values.put(PaperTable.COL_HEIGHT, paper.height)
        values.put(PaperTable.COL_CAPTION, paper.caption)

        values.put(PaperTable.COL_THUMB_PATH, paper.thumbnailPath?.canonicalPath ?: "")
        values.put(PaperTable.COL_THUMB_WIDTH, paper.thumbnailWidth)
        values.put(PaperTable.COL_THUMB_HEIGHT, paper.thumbnailHeight)

        // The rest part of Paper is converted to JSON
        val json = mTranslator.toJson(paper)
        values.put(PaperTable.COL_DATA, json)

        return values
    }

    private fun convertCursorToPaper(cursor: Cursor,
                                     fullyRead: Boolean): PaperModel {
        val paper = PaperModel(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(PaperTable.COL_ID)),
            uuid = UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_UUID))),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(PaperTable.COL_CREATED_AT)))

        val colOfModifiedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_MODIFIED_AT)
        paper.modifiedAt = cursor.getLong(colOfModifiedAt)

        paper.width = cursor.getFloat(cursor.getColumnIndexOrThrow(PaperTable.COL_WIDTH))
        paper.height = cursor.getFloat(cursor.getColumnIndexOrThrow(PaperTable.COL_HEIGHT))

        paper.caption = cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_CAPTION))

        paper.thumbnailPath = File(cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_PATH)))
        paper.thumbnailWidth = cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_WIDTH))
        paper.thumbnailHeight = cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_HEIGHT))

        if (fullyRead) {
            val paperDetail = mTranslator.fromJson(
                cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_DATA)),
                PaperModel::class.java)

            paperDetail.sketch.forEach { paper.addStrokeToSketch(it) }
            paperDetail.scraps.forEach { paper.addScrap(it) }
        }

        return paper
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    companion object {
        const val CHANGE_ADD = "add"
        const val CHANGE_REMOVE = "remove"
        const val CHANGE_UPDATE = "update"
    }
}
