// Copyright (c) 2018-present Paper
//
// Author: boyw165@gmail.com
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.paper.model

import io.reactivex.Observable
import io.reactivex.Single

interface IPreferenceService {

    fun putBoolean(key: String, value: Boolean): Single<Boolean>

    fun getBoolean(key: String, defaultValue: Boolean): Observable<Boolean>

    fun putString(key: String, value: String): Single<Boolean>

    fun getString(key: String, defaultValue: String): Observable<String>

    fun putInt(key: String, value: Int): Single<Boolean>

    fun getInt(key: String, defaultValue: Int): Observable<Int>

    fun putLong(key: String, value: Long): Single<Boolean>

    fun getLong(key: String, defaultValue: Long): Observable<Long>

    fun putFloat(key: String, value: Float): Single<Boolean>

    fun getFloat(key: String, defaultValue: Float): Observable<Float>
}
