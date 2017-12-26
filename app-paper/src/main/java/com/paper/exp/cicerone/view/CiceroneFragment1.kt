package com.paper.exp.cicerone.view

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.paper.R
import com.paper.exp.cicerone.CiceroneContract
import ru.terrakok.cicerone.Navigator
import ru.terrakok.cicerone.android.SupportFragmentNavigator

class CiceroneFragment1 : Fragment() {

    // Router.
    var mRouter: CiceroneContract.CiceroneProvider? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cicerone_fragment1, container, false)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        mRouter = activity as CiceroneContract.CiceroneProvider
    }

    override fun onDetach() {
        super.onDetach()

        mRouter = null
    }

    companion object {
        fun create(): CiceroneFragment1 {
            val fragment = CiceroneFragment1()
//            val args = Bundle()
//            args.putString(ARG_PARAM1, param1)
//            args.putString(ARG_PARAM2, param2)
//            fragment.arguments = args
            return fragment
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    // Navigator.
    var mInnerNavigator: Navigator? = null

    private val mNavigator: Navigator = object :
        SupportFragmentNavigator(activity.supportFragmentManager,
                                 ) {

        override fun createFragment(screenKey: String?, data: Any?): Fragment {
            TODO("not supported")
        }

        override fun exit() {
            TODO("not supported")
        }

        override fun showSystemMessage(message: String?) {
            TODO("not supported")
        }
    }
}
