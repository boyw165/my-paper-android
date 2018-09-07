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
import com.google.gson.Gson
import com.paper.model.Whiteboard
import com.paper.model.IPreferenceService
import com.paper.model.ModelConst
import com.paper.model.event.UpdateDatabaseEvent
import com.paper.model.repository.sqlite.PaperTable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class WhiteboardRepoSQLite(private val authority: String,
                           private val resolver: ContentResolver,
                           private val jsonTranslator: Gson,
                           private val bmpCacheDir: File,
                           private val prefs: IPreferenceService,
                           private val dbIoScheduler: Scheduler)
    : IWhiteboardRepository,
      IBitmapRepository {

    /**
     * Observer observing the database change.
     */
    private val contentObserver by lazy {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri) {
                super.onChange(selfChange, uri)

                when (uri.lastPathSegment) {
                    CHANGE_ADD,
                    CHANGE_REMOVE,
                    CHANGE_UPDATE -> {
                        if (!paperListRefreshSignal.hasObservers()) return

                        getPapersSingleImpl(true)
                            .subscribe(
                                { papers ->
                                    paperListRefreshSignal.onNext(papers)
                                },
                                { err ->
                                    paperListRefreshSignal.onError(err)
                                })
                    }
                }
            }
        }
    }

    /**
     * A signal to put paper in the database.
     */
    private val putSignal = PublishSubject.create<Pair<Whiteboard, SingleSubject<UpdateDatabaseEvent>>>()
    private val disposableBag = CompositeDisposable()

    init {
        val uri: Uri = Uri.Builder()
            .scheme("content")
            .authority(this.authority)
            .path("paper")
            .build()
        this.resolver.registerContentObserver(uri, true, contentObserver)

        // Database writes
        disposableBag.add(
            putSignal
                .debounce(150, TimeUnit.MILLISECONDS, this.dbIoScheduler)
                // Writes operation should be atomic and not stoppable, thus
                // guarantees the database integrity.
                .flatMap { (paper, doneSignal) ->
                    putPaperImpl(paper)
                        .doOnSuccess { event ->
                            doneSignal.onSuccess(event)
                        }
                        .toObservable()
                }
                .observeOn(this.dbIoScheduler)
                .subscribe())
    }

    private val paperListRefreshSignal = PublishSubject.create<List<Whiteboard>>().toSerialized()

    /**
     * Get paper list.
     *
     * @param isSnapshot Readying the whole paper structure is sometimes
     *                   time-consuming. True to ready part only matter with
     *                   thumbnails; False is fully read.
     */
    override fun getBoards(isSnapshot: Boolean): Observable<List<Whiteboard>> {
        return Observable.merge(
            getPapersSingleImpl(isSnapshot).toObservable(),
            paperListRefreshSignal)
    }

    private fun getPapersSingleImpl(isSnapshot: Boolean): Single<List<Whiteboard>> {
        return Single
            .create { observer: SingleEmitter<List<Whiteboard>> ->
                var cursor: Cursor? = null

                try {
                    val uri: Uri = Uri.Builder()
                        .scheme("content")
                        .authority(authority)
                        .path("paper")
                        .build()
                    // Query content provider.
                    cursor = resolver.query(
                        uri,
                        // project:
                        arrayOf(PaperTable.COL_ID,
                                PaperTable.COL_UUID,
                                PaperTable.COL_CREATED_AT,
                                PaperTable.COL_MODIFIED_AT,
                                PaperTable.COL_CAPTION,
                                PaperTable.COL_THUMB_URI,
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
                    val papers = mutableListOf<Whiteboard>()
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
            .subscribeOn(this.dbIoScheduler)
    }

    /**
     * Read full structure of the paper by ID.
     */
    override fun getBoardById(id: Long): Single<Whiteboard> {
        return if (id == ModelConst.TEMP_ID) {
            Single
                .fromCallable {
                    val timestamp = getCurrentTime()
                    val newPaper = Whiteboard(
                        createdAt = timestamp)
                    newPaper.setModifiedAt(timestamp)

                    return@fromCallable newPaper
                }
                .subscribeOn(this.dbIoScheduler)
        } else {
            Single
                .create { observer: SingleEmitter<Whiteboard> ->
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(authority)
                        .path("paper/$id")
                        .build()

                    // Query content provider.
                    val cursor = resolver.query(
                        uri,
                        // projection:
                        arrayOf(PaperTable.COL_ID,
                                PaperTable.COL_UUID,
                                PaperTable.COL_CREATED_AT,
                                PaperTable.COL_MODIFIED_AT,
                                PaperTable.COL_CAPTION,
                                PaperTable.COL_THUMB_URI,
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

                        if (!observer.isDisposed) {
                            observer.onSuccess(paper)
                        }
                    }
                }
                .subscribeOn(this.dbIoScheduler)
        }
    }

    override fun putBoard(board: Whiteboard): Single<UpdateDatabaseEvent> {
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

        putSignal.onNext(Pair(board, doneSignal))

        return doneSignal
    }

    private fun putPaperImpl(paper: Whiteboard): Single<UpdateDatabaseEvent> {
        return Single
            .create { emitter: SingleEmitter<UpdateDatabaseEvent> ->
                val id = paper.getID()
                paper.setModifiedAt(getCurrentTime())

                if (id == ModelConst.TEMP_ID) {
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(authority)
                        .path("paper")
                        .build()
                    if (emitter.isDisposed) return@create

                    try {
                        // Lock paper and convert to database format
                        val values = convertPaperToValues(paper)

                        val newURI = resolver.insert(uri, values)
                        if (newURI != null) {
                            // Remember the ID
                            val newID = newURI.lastPathSegment.toLong()
                            prefs.putLong(ModelConst.PREFS_BROWSE_WHITEBOARD_ID, newID).blockingGet()

                            paper.setID(newID)

                            if (!emitter.isDisposed) {
                                println("${ModelConst.TAG}: put paper (id=$newID) successfully")

                                emitter.onSuccess(UpdateDatabaseEvent(
                                    successful = true,
                                    id = newURI.lastPathSegment.toLong()))
                            }

                            // Notify a addition just happens
                            resolver.notifyChange(Uri.parse("$newURI/$CHANGE_ADD"), null)
                        } else {
                            emitter.onError(IllegalArgumentException("Null data gets null URL"))
                        }
                    } catch (err: Throwable) {
                        emitter.onError(err)
                    }
                } else {
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(authority)
                        .path("paper/$id")
                        .build()
                    if (emitter.isDisposed) return@create

                    try {
                        // Lock paper and convert to database format
                        val values = convertPaperToValues(paper)

                        if (0 < resolver.update(uri, values, null, null)) {
                            if (!emitter.isDisposed) {
                                println("${ModelConst.TAG}: put paper (id=$id) successfully")

                                emitter.onSuccess(UpdateDatabaseEvent(
                                    successful = true,
                                    id = id))
                            }

                            // Notify an update just happens
                            resolver.notifyChange(Uri.parse("$uri/$CHANGE_UPDATE"), null)
                        } else {
                            emitter.onError(NoSuchElementException("Cannot find paper with id=$id"))
                        }
                    } catch (err: Throwable) {
                        emitter.onError(err)
                    }
                }
            }
            .subscribeOn(this.dbIoScheduler)
    }

    override fun duplicateBoardById(id: Long): Single<Whiteboard> {
        TODO("not implemented")
    }

    override fun deleteBoardById(id: Long): Single<UpdateDatabaseEvent> {
        return Single
            .create { emitter: SingleEmitter<UpdateDatabaseEvent> ->
                val uri = Uri.Builder()
                    .scheme("content")
                    .authority(authority)
                    .path("paper/$id")
                    .build()
                if (emitter.isDisposed) return@create

                try {
                    if (0 < resolver.delete(uri, null, null)) {
                        if (!emitter.isDisposed) {
                            emitter.onSuccess(UpdateDatabaseEvent(
                                successful = true,
                                id = id))
                        }

                        // Notify a deletion just happens
                        resolver.notifyChange(Uri.parse("$uri/$CHANGE_REMOVE"), null)
                    } else {
                        emitter.onError(NoSuchElementException("Cannot delete paper with id=$id"))
                    }
                } catch (err: Throwable) {
                    emitter.onError(err)
                }
            }
            .subscribeOn(this.dbIoScheduler)
    }

    private val bitmapJournal = HashMap<Int, File>()

    override fun putBitmap(key: Int, bmp: Bitmap): Single<File> {
        return Single
            .fromCallable {
                if (!bmpCacheDir.exists()) {
                    bmpCacheDir.mkdir()
                }

                // TODO: Use LruCache?
                val bmpFile = File(bmpCacheDir, "$key.png")

                FileOutputStream(bmpFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // TODO: Save the journal file somewhere
                bitmapJournal[key] = bmpFile

                println("${ModelConst.TAG}: put Bitmap to cache (key=$key, file=$bmpFile")

                return@fromCallable bmpFile
            }
            .subscribeOn(this.dbIoScheduler)
    }

    override fun getBitmap(key: Int): Single<Bitmap> {
        val file = bitmapJournal[key] ?: File(bmpCacheDir, "$key.png")

        return Single
                .fromCallable {
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        throw FileNotFoundException("cannot find the Bitmap")
                    }
                }
                .subscribeOn(this.dbIoScheduler)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun getCurrentTime(): Long = System.currentTimeMillis() / 1000

    private fun convertPaperToValues(paper: Whiteboard): ContentValues {
        val values = ContentValues()

        values.put(PaperTable.COL_UUID, paper.getUUID().toString())
        values.put(PaperTable.COL_CREATED_AT, paper.getCreatedAt())
        values.put(PaperTable.COL_MODIFIED_AT, paper.getModifiedAt())
        values.put(PaperTable.COL_CAPTION, paper.getCaption())

        values.put(PaperTable.COL_THUMB_URI, paper.getThumbnail().toString())

        val (thumbWidth, thumbHeight) = paper.getThumbnailSize()
        values.put(PaperTable.COL_THUMB_WIDTH, thumbWidth)
        values.put(PaperTable.COL_THUMB_HEIGHT, thumbHeight)

        // The rest part of Paper is converted to JSON
        val json = jsonTranslator.toJson(paper, Whiteboard::class.java)
        values.put(PaperTable.COL_DATA, json)

        return values
    }

    private fun convertCursorToPaper(cursor: Cursor,
                                     fullyRead: Boolean): Whiteboard {
        val paper = Whiteboard(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(PaperTable.COL_ID)),
            uuid = UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_UUID))),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(PaperTable.COL_CREATED_AT)))

        // Mutable modification time stamp
        val colOfModifiedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_MODIFIED_AT)
        paper.setModifiedAt(cursor.getLong(colOfModifiedAt))

        // Thumbnail
        val thumbWidth = cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_WIDTH))
        val thumbHeight = cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_HEIGHT))
        paper.setThumbnail(URI(cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_URI))), thumbWidth, thumbHeight)

        if (fullyRead) {
            val paperDetail = jsonTranslator.fromJson(
                cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_DATA)),
                Whiteboard::class.java)

            // Canvas size
            paper.setSize(paperDetail.getSize())

            // View port
            paper.setViewPort(paperDetail.getViewPort())

            // Scrap
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
    }
}
