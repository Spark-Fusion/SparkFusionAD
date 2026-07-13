package com.sparkfusionad.sdk

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView

internal class SparkFusionSplashAD {

    fun createView(
        context: Context,
        adData: SparkFusionAdData,
        onSkipClick: () -> Unit = {},
        onActionClick: () -> Unit = {}
    ): View {
        val view = LayoutInflater.from(context).inflate(R.layout.splash_ad_view, null, false)
        view.setOnClickListener { onActionClick() }

        SparkFusionImageLoader.load(
            imageView = view.findViewById(R.id.applogo),
            url = adData.appLogoUrl,
            fallbackResId = adData.appLogoRes
        )
        SparkFusionImageLoader.load(
            imageView = view.findViewById(R.id.promotional),
            url = adData.promoImageUrl,
            fallbackResId = adData.promoImageRes
        )
        view.findViewById<TextView>(R.id.appname).text = adData.appName
        view.findViewById<TextView>(R.id.content).text = adData.description
        view.findViewById<TextView>(R.id.skip).apply {
            text = "跳过${adData.splashDurationSeconds}秒"
            setOnClickListener { onSkipClick() }
        }
        view.findViewById<View>(R.id.actionContainer).setOnClickListener { onActionClick() }

        return view
    }
}
