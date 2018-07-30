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
import io.reactivex.functions.Predicate
import io.reactivex.subjects.BehaviorSubject

/**
 * A thread-safe and observable dirty flag.
 */
open class DirtyFlag(protected open var flag: Int = 0) {

    private val mLock = Any()

    /**
     * Mark the given types dirty.
     */
    open fun markDirty(vararg types: Int) {
        synchronized(mLock) {
            types.forEach { type ->
                flag = flag.or(type)
                mFlagSignal.onNext(DirtyEvent(flag = flag,
                                              changedType = type))
            }
        }
    }

    /**
     * Mark the given types not dirty.
     */
    open fun markNotDirty(vararg types: Int) {
        synchronized(mLock) {
            types.forEach { type ->
                flag = flag.and(type.inv())
                mFlagSignal.onNext(DirtyEvent(flag = flag,
                                              changedType = type))
            }
        }
    }

    /**
     * To know the given types are all dirty or not.
     */
    fun isDirty(vararg types: Int): Boolean {
        synchronized(mLock) {
            return DirtyFlag.isDirty(flag = this.flag,
                                     types = *types)
        }
    }

    protected val mFlagSignal = BehaviorSubject.create<DirtyEvent>().toSerialized()

    /**
     * Observe the update of the flag, where you could assign the particular types
     * and get notified with the changes corresponding the the types.
     */
    open fun onUpdate(vararg withTypes: Int): Observable<DirtyEvent> {
        return if (withTypes.isNotEmpty()) {
            // Prepare the mask for only showing the cared types to provide the
            // separate flag environment
            var mask = 0
            withTypes.forEach { mask = mask.or(it) }

            val withTypesArray = Array(withTypes.size) { i -> withTypes[i] }
            val filter = Predicate<DirtyEvent> { event ->
                withTypesArray.contains(event.changedType)
            }
            mFlagSignal
                .filter(filter)
                .map { event -> event.copy(flag = event.flag.and(mask)) }
        } else {
            mFlagSignal
        }
    }

    companion object {

        /**
         * A util method for checking if the types in flag are dirty or not.
         */
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
