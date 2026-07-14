package com.sparkfusionad.sdk

import android.app.Activity
import android.content.Context
import android.view.ViewGroup

interface SparkFusionThirdPartyAdLoader {
    fun loadSplashAd(context: Context, adId: String, listener: SparkFusionAdLoadListener = SparkFusionAdLoadListener())
    fun loadBannerAd(context: Context, adId: String, listener: SparkFusionAdLoadListener = SparkFusionAdLoadListener())
    fun loadInterstitialAd(context: Context, adId: String, listener: SparkFusionAdLoadListener = SparkFusionAdLoadListener())
    fun loadRewardAd(context: Context, adId: String, listener: SparkFusionAdLoadListener = SparkFusionAdLoadListener())

    fun showSplashAd(container: ViewGroup, listener: SparkFusionAdShowListener = SparkFusionAdShowListener())
    fun showBannerAd(container: ViewGroup, listener: SparkFusionAdShowListener = SparkFusionAdShowListener())
    fun showInterstitialAd(activity: Activity, listener: SparkFusionAdShowListener = SparkFusionAdShowListener())
    fun showRewardAd(activity: Activity, listener: SparkFusionRewardAdShowListener = SparkFusionRewardAdShowListener())
}

