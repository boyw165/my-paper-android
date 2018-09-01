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

import com.paper.model.sketch.VectorGraphics
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class SVGScrapTest : BaseModelTest() {

    @Test
    fun `copy, ID should be different`() {
        val tester1 = createRandomSVGScrap()
        val tester2 = tester1.copy() as ISVGScrap
        tester2.setFrame(Frame(100f, 100f))
        tester2.setSVGs(listOf(VectorGraphics()))

        Assert.assertNotEquals(tester2, tester1)
        Assert.assertNotEquals(tester2.getID(), tester1.getID())
        Assert.assertNotEquals(tester2.getFrame(), tester1.getFrame())
        Assert.assertNotEquals(tester2.getSVGs().size, tester1.getSVGs().size)
    }

    @Test
    fun `observe add svg`() {
        val tester = createRandomSVGScrap()

        val addTestObserver = tester.observeAddSVG().test()

        tester.addSVG(createRandomSVG())
        tester.addSVG(createRandomSVG())
        tester.addSVG(createRandomSVG())

        addTestObserver.assertValueCount(3)
    }

    @Test
    fun `observe remove svg`() {
        val tester = createRandomSVGScrap()

        val removeTestObserver = tester.observeRemoveSVG().test()

        tester.removeSVG(tester.getSVGs()[0])
        tester.removeSVG(tester.getSVGs()[0])

        removeTestObserver.assertValueCount(2)
    }
}

