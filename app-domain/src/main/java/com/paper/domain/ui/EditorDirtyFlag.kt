// Copyright Jun 2018-present Paper
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

package com.paper.domain.ui

import androidx.annotation.IntDef
import io.reactivex.Observable
import io.useful.dirtyflag.DirtyEvent
import io.useful.dirtyflag.DirtyFlag

/**
 * Dirty flag for editor.
 */
data class EditorDirtyFlag(override var flag: Int = 0)
    : DirtyFlag(flag) {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(READ_PAPER_FROM_REPO,
            WRITE_PAPER_TO_REPO,
            EXPORT_PAPER_THUMBNAIL)
    annotation class Type

    companion object {
        const val READ_PAPER_FROM_REPO = 1.shl(0)
        const val WRITE_PAPER_TO_REPO = 1.shl(1)
        const val EXPORT_PAPER_THUMBNAIL = 1.shl(2)
    }

    override fun markDirty(@Type vararg types: Int) {
        super.markDirty(*types)
    }

    override fun markNotDirty(@Type vararg types: Int) {
        super.markNotDirty(*types)
    }

    override fun onUpdate(@Type vararg withTypes: Int): Observable<DirtyEvent> {
        return super.onUpdate(*withTypes)
    }
}
