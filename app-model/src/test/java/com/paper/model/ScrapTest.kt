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

@RunWith(MockitoJUnitRunner::class)
class ScrapTest {

    @Test
    fun changeXY_hashCodeShouldBeDifferent() {
        val scrap = BaseScrap()
        val hashCode1 = scrap.hashCode()

        scrap.setFrame(scrap.getFrame()
                           .copy(x = 1f,
                                 y = 2f))
        val hashCode2 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)
    }

    @Test
    fun changeXYRestoreXY_hashCodeShouldBeSame() {
        val scrap = BaseScrap()
        val startX = scrap.getFrame().x
        val startY = scrap.getFrame().y
        val hashCode1 = scrap.hashCode()


        scrap.setFrame(scrap.getFrame()
                           .copy(x = 1f,
                                 y = 2f))
        scrap.setFrame(scrap.getFrame()
                           .copy(x = startX,
                                 y = startY))
        val hashCode2 = scrap.hashCode()

        Assert.assertEquals(hashCode1, hashCode2)
    }

    @Test
    fun changeZ_hashCodeShouldBeDifferent() {
        val scrap = BaseScrap()
        val hashCode1 = scrap.hashCode()

        scrap.setFrame(scrap.getFrame()
                           .copy(z = ModelConst.MOST_BOTTOM_Z))
        val hashCode2 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)

        scrap.setFrame(scrap.getFrame()
                           .copy(z = ModelConst.MOST_TOP_Z))
        val hashCode3 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode3)
    }

    @Test
    fun changeScale_hashCodeShouldBeDifferent() {
        val scrap = BaseScrap()
        val hashCode1 = scrap.hashCode()

        scrap.setFrame(scrap.getFrame()
                           .copy(scaleX = 2f,
                                 scaleY = 2f))
        val hashCode2 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)
    }

    @Test
    fun changeScaleRestoreScale_hashCodeShouldBeSame() {
        val scrap = BaseScrap()
        val startScale = scrap.getFrame().scaleX
        val hashCode1 = scrap.hashCode()

        scrap.setFrame(scrap.getFrame()
                           .copy(scaleX = 2f))
        scrap.setFrame(scrap.getFrame()
                           .copy(scaleX = startScale))
        val hashCode2 = scrap.hashCode()

        Assert.assertEquals(hashCode2, hashCode1)
    }

    @Test
    fun changeRotation_hashCodeShouldBeDifferent() {
        val scrap = BaseScrap()
        val hashCode1 = scrap.hashCode()

        scrap.setFrame(scrap.getFrame()
                           .copy(rotationInDegrees = 180f))
        val hashCode2 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)
    }

    @Test
    fun changeRotationRestoreRotation_hashCodeShouldBeSame() {
        val scrap = BaseScrap()
        val startRotation = scrap.getFrame().rotationInDegrees
        val hashCode1 = scrap.hashCode()

        scrap.setFrame(scrap.getFrame()
                           .copy(rotationInDegrees = 180f))
        scrap.setFrame(scrap.getFrame()
                           .copy(rotationInDegrees = startRotation))
        val hashCode2 = scrap.hashCode()

        Assert.assertEquals(hashCode2, hashCode1)
    }
}

