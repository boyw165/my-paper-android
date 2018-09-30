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
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.paper.model.IPreferenceService
import com.paper.model.ISchedulers
import com.paper.model.ModelConst
import com.paper.model.Whiteboard
import com.paper.model.command.WhiteboardCommand
import com.paper.model.repository.sqlite.PaperTable
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.net.URI
import java.util.*

class WhiteboardRepoSQLite(private val authority: String,
                           private val resolver: ContentResolver,
                           private val jsonTranslator: Gson,
                           private val prefs: IPreferenceService,
                           private val schedulers: ISchedulers)
    : IWhiteboardRepository {

    private val paperListRefreshSignal = PublishSubject.create<List<Whiteboard>>().toSerialized()

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

    init {
        val uri: Uri = Uri.Builder()
            .scheme("content")
            .authority(this.authority)
            .path("paper")
            .build()
        this.resolver.registerContentObserver(uri, true, contentObserver)
    }

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
            .subscribeOn(schedulers.db())
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
                    newPaper.modifiedAt = timestamp

                    return@fromCallable newPaper
                }
                .subscribeOn(schedulers.db())
                .observeOn(schedulers.main())
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
                    if (cursor.count == 0) throw IllegalArgumentException("Cannot find paper, commandID=$id")
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
                .subscribeOn(schedulers.db())
                .observeOn(schedulers.main())
        }
    }

    override fun putBoard(board: Whiteboard): Single<Long> {
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

        return Single
            .create { emitter: SingleEmitter<Long> ->
                val id = board.id
                board.modifiedAt = getCurrentTime()

                if (id == ModelConst.TEMP_ID) {
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(authority)
                        .path("paper")
                        .build()
                    if (emitter.isDisposed) return@create

                    try {
                        // Lock paper and convert to database format
                        val values = convertPaperToValues(board)

                        val newURI = resolver.insert(uri, values)
                        if (newURI != null) {
                            // Remember the ID
                            val newID = newURI.lastPathSegment.toLong()
                            prefs.putLong(ModelConst.PREFS_BROWSE_WHITEBOARD_ID, newID).blockingGet()

                            board.id = newID

                            if (!emitter.isDisposed) {
                                println("${ModelConst.TAG}: put paper (commandID=$newID) successfully")

                                emitter.onSuccess(newURI.lastPathSegment.toLong())
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
                        val values = convertPaperToValues(board)

                        if (0 < resolver.update(uri, values, null, null)) {
                            if (!emitter.isDisposed) {
                                println("${ModelConst.TAG}: put paper (commandID=$id) successfully")

                                emitter.onSuccess(id)
                            }

                            // Notify an update just happens
                            resolver.notifyChange(Uri.parse("$uri/$CHANGE_UPDATE"), null)
                        } else {
                            emitter.onError(NoSuchElementException("Cannot find paper with commandID=$id"))
                        }
                    } catch (err: Throwable) {
                        emitter.onError(err)
                    }
                }
            }
            .subscribeOn(schedulers.db())
            .observeOn(schedulers.main())
    }

    override fun duplicateBoardById(id: Long): Single<Long> {
        TODO()
    }

    override fun deleteBoardById(id: Long): Completable {
        return Completable
            .fromCallable {
                val uri = Uri.Builder()
                    .scheme("content")
                    .authority(authority)
                    .path("paper/$id")
                    .build()

                if (0 < resolver.delete(uri, null, null)) {
                    // Notify a deletion just happens
                    resolver.notifyChange(Uri.parse("$uri/$CHANGE_REMOVE"), null)
                } else {
                    throw NoSuchElementException("Cannot delete paper with commandID=$id")
                }
            }
            .subscribeOn(schedulers.db())
            .observeOn(schedulers.main())
    }

    private fun getCurrentTime(): Long = System.currentTimeMillis() / 1000

    private fun convertPaperToValues(paper: Whiteboard): ContentValues {
        val values = ContentValues()

        values.put(PaperTable.COL_UUID, paper.id.toString())
        values.put(PaperTable.COL_CREATED_AT, paper.createdAt)
        values.put(PaperTable.COL_MODIFIED_AT, paper.modifiedAt)
        values.put(PaperTable.COL_CAPTION, paper.getCaption())

        values.put(PaperTable.COL_THUMB_URI, paper.thumbnail.first.toString())

        val (_, thumbWidth, thumbHeight) = paper.thumbnail
        values.put(PaperTable.COL_THUMB_WIDTH, thumbWidth)
        values.put(PaperTable.COL_THUMB_HEIGHT, thumbHeight)

        // The rest part of Paper is converted to JSON
        val json = jsonTranslator.toJson(paper, Whiteboard::class.java)
        values.put(PaperTable.COL_DATA, json)

        return values
    }

    private fun convertCursorToPaper(cursor: Cursor,
                                     fullyRead: Boolean): Whiteboard {
        val whiteboard = Whiteboard(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(PaperTable.COL_ID)),
            uuid = UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_UUID))),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(PaperTable.COL_CREATED_AT)))

        // Mutable modification time stamp
        val colOfModifiedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_MODIFIED_AT)
        whiteboard.modifiedAt = cursor.getLong(colOfModifiedAt)

        // Thumbnail
        val thumbWidth = cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_WIDTH))
        val thumbHeight = cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_HEIGHT))
        whiteboard.thumbnail = Triple(URI(cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_URI))), thumbWidth, thumbHeight)

        if (fullyRead) {
            val detail = jsonTranslator.fromJson(
                cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_DATA)),
                Whiteboard::class.java)

            // Canvas size
            whiteboard.size = detail.size

            // View port
            whiteboard.viewPort = detail.viewPort

            // Scrap
            whiteboard.scraps.addAll(detail.scraps)
        }

        return whiteboard
    }

    // Clazz //////////////////////////////////////////////////////////////////

    companion object {
        private const val CHANGE_ADD = "add"
        private const val CHANGE_REMOVE = "remove"
        private const val CHANGE_UPDATE = "update"
    }
}
