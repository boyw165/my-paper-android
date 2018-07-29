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

package com.paper.model

import com.paper.model.event.DirtyEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * An observable dirty flag.
 */
open class DirtyFlag(open var flag: Int = 0) {

    private val mLock = Any()

    open fun markDirty(vararg types: Int) {
        synchronized(mLock) {
            types.forEach { type ->
                flag = flag.or(type)
                mFlagSignal.onNext(DirtyEvent(flag = flag))
            }
        }
    }

    open fun markNotDirty(vararg types: Int) {
        synchronized(mLock) {
            types.forEach { type ->
                flag = flag.and(type.inv())
                mFlagSignal.onNext(DirtyEvent(flag = flag))
            }
        }
    }

    fun isDirty(vararg types: Int): Boolean {
        synchronized(mLock) {
            return DirtyFlag.isDirty(flag = this.flag,
                                     types = *types)
        }
    }

    private val mFlagSignal = PublishSubject.create<DirtyEvent>().toSerialized()

    fun onUpdate(): Observable<DirtyEvent> {
        return mFlagSignal
    }

    companion object {

        @JvmStatic
        fun isDirty(flag: Int, vararg types: Int): Boolean {
            var dirty = false
            types.forEach { type ->
                dirty = dirty.or(flag.and(type) != 0)
            }
            return dirty
        }
    }
}
