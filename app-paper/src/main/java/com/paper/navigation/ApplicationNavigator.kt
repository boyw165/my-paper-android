// Copyright (c) 2017-present CardinalBlue
//
// Author: jack.huang@cardinalblue.com
//         boy@cardinalblue.com
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.paper.navigation

import android.app.Application
import android.content.Intent
import com.paper.MyPaperGalleryActivity
import com.paper.router.INavigator
import com.paper.router.NavigationContract
import ru.terrakok.cicerone.commands.Command
import ru.terrakok.cicerone.commands.Forward
import java.lang.ref.WeakReference

class ApplicationNavigator(
    private val mApplication: WeakReference<Application>)
    : INavigator {

    override fun onEnter() {
    }

    override fun onExit() {
    }

    override fun applyCommandAndWait(command: Command,
                                     future: INavigator.FutureResult) {
        val application: Application = mApplication.get() ?: return

        if (command is Forward) {
            when (command.screenKey) {
                NavigationContract.SCREEN_OF_HOME -> {
                    application.startActivity(
                        Intent(application,
                               MyPaperGalleryActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                }
                NavigationContract.SCREEN_OF_FLOW1_PAGE1 -> {

                }
            }
        }
    }
}
