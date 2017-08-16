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
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

class SketchContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun insert(uri: Uri?,
                        values: ContentValues?): Uri {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun query(uri: Uri?,
                       projection: Array<out String>?,
                       selection: String?,
                       selectionArgs: Array<out String>?,
                       sortOrder: String?): Cursor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(uri: Uri?,
                        values: ContentValues?,
                        whereClause: String?,
                        whereArgs: Array<out String>?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(uri: Uri?,
                        selection: String?,
                        selectionArgs: Array<out String>?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType(uri: Uri?): String? {
        return null
    }
}
