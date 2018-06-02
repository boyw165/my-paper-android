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
        val scrap = Scrap()
        val hashCode1 = scrap.hashCode()

        scrap.x = 1f
        scrap.y = 2f
        val hashCode2 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)
    }

    @Test
    fun changeXYRestoreXY_hashCodeShouldBeSame() {
        val scrap = Scrap()
        val startX = scrap.x
        val startY = scrap.y
        val hashCode1 = scrap.hashCode()

        scrap.x = 1f
        scrap.y = 2f
        scrap.x = startX
        scrap.y = startY
        val hashCode2 = scrap.hashCode()

        Assert.assertEquals(hashCode1, hashCode2)
    }

    @Test
    fun changeZ_hashCodeShouldBeDifferent() {
        val scrap = Scrap()
        val hashCode1 = scrap.hashCode()

        scrap.z = ModelConst.MOST_BOTTOM_Z
        val hashCode2 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)

        scrap.z = ModelConst.MOST_TOP_Z
        val hashCode3 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode3)
    }

    @Test
    fun changeScale_hashCodeShouldBeDifferent() {
        val scrap = Scrap()
        val hashCode1 = scrap.hashCode()

        scrap.scale = 2f
        val hashCode2 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)
    }

    @Test
    fun changeScaleRestoreScale_hashCodeShouldBeSame() {
        val scrap = Scrap()
        val startScale = scrap.scale
        val hashCode1 = scrap.hashCode()

        scrap.scale = 2f
        scrap.scale = startScale
        val hashCode2 = scrap.hashCode()

        Assert.assertEquals(hashCode2, hashCode1)
    }

    @Test
    fun changeRotation_hashCodeShouldBeDifferent() {
        val scrap = Scrap()
        val hashCode1 = scrap.hashCode()

        scrap.rotationInRadians = Math.PI.toFloat()
        val hashCode2 = scrap.hashCode()

        Assert.assertNotEquals(hashCode2, hashCode1)
    }

    @Test
    fun changeRotationRestoreRotation_hashCodeShouldBeSame() {
        val scrap = Scrap()
        val startRotation = scrap.rotationInRadians
        val hashCode1 = scrap.hashCode()

        scrap.rotationInRadians = Math.PI.toFloat()
        scrap.rotationInRadians = startRotation
        val hashCode2 = scrap.hashCode()

        Assert.assertEquals(hashCode2, hashCode1)
    }
}

