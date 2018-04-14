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

package com.paper.model.repository.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SQLiteHelper(context: Context,
                   name: String,
                   version: Int,
                   dbHelperListener: DbHelperListener)
    : SQLiteOpenHelper(context, name, null, version) {

    val listener: DbHelperListener = dbHelperListener

    override fun onCreate(db: SQLiteDatabase) {
        listener.onDbCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase,
                           oldVersion: Int,
                           newVersion: Int) {
        listener.onDbUpgrade(db, oldVersion, newVersion)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    interface DbHelperListener {

        /**
         * When the database is created.
         */
        fun onDbCreate(db: SQLiteDatabase)

        /**
         * When the database is requested to update.
         */
        fun onDbUpgrade(db: SQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int)
    }
}
