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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paper.shared.model.PaperConsts
import com.paper.shared.model.PaperModel
import com.paper.shared.model.repository.json.PaperModelTranslator
import com.paper.shared.model.repository.protocol.IPaperModelRepo
import com.paper.shared.model.repository.sqlite.PaperTable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import java.io.File

class PaperRepo(private val mAuthority: String,
                private val mResolver: ContentResolver,
                private val mCacheDirFile: File,
                private val mIoScheduler: Scheduler) : IPaperModelRepo {

    private val mTempFile by lazy { File(mCacheDirFile, "$mAuthority.temp_paper") }

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
                            PaperTable.COL_WIDTH,
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
        // FIXME: Workaround.
        return if (id == PaperConsts.INVALID_ID) {
            getTempPaper()
        } else {
            TODO()
        }
    }

    override fun putPaperById(id: Long,
                              paper: PaperModel) {
        Observable
            .fromCallable {
                val uri = Uri.Builder()
                    .scheme("content")
                    .authority(mAuthority)
                    .path("paper/$id")
                    .build()

                mResolver.insert(uri, convertPaperToValues(paper))

                return@fromCallable 100
            }
            .subscribeOn(mIoScheduler)
    }

    override fun duplicatePaperById(id: Long): Observable<PaperModel> {
        TODO("not implemented")
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
//                stroke1.add(PathTuple(0f, 0f))
//                stroke1.add(PathTuple(1f, 0.2f))
//                stroke1.add(PathTuple(0.2f, 0.6f))
//
//                val stroke2 = SketchStroke()
//                stroke2.setWidth(0.2f)
//                stroke1.add(PathTuple(0f, 0f))
//                stroke2.add(PathTuple(1f, 0.1f))
//                stroke2.add(PathTuple(0.8f, 0.3f))
//                stroke2.add(PathTuple(0.2f, 0.6f))
//                stroke2.add(PathTuple(0f, 0.9f))
//
//                // Add testing scraps.
//                val scrap1 = ScrapModel()
//                scrap1.x = 0f
//                scrap1.y = 0f
//                scrap1.width = 0.5f
//                scrap1.height = 0.5f
//                scrap1.sketch = Sketch()
//                scrap1.sketch?.addStroke(stroke1)
//
//                val scrap2 = ScrapModel()
//                scrap2.x = 0.2f
//                scrap2.y = 0.3f
//                scrap2.sketch = Sketch()
//                scrap2.sketch?.addStroke(stroke2)
//
//                paper.scraps.add(scrap1)
//                paper.scraps.add(scrap2)

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

    override fun getTempPaper(): Single<PaperModel> {
        return Single
            .fromCallable {
                mTempFile
                    .bufferedReader()
                    .use { reader ->
                        mGson.fromJson(reader, PaperModel::class.java)
                    }
            }
            .subscribeOn(mIoScheduler)
    }

    override fun newTempPaper(caption: String): Single<PaperModel> {
        return Single
            .fromCallable {
                // TODO: Assign default portrait size.
                val timestamp = getCurrentTime()
                val newPaper = PaperModel()
                newPaper.id = PaperConsts.INVALID_ID
                newPaper.createdAt = timestamp
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
        val values = ContentValues()

        values.put(PaperTable.COL_CREATED_AT, paper.createdAt)
        values.put(PaperTable.COL_MODIFIED_AT, paper.modifiedAt)
        values.put(PaperTable.COL_WIDTH, paper.width)
        values.put(PaperTable.COL_HEIGHT, paper.height)
        values.put(PaperTable.COL_CAPTION, paper.caption)

        values.put(PaperTable.COL_THUMB_PATH, paper.thumbnailPath)
        values.put(PaperTable.COL_THUMB_WIDTH, paper.thumbnailWidth)
        values.put(PaperTable.COL_THUMB_HEIGHT, paper.thumbnailHeight)

        val json = mGson.toJson(paper)
        values.put(PaperTable.COL_DATA_BLOB, json)

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

        paper.width = cursor.getFloat(cursor.getColumnIndexOrThrow(PaperTable.COL_WIDTH))
        paper.height = cursor.getFloat(cursor.getColumnIndexOrThrow(PaperTable.COL_HEIGHT))

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
