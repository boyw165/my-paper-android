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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paper.shared.model.PaperModel
import com.paper.shared.model.repository.json.SketchModelTranslator
import com.paper.shared.model.repository.protocol.ISketchModelRepo
import com.paper.shared.model.repository.sqlite.PaperTable
import com.paper.shared.model.repository.sqlite.SketchTable
import com.paper.shared.model.sketch.SketchModel
import io.reactivex.Scheduler
import io.reactivex.Single
import java.io.File

class SketchModelRepo(authority: String,
                      resolver: ContentResolver,
                      cacheDirFile: File,
                      ioScheduler: Scheduler) : ISketchModelRepo {

    // Given...
    private val mAuthority: String = authority
    private val mResolver: ContentResolver = resolver
    private val mCacheDirFile = cacheDirFile
    private val mTempFile = File(cacheDirFile, authority + ".temp_sketch")
    private val mIoScheduler: Scheduler = ioScheduler

    // JSON translator.
    private val mGson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(SketchModel::class.java,
                                 SketchModelTranslator())
            .create()
    }

    override fun getSketchById(id: Long): Single<SketchModel> {
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

    override fun getTempPaper(): Single<SketchModel> {
        return Single
            .fromCallable {
                var sketch: SketchModel? = null

                mTempFile
                    .bufferedReader()
                    .use { reader ->
                        sketch = mGson.fromJson(reader, SketchModel::class.java)
                    }

                // Return.
                sketch!!
            }
            .subscribeOn(mIoScheduler)
    }

    override fun newTempSketch(width: Int,
                               height: Int): Single<SketchModel> {
        return Single
            .fromCallable {
                val newSketch = SketchModel(width, height)
                val json = mGson.toJson(newSketch)

                mTempFile
                    .bufferedWriter()
                    .use { out ->
                        out.write(json)
                    }

                // Return.
                newSketch
            }
            .subscribeOn(mIoScheduler)
    }

    override fun newTempPaper(other: SketchModel): Single<SketchModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun commitTempPaper(): Single<SketchModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun convertSketchToValues(sketch: SketchModel): ContentValues {
        val values = ContentValues()

        values.put(SketchTable.COL_WIDTH, sketch.width)
        values.put(SketchTable.COL_HEIGHT, sketch.height)

        // FIXME:
        values.put(SketchTable.COL_THUMB_PATH, "")
        values.put(SketchTable.COL_THUMB_WIDTH, 0)
        values.put(SketchTable.COL_THUMB_HEIGHT, 0)
        // FIXME:
        values.put(SketchTable.COL_DATA_BLOB, "")

        return values
    }

    private fun convertCursorToSketch(cursor: Cursor): SketchModel {
        val paper = SketchModel(0, 0)

//        val colOfId = cursor.getColumnIndexOrThrow(PaperTable.COL_ID)
//        paper.id = cursor.getLong(colOfId)
//
//        val colOfCreatedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_CREATED_AT)
//        paper.createdAt = cursor.getLong(colOfCreatedAt)
//
//        val colOfModifiedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_MODIFIED_AT)
//        paper.modifiedAt = cursor.getLong(colOfModifiedAt)
//
//        val colOfWidth = cursor.getColumnIndexOrThrow(PaperTable.COL_WIDTH)
//        paper.width = cursor.getInt(colOfWidth)
//
//        val colOfHeight = cursor.getColumnIndexOrThrow(PaperTable.COL_HEIGHT)
//        paper.height = cursor.getInt(colOfHeight)
//
//        val colOfCaption = cursor.getColumnIndexOrThrow(PaperTable.COL_CAPTION)
//        paper.caption = cursor.getString(colOfCaption)
//
//        val colOfThumb = cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_PATH)
//        paper.thumbnailPath = cursor.getString(colOfThumb)
//
//        val colOfThumbWidth = cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_WIDTH)
//        paper.thumbnailWidth = cursor.getInt(colOfThumbWidth)
//
//        val colOfThumbHeight = cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_HEIGHT)
//        paper.thumbnailHeight = cursor.getInt(colOfThumbHeight)

        return paper
    }
}
