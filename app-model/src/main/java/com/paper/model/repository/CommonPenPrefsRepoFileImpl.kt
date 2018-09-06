// Copyright Apr 2018-present boyw165@gmail.com
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

import com.google.gson.*
import com.paper.model.Color
import com.paper.model.ModelConst
import com.paper.model.observables.WriteStringToFileObservable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.io.FileReader
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

class CommonPenPrefsRepoFileImpl(dir: File,
                                 ioScheduler: Scheduler? = null)
    : ICommonPenPrefsRepo,
      JsonSerializer<CommonPenPrefsRepoFileImpl.PrefsData>,
      JsonDeserializer<CommonPenPrefsRepoFileImpl.PrefsData> {

    private val mFile = File(dir, "pen_prefs.json")
    private val mGson = GsonBuilder()
        .registerTypeAdapter(PrefsData::class.java, this)
        .create()
    private val mIoScheduler = ioScheduler ?: SingleScheduler()

    private val mPenColorsSignal = BehaviorSubject.create<List<Int>>()
    private val mChosenPenColorSignal = BehaviorSubject.create<Int>()
    private val mPenSizeSignal = BehaviorSubject.create<Float>()

    override fun getPenColors(): Observable<List<Int>> {
        return Observables.combineLatest(
            mPenColorsSignal,
            // You want the result of reading file sent to the subject also want
            // to be able to cancel the reading operation.
            readFromFile()
                .map { (colors, _, _) ->
                    mPenColorsSignal.onNext(colors)
                    true
                })
            .map { it.first }
    }

    override fun putPenColors(colors: List<Int>): Single<Boolean> {
        val clone = colors.toList()
        mPenColorsSignal.onNext(clone)

        return writeToDisk()
    }

    override fun getChosenPenColor(): Observable<Int> {
        return Observables.combineLatest(
            mChosenPenColorSignal,
            // You want the result of reading file sent to the subject also want
            // to be able to cancel the reading operation.
            readFromFile()
                .map { (_, chosenColor, _) ->
                    mChosenPenColorSignal.onNext(chosenColor)
                    true
                })
            .map { it.first }
    }

    override fun putChosenPenColor(color: Int): Single<Boolean> {
        mChosenPenColorSignal.onNext(color)

        return writeToDisk()
    }

    override fun getPenSize(): Observable<Float> {
        return Observables.combineLatest(
            mPenSizeSignal,
            // You want the result of reading file sent to the subject also want
            // to be able to cancel the reading operation.
            readFromFile()
                .map { (_, _, penSize) ->
                    mPenSizeSignal.onNext(penSize)
                    true
                })
            .map { it.first }
    }

    override fun putPenSize(size: Float): Single<Boolean> {
        mPenSizeSignal.onNext(size)

        return writeToDisk()
    }

    private fun readFromFile(): Observable<PrefsData> {
        return Maybe
            .fromCallable {
                if (mFile.exists()) {
                    println("${ModelConst.TAG} read pen preferences from $mFile")

                    FileReader(mFile).use { reader ->
                        return@fromCallable mGson.fromJson(
                            reader,
                            PrefsData::class.java)
                    }
                } else {
                    return@fromCallable PrefsData(
                        colors = ICommonPenPrefsRepo.DEFAULT_COLORS,
                        chosenColor = ICommonPenPrefsRepo.DEFAULT_CHOSEN_COLOR,
                        penSize = ICommonPenPrefsRepo.DEFAULT_PEN_SIZE)
                }
            }
            .subscribeOn(mIoScheduler)
            .toObservable()
    }

    private fun writeToDisk(): Single<Boolean> {
        return Observables
            .combineLatest(
                mPenColorsSignal,
                mChosenPenColorSignal,
                mPenSizeSignal)
            .observeOn(mIoScheduler)
            .debounce(1000, TimeUnit.MILLISECONDS, mIoScheduler)
            .switchMap { (colors, chosenColor, penSize) ->
                return@switchMap Single
                    .fromCallable {
                        val data = PrefsData(
                            colors = colors,
                            chosenColor = chosenColor,
                            penSize = penSize)
                        mGson.toJson(data, PrefsData::class.java)
                    }
                    .subscribeOn(mIoScheduler)
                    .toObservable()
                    .switchMap { json ->
                        WriteStringToFileObservable(
                            file = mFile,
                            txt = json)
                            .subscribeOn(mIoScheduler)
                            .filter { progress -> progress == 100 }
                            .map { true }
                    }
            }
            .firstOrError()
    }

    // JSON translation ///////////////////////////////////////////////////////

    override fun serialize(src: PrefsData,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        val colorArray = JsonArray()
        src.colors.forEach { color ->
            colorArray.add("#${Integer.toHexString(color)}")
        }

        root.add("colors", colorArray)
        root.addProperty("chosenColor", "#${Integer.toHexString(src.chosenColor)}")
        root.addProperty("penSize", src.penSize)

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): PrefsData {
        val root = json.asJsonObject

        val colors = mutableListOf<Int>()
        if (root.has("colors")) {
            root["colors"].asJsonArray.forEach { el ->
                colors.add(Color.parseColor(el.asString))
            }
        } else {
            colors.addAll(ICommonPenPrefsRepo.DEFAULT_COLORS)
        }

        val chosenColor = if (root.has("chosenColor")) {
            Color.parseColor(root["chosenColor"].asString)
        } else {
            ICommonPenPrefsRepo.DEFAULT_CHOSEN_COLOR
        }

        val penSize = if (root.has("penSize")) {
            root["penSize"].asFloat
        } else {
            ICommonPenPrefsRepo.DEFAULT_PEN_SIZE
        }

        return PrefsData(
            colors = colors,
            chosenColor = chosenColor,
            penSize = penSize)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    data class PrefsData(val colors: List<Int>,
                         val chosenColor: Int,
                         val penSize: Float)
}
