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

package com.paper.shared.model.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.paper.shared.model.PaperConsts
import com.paper.shared.model.PaperModel
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.repository.json.PaperModelTranslator
import com.paper.shared.model.repository.json.ScrapModelTranslator
import com.paper.shared.model.repository.json.SketchModelTranslator
import com.paper.shared.model.repository.protocol.IPaperModelRepo
import com.paper.shared.model.repository.sqlite.PaperTable
import com.paper.shared.model.sketch.SketchModel
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import java.io.File
import java.util.*

class PaperRepo(private val mAuthority: String,
                private val mResolver: ContentResolver,
                private val mCacheDirFile: File,
                private val mDbIoScheduler: Scheduler) : IPaperModelRepo {

    private val mTempFile by lazy { File(mCacheDirFile, "$mAuthority.temp_paper") }

    // JSON translator.
    private val mGson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(PaperModel::class.java,
                                 PaperModelTranslator())
            .registerTypeAdapter(ScrapModel::class.java,
                                 ScrapModelTranslator())
            .registerTypeAdapter(SketchModel::class.java,
                                 SketchModelTranslator())
            .create()
    }

    override fun getPaperSnapshotList(): Observable<List<PaperModel>> {
        return Observable
            .fromCallable {
                val uri: Uri = Uri.Builder()
                    .scheme("content")
                    .authority(mAuthority)
                    .path("paper")
                    .build()
                // Query content provider.
                val cursor = mResolver.query(
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
                cursor.moveToFirst()
                val papers = (1..cursor.count).map {
                    val paper = convertCursorToPaper(cursor, false)
                    cursor.moveToNext()
                    paper
                }
                cursor.close()

                // Return..
                papers
            }
            .subscribeOn(mDbIoScheduler)
    }

    override fun getPaperById(id: Long): Single<PaperModel> {
        // FIXME: Workaround.
        return if (id == PaperConsts.TEMP_ID) {
            getTempPaper()
        } else {
            Single.fromCallable {
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
                            PaperTable.COL_SCRAPS),
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
        }
    }

    override fun putPaperById(id: Long,
                              paper: PaperModel) {
        // TODO: Delegate to a Service or some component that guarantees the
        // TODO: I/O is atomic.
        Observable
            .fromCallable {
                if (id == PaperConsts.TEMP_ID) {
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(mAuthority)
                        .path("paper")
                        .build()

                    mResolver.insert(uri, convertPaperToValues(paper))
                } else {
                    val uri = Uri.Builder()
                        .scheme("content")
                        .authority(mAuthority)
                        .path("paper/$id")
                        .build()

                    mResolver.update(uri, convertPaperToValues(paper), null, null)
                }

                return@fromCallable 100
            }
            .subscribeOn(mDbIoScheduler)
            // FIXME: The insertion might happen after refreshing the list.
            // FIXME: Solution could be using a single thread scheduler.
            .subscribe()
    }

    override fun duplicatePaperById(id: Long): Observable<PaperModel> {
        TODO("not implemented")
    }

    override fun deleteAllPapers(): Observable<Boolean> {
        return Observable
            .fromCallable {
                // TODO: Implement it.
                return@fromCallable true
            }
            .subscribeOn(mDbIoScheduler)
    }

    override fun deletePaperById(id: Long): Observable<Boolean> {
        TODO("not implemented")
    }

    override fun getTestPaper(): Single<PaperModel> {
        return Single
            .fromCallable {
                val paper = PaperModel()

                //                val stroke1 = SketchStroke()
                //                stroke1.setWidth(0.2f)
                //                stroke1.addPathTuple(PathTuple(0f, 0f))
                //                stroke1.addPathTuple(PathTuple(1f, 0.2f))
                //                stroke1.addPathTuple(PathTuple(0.2f, 0.6f))
                //
                //                val stroke2 = SketchStroke()
                //                stroke2.setWidth(0.2f)
                //                stroke1.addPathTuple(PathTuple(0f, 0f))
                //                stroke2.addPathTuple(PathTuple(1f, 0.1f))
                //                stroke2.addPathTuple(PathTuple(0.8f, 0.3f))
                //                stroke2.addPathTuple(PathTuple(0.2f, 0.6f))
                //                stroke2.addPathTuple(PathTuple(0f, 0.9f))
                //
                //                // Add testing scraps.
                //                val scrap1 = ScrapModel()
                //                scrap1.x = 0f
                //                scrap1.y = 0f
                //                scrap1.width = 0.5f
                //                scrap1.height = 0.5f
                //                scrap1.sketch = SketchModel()
                //                scrap1.sketch?.addStroke(stroke1)
                //
                //                val scrap2 = ScrapModel()
                //                scrap2.x = 0.2f
                //                scrap2.y = 0.3f
                //                scrap2.sketch = SketchModel()
                //                scrap2.sketch?.addStroke(stroke2)
                //
                //                paper.scraps.addPathTuple(scrap1)
                //                paper.scraps.addPathTuple(scrap2)

                return@fromCallable paper
            }
            .subscribeOn(mDbIoScheduler)
    }

    override fun hasTempPaper(): Observable<Boolean> {
        return Observable
            .fromCallable {
                mTempFile.exists()
            }
            .subscribeOn(mDbIoScheduler)
    }

    override fun getTempPaper(): Single<PaperModel> {
        return Single
            .fromCallable {
                // Sol#1
                //                mTempFile
                //                    .bufferedReader()
                //                    .use { reader ->
                //                        mGson.fromJson(reader, PaperModel::class.java)
                //                    }

                // Sol#2
                // TODO: Assign default portrait size.
                val timestamp = getCurrentTime()
                val newPaper = PaperModel(
                    createdAt = timestamp)
                newPaper.modifiedAt = timestamp

                // Return..
                newPaper
            }
            .subscribeOn(mDbIoScheduler)
    }

    override fun newTempPaper(caption: String): Single<PaperModel> {
        return Single
            .fromCallable {
                // TODO: Assign default portrait size.
                val timestamp = getCurrentTime()
                val newPaper = PaperModel()
                newPaper.modifiedAt = timestamp
                newPaper.caption = caption

                val json = mGson.toJson(newPaper)
                // TODO: Open an external file and write json to it.
                mTempFile
                    .bufferedWriter()
                    .use { out ->
                        out.write(json)
                    }

                // Return..
                newPaper
            }
            .subscribeOn(mDbIoScheduler)
    }

    override fun newTempPaper(other: PaperModel): Observable<PaperModel> {
        TODO("not implemented")
    }

    override fun removeTempPaper(): Observable<Boolean> {
        return Observable
            .fromCallable {
                if (mTempFile.exists()) {
                    mTempFile.delete()
                }

                true
            }
    }

    override fun commitTempPaper(): Observable<PaperModel> {
        TODO("not implemented")
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

        values.put(PaperTable.COL_THUMB_PATH, paper.thumbnailPath)
        values.put(PaperTable.COL_THUMB_WIDTH, paper.thumbnailWidth)
        values.put(PaperTable.COL_THUMB_HEIGHT, paper.thumbnailHeight)

        // Scraps.
        paper.scraps.let { scraps ->
            val json = JsonObject()
            val jsonScraps = JsonArray()

            json.add(PaperTable.COL_DATA, jsonScraps)
            scraps.forEach { scrap ->
                jsonScraps.add(mGson.toJsonTree(scrap))
            }

            values.put(PaperTable.COL_SCRAPS, json.toString())
            Log.d("xyz", json.toString())
        }

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

        paper.thumbnailPath = cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_PATH))
        paper.thumbnailWidth = cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_WIDTH))
        paper.thumbnailHeight = cursor.getInt(cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_HEIGHT))

        if (fullyRead) {
            // Scraps
            val jsonString = cursor.getString(cursor.getColumnIndexOrThrow(PaperTable.COL_SCRAPS))
            val json = mGson.fromJson(jsonString, JsonObject::class.java)
            json.get(PaperTable.COL_DATA).asJsonArray.forEach { element ->
                paper.scraps.add(mGson.fromJson(element, ScrapModel::class.java))
            }
        }

        return paper
    }
}
