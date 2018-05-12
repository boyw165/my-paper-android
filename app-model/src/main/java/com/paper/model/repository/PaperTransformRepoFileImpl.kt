// Copyright Apr 2018-present Paper
//
// Author: boyw165@gmail.com
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

package com.paper.model.repository

import com.paper.model.IPaperTransform
import com.paper.model.IPaperTransformRepo
import com.paper.model.ModelConst
import io.reactivex.Single
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class PaperTransformRepoFileImpl(fileDir: File) : IPaperTransformRepo {

    // TODO: Serialize the transform to file

    private val mLookupTable = HashMap<UUID, IPaperTransform>()

    override fun putRecord(key: UUID,
                           transform: IPaperTransform): Single<Boolean> {
        mLookupTable[key] = transform

        println("${ModelConst.TAG}: put $transform to transformation repo (file impl)")

        return Single.just(true)
    }

    override fun getRecord(key: UUID): Single<IPaperTransform> {
        val transform = mLookupTable[key]

        println("${ModelConst.TAG}: get $transform from transformation repo (file impl)")

        return Single.just(transform)
    }

    override fun toString(): String {
        val builder = StringBuilder("${javaClass.simpleName} {\n")

        val values = mLookupTable.values.toList()
        values.forEachIndexed { i, x ->
            val separator = if (i == values.lastIndex) {
                ""
            } else {
                ","
            }
            builder.append("    $x$separator\n")
        }

        builder.append("}")

        return builder.toString()
    }

    val recordSize: Int
        get() = mLookupTable.size
}
