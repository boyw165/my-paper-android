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

package com.paper

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.paper.router.NavigationContract
import com.paper.router.IMyRouterHolderProvider
import com.paper.router.INavigator
import com.paper.router.MyRouter
import com.paper.router.MyRouterHolder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import ru.terrakok.cicerone.commands.Back
import ru.terrakok.cicerone.commands.Command
import ru.terrakok.cicerone.commands.Forward

class ExampleOfFlow1Page1Activity : AppCompatActivity() {

    // Router and router holder.
    private val mRouter: MyRouter by lazy {
        MyRouter(Handler(Looper.getMainLooper()))
    }
    private val mRouterHolder: MyRouterHolder
        get() = (application as IMyRouterHolderProvider).holder

    // View.
    private val mBtnBack: View by lazy { findViewById<View>(R.id.btn_back) }
    private val mBtnNext: View by lazy { findViewById<View>(R.id.btn_next) }

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_flow1_page1)

        // Link router to the router holder.
        mRouterHolder.pushAndBindParent(mRouter)

        // Next.
        mDisposablesOnCreate.add(
            RxView.clicks(mBtnNext)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mRouter.navigateTo(NavigationContract.SCREEN_OF_FLOW1_PAGE2)
                })

        // Back.
        mDisposablesOnCreate.add(
            Observable
                .merge(mOnClickSystemBack,
                       RxView.clicks(mBtnBack))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mRouter.exit()
                })
    }

    override fun onDestroy() {
        super.onDestroy()

        mDisposablesOnCreate.clear()

        // Unlink router to the router holder.
        mRouterHolder.popAndUnbindParent()
    }

    override fun onResume() {
        super.onResume()

        // Set navigator.
        mRouter.setNavigator(mNavigator)

        // Get the buffered Activity result.
        mRouter.dispatchResultOnResume()
    }

    override fun onPause() {
        super.onPause()

        // Remove navigator.
        mRouter.unsetNavigator()
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // RxCancelContract.View //////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // INavigator /////////////////////////////////////////////////////////////

    private val mNavigator: INavigator = object : INavigator {

        override fun onEnter() {
//            Log.d("navigation", "enter ----> $screenKey")
            Log.d("navigation", "enter ----> flow 1 - page 1")
        }

        override fun onExit() {
            Log.d("navigation", "exit <-----")
        }

        override fun applyCommandAndWait(command: Command,
                                         future: INavigator.FutureResult): Boolean {
            if (command is Back) {
                finish()
            } else if (command is Forward) {
                when (command.screenKey) {
                    NavigationContract.SCREEN_OF_FLOW1_PAGE2 -> {
                        val intent = Intent(this@ExampleOfFlow1Page1Activity,
                                            ExampleOfFlow1Page2Activity::class.java)
                        startActivity(intent)
                    }
                }
            }

            // Indicate the router this command is finished.
            future.finish()

            return true
        }
    }
}
