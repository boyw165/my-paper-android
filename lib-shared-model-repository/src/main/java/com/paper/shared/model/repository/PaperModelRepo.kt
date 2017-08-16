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
import com.paper.shared.model.PaperModel
import com.paper.shared.model.repository.sqlite.PaperTable
import io.reactivex.Scheduler
import io.reactivex.Single

class PaperModelRepo(resolver: ContentResolver,
                     ioScheduler: Scheduler) {

    private val mResolver: ContentResolver = resolver
    private val mIoScheduler: Scheduler = ioScheduler

    val paperSnapshotList: Single<List<PaperModel>>
        get() {
            return Single
                .fromCallable {
                    val uri: Uri = Uri.Builder()
                        .scheme("content")
                        .authority("com.paper")
                        .path("paper")
                        .build()
                    // Query database.
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
                    val papers = (1 .. cursor.count).map {
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

    fun newTempPaper(): Single<PaperModel> {
        return Single
            .fromCallable {
                val newPaper: PaperModel = PaperModel()
                val uri: Uri = Uri.Builder()
                    .scheme("content")
                    .authority("com.paper")
                    // FIXME: To temp table.
                    .path("paper")
                    .build()

                val newUri: Uri = mResolver.insert(uri, convertPaperToValues(newPaper))
                val newId: Long = newUri.lastPathSegment.toLong()

                // Most IMPORTANTLY, assign newly generated ID to the model in memory.
                newPaper.id = newId

                // Return..
                newPaper
            }
            .subscribeOn(mIoScheduler)
    }

    fun addPaper(data: PaperModel): Single<Any>? {
        // TODO: Complete it.
        return null
    }

    fun removePaper(data: PaperModel): Single<Any>? {
        // TODO: Complete it.
        return null
    }

    fun updatePaper(data: PaperModel): Single<Any>? {
        // TODO: Complete it.
        return null
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun convertPaperToValues(paper: PaperModel): ContentValues {
        val values: ContentValues = ContentValues()
        val timestamp: Long = System.currentTimeMillis() / 1000

        values.put(PaperTable.COL_CREATED_AT, timestamp)
        values.put(PaperTable.COL_MODIFIED_AT, timestamp)
        values.put(PaperTable.COL_WIDTH, paper.baseWidth)
        values.put(PaperTable.COL_HEIGHT, paper.baseHeight)
        values.put(PaperTable.COL_CAPTION, paper.caption)

        // FIXME:
        values.put(PaperTable.COL_THUMB_PATH, "")
        values.put(PaperTable.COL_THUMB_WIDTH, paper.thumbnailWidth)
        values.put(PaperTable.COL_THUMB_HEIGHT, paper.thumbnailHeight)
        // FIXME:
        values.put(PaperTable.COL_BLOB, "")

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
        paper.baseWidth = cursor.getInt(colOfWidth)

        val colOfHeight = cursor.getColumnIndexOrThrow(PaperTable.COL_HEIGHT)
        paper.baseHeight = cursor.getInt(colOfHeight)

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
