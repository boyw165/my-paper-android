// Copyright Sep 2018-present Paper
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

import com.paper.domain.ui.UndoManager
import com.paper.model.repository.CommandRepository
import com.paper.model.repository.ICommandRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner.Silent::class)
class UndoManagerTest : BaseDomainTest() {

    companion object {
        const val LOG_DIR = "/tmp/command_test"
    }

    private val undoRepo: ICommandRepository by lazy {
        CommandRepository(logDir = File(LOG_DIR),
                          logJournalFileName = "_undoJournal",
                          jsonTranslator = jsonTranslator,
                          capacity = 3,
                          schedulers = mockSchedulers)
    }
    private val redoRepo: ICommandRepository by lazy {
        CommandRepository(logDir = File(LOG_DIR),
                          logJournalFileName = "_redoJournal",
                          jsonTranslator = jsonTranslator,
                          capacity = 3,
                          schedulers = mockSchedulers)
    }

    @Before
    override fun setup() {
        super.setup()
        removeLogDir()
    }

    @After
    override fun clean() {
        removeLogDir()
    }

    @Test
    fun test() {
        val candidate = UndoManager(undoRepo = undoRepo,
                                    redoRepo = redoRepo,
                                    schedulers = mockSchedulers)
        // Start widget
        candidate.start().test().assertSubscribed()

        // TODO
    }

    private fun removeLogDir() {
        val file = File(LOG_DIR)
        file.deleteRecursively()
    }
}
