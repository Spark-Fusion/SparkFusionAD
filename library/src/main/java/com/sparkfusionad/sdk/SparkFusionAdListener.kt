package com.sparkfusionad.sdk

data class SparkFusionAdLoadListener(
    val onAdLoadSuccess: () -> Unit = {},
    val onAdLoadFailure: (Throwable) -> Unit = {}
)

data class SparkFusionAdShowListener(
    val onAdShowSuccess: () -> Unit = {},
    val onAdShowFailure: (Throwable) -> Unit = {},
    val onAdClick: () -> Unit = {},
    val onAdClose: () -> Unit = {}
)
