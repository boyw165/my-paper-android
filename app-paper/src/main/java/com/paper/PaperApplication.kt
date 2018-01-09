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

import android.os.Handler
import android.os.Looper
import android.support.multidex.MultiDexApplication
import com.paper.router.IMyRouterHolderProvider
import com.paper.router.INavigator
import com.paper.router.MyRouter
import com.paper.router.MyRouterHolder
import ru.terrakok.cicerone.commands.Command

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
        mRouterHolder.push(mRouter)
    }

    override fun getHolder(): MyRouterHolder? {
        return mRouterHolder
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private val mNavigator:INavigator by lazy {
        object : INavigator {

            override fun onEnter() {
            }

            override fun onExit() {
            }

            override fun applyCommand(command: Command,
                                      future: INavigator.FutureResult): Boolean {
                return true
            }
        }
    }
}
