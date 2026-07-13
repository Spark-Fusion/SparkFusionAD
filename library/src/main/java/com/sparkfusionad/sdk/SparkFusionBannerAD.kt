package com.sparkfusionad.sdk

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView

internal class SparkFusionBannerAD {

    fun createView(
        context: Context,
        adData: SparkFusionAdData,
        onClose: () -> Unit = {},
        onActionClick: () -> Unit = {}
    ): View {
        val view = LayoutInflater.from(context).inflate(R.layout.banner_ad_view, null, false)
        view.setOnClickListener { onActionClick() }

        SparkFusionImageLoader.load(
            imageView = view.findViewById(R.id.applogo),
            url = adData.appLogoUrl,
            fallbackResId = adData.appLogoRes
        )
        view.findViewById<TextView>(R.id.appname).text = adData.appName
        view.findViewById<TextView>(R.id.content).text = adData.description
        view.findViewById<TextView>(R.id.download).apply {
            text = adData.callToAction
            setOnClickListener { onActionClick() }
        }
        view.findViewById<ImageView>(R.id.close).setOnClickListener { onClose() }

        return view
    }
}
