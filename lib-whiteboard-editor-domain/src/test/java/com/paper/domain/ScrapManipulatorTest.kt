// Copyright Aug 2018-present Paper
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

package com.paper.domain

import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.only
import com.paper.domain.ui.manipulator.ScrapManipulator
import com.paper.model.command.GroupCommand
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class ScrapManipulatorTest : BaseEditorDomainTest() {

    @Before
    override fun setup() {
        super.setup()
    }

    @After
    override fun clean() {
        super.clean()
    }

    @Test
    fun `given a sequence of touch sequence, must see offering command at the end`() {
        val widget = createRandomScrapWidget()
        val candidate = ScrapManipulator(scrapWidget = widget,
                                         editorWidget = mockWhiteboardEditorWidget)

        val commandTester = mockWhiteboardEditorWidget.undoWidget
        val completionTester = candidate
            .apply(Observable.just(mockTouchBegin,
                                   mockDragSequence,
                                   mockPinchSequence,
                                   mockTouchEnd))
            .test()

        moveScheduler()

        completionTester.assertComplete()
        Mockito.verify(commandTester).offerCommand(argWhere { command ->
            command is GroupCommand
        })
    }
}
