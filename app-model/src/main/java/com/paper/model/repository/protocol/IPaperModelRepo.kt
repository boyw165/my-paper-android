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

package com.paper.model.repository.protocol

import android.graphics.Bitmap
import com.paper.model.PaperModel
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File

interface IPaperModelRepo {

    // For persistent store.

    fun getPapers(isSnapshot: Boolean): Observable<PaperModel>

    fun getPaperById(id: Long): Single<PaperModel>

    fun putPaperById(id: Long, paper: PaperModel): Single<Boolean>

    fun duplicatePaperById(id: Long): Observable<PaperModel>

    fun deleteAllPapers(): Observable<Boolean>

    fun deletePaperById(id: Long): Observable<Boolean>

    fun putBitmap(bmp: Bitmap): Observable<File>

    // For testing data.

    fun getTestPaper(): Single<PaperModel>

    // For temporary store.

    fun hasTempPaper(): Observable<Boolean>

    fun getTempPaper(): Single<PaperModel>

    /**
     * There is only one inventory for the temporary paper.
     */
    fun newTempPaper(caption: String): Single<PaperModel>

    /**
     * There is only one inventory for the temporary paper.
     */
    fun newTempPaper(other: PaperModel): Observable<PaperModel>

    fun removeTempPaper(): Observable<Boolean>

    fun commitTempPaper(): Observable<PaperModel>
}
