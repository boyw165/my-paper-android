package com.paper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.paper.router.IMyRouterProvider
import com.paper.router.NavigationContract
import com.paper.router.Router
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class ExampleOfCommonPageActivity : AppCompatActivity() {

    // View.
    private val mBtnBack: View by lazy { findViewById<View>(R.id.btn_back) }
    private val mBtnNext: View by lazy { findViewById<View>(R.id.btn_next) }

    // Router and router holder.
    private val mRouter: Router
        get() = (application as IMyRouterProvider).router

    // Disposables.
    private val mDisposablesOnCreate = CompositeDisposable()

    // Subjects.
    private val mOnClickSystemBack: Subject<Any> = PublishSubject.create()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        setContentView(R.layout.activity_common)

        mDisposablesOnCreate.add(
            RxView.clicks(mBtnNext)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    mRouter.navigateTo(NavigationContract.SCREEN_OF_FLOW1_PAGE3)
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

        // Set context to navigator.
        mRouter.bindContextToNavigator(Router.LEVEL_ACTIVITY,
                                       this@ExampleOfCommonPageActivity)

        // Get the buffered Activity result.
        mRouter.dispatchResultOnResume()
    }

    override fun onPause() {
        super.onPause()

        // Remove navigator's context.
        mRouter.unBindContextFromNavigator(Router.LEVEL_ACTIVITY)
    }

    override fun onBackPressed() {
        mOnClickSystemBack.onNext(0)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////
}
