// Copyright Jan 2018-present CardinalBlue
//
// Author: jack.huang@cardinalblue.com
//         boy@cardinalblue.com
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

package com.paper.router;

import android.support.annotation.NonNull;

import ru.terrakok.cicerone.commands.Command;

public interface INavigator {

    void onEnter();

    void onExit();

    /**
     * Performs transition described by the navigation command
     *
     * @param command the navigation command to apply
     * @param future  Future to let you indicate the router the command is
     *                consumed.
     * @return If this navigator cab recognize the command (screen key).
     */
    boolean applyCommandAndWait(@NonNull Command command,
                                @NonNull FutureResult future);

    interface FutureResult {

        void finish();
    }
}
