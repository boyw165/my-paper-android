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
import com.facebook.ads.AdChoicesView
import com.facebook.ads.NativeAd
import com.paper.R
import io.reactivex.Observer
import android.widget.*
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.facebook.ads.MediaView

class NativeAdsEpoxyViewModel(ads: NativeAd) : EpoxyModelWithHolder<EpoxyHolder>() {

    private val mAds = ads

    override fun getDefaultLayout(): Int {
        return R.layout.gallery_item_of_facebook_native_ads
    }

    override fun createNewHolder(): EpoxyHolder {
        return NativeAdsEpoxyViewModel.Holder()
    }

    override fun bind(holder: EpoxyHolder) {
        super.bind(holder)

        // Smart casting
        holder as NativeAdsEpoxyViewModel.Holder

        // Click listener
        holder.itemView.setOnClickListener {
            mOnClickSignal?.onNext(0)
        }

        // The eplise style depends on devices, so we manually apply a fixed
        // style here.
        val maxLength = 20
        val title = if (mAds.adTitle.length <= maxLength) {
            mAds.adTitle
        } else {
            mAds.adTitle.substring(0, maxLength) + "..."
        }
        holder.nativeAdTitle.text = title
        holder.nativeAdCallToAction.text = mAds.adCallToAction

        // Download and display the cover image.
        holder.mediaView.setNativeAd(mAds)

        // Add the AdChoices icon
        if (holder.adChoicesParentView.findViewWithTag<View>("ad_choices_view") == null) {
            val adChoicesView = AdChoicesView(holder.itemView.context, mAds, true)
            adChoicesView.tag = "ad_choices_view"
            holder.adChoicesParentView.addView(adChoicesView)
        }

        // Register the Title and CTA button to listen for clicks.
        val clickableViews = listOf(holder.nativeAdTitle,
                                    holder.nativeAdCallToAction,
                                    holder.mediaView)
        mAds.registerViewForInteraction(holder.itemView, clickableViews)
    }

    override fun unbind(holder: EpoxyHolder) {
        super.unbind(holder)

        // Smart casting
        holder as NativeAdsEpoxyViewModel.Holder

        mAds.unregisterView()

        // Click listener
        holder.itemView.setOnClickListener(null)
    }

    // Click //////////////////////////////////////////////////////////////////

    private var mOnClickSignal: Observer<Any>? = null

    fun onClick(clickSignal: Observer<Any>): NativeAdsEpoxyViewModel {
        mOnClickSignal = clickSignal
        return this
    }

    // Equality & hash ////////////////////////////////////////////////////////

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as NativeAdsEpoxyViewModel

        if (mAds != other.mAds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mAds.hashCode()
        return result
    }

    // Clazz //////////////////////////////////////////////////////////////////

    class Holder : EpoxyHolder() {

        lateinit var itemView: View

        // View
        lateinit var nativeAdTitle: TextView
        lateinit var nativeAdCallToAction: Button
        lateinit var mediaView: MediaView
        lateinit var adChoicesParentView: FrameLayout

        override fun bindView(itemView: View) {
            this.itemView = itemView

            nativeAdTitle = itemView.findViewById(R.id.native_ad_title)
            nativeAdCallToAction = itemView.findViewById(R.id.native_ad_call_to_action)
            mediaView = itemView.findViewById(R.id.native_ad_media)
            adChoicesParentView = itemView.findViewById(R.id.native_ad_choice)
        }
    }
}
