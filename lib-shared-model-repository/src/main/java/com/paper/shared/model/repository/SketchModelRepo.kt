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

import com.paper.shared.model.PaperModel
import com.paper.shared.model.sketch.SketchModel

import io.reactivex.Single
import io.realm.Realm

class SketchModelRepo {

    fun addTempSketch(data: SketchModel) {
        // TODO: Complete it.
    }

    // TODO: Complete it.
    val tempSketch: Single<PaperModel>?
        get() = null

    fun containsSketch(data: SketchModel): Single<Boolean>? {
        // TODO: Complete it.
        return null
    }

    fun addSketch(data: SketchModel) {
        // TODO: Complete it.
    }

    fun removeSketch(data: SketchModel) {
        // TODO: Complete it.
    }

    fun updateSketch(data: SketchModel) {
        // TODO: Complete it.
    }
}
