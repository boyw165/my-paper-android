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

import android.os.StrictMode
import android.support.multidex.MultiDexApplication
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.firebase.FirebaseApp
import com.paper.model.*
import com.paper.model.repository.IBitmapRepository
import com.paper.model.repository.IPaperRepo
import com.paper.model.repository.PaperRepoSqliteImpl
import com.paper.model.repository.PaperTransformRepoFileImpl
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.plugins.RxJavaPlugins

class PaperApplication : MultiDexApplication(),
                         IPaperRepoProvider,
                         IPaperTransformRepoProvider,
                         IBitmapRepoProvider,
                         IPreferenceServiceProvider {

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

    private val mDbScheduler = SingleScheduler()
    private val mPaperRepo by lazy {
        PaperRepoSqliteImpl(authority = packageName,
                            resolver = contentResolver,
                            fileDir = getExternalFilesDir("media"),
                            prefs = preference,
                            dbIoScheduler = mDbScheduler)
    }

    override fun getPaperRepo(): IPaperRepo {
        return mPaperRepo
    }

    override fun getBitmapRepo(): IBitmapRepository {
        return mPaperRepo
    }

    private val mPaperTransformRepo by lazy {
        PaperTransformRepoFileImpl(fileDir = getExternalFilesDir("transform"))
    }

    override fun getPaperTransformRepo(): IPaperTransformRepo {
        return mPaperTransformRepo
    }


    // Shared preference //////////////////////////////////////////////////////

    private val mPrefsScheduler = SingleScheduler()
    override val preference by lazy {
        PreferenceAndroidImpl(context = this@PaperApplication,
                              workerScheduler = mPrefsScheduler)
    }
}
