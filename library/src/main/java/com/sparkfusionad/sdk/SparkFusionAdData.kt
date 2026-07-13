package com.sparkfusionad.sdk

import androidx.annotation.DrawableRes

internal data class SparkFusionAdData(
    val appName: String,
    val description: String,
    val callToAction: String,
    val versionName: String,
    val clickUrl: String? = null,
    val appLogoUrl: String? = null,
    val promoImageUrl: String? = null,
    @DrawableRes val appLogoRes: Int,
    @DrawableRes val promoImageRes: Int,
    val splashDurationSeconds: Int = 5
)
