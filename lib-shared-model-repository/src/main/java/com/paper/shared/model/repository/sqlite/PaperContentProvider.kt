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

package com.paper.shared.model.repository.sqlite

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.net.Uri

class PaperContentProvider : ContentProvider(), SQLiteHelper.DbHelperListener {

    // SQLite.
    private var mDbHelper: SQLiteHelper? = null
    // Resolver.
    private var mResolver: ContentResolver? = null

    // URI.
    private val MATCHER_CODE_PAPER_ID: Int = 1
    private val MATCHER_CODE_PAPER_ALL: Int = 2

    private var mUriMatcher: UriMatcher? = null

    // Table commands.
    private val COMMA: String = ", "

    override fun onCreate(): Boolean {
        val authority = context.packageName
        val dbName = context.packageName

        mUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        mUriMatcher?.addURI(authority, "paper", MATCHER_CODE_PAPER_ALL)
        mUriMatcher?.addURI(authority, "paper/#", MATCHER_CODE_PAPER_ID)

        // TODO: Dynamic DB version?
        mDbHelper = SQLiteHelper(context, dbName, 1, this)
        mResolver = context.contentResolver

        return true
    }

    override fun shutdown() {
        super.shutdown()

        mDbHelper?.close()
    }

    override fun onDbCreate(db: SQLiteDatabase) {
        val sharedCommand: String =
            "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT $COMMA" +
            "${PaperTable.COL_CREATED_AT} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_MODIFIED_AT} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_WIDTH_OVER_HEIGHT} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_CAPTION} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_THUMB_PATH} STRING NOT NULL $COMMA" +
            "${PaperTable.COL_THUMB_WIDTH} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_THUMB_HEIGHT} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_DATA_BLOB} BLOB NOT NULL"

        // Normal table.
        db.execSQL("create table ${PaperTable.TABLE_NAME} ($sharedCommand)")
        // Temporary table.
        db.execSQL("create table ${PaperTable.TABLE_NAME_TEMP} ($sharedCommand)")
    }

    override fun onDbUpgrade(db: SQLiteDatabase,
                             oldVersion: Int,
                             newVersion: Int) {
        TODO("onDbUpgrade")
    }

    override fun insert(uri: Uri,
                        values: ContentValues?): Uri {
        val db: SQLiteDatabase = mDbHelper!!.writableDatabase

        when (mUriMatcher!!.match(uri)) {
            MATCHER_CODE_PAPER_ALL -> {
                val newId: Long = db.insert(PaperTable.TABLE_NAME, null, values)
                if (newId == -1L) {
                    throw SQLiteDatabaseCorruptException("Cannot insert data.")
                }
                val newUri: Uri = Uri.parse("$uri/$newId")

                // Notify the observers.
                mResolver?.notifyChange(newUri, null)

                return newUri
            }
            else -> {
                throw IllegalArgumentException("Unrecognized insert URI, $uri.")
            }
        }
    }

    override fun query(uri: Uri,
                       projection: Array<out String>?,
                       selection: String?,
                       selectionArgs: Array<out String>?,
                       sortOrder: String?): Cursor {
        val db: SQLiteDatabase? = mDbHelper?.readableDatabase

        when (mUriMatcher!!.match(uri)) {
            MATCHER_CODE_PAPER_ID -> {
                throw RuntimeException("Yet implemented!")
            }
            MATCHER_CODE_PAPER_ALL -> {
                return db!!.query(PaperTable.TABLE_NAME,
                                  projection,
                                  selection,
                                  selectionArgs,
                                  null, null,
                                  sortOrder)
            }
            else -> {
                throw SQLiteDatabaseCorruptException("Cannot query $uri")
            }
        }

    }

    override fun update(uri: Uri,
                        values: ContentValues?,
                        whereClause: String?,
                        whereArgs: Array<out String>?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(uri: Uri,
                        selection: String?,
                        selectionArgs: Array<out String>?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType(uri: Uri): String? = null
}
