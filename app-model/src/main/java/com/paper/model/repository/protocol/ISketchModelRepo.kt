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

import com.paper.model.sketch.SketchModel
import io.reactivex.Observable

interface ISketchModelRepo {

    // For persistent store.

    fun getSketchById(id: Long): Observable<SketchModel>

    fun deletePaperById(id: Long): Observable<Boolean>

    // For temporary store.

    fun hasTempSketch(): Observable<Boolean>

    fun getTempSketch(): Observable<SketchModel>

    /**
     * There is only one inventory for the temporary paper.
     */
    fun newTempSketch(width: Int, height: Int): Observable<SketchModel>

    /**
     * There is only one inventory for the temporary paper.
     */
    fun newTempSketch(other: SketchModel): Observable<SketchModel>

    /**
     * There is only one inventory for the temporary paper.
     */
    fun saveTempSketch(other: SketchModel): Observable<SketchModel>

    fun commitTempSketch(): Observable<SketchModel>
}
