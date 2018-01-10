// Copyright (c) 2017-present boyw165
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

package com.paper

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.multidex.MultiDexApplication
import com.paper.router.*
import ru.terrakok.cicerone.commands.Command
import ru.terrakok.cicerone.commands.Forward

class PaperApplication : MultiDexApplication(),
                         IMyRouterHolderProvider {

    // Router holder.
    private val mRouterHolder: MyRouterHolder by lazy { MyRouterHolder() }

    // Router.
    private val mRouter: MyRouter by lazy {
        val uiHandler = Handler(Looper.getMainLooper())
        MyRouter(uiHandler)
    }

    override fun onCreate() {
        super.onCreate()

        mRouter.setNavigator(mNavigator)

        // Inject the application level router.
        mRouterHolder.pushAndBindParent(mRouter)
    }

    override fun onTerminate() {
        super.onTerminate()

        mRouter.unsetNavigator()
    }

    override fun getHolder(): MyRouterHolder? {
        return mRouterHolder
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    /**
     * Used by deep-link.
     */
    private val mNavigator: INavigator by lazy {
        object : INavigator {

            override fun onEnter() {
            }

            override fun onExit() {
            }

            override fun applyCommandAndWait(command: Command,
                                             future: INavigator.FutureResult): Boolean {
                if (command is Forward) {
                    when (command.screenKey) {
                        NavigationContract.SCREEN_OF_HOME -> {
                            startActivity(Intent(this@PaperApplication,
                                                 MyPaperGalleryActivity::class.java)
                                              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                              .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        }
                    }
                }

                // This navigator belongs to the top most router and it always
                // recognizes the given command.
                return true
            }
        }
    }
}
