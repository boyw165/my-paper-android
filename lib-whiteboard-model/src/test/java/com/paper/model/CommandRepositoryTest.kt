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

import com.paper.model.command.AddScrapCommand
import com.paper.model.repository.CommandRepository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner.Silent::class)
class CommandRepositoryTest : BaseModelTest() {

    companion object {
        const val LOG_DIR = "/tmp/command_test"
        const val LOG_JOURNAL_FILE = "_journal"
    }

    @Before
    fun setup() {
        val journalFile = File(LOG_DIR, LOG_JOURNAL_FILE)
        journalFile.deleteRecursively()
    }

    @Test
    fun `prepare with new directory`() {
        val journalFile = File(LOG_DIR, LOG_JOURNAL_FILE)
        journalFile.delete()

        val candidate = initCandidate()

        // Add one particular stroke
        val tester = candidate.prepare().test()

        // Make sure the stream moves
        moveScheduler()

        // Must see one record!
        tester.assertValue(0)
    }

    @Test
    fun `put operation and see one record`() {
        val candidate = initCandidate()

        // Add one command
        val scrap = createRandomSVGScrap()
        val tester = candidate.push(AddScrapCommand(scrap = scrap)).test()

        // Make sure the stream moves
        moveScheduler()

        // Must see one record!
        tester.assertValue(1)
    }

    @Test
    fun `put operation and undo, should see no record`() {
        val candidate = initCandidate()

        // Add one command
        val scrap = createRandomSVGScrap()
        candidate.push(AddScrapCommand(scrap = scrap)).subscribe()

        // Make sure the stream moves
        moveScheduler()

        val tester = candidate.pop().test()
        // Make sure the stream moves
        moveScheduler()
        tester.assertValue { (size, _) ->
            size == 0
        }
    }

    private fun initCandidate(): CommandRepository {
        return CommandRepository(logDir = File(LOG_DIR),
                                 logJournalFileName = LOG_JOURNAL_FILE,
                                 jsonTranslator = jsonTranslator,
                                 capacity = 3,
                                 schedulers = mockSchedulers)
    }
}
