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

package com.paper.model

import com.google.gson.*
import com.paper.model.PenColorRepoFileImpl.ColorData
import com.paper.model.repository.IPenColorRepo
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type

class PenColorRepoFileImpl(dir: File,
                           ioScheduler: Scheduler? = null)
    : IPenColorRepo,
      JsonSerializer<ColorData>,
      JsonDeserializer<ColorData> {

    private val mFile = File(dir, "pen_color.json")
    private val mGson = GsonBuilder()
        .registerTypeAdapter(ColorData::class.java, this)
        .create()
    private val mIoScheduler = ioScheduler ?: SingleScheduler()

    private val mPenColors = BehaviorSubject.create<List<Int>>()
    private val mChosenPenColor = BehaviorSubject.create<Int>()

    override fun getPenColors(): Observable<List<Int>> {
        return Observables.combineLatest(
            mPenColors,
            // You want the result of reading file sent to the subject also want
            // to be able to cancel the reading operation.
            readFromFile()
                .map { (colors, _) ->
                    mPenColors.onNext(colors)
                    true
                })
            .map { (colors, _) -> colors }
    }

    override fun putPenColors(colors: List<Int>): Single<Boolean> {
        val clone = colors.toList()
        mPenColors.onNext(clone)

        return flushMemoryCacheToDisk()
    }

    override fun getChosenPenColor(): Observable<Int> {
        return Observables.combineLatest(
            mChosenPenColor,
            // You want the result of reading file sent to the subject also want
            // to be able to cancel the reading operation.
            readFromFile()
                .map { (_, chosenColor) ->
                    mChosenPenColor.onNext(chosenColor)
                    true
                })
            .map { (chosenColor, _) -> chosenColor }
    }

    override fun putChosenPenColor(color: Int): Single<Boolean> {
        mChosenPenColor.onNext(color)

        return flushMemoryCacheToDisk()
    }

    private fun readFromFile(): Observable<ColorData> {
        return Maybe
            .fromCallable {
                if (mFile.exists()) {
                    FileReader(mFile).use { reader ->
                        return@fromCallable mGson.fromJson(
                            reader,
                            ColorData::class.java)
                    }
                } else {
                    return@fromCallable ColorData(
                        colors = IPenColorRepo.DEFAULT_COLORS,
                        chosenColor = IPenColorRepo.DEFAULT_CHOSEN_COLOR)
                }
            }
            .subscribeOn(mIoScheduler)
            .toObservable()
    }

    private fun flushMemoryCacheToDisk(): Single<Boolean> {
        return Observables.combineLatest(
            mPenColors,
            mChosenPenColor)
            .observeOn(mIoScheduler)
            .map { (colors, chosenColor) ->
                val data = ColorData(
                    colors = colors,
                    chosenColor = chosenColor)
                val json = mGson.toJson(data)

                if (!mFile.exists()) {
                    mFile.createNewFile()
                }

                FileWriter(mFile).use { writer ->
                    writer.write(json)
                }

                return@map true
            }
            .firstOrError()
    }

    // JSON translation ///////////////////////////////////////////////////////

    override fun serialize(src: ColorData,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        val colorArray = JsonArray()
        src.colors.forEach { color ->
            colorArray.add("#${Integer.toHexString(color)}")
        }

        root.add("colors", colorArray)
        root.addProperty("chosenColor", "#${Integer.toHexString(src.chosenColor)}")

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): ColorData {
        val root = json.asJsonObject

        val colors = mutableListOf<Int>()
        root["colors"].asJsonArray.forEach { el ->
            colors.add(Color.parseColor(el.asString))
        }
        val chosenColor = Color.parseColor(root["chosenColor"].asString)

        return ColorData(
            colors = colors,
            chosenColor = chosenColor)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    data class ColorData(val colors: List<Int>,
                         val chosenColor: Int)
}
