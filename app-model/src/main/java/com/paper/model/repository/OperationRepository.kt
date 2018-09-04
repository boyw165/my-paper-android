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

import androidx.annotation.RequiresPermission
import com.paper.model.ISchedulers
import com.paper.model.ModelConst
import io.reactivex.Completable
import io.reactivex.Single
import java.io.File
import java.util.*

class OperationRepository(private val logDir: File,
                          logJournalFileName: String,
                          private val schedulers: ISchedulers)
    : IOperationRepository {

    companion object {
        const val SEPARATOR = ","
    }

    private val lock = Any()

    private val logJournal = mutableListOf<UUID>()
    private val logJournalFile = File(logDir, logJournalFileName)

    override fun prepare(): Single<Int> {
        return Single
            .fromCallable {
                synchronized(lock) {
                    val exist = if (!logJournalFile.exists()) {
                        logJournalFile.mkdirs()
                        logJournalFile.createNewFile()
                    } else false

                    if (exist) {
                        val journal = logJournalFile.readText().split(SEPARATOR)

                        logJournal.addAll(journal.map { UUID.fromString(it) })

                        journal.size
                    } else {
                        0
                    }
                }
            }
            .subscribeOn(schedulers.io())
    }

    override fun push(operation: EditorOperation): Single<Int> {
        return Single
            .fromCallable {
                synchronized(lock) {
                    println("${ModelConst.TAG}: put $operation to transformation repo (file impl)")

                    logJournal.add(operation.id)

                    // TODO: write ID to the end of the journal file

                    logJournal.size
                }
            }
            .subscribeOn(schedulers.io())
    }

    override fun pop(): Single<Pair<Int, EditorOperation>> {
        // TODO
        return Single
            .never()
    }

    override fun deleteAll(): Completable {
        return Completable
            .fromCallable {
                if (logDir.exists()) {
                    logDir.delete()
                }
            }
            .subscribeOn(schedulers.io())
    }
}
