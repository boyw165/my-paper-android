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
import com.paper.shared.model.repository.json.PaperModelTranslator
import com.paper.shared.model.repository.protocol.IPaperModelRepo
import com.paper.shared.model.repository.sqlite.PaperTable
import io.reactivex.Scheduler
import io.reactivex.Single
import java.io.File

class PaperModelRepo(authority: String,
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

    override fun getPaperSnapshotList(): Single<List<PaperModel>> {
        return Single
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

    override fun getPaperById(id: Long): Single<PaperModel> {
        TODO("not implemented")
    }

    override fun duplicatePaperById(id: Long): Single<PaperModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deletePaperById(id: Long): Single<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasTempPaper(): Single<Boolean> {
        return Single
            .fromCallable {
                mTempFile.exists()
            }
            .subscribeOn(mIoScheduler)
    }

    override fun getTempPaper(): Single<PaperModel> {
        return Single
            .fromCallable {
                var paper: PaperModel? = null
                mTempFile
                    .bufferedReader()
                    .use { reader ->
                        paper = mGson.fromJson(reader, PaperModel::class.java)
                    }

                // Return.
                paper!!
            }
            .subscribeOn(mIoScheduler)
    }

    override fun newTempPaper(caption: String): Single<PaperModel> {
        return Single
            .fromCallable {
                // TODO: Assign default portrait size.
                val timestamp = getCurrentTime()
                val newPaper = PaperModel()
                newPaper.createdAt = timestamp
                newPaper.modifiedAt = timestamp
                newPaper.width = 210
                newPaper.height = 297
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

    override fun newTempPaper(other: PaperModel): Single<PaperModel> {
        TODO("not implemented")
    }

    override fun removeTempPaper(): Single<Boolean> {
        return Single
            .fromCallable {
                if (mTempFile.exists()) {
                    mTempFile.delete()
                }

                true
            }
    }

    override fun commitTempPaper(): Single<PaperModel> {
        TODO("not implemented")
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun getCurrentTime(): Long = System.currentTimeMillis() / 1000

    private fun convertPaperToValues(paper: PaperModel): ContentValues {
        val values: ContentValues = ContentValues()

        values.put(PaperTable.COL_CREATED_AT, paper.createdAt)
        values.put(PaperTable.COL_MODIFIED_AT, paper.modifiedAt)
        values.put(PaperTable.COL_WIDTH, paper.width)
        values.put(PaperTable.COL_HEIGHT, paper.height)
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

        val colOfWidth = cursor.getColumnIndexOrThrow(PaperTable.COL_WIDTH)
        paper.width = cursor.getInt(colOfWidth)

        val colOfHeight = cursor.getColumnIndexOrThrow(PaperTable.COL_HEIGHT)
        paper.height = cursor.getInt(colOfHeight)

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
