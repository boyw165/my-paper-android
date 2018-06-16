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
import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.multidex.MultiDexApplication
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.firebase.FirebaseApp
import com.paper.domain.IBitmapRepoProvider
import com.paper.domain.IDatabaseIOSchedulerProvider
import com.paper.domain.IPaperRepoProvider
import com.paper.domain.IPaperTransformRepoProvider
import com.paper.model.IPaperTransformRepo
import com.paper.model.ISharedPreferenceService
import com.paper.model.repository.IBitmapRepo
import com.paper.model.repository.IPaperRepo
import com.paper.model.repository.PaperRepoSqliteImpl
import com.paper.model.repository.PaperTransformRepoFileImpl
import io.reactivex.Scheduler
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.plugins.RxJavaPlugins

class PaperApplication : MultiDexApplication(),
                         IDatabaseIOSchedulerProvider,
                         IPaperRepoProvider,
                         IPaperTransformRepoProvider,
                         IBitmapRepoProvider,
                         ISharedPreferenceService {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Fresco.initialize(this)

        // RxJava global exception
        if (!BuildConfig.DEBUG) {
            RxJavaPlugins.setErrorHandler { err ->
                err.printStackTrace()
            }
        }

        // Enable the strict mode for DEBUG
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    // Temporarily disable this because there're too many
                    // system calls implicitly do IO.
                    //.detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyFlashScreen()
                    .build())
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())
        }
    }

    // Repository and scheduler ///////////////////////////////////////////////

    private val mPaperRepo by lazy {
        PaperRepoSqliteImpl(authority = packageName,
                            resolver = contentResolver,
                            fileDir = getExternalFilesDir("media"),
                            prefs = this@PaperApplication,
                            dbIoScheduler = getScheduler())
    }

    override fun getPaperRepo(): IPaperRepo {
        return mPaperRepo
    }

    override fun getBitmapRepo(): IBitmapRepo {
        return mPaperRepo
    }

    private val mPaperTransformRepo by lazy {
        PaperTransformRepoFileImpl(fileDir = getExternalFilesDir("transform"))
    }

    override fun getPaperTransformRepo(): IPaperTransformRepo {
        return mPaperTransformRepo
    }

    private val mDbScheduler = SingleScheduler()

    override fun getScheduler(): Scheduler {
        return mDbScheduler
    }

    // Shared preference //////////////////////////////////////////////////////

    // Shared preference.
    private val mPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

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

    override fun putLong(key: String, value: Long) {
        mPreferences
            .edit()
            .putLong(key, value)
            .apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return mPreferences.getLong(key, defaultValue)
    }

    override fun putFloat(key: String, value: Float) {
        mPreferences
            .edit()
            .putFloat(key, value)
            .apply()
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return mPreferences.getFloat(key, defaultValue)
    }
}
