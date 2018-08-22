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
import com.google.gson.GsonBuilder
import com.paper.domain.ISchedulerProvider
import com.paper.model.*
import com.paper.model.repository.IBitmapRepository
import com.paper.model.repository.IPaperRepo
import com.paper.model.repository.PaperRepoSQLiteImpl
import com.paper.model.repository.PaperCanvasOperationRepoFileLRUImpl
import com.paper.model.repository.json.PaperJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers

class PaperApplication : MultiDexApplication(),
                         ISchedulerProvider,
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

    private val mJsonTranslator by lazy {
        GsonBuilder()
            .registerTypeAdapter(BasePaper::class.java,
                                 PaperJSONTranslator())
            .registerTypeAdapter(BaseScrap::class.java,
                                 ScrapJSONTranslator())
            .registerTypeAdapter(VectorGraphics::class.java,
                                 VectorGraphicsJSONTranslator())
            .create()
    }

    override fun main(): Scheduler {
        return AndroidSchedulers.mainThread()
    }

    override fun computation(): Scheduler {
        return Schedulers.computation()
    }

    override fun io(): Scheduler {
        return Schedulers.io()
    }

    private val mDbScheduler = SingleScheduler()

    override fun db(): Scheduler {
        return mDbScheduler
    }

    private val mPaperRepo by lazy {
        PaperRepoSQLiteImpl(authority = packageName,
                            resolver = contentResolver,
                            jsonTranslator = mJsonTranslator,
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
        PaperCanvasOperationRepoFileLRUImpl(fileDir = getExternalFilesDir("transform"))
    }

    override fun getPaperTransformRepo(): ICanvasOperationRepo {
        return mPaperTransformRepo
    }

    // Shared preference //////////////////////////////////////////////////////

    private val mPrefsScheduler = SingleScheduler()
    override val preference by lazy {
        PreferenceAndroidImpl(context = this@PaperApplication,
                              workerScheduler = mPrefsScheduler)
    }
}
