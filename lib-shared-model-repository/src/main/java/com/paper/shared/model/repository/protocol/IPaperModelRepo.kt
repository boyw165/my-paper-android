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

package com.paper.shared.model.repository.protocol

import com.paper.shared.model.PaperModel
import io.reactivex.Observable
import io.reactivex.Single

interface IPaperModelRepo {

    // For persistent store.

    fun getPaperSnapshotList(): Observable<List<PaperModel>>

    fun getPaperById(id: Long): Observable<PaperModel>

    fun duplicatePaperById(id: Long): Observable<PaperModel>

    fun deletePaperById(id: Long): Observable<Boolean>

    // For testing data.

    fun getTestPaper(): Single<PaperModel>

    // For temporary store.

    fun hasTempPaper(): Observable<Boolean>

    fun getTempPaper(): Observable<PaperModel>

    /**
     * There is only one inventory for the temporary paper.
     */
    fun newTempPaper(caption: String): Observable<PaperModel>

    /**
     * There is only one inventory for the temporary paper.
     */
    fun newTempPaper(other: PaperModel): Observable<PaperModel>

    fun removeTempPaper(): Observable<Boolean>

    fun commitTempPaper(): Observable<PaperModel>
}
