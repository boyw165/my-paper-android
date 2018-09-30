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
import com.paper.model.ISchedulers
import com.paper.model.*
import com.paper.model.command.WhiteboardCommand
import com.paper.model.command.WhiteboardCommandJSONTranslator
import com.paper.model.repository.IBitmapRepository
import com.paper.model.repository.IWhiteboardRepository
import com.paper.model.repository.WhiteboardRepoSQLite
import com.paper.model.repository.json.FrameJSONTranslator
import com.paper.model.repository.json.WhiteboardJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.sketch.VectorGraphics
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers

class WhiteboardApplication : MultiDexApplication(),
                              ISchedulers,
                              IWhiteboardRepoProvider,
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

    private val jsonTranslator by lazy {
        GsonBuilder()
            .registerTypeAdapter(Whiteboard::class.java,
                                 WhiteboardJSONTranslator())
            .registerTypeAdapter(Scrap::class.java,
                                 ScrapJSONTranslator())
            .registerTypeAdapter(Frame::class.java,
                                 FrameJSONTranslator())
            .registerTypeAdapter(VectorGraphics::class.java,
                                 VectorGraphicsJSONTranslator())
            .registerTypeAdapter(WhiteboardCommand::class.java,
                                 WhiteboardCommandJSONTranslator())
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

    private val dbScheduler = SingleScheduler()

    override fun db(): Scheduler {
        return dbScheduler
    }

    private val mPaperRepo by lazy {
        WhiteboardRepoSQLite(authority = packageName,
                             resolver = contentResolver,
                             jsonTranslator = jsonTranslator,
                             bmpCacheDir = getExternalFilesDir("media"),
                             prefs = preference,
                             dbIoScheduler = dbScheduler)
    }

    override fun getWhiteboardRepo(): IWhiteboardRepository {
        return mPaperRepo
    }

    override fun getBitmapRepo(): IBitmapRepository {
        return mPaperRepo
    }

    private val mPaperTransformRepo by lazy {
        CanvasOperationRepoFileLRUImpl(fileDir = getExternalFilesDir("transform"))
    }

    override fun getPaperTransformRepo(): ICanvasOperationRepo {
        return mPaperTransformRepo
    }

    // Shared preference //////////////////////////////////////////////////////

    private val mPrefsScheduler = SingleScheduler()
    override val preference by lazy {
        AndroidPreference(context = this@WhiteboardApplication,
                          workerScheduler = mPrefsScheduler)
    }
}
