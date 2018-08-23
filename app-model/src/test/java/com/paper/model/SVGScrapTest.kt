// Copyright May 2018-present Paper
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

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class SVGScrapTest {

    @Test
    fun `call moveTo without a close, should see no change`() {
        val scrap = SVGScrap()

        scrap.moveTo(x = 100f, y = 100f, style = emptySet())

        Assert.assertEquals(0, scrap.getSVGs().size)
    }

    @Test
    fun `call moveTo with a close, should see change`() {
        val scrap = SVGScrap()

        scrap.moveTo(x = 100f, y = 100f, style = emptySet())
        scrap.close()

        Assert.assertEquals(1, scrap.getSVGs().size)
    }

//    @Test
//    fun `concurrent test`() {
//        val scrap = SVGScrap()
//
//        val testObserver = Completable
//            .mergeArrayDelayError(
//                Completable
//                    .fromCallable {
//                        scrap.moveTo(11f, 12f, emptySet())
//                        scrap.lineTo(13f, 14f)
//                        scrap.close()
//                    }
//                    .subscribeOn(Schedulers.computation()),
//                Completable
//                    .fromCallable {
//                        scrap.moveTo(21f, 22f, emptySet())
//                        scrap.lineTo(23f, 24f)
//                        scrap.close()
//                    }
//                    .subscribeOn(Schedulers.computation()))
//            .test()
//
//        // Await until two concurrent jobs finish
//        testObserver.await()
//        testObserver.assertComplete()
//        Assert.assertEquals(2, scrap.getSVGs().size)
//    }
}

