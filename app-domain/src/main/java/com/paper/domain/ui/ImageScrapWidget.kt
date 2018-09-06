// Copyright Mar 2018-present boyw165@gmail.com
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

package com.paper.domain.ui

import com.paper.domain.DomainConst
import com.paper.model.ISchedulers
import com.paper.model.ImageScrap
import io.reactivex.Observable

class ImageScrapWidget(scrap: ImageScrap,
                       schedulers: ISchedulers)
    : BaseScrapWidget(scrap,
                      schedulers),
      IWidget {

    override fun start(): Observable<Boolean> {
        return autoStop {
            synchronized(lock) {
                // DO NOTHING
            }
            println("${DomainConst.TAG}: Start \"${javaClass.simpleName}\"")
        }
    }

    override fun stop() {
        super.stop()
        println("${DomainConst.TAG}: Stop \"${javaClass.simpleName}\"")
    }
}
