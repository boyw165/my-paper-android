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
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.GsonBuilder
import com.paper.model.*
import com.paper.model.event.UpdateDatabaseEvent
import com.paper.model.repository.json.PaperJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.SketchStrokeJSONTranslator
import com.paper.model.repository.sqlite.PaperTable
import com.paper.model.sketch.SketchStroke
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class PaperRepoSqliteImpl(authority: String,
                          resolver: ContentResolver,
                          fileDir: File,
                          prefs: IPreferenceService,
                          dbIoScheduler: Scheduler) : IPaperRepo,
                                                      IBitmapRepo {
    private val mAuthority = authority
    private val mResolver = resolver
    private val mFileDir = fileDir
    private val mPrefs = prefs
    /**
     * A DB specific scheduler with only one thread in the thread pool.
     */
    private val mDbIoScheduler = dbIoScheduler

    /**
     * JSON translator.
     */
    private val mTranslator by lazy {
        GsonBuilder()
            .registerTypeAdapter(PaperAutoSaveImpl::class.java,
                                 PaperJSONTranslator())
            .registerTypeAdapter(SketchStroke::class.java,
                                 SketchStrokeJSONTranslator())
            .registerTypeAdapter(Scrap::class.java,
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
                    CHANGE_UPDATE -> {
                        if (!mPaperListSignal.hasObservers()) return

                        getPapersSingleImpl(true)
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
    }

    /**
     * A signal to put paper in the database.
     */
    private val mPutSignal = PublishSubject.create<Pair<IPaper, SingleSubject<UpdateDatabaseEvent>>>()
    private val mDisposables = CompositeDisposable()

    init {
        val uri: Uri = Uri.Builder()
            .scheme("content")
            .authority(mAuthority)
            .path("paper")
            .build()
        mResolver.registerContentObserver(uri, true, mObserver)

        // Database writes
        mDisposables.add(
            mPutSignal
                .debounce(150, TimeUnit.MILLISECONDS, dbIoScheduler)
                // Writes operation should be atomic and not stoppable, thus
                // guarantees the database integrity.
                .flatMap { (paper, doneSignal) ->
                    putPaperImpl(paper)
                        .doOnSuccess { event ->
                            doneSignal.onSuccess(event)
                        }
                        .toObservable()
                }
                .observeOn(dbIoScheduler)
                .subscribe())
    }

    private var mTmpPaperWidth: Float = 297f
    private var mTmpPaperHeight: Float = 210f

    override fun setTmpPaperSize(width: Float,
                                 height: Float): Single<Boolean> {
        mTmpPaperWidth = width
        mTmpPaperHeight = height

        return Single.just(true)
    }

    private val mPaperListSignal = PublishSubject.create<List<IPaper>>()

    /**
     * Get paper list.
     *
     * @param isSnapshot Readying the whole paper structure is sometimes
     *                   time-consuming. True to ready part only matter with
     *                   thumbnails; False is fully read.
     */
    override fun getPapers(isSnapshot: Boolean): Observable<List<IPaper>> {
        return Observable.merge(
            getPapersSingleImpl(isSnapshot).toObservable(),
            mPaperListSignal)
    }

    private fun getPapersSingleImpl(isSnapshot: Boolean): Single<List<IPaper>> {
        return Single
            .create { observer: SingleEmitter<List<IPaper>> ->
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
                    val papers = mutableListOf<IPaper>()
                    if (cursor.moveToFirst() &&
                        !observer.isDisposed) {
                        do {
                            papers.add(convertCursorToPaper(
                                cursor = cursor,
                                fullyRead = !isSnapshot))
                        } while (cursor.moveToNext() &&
                                 !observer.isDisposed)
                    }

                    if (!observer.isDisposed) {
                        observer.onSuccess(papers)
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

    /**
     * Read full structure of the paper by ID.
     */
    override fun getPaperById(id: Long): Single<IPaper> {
        return if (id == ModelConst.TEMP_ID) {
            Single
                .fromCallable {
                    val timestamp = getCurrentTime()
                    val newPaper = PaperAutoSaveImpl(
                        createdAt = timestamp)
                    newPaper.setModifiedAt(timestamp)
                    newPaper.setWidth(mTmpPaperWidth)
                    newPaper.setHeight(mTmpPaperHeight)

                    // Enable auto-save
                    newPaper.setAutoSaveRepo(this@PaperRepoSqliteImpl)

                    return@fromCallable newPaper as IPaper
                }
                .subscribeOn(mDbIoScheduler)
        } else {
            Single
                .create { observer: SingleEmitter<IPaper> ->
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
                    if (observer.isDisposed) return@create

                    // Translate cursor.
                    cursor.moveToFirst()
                    cursor.use { c ->
                        val paper = convertCursorToPaper(
                            c,
                            fullyRead = true)

                        // Enable auto-save
                        if (paper is PaperAutoSaveImpl) {
                            paper.setAutoSaveRepo(this@PaperRepoSqliteImpl)
                        }

                        if (!observer.isDisposed) {
                            observer.onSuccess(paper)
                        }
                    }
                }
                .subscribeOn(mDbIoScheduler)
        }
    }

    override fun putPaper(paper: IPaper): Single<UpdateDatabaseEvent> {
//        val requestConstraint = Constraints.Builder()
//            .setRequiresStorageNotLow(true)
//            .build()
//        val request = OneTimeWorkRequestBuilder<WritePaperWork>()
//            .setConstraints(requestConstraint)
//            .setInputData()
//            .setInitialDelay(550, TimeUnit.MILLISECONDS)
//            .build()
//
//        WorkManager.getInstance()
//            .beginUniqueWork(WORK_TAG_PUT_PAPER, ExistingWorkPolicy.REPLACE, request)

        val doneSignal = SingleSubject.create<UpdateDatabaseEvent>()

        mPutSignal.onNext(Pair(paper, doneSignal))

        return doneSignal
    }

    private fun putPaperImpl(paper: IPaper): Single<UpdateDatabaseEvent> {
        return Single
            .create { emitter: SingleEmitter<UpdateDatabaseEvent> ->
                paper.lock()
                val id = paper.getId()
                paper.setModifiedAt(getCurrentTime())
                paper.unlock()

                if (id == ModelConst.TEMP_ID) {
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(mAuthority)
                        .path("paper")
                        .build()
                    if (emitter.isDisposed) return@create

                    try {
                        // Lock paper and convert to database format
                        paper.lock()
                        val values = convertPaperToValues(paper)
                        paper.unlock()

                        val newURI = mResolver.insert(uri, values)
                        if (newURI != null) {
                            // Remember the ID
                            val newID = newURI.lastPathSegment.toLong()
                            mPrefs.putLong(ModelConst.PREFS_BROWSE_PAPER_ID, newID).blockingGet()

                            paper.lock()
                            if (paper is PaperAutoSaveImpl) {
                                paper.mID = newID
                            }
                            paper.unlock()

                            if (!emitter.isDisposed) {
                                println("${ModelConst.TAG}: put paper (id=$newID) successfully")

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
                        // Lock paper and convert to database format
                        paper.lock()
                        val values = convertPaperToValues(paper)
                        paper.unlock()

                        if (0 < mResolver.update(uri, values, null, null)) {
                            if (!emitter.isDisposed) {
                                println("${ModelConst.TAG}: put paper (id=$id) successfully")

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

    override fun duplicatePaperById(id: Long): Single<IPaper> {
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

    private val mBitmapMap = HashMap<Int, File>()

    override fun putBitmap(key: Int, bmp: Bitmap): Single<File> {
        return Single
            .fromCallable {
                if (!mFileDir.exists()) {
                    mFileDir.mkdir()
                }

                val bmpFile = File(mFileDir, "$key.png")

                FileOutputStream(bmpFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // TODO: Use LruCache?
                mBitmapMap[key] = bmpFile

                println("${ModelConst.TAG}: put Bitmap to cache (key=$key, file=$bmpFile")

                return@fromCallable bmpFile
            }
            .subscribeOn(mDbIoScheduler)
    }

    override fun getBitmap(key: Int): Single<Bitmap> {
        val file = mBitmapMap[key] ?: File(mFileDir, "$key.png")

        return Single
                .fromCallable {
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        throw FileNotFoundException("cannot find the Bitmap")
                    }
                }
                .subscribeOn(mDbIoScheduler)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun getCurrentTime(): Long = System.currentTimeMillis() / 1000

    private fun convertPaperToValues(paper: IPaper): ContentValues {
        val values = ContentValues()

        values.put(PaperTable.COL_UUID, paper.getUUID().toString())
        values.put(PaperTable.COL_CREATED_AT, paper.getCreatedAt())
        values.put(PaperTable.COL_MODIFIED_AT, paper.getModifiedAt())
        values.put(PaperTable.COL_WIDTH, paper.getWidth())
        values.put(PaperTable.COL_HEIGHT, paper.getHeight())
        values.put(PaperTable.COL_CAPTION, paper.getCaption())

        values.put(PaperTable.COL_THUMB_PATH, paper.getThumbnail()?.canonicalPath ?: "")
        values.put(PaperTable.COL_THUMB_WIDTH, paper.getThumbnailWidth())
        values.put(PaperTable.COL_THUMB_HEIGHT, paper.getThumbnailHeight())

        // The rest part of Paper is converted to JSON
        val json = mTranslator.toJson(paper)
        values.put(PaperTable.COL_DATA, json)

        return values
    }

    private fun convertCursorToPaper(cursor: Cursor,
                                     fullyRead: Boolean): IPaper {
        val paper = PaperAutoSaveImpl(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(PaperTable.COL_ID)),
            uuid = UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_UUID))),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(PaperTable.COL_CREATED_AT)))

        val colOfModifiedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_MODIFIED_AT)
        paper.setModifiedAt(cursor.getLong(colOfModifiedAt))

        paper.setWidth(cursor.getFloat(cursor.getColumnIndexOrThrow(PaperTable.COL_WIDTH)))
        paper.setHeight(cursor.getFloat(cursor.getColumnIndexOrThrow(PaperTable.COL_HEIGHT)))

        paper.setThumbnail(File(cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_PATH))))
        paper.setThumbnailWidth(cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_WIDTH)))
        paper.setThumbnailHeight(cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_HEIGHT)))

        if (fullyRead) {
            val paperDetail = mTranslator.fromJson(
                cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_DATA)),
                PaperAutoSaveImpl::class.java)

            paperDetail.getSketch().forEach { paper.pushStroke(it) }
            paperDetail.getScraps().forEach { paper.addScrap(it) }
        }

        return paper
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    companion object {
        private const val CHANGE_ADD = "add"
        private const val CHANGE_REMOVE = "remove"
        private const val CHANGE_UPDATE = "update"

        private const val WORK_TAG_PUT_PAPER = "put_paper"
    }
}
