package com.paper

import android.content.Intent
import android.os.Bundle
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

class ExampleOfFlow1Page2Activity : AppCompatActivity() {

    // Router and router holder.
    private val mRouterHolder: MyRouterHolder
        get() = (application as IMyRouterHolderProvider).holder
    private val mRouter: MyRouter
        get() = mRouterHolder.peek()

    // View.
    private val mBtnBack: View by lazy { findViewById<View>(R.id.btn_back) }
    private val mBtnNext: View by lazy { findViewById<View>(R.id.btn_next) }
    private val mBtnGotoFlow2: View by lazy { findViewById<View>(R.id.btn_goto_flow2) }

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_flow1_page2)

        mDisposablesOnCreate.add(
            RxView.clicks(mBtnNext)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    mRouter.navigateTo(NavigationContract.SCREEN_OF_FLOW1_PAGE3)
                })

        mDisposablesOnCreate.add(
            RxView.clicks(mBtnGotoFlow2)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    mRouter.navigateTo(NavigationContract.SCREEN_OF_FLOW2_PAGE1)
                })

        mDisposablesOnCreate.add(
            Observable
                .merge(RxView.clicks(mBtnBack),
                       mOnClickSystemBack)
                .subscribe {
                    mRouter.exitWithResult(NavigationContract.ACTIVITY_RESULT_CODE,
                                           "Back from flow 1 - page 2.")
                })
    }

    override fun onDestroy() {
        super.onDestroy()

        mDisposablesOnCreate.clear()
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
            Log.d("navigation", "enter ----> flow 1 - page 2")
        }

        override fun onExit() {
            Log.d("navigation", "exit <-----")
        }

        override fun applyCommandAndWait(command: Command,
                                         future: INavigator.FutureResult): Boolean {
            when (command) {
                is Back -> finish()
                is Forward -> when (command.screenKey) {
                    NavigationContract.SCREEN_OF_FLOW1_PAGE3 -> {
                        val intent = Intent(this@ExampleOfFlow1Page2Activity,
                                            ExampleOfFlow1Page3Activity::class.java)
                        startActivity(intent)
                    }
                    NavigationContract.SCREEN_OF_FLOW2_PAGE1 -> {
                        val intent = Intent(this@ExampleOfFlow1Page2Activity,
                                            ExampleOfFlow2Page1Activity::class.java)
                        startActivity(intent)
                    }
                }
                else -> {
                    // Unrecognized command.
                    return false
                }
            }

            // Indicate the router this command is finished.
            future.finish()

            return true
        }
    }
}
