// Copyright Apr 2018-present Paper
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

package com.paper.model.repository

import com.google.gson.Gson
import com.paper.model.ISchedulers
import com.paper.model.ModelConst
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Completable
import io.reactivex.Single
import java.io.File
import java.util.*

class CommandRepository(private val logDir: File,
                        logJournalFileName: String,
                        private val jsonTranslator: Gson,
                        private val capacity: Int,
                        private val schedulers: ISchedulers)
    : ICommandRepository {

    companion object {
        const val SEPARATOR = ","
    }

    private val lock = Any()

    // TODO: Add limitation
    private val logJournal = ArrayDeque<UUID>()
    private val logJournalFile = File(logDir, logJournalFileName)

    override fun prepare(): Single<Int> {
        return Single
            .fromCallable {
                synchronized(lock) {
                    logJournal.clear()
                    logJournal.addAll(journalFromFile())
                    logJournal.size
                }
            }
            .subscribeOn(schedulers.io())
    }

    override fun push(command: WhiteboardCommand): Single<Int> {
        return Single
            .fromCallable {
                synchronized(lock) {
                    println("${ModelConst.TAG}: put $command to transformation repo (file impl)")

                    ensureJournalFileExist()

                    // Serialize command
                    val commandText = jsonTranslator.toJson(command, WhiteboardCommand::class.java)
                    val commandFile = File(logDir, "${command.id}.json")
                    if (!commandFile.exists()) {
                        commandFile.parentFile.mkdirs()
                        commandFile.createNewFile()
                    }
                    commandFile.writeText(commandText, charset = Charsets.UTF_8)

                    // Update journal and constraint the capacity
                    logJournal.push(command.id)
                    if (logJournal.size > capacity) {
                        logJournal.removeFirst()
                    }
                    // Update journal file
                    journalToFile()

                    logJournal.size
                }
            }
            .subscribeOn(schedulers.io())
    }

    override fun pop(): Single<Pair<Int, WhiteboardCommand>> {
        return Single
            .fromCallable {
                synchronized(lock) {
                    val commandID = logJournal.pop()
                    val commandFile = File(logDir, commandID.toString())
                    val commandText = commandFile.readText(Charsets.UTF_8)

                    // Inflate command from file
                    val command = jsonTranslator.fromJson<WhiteboardCommand>(
                        commandText, WhiteboardCommand::class.java)

                    // Remove ID from memory cache
                    logJournal.remove(commandID)
                    // Update journal file
                    journalToFile()

                    Pair(logJournal.size, command)
                }
            }
            .subscribeOn(schedulers.io())
    }

    private fun journalFromFile(): List<UUID> {
        ensureJournalFileExist()

        return logJournalFile.readText()
            .split(SEPARATOR)
            .filter { it.isNotEmpty() }
            .map { UUID.fromString(it) }
    }

    private fun journalToFile() {
        ensureJournalFileExist()

        val builder = StringBuilder()
        logJournal.forEachIndexed { i, uuid ->
            if (i > 0) {
                builder.append(SEPARATOR)
            }
            builder.append(uuid)
        }

        logJournalFile.writeText(builder.toString())
    }

    override fun deleteAll(): Completable {
        return Completable
            .fromCallable {
                logDir.deleteRecursively()
                logJournal.clear()
            }
            .subscribeOn(schedulers.io())
    }

    override fun purgeGarbageCommands(): Completable {
        TODO("not implemented")
    }

    private fun ensureJournalFileExist() {
        if (!logJournalFile.exists()) {
            logJournalFile.parentFile.mkdirs()
            logJournalFile.createNewFile()
        }
    }
}
