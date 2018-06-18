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

package com.paper.domain.event

import com.paper.model.event.EventLifecycle

data class ProgressBarEvent(val lifecycle: EventLifecycle,
                            val progress: Int = 0,
                            val fromUser: Boolean = false) {
    companion object {

        @JvmStatic
        fun start(progress: Int,
                  fromUser: Boolean): ProgressBarEvent {
            return ProgressBarEvent(lifecycle = EventLifecycle.START,
                                    progress = progress,
                                    fromUser = fromUser)
        }

        @JvmStatic
        fun doing(progress: Int,
                  fromUser: Boolean): ProgressBarEvent {
            return ProgressBarEvent(lifecycle = EventLifecycle.DOING,
                                    progress = progress,
                                    fromUser = fromUser)
        }

        @JvmStatic
        fun stop(progress: Int,
                 fromUser: Boolean): ProgressBarEvent {
            return ProgressBarEvent(lifecycle = EventLifecycle.STOP,
                                    progress = progress,
                                    fromUser = fromUser)
        }
    }
}
