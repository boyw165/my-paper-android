package com.paper.view

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.paper.R
import com.paper.exp.cicerone.CiceroneContract
import com.paper.exp.cicerone.view.CiceroneFragment1
import com.paper.protocol.IRouterProvider
import com.paper.router.MyRouter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import ru.terrakok.cicerone.Navigator
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.android.SupportAppNavigator

class ExampleOfCiceroneActivity2 : AppCompatActivity() {

    // Cicerone.
    private val mRouter: MyRouter by lazy { (application as IRouterProvider).router }
    private val mNavigatorHolder: NavigatorHolder by lazy { (application as IRouterProvider).holder }

    // View.
    private val mBtnExit: View by lazy { findViewById<View>(R.id.btn_exit) }
    private val mBtnExitWithResult: View by lazy { findViewById<View>(R.id.btn_exit_with_result) }

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_example_of_cicerone2)

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
                    mRouter.exitWithResult(CiceroneContract.ACTIVITY_RESULT_CODE, "From Act 2.")
                })
    }

    override fun onResume() {
        super.onResume()

        // Set navigator.
        mNavigatorHolder.setNavigator(mNavigator)
    }

    override fun onPause() {
        super.onPause()

        // Remove navigator.
        mNavigatorHolder.removeNavigator()
    }

    override fun onDestroy() {
        super.onDestroy()

        mDisposablesOnCreate.clear()
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private val mNavigator: Navigator by lazy {
        object : SupportAppNavigator(this@ExampleOfCiceroneActivity2,
                                     R.id.frame_container) {

            override fun createActivityIntent(screenKey: String,
                                              data: Any?): Intent? {
                return when (screenKey) {
                    CiceroneContract.SCREEN_NEW_ACTIVITY -> {
                        // TODO: set the data to intent.
                        Intent(this@ExampleOfCiceroneActivity2, ExampleOfCiceroneActivity2::class.java)
                            .putExtra(CiceroneContract.ACTIVITY_NUMBER_FLAG, data as Int)
                    }
                //                    else -> throw IllegalArgumentException("Unknown screen key.")
                    else -> null
                }
            }

            override fun createFragment(screenKey: String,
                                        data: Any?): Fragment? {
                return when (screenKey) {
                    CiceroneContract.SCREEN_NEW_FRAGMENT -> {
                        CiceroneFragment1.create(data as Int)
                    }
                //                    else -> throw IllegalArgumentException("Unknown screen key.")
                    else -> null
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // RxCancelContract.View //////////////////////////////////////////////////
}
