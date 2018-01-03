package com.paper.exp.cicerone.view

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import com.paper.R
import com.paper.exp.cicerone.CiceroneContract
import com.paper.protocol.IRouterProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class CiceroneFragment1 : Fragment() {

    // Router.
    //    var mRouter: CiceroneContract.CiceroneProvider? = null

    // View.
    private lateinit var mTvFragmentNumber: TextView
    private lateinit var mBtnBack: Button

    // Disposables.
    private val mDisposablesOnCreateView = CompositeDisposable()

    private var mFragmentNum = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val viewGroup: View = inflater.inflate(R.layout.fragment_cicerone_fragment1, container, false)
        mTvFragmentNumber = viewGroup.findViewById(R.id.tv_fragment_number)
        mBtnBack = viewGroup.findViewById(R.id.btn_back_prev_frag)

        mFragmentNum = arguments!!.getInt(CiceroneContract.FRAGMENT_NUMBER_FLAG)
        mTvFragmentNumber.text = "Fragment ${mFragmentNum.toString()}"

        mDisposablesOnCreateView.add(
            RxView.clicks(mBtnBack)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    (activity!!.application as IRouterProvider).router.exit()
                })
        return viewGroup
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mDisposablesOnCreateView.clear()
    }

    fun getCurrentNumber(): Int {
        return mFragmentNum
    }

    companion object {
        fun create(fragmentNumber: Int): CiceroneFragment1 {
            val fragment = CiceroneFragment1()
            val args = Bundle()
            args.putInt(CiceroneContract.FRAGMENT_NUMBER_FLAG, fragmentNumber)
            fragment.arguments = args
            return fragment
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////
    //
    //    // Navigator.
    //    var mInnerNavigator: Navigator? = null
    //
    //    private val mNavigator: Navigator = object :
    //        SupportFragmentNavigator(activity.supportFragmentManager,
    //                                 ) {
    //
    //        override fun createFragment(screenKey: String?, data: Any?): Fragment {
    //            TODO("not supported")
    //        }
    //
    //        override fun exit() {
    //            TODO("not supported")
    //        }
    //
    //        override fun showSystemMessage(message: String?) {
    //            TODO("not supported")
    //        }
    //    }
}
