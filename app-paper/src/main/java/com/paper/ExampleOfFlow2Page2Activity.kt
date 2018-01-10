package com.paper

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

class ExampleOfFlow2Page2Activity : AppCompatActivity() {

    // Router and router holder.
    private val mRouterHolder: MyRouterHolder
        get() = (application as IMyRouterHolderProvider).holder
    private val mRouter: MyRouter
        get() = mRouterHolder.peek()

    // View.
    private val mBtnExit: View by lazy { findViewById<View>(R.id.btn_exit) }
    private val mBtnExitWithResult: View by lazy { findViewById<View>(R.id.btn_exit_with_result) }

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_flow2_page2)

        mDisposablesOnCreate.add(
            RxView.clicks(mBtnExit)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    mRouter.exit()
                })

        mDisposablesOnCreate.add(
            Observable
                .merge(RxView.clicks(mBtnExitWithResult),
                       mOnClickSystemBack)
                .subscribe {
                    mRouter.exitWithResult(NavigationContract.ACTIVITY_RESULT_CODE, "From Act 2.")
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
            Log.d("/routing#2", "enter ---->")
        }

        override fun onExit() {
            Log.d("/routing#2", "exit <-----")
        }

        override fun applyCommandAndWait(command: Command,
                                         future: INavigator.FutureResult): Boolean {
            if (command is Back) {
                finish()
            }

            // Indicate the router this command is finished.
            future.finish()

            return true
        }
    }
}
