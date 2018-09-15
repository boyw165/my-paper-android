// Copyright Sep 2018-present Whiteboard
//
// Author: tc@sodalabs.co
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

import android.os.Bundle

class AndroidBundle(val bundle: Bundle = Bundle()) : IBundle {

    override fun putBoolean(key: String,
                            value: Boolean) {
        bundle.putBoolean(key, value)
    }

    override fun putInt(key: String, value: Int) {
        bundle.putInt(key, value)
    }

    override fun putLong(key: String, value: Long) {
        bundle.putLong(key, value)
    }

    override fun putFloat(key: String, value: Float) {
        bundle.putFloat(key, value)
    }

    override fun putString(key: String, value: String) {
        bundle.putString(key, value)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return bundle.getBoolean(key, default)
    }

    override fun getInt(key: String, default: Int): Int {
        return bundle.getInt(key, default)
    }

    override fun getLong(key: String, default: Long): Long {
        return bundle.getLong(key, default)
    }

    override fun getFloat(key: String, default: Float): Float {
        return bundle.getFloat(key, default)
    }

    override fun getString(key: String, default: String): String {
        return bundle.getString(key, default)
    }
}
