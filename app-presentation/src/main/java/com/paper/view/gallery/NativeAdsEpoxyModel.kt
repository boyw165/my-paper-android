// Copyright May 2018-present Paper
//
// Author: boyw165@gmail.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.view.gallery

import android.view.View
import com.airbnb.epoxy.EpoxyModel
import com.facebook.ads.AdChoicesView
import com.facebook.ads.NativeAd
import com.paper.R
import io.reactivex.Observer
import android.widget.*
import com.facebook.ads.MediaView


class NativeAdsEpoxyModel(ads: NativeAd) : EpoxyModel<View>() {

    private val mAds = ads

    // View
    private var mNativeAdTitle: TextView? = null
    private var mNativeAdCallToAction: Button? = null
    private var mMediaView: MediaView? = null
    private var mAdChoicesParentView: FrameLayout? = null

    override fun getDefaultLayout(): Int {
        return R.layout.gallery_item_of_facebook_native_ads
    }

    override fun bind(view: View) {
        super.bind(view)

        // Click listener
        view.setOnClickListener {
            mOnClickSignal?.onNext(0)
        }

        if (mNativeAdTitle == null) {
            mNativeAdTitle = view.findViewById(R.id.native_ad_title)
        }
        if (mNativeAdCallToAction == null) {
            mNativeAdCallToAction = view.findViewById(R.id.native_ad_call_to_action)
        }
        if (mMediaView == null) {
            mMediaView = view.findViewById(R.id.native_ad_media)
        }
        if (mAdChoicesParentView == null) {
            mAdChoicesParentView = view.findViewById(R.id.native_ad_choice)
        }
        // The eplise style depends on devices, so we manually apply a fixed
        // style here.
        val maxLength = 20
        val title = if (mAds.adTitle.length <= maxLength) {
            mAds.adTitle
        } else {
            mAds.adTitle.substring(0, maxLength) + "..."
        }
        mNativeAdTitle?.text = title
        mNativeAdCallToAction?.text = mAds.adCallToAction

        // Download and display the cover image.
        mMediaView?.setNativeAd(mAds)

        // Add the AdChoices icon
        if (mAdChoicesParentView?.findViewWithTag<View>("ad_choices_view") == null) {
            val adChoicesView = AdChoicesView(view.context, mAds, true)
            adChoicesView.tag = "ad_choices_view"
            mAdChoicesParentView?.addView(adChoicesView)
        }

        // Register the Title and CTA button to listen for clicks.
        val clickableViews = listOf(mNativeAdTitle!!,
                                    mNativeAdCallToAction!!,
                                    mMediaView!!)
        mAds.registerViewForInteraction(view, clickableViews)
    }

    override fun unbind(view: View) {
        super.unbind(view)

        mAds.unregisterView()
    }

    // Click //////////////////////////////////////////////////////////////////

    private var mOnClickSignal: Observer<Any>? = null

    fun onClick(clickSignal: Observer<Any>): NativeAdsEpoxyModel {
        mOnClickSignal = clickSignal
        return this
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as NativeAdsEpoxyModel

        if (mAds != other.mAds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mAds.hashCode()
        return result
    }
}
