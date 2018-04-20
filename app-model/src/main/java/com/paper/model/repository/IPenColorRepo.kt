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

import com.paper.model.Color
import io.reactivex.Observable
import io.reactivex.Single

interface IPenColorRepo {

    companion object {
        val DEFAULT_COLORS = listOf(Color.parseColor("#E75058"),
                                    Color.parseColor("#DDB543"),
                                    Color.parseColor("#E5D5C8"),
                                    Color.parseColor("#C79E80"),
                                    Color.parseColor("#848F94"),
                                    Color.parseColor("#010101"))
        val DEFAULT_CHOSEN_COLOR = DEFAULT_COLORS[1]
    }

    /**
     * Get the colors saved last time. If no, will give you the default color
     * set.
     */
    fun getPenColors(): Observable<List<Int>>

    /**
     * Will trigger [getPenColors] signal also flush memory to a JSON file.
     *
     * @return Observable of the success; True is successful, false is failed
     */
    fun putPenColors(colors: List<Int>): Single<Boolean>

    /**
     * Get the chosen saved last time. If no, will given you a color from the
     * default color set.
     */
    fun getChosenPenColor(): Observable<Int>

    /**
     * Will trigger [getChosenPenColor] signal also flush memory cache to a JSON
     * file.
     *
     * @return Observable of the success; True is successful, false is failed
     */
    fun putChosenPenColor(color: Int): Single<Boolean>
}
