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

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.preference.PreferenceManager
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

class AndroidPreference(context: Context,
                        workerScheduler: Scheduler)
    : IPreferenceService,
      SharedPreferences.OnSharedPreferenceChangeListener {

    private val mWorkerScheduler = workerScheduler

    // Shared preference.
    private val mPreferencesSignal = PublishSubject.create<String>().toSerialized()
    private val mPreferences by lazy {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw IllegalThreadStateException("Shared preference initializes " +
                                              "on UI thread")
        }
        val field = PreferenceManager.getDefaultSharedPreferences(context)
        field.registerOnSharedPreferenceChangeListener(this@AndroidPreference)
        field
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences,
                                           key: String) {
        mPreferencesSignal.onNext(key)
    }

    override fun putBoolean(key: String, value: Boolean): Single<Boolean> {
        return Single
            .fromCallable {
                mPreferences
                    .edit()
                    .putBoolean(key, value)
                    .apply()
                true
            }
            .subscribeOn(mWorkerScheduler)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Observable<Boolean> {
        val src = Observable
            .fromCallable {
                mPreferences.getBoolean(key, defaultValue)
            }
            .subscribeOn(mWorkerScheduler)

        return Observable
            .merge(
                src,
                mPreferencesSignal
                    .filter { it == key }
                    .flatMap { src })
    }

    override fun putString(key: String, value: String): Single<Boolean> {
        return Single
            .fromCallable {
                mPreferences
                    .edit()
                    .putString(key, value)
                    .apply()
                true
            }
            .subscribeOn(mWorkerScheduler)
    }

    override fun getString(key: String, defaultValue: String): Observable<String> {
        val src = Observable
            .fromCallable {
                mPreferences.getString(key, defaultValue)
            }
            .subscribeOn(mWorkerScheduler)

        return Observable
            .merge(
                src,
                mPreferencesSignal
                    .filter { it == key }
                    .flatMap { src })
    }

    override fun putInt(key: String, value: Int): Single<Boolean> {
        return Single
            .fromCallable {
                mPreferences
                    .edit()
                    .putInt(key, value)
                    .apply()
                true
            }
    }

    override fun getInt(key: String, defaultValue: Int): Observable<Int> {
        val src = Observable
            .fromCallable {
                mPreferences.getInt(key, defaultValue)
            }
            .subscribeOn(mWorkerScheduler)

        return Observable
            .merge(
                src,
                mPreferencesSignal
                    .filter { it == key }
                    .flatMap { src })
    }

    override fun putLong(key: String, value: Long): Single<Boolean> {
        return Single
            .fromCallable {
                mPreferences
                    .edit()
                    .putLong(key, value)
                    .apply()
                true
            }
    }

    override fun getLong(key: String, defaultValue: Long): Observable<Long> {
        val src = Observable
            .fromCallable {
                mPreferences.getLong(key, defaultValue)
            }
            .subscribeOn(mWorkerScheduler)

        return Observable
            .merge(
                src,
                mPreferencesSignal
                    .filter { it == key }
                    .flatMap { src })
    }

    override fun putFloat(key: String, value: Float): Single<Boolean> {
        return Single
            .fromCallable {
                mPreferences
                    .edit()
                    .putFloat(key, value)
                    .apply()
                true
            }
    }

    override fun getFloat(key: String, defaultValue: Float): Observable<Float> {
        val src = Observable
            .fromCallable {
                mPreferences.getFloat(key, defaultValue)
            }
            .subscribeOn(mWorkerScheduler)

        return Observable
            .merge(
                src,
                mPreferencesSignal
                    .filter { it == key }
                    .flatMap { src })
    }
}
