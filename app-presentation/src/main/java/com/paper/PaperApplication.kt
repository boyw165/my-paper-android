// Copyright Feb 2018-present boyw165@gmail.com
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

package com.paper

import android.content.Context
import android.support.multidex.MultiDexApplication
import com.facebook.drawee.backends.pipeline.Fresco
import com.paper.domain.IDatabaseIOSchedulerProvider
import com.paper.domain.IPaperRepoProvider
import com.paper.domain.ISharedPreferenceService
import com.paper.model.repository.PaperRepo
import com.paper.model.repository.protocol.IPaperModelRepo
import io.reactivex.Scheduler
import io.reactivex.internal.schedulers.SingleScheduler

class PaperApplication : MultiDexApplication(),
                         IDatabaseIOSchedulerProvider,
                         IPaperRepoProvider,
                         ISharedPreferenceService {

    override fun onCreate() {
        super.onCreate()

        Fresco.initialize(this)
    }

    // Repository and scheduler ///////////////////////////////////////////////

    // Database.
    private val mPaperRepo: PaperRepo by lazy {
        PaperRepo(packageName,
                                                    contentResolver,
                                                    externalCacheDir,
                                                    getScheduler())
    }

    override fun getRepo(): IPaperModelRepo {
        return mPaperRepo
    }

    private val mDbScheduler = SingleScheduler()

    override fun getScheduler(): Scheduler {
        return mDbScheduler
    }

    // Shared preference //////////////////////////////////////////////////////

    // Shared preference.
    private val mPreferences by lazy { getSharedPreferences(packageName, Context.MODE_PRIVATE) }

    override fun putString(key: String, value: String) {
        mPreferences
            .edit()
            .putString(key, value)
            .apply()
    }

    override fun getString(key: String, defaultValue: String): String {
        return mPreferences.getString(key, defaultValue)
    }

    override fun putInt(key: String, value: Int) {
        mPreferences
            .edit()
            .putInt(key, value)
            .apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return mPreferences.getInt(key, defaultValue)
    }
}
