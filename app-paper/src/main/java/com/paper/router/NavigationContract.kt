// Copyright (c) 2017-present WANG, TAI-CHUN
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

package com.paper.router

abstract class NavigationContract private constructor() {

    companion object {
        const val SCREEN_OF_HOME: String = "/"

        const val SCREEN_OF_RXCANCEL_PAGE1: String = "/rxCancel/1"

        // flow1: #1 -> c -> #3
        const val SCREEN_OF_FLOW1_PAGE1: String = "/flow1/1"
        const val SCREEN_OF_FLOW1_PAGE2: String = "/flow1/c" // c -> common activity.
        const val SCREEN_OF_FLOW1_PAGE3: String = "/flow1/3"

        // flow2: #1 -> c
        const val SCREEN_OF_FLOW2_PAGE1: String = "/flow2/1"
        const val SCREEN_OF_FLOW2_PAGE2: String = "/flow2/c" // c -> common activity.

        const val ACTIVITY_NUMBER_FLAG: String = "activity_number_flag"
        const val FRAGMENT_NUMBER_FLAG: String = "fragment_number_flag"

        const val ACTIVITY_RESULT_CODE: Int = 666
    }
}
