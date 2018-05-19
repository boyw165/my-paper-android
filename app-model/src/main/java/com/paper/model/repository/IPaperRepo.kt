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

package com.paper.model.repository

import com.paper.model.IPaper
import com.paper.model.event.UpdateDatabaseEvent
import io.reactivex.Observable
import io.reactivex.Single

interface IPaperRepo {

    fun setTmpPaperSize(width: Float, height: Float): Single<Boolean>

    /**
     * Get all papers, and return a stoppable concurrent [Observable] instance.
     * Remaining the subscription to the returned observable lets the observer
     * get notified if there is any new update.
     */
    fun getPapers(isSnapshot: Boolean): Observable<List<IPaper>>

    /**
     * Get specific paper by ID, and return a stoppable concurrent [Single]
     * instance.
     */
    fun getPaperById(id: Long): Single<IPaper>

    /**
     * Put the paper to repository, and return a non-stoppable [Single], which
     * means event you destroy the reactive graph, the writes operation is
     * eventually executed, and you're just not interested to the result.
     */
    fun putPaper(paper: IPaper): Single<UpdateDatabaseEvent>

    fun duplicatePaperById(id: Long): Observable<IPaper>

    fun deletePaperById(id: Long): Single<UpdateDatabaseEvent>
}
