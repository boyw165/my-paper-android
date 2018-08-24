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

import android.provider.BaseColumns

object PaperTable {

    // Table names.
    const val TABLE_NAME: String = "paper"

    /**
     * The initial version with basic x, y, scale, rotation, sketch strokes,
     * and scraps.
     */
    internal const val TABLE_VERSION_CODE_10000_BETA = 1
    /**
     * Change STRING to TEXT.
     */
    internal const val TABLE_VERSION_CODE_10001_BETA = 2
    /**
     * Change STRING to TEXT.
     */
    internal const val TABLE_VERSION_CODE_10002_BETA = 3
    /**
     * The SQLite database version.
     */
    const val TABLE_VERSION_CODE = TABLE_VERSION_CODE_10002_BETA

    // Common.
    const val COL_DATA: String = "data"

    // Table columns.
    // TODO: How to handle tags, location, or more?
    const val COL_ID: String = BaseColumns._ID
    const val COL_UUID: String = "uuid"
    const val COL_COUNT: String = BaseColumns._COUNT
    const val COL_CREATED_AT: String = "create_time"
    const val COL_MODIFIED_AT: String = "modify_time"
    const val COL_CAPTION: String = "caption"
    const val COL_TAG: String = "tag"
    const val COL_THUMB_URI: String = "thumb_uri"
    const val COL_THUMB_WIDTH: String = "thumb_width"
    const val COL_THUMB_HEIGHT: String = "thumb_height"
}
