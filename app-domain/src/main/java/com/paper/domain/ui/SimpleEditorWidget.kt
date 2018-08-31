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

package com.paper.domain.ui

import com.paper.domain.ISchedulerProvider
import com.paper.model.IPaper
import com.paper.model.repository.IPaperRepo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single

// TODO: Use dagger 2 to inject the dependency gracefully

// TODO: Shouldn't depend on any Android package!

open class SimpleEditorWidget(protected val paperID: Long,
                              protected val paperRepo: IPaperRepo,
                              protected val caughtErrorSignal: Observer<Throwable>,
                              schedulers: ISchedulerProvider)
    : EditorWidget(schedulers = schedulers) {

    override fun start(): Observable<Boolean> {
        return inflatePaperJob
    }

    protected val loadPaperJob: Single<IPaper> by lazy {
        paperRepo
            .getPaperById(paperID)
            .cache()
    }

    protected val inflatePaperJob: Observable<Boolean>
        get() {
            return loadPaperJob
                .doOnSubscribe {
                    dirtyFlag.markDirty(EditorDirtyFlag.READ_PAPER_FROM_REPO)
                }
                .flatMapObservable { paper ->
                    dirtyFlag.markNotDirty(EditorDirtyFlag.READ_PAPER_FROM_REPO)

                    inject(paper)

                    super.start()
                }
        }
}
