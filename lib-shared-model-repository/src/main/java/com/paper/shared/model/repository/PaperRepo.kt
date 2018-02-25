//  Copyright Aug 2017-present boyw165@gmail.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.paper.shared.model.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paper.shared.model.PaperModel
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.repository.json.PaperModelTranslator
import com.paper.shared.model.repository.protocol.IPaperModelRepo
import com.paper.shared.model.repository.sqlite.PaperTable
import com.paper.shared.model.sketch.PathTuple
import com.paper.shared.model.sketch.Sketch
import com.paper.shared.model.sketch.SketchStroke
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import java.io.File

class PaperRepo(authority: String,
                resolver: ContentResolver,
                cacheDirFile: File,
                ioScheduler: Scheduler) : IPaperModelRepo {

    // Given...
    private val mAuthority = authority
    private val mResolver = resolver
    private val mCacheDirFile = cacheDirFile
    private val mTempFile = File(cacheDirFile, authority + ".temp_paper")
    private val mIoScheduler = ioScheduler

    // JSON translator.
    private val mGson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(PaperModel::class.java,
                                 PaperModelTranslator())
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
                            PaperTable.COL_CREATED_AT,
                            PaperTable.COL_MODIFIED_AT,
                            PaperTable.COL_WIDTH_OVER_HEIGHT,
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
                    val paper = convertCursorToPaper(cursor)
                    cursor.moveToNext()
                    paper
                }
                cursor.close()

                // Return..
                papers
            }
            .subscribeOn(mIoScheduler)
    }

    override fun getPaperById(id: Long): Observable<PaperModel> {
        TODO("not implemented")
    }

    override fun duplicatePaperById(id: Long): Observable<PaperModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deletePaperById(id: Long): Observable<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTestPaper(): Single<PaperModel> {
        return Single
            .fromCallable {
                val paper = PaperModel()

                paper.widthOverHeight = 1f

                val stroke1 = SketchStroke()
                stroke1.setWidth(0.2f)
                stroke1.add(PathTuple(0f, 0f))
                stroke1.add(PathTuple(1f, 0.2f))
                stroke1.add(PathTuple(0.2f, 0.6f))

                val stroke2 = SketchStroke()
                stroke2.setWidth(0.2f)
                stroke2.add(PathTuple(0.2f, 0.6f))
                stroke2.add(PathTuple(1f, 0.2f))
                stroke2.add(PathTuple(0f, 0f))

                // Add testing scraps.
                val scrap1 = ScrapModel(0)
                scrap1.x = 0f
                scrap1.y = 0f
                scrap1.width = 0.25f
                scrap1.height = 0.5f
                scrap1.sketch = Sketch()
                scrap1.sketch?.addStroke(stroke1)

                val scrap2 = ScrapModel(1)
                scrap2.x = 0.2f
                scrap2.y = 0.3f
                scrap2.width = 0.6f
                scrap2.height = 0.4f
                scrap2.sketch = Sketch()
                scrap2.sketch?.addStroke(stroke2)

                paper.scraps.add(scrap1)
                paper.scraps.add(scrap2)

                return@fromCallable paper
            }
            .subscribeOn(mIoScheduler)
    }

    override fun hasTempPaper(): Observable<Boolean> {
        return Observable
            .fromCallable {
                mTempFile.exists()
            }
            .subscribeOn(mIoScheduler)
    }

    override fun getTempPaper(): Observable<PaperModel> {
        return Observable
            .fromCallable {
                mTempFile
                    .bufferedReader()
                    .use { reader ->
                        mGson.fromJson(reader, PaperModel::class.java)
                    }
            }
            .subscribeOn(mIoScheduler)
    }

    override fun newTempPaper(caption: String): Observable<PaperModel> {
        return Observable
            .fromCallable {
                // TODO: Assign default portrait size.
                val timestamp = getCurrentTime()
                val newPaper = PaperModel()
                newPaper.createdAt = timestamp
                newPaper.modifiedAt = timestamp
                //                newPaper.width = 210
                //                newPaper.height = 297
                newPaper.widthOverHeight = 840f / 1188f
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
            .subscribeOn(mIoScheduler)
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
        val values: ContentValues = ContentValues()

        values.put(PaperTable.COL_CREATED_AT, paper.createdAt)
        values.put(PaperTable.COL_MODIFIED_AT, paper.modifiedAt)
        values.put(PaperTable.COL_WIDTH_OVER_HEIGHT, paper.widthOverHeight)
        values.put(PaperTable.COL_CAPTION, paper.caption)

        // FIXME:
        values.put(PaperTable.COL_THUMB_PATH, "")
        values.put(PaperTable.COL_THUMB_WIDTH, paper.thumbnailWidth)
        values.put(PaperTable.COL_THUMB_HEIGHT, paper.thumbnailHeight)
        // FIXME:
        values.put(PaperTable.COL_DATA_BLOB, "")

        return values
    }

    private fun convertCursorToPaper(cursor: Cursor): PaperModel {
        val paper = PaperModel()

        val colOfId = cursor.getColumnIndexOrThrow(PaperTable.COL_ID)
        paper.id = cursor.getLong(colOfId)

        val colOfCreatedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_CREATED_AT)
        paper.createdAt = cursor.getLong(colOfCreatedAt)

        val colOfModifiedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_MODIFIED_AT)
        paper.modifiedAt = cursor.getLong(colOfModifiedAt)

        val colOfWidthOverHeight = cursor.getColumnIndexOrThrow(PaperTable.COL_WIDTH_OVER_HEIGHT)
        paper.widthOverHeight = cursor.getFloat(colOfWidthOverHeight)

        val colOfCaption = cursor.getColumnIndexOrThrow(PaperTable.COL_CAPTION)
        paper.caption = cursor.getString(colOfCaption)

        val colOfThumb = cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_PATH)
        paper.thumbnailPath = cursor.getString(colOfThumb)

        val colOfThumbWidth = cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_WIDTH)
        paper.thumbnailWidth = cursor.getInt(colOfThumbWidth)

        val colOfThumbHeight = cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_HEIGHT)
        paper.thumbnailHeight = cursor.getInt(colOfThumbHeight)

        return paper
    }
}
