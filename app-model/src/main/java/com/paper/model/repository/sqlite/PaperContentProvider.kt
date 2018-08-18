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

package com.paper.model.repository.sqlite

import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.net.Uri

class PaperContentProvider : ContentProvider(),
                             SQLiteHelper.DbHelperListener {

    companion object {
        // URI.
        private const val MATCHER_CODE_PAPER_ID: Int = 1
        private const val MATCHER_CODE_PAPER_ALL: Int = 2

        // Table commands.
        private const val COMMA: String = ", "
    }

    // SQLite.
    private var mDbHelper: SQLiteHelper? = null

    private var mResolver: ContentResolver? = null
    private var mUriMatcher: UriMatcher? = null

    override fun onCreate(): Boolean {
        val authority = context.packageName
        val dbName = context.packageName

        mUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        mUriMatcher?.addURI(authority, "paper", MATCHER_CODE_PAPER_ALL)
        mUriMatcher?.addURI(authority, "paper/#", MATCHER_CODE_PAPER_ID)

        // TODO: Dynamic DB version?
        mDbHelper = SQLiteHelper(context, dbName, PaperTable.TABLE_VERSION_CODE, this)
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
            "${PaperTable.COL_UUID} TEXT NOT NULL $COMMA" +
            "${PaperTable.COL_CREATED_AT} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_MODIFIED_AT} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_WIDTH} REAL NOT NULL $COMMA" +
            "${PaperTable.COL_HEIGHT} REAL NOT NULL $COMMA" +
            "${PaperTable.COL_CAPTION} TEXT $COMMA" +
            "${PaperTable.COL_TAG} TEXT $COMMA" +
            "${PaperTable.COL_THUMB_PATH} TEXT NOT NULL $COMMA" +
            "${PaperTable.COL_THUMB_WIDTH} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_THUMB_HEIGHT} INTEGER NOT NULL $COMMA" +
            "${PaperTable.COL_DATA} TEXT"

        // Normal table.
        db.execSQL("CREATE TABLE ${PaperTable.TABLE_NAME} ($sharedCommand)")
    }

    override fun onDbUpgrade(db: SQLiteDatabase,
                             oldVersion: Int,
                             newVersion: Int) {
        // Don't support downgrade
        db.execSQL("DROP TABLE IF EXISTS ${PaperTable.TABLE_NAME}")
        onDbCreate(db)

        // e.g. db.execSQL("ALTER TABLE ${PaperTable.TABLE_NAME} ADD COLUMN ${PaperTable.COL_THUMB_DIRTY} INTEGER DEFAULT 0")
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
                val paperId = ContentUris.parseId(uri)

                return db!!.query(PaperTable.TABLE_NAME,
                                  Array(0, { "" }),
                                  "${PaperTable.COL_ID}=$paperId",
                                  null,
                                  null, null,
                                  sortOrder)
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
        val db: SQLiteDatabase = mDbHelper!!.writableDatabase

        when (mUriMatcher!!.match(uri)) {
            MATCHER_CODE_PAPER_ID -> {
                val paperId = ContentUris.parseId(uri)
                val num = db.update(
                    PaperTable.TABLE_NAME,
                    values,
                    // Where clause
                    "${PaperTable.COL_ID}=$paperId",
                    // Where clause args
                    null)

                // Notify the observers.
                mResolver?.notifyChange(uri, null)

                return num
            }
            else -> {
                throw IllegalArgumentException("Unrecognized update URI, $uri.")
            }
        }
    }

    override fun delete(uri: Uri,
                        selection: String?,
                        selectionArgs: Array<out String>?): Int {
        val db: SQLiteDatabase = mDbHelper!!.writableDatabase

        when (mUriMatcher!!.match(uri)) {
            MATCHER_CODE_PAPER_ID -> {
                val paperId = ContentUris.parseId(uri)
                val num = db.delete(
                    PaperTable.TABLE_NAME,
                    // Where clause
                    "${PaperTable.COL_ID}=$paperId",
                    // Where clause args
                    null)

                // Notify the observers.
                mResolver?.notifyChange(uri, null)

                return num
            }
            else -> {
                throw IllegalArgumentException("Unrecognized deletion URI, $uri.")
            }
        }
    }

    override fun getType(uri: Uri): String? = null
}
