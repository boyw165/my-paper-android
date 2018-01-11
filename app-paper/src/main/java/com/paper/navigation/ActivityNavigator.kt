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

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.paper.ExampleOfCommonPageActivity
import com.paper.ExampleOfFlow1Page1Activity
import com.paper.ExampleOfFlow1Page3Activity
import com.paper.ExampleOfFlow2Page1Activity
import com.paper.router.INavigator
import com.paper.router.NavigationContract
import ru.terrakok.cicerone.commands.Back
import ru.terrakok.cicerone.commands.Command
import ru.terrakok.cicerone.commands.Forward
import java.lang.ref.WeakReference

class ActivityNavigator : INavigator {

    private var mActivity: WeakReference<Activity>? = null

    override fun onEnter() {
    }

    override fun onExit() {
    }

    override fun applyCommandAndWait(command: Command,
                                     future: INavigator.FutureResult) {
        val activity = mActivity?.get() ?: return

        when (command) {
            is Back -> activity.finish()
            is Forward -> when (command.screenKey) {
                NavigationContract.SCREEN_OF_RXCANCEL_PAGE1 -> {
                    activity.startActivity(Intent(
                        activity,
                        ExampleOfFlow1Page1Activity::class.java))
                }

                NavigationContract.SCREEN_OF_FLOW1_PAGE1 -> {
                    val intent = Intent(activity,
                                        ExampleOfFlow1Page1Activity::class.java)
                    activity.startActivity(intent)
                }
                NavigationContract.SCREEN_OF_FLOW1_PAGE2 -> {
                    val intent = Intent(activity,
                                        ExampleOfCommonPageActivity::class.java)
                    activity.startActivity(intent)
                }
                NavigationContract.SCREEN_OF_FLOW1_PAGE3 -> {
                    val intent = Intent(activity,
                                        ExampleOfFlow1Page3Activity::class.java)
                    activity.startActivity(intent)
                }

                NavigationContract.SCREEN_OF_FLOW2_PAGE1 -> {
                    val intent = Intent(activity,
                                        ExampleOfFlow2Page1Activity::class.java)
                    activity.startActivity(intent)
                }
                NavigationContract.SCREEN_OF_FLOW2_PAGE2 -> {
                    val intent = Intent(activity,
                                        ExampleOfCommonPageActivity::class.java)
                    activity.startActivity(intent)
                }
            }
        }

        // Indicate the router this command is finished.
        future.finish()
    }

    override fun bindContext(context: Context) {
        mActivity = WeakReference(context as Activity)
    }

    override fun unBindContext() {
        mActivity = null
    }
}
