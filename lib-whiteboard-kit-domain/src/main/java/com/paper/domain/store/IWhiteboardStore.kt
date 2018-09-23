// Copyright Sep 2018-present Whiteboard
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

package com.paper.domain.store

import com.paper.domain.ui.ILifecycleAware
import com.paper.model.Whiteboard
import com.paper.model.command.WhiteboardCommand
import io.reactivex.Observable
import io.reactivex.Single

interface IWhiteboardStore : ILifecycleAware {

    val whiteboard: Whiteboard?

    val whiteboardLoaded: Single<Whiteboard>

    val busy: Observable<Boolean>

    fun offerCommandDoo(command: WhiteboardCommand)

    fun offerCommandUndo(command: WhiteboardCommand)
}
