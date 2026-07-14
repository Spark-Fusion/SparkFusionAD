package com.sparkfusionad.sdk

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView

internal class SparkFusionRewardAD {

    fun createView(
        context: Context,
        adData: SparkFusionAdData,
        durationSeconds: Int,
        onSkipClick: () -> Unit = {},
        onActionClick: () -> Unit = {}
    ): View {
        val view = LayoutInflater.from(context).inflate(R.layout.reward_ad_view, null, false)
        view.setOnClickListener { onActionClick() }

        SparkFusionImageLoader.load(
            imageView = view.findViewById(R.id.applogo),
            url = adData.appLogoUrl,
            fallbackResId = adData.appLogoRes
        )
        SparkFusionImageLoader.load(
            imageView = view.findViewById(R.id.notice_applogo),
            url = adData.appLogoUrl,
            fallbackResId = adData.appLogoRes
        )

        view.findViewById<TextView>(R.id.appname).text = adData.appName
        view.findViewById<TextView>(R.id.content).text = adData.description
        view.findViewById<TextView>(R.id.notice_appname).text = adData.appName

        view.findViewById<TextView>(R.id.download).apply {
            text = adData.callToAction
            setOnClickListener { onActionClick() }
        }

        view.findViewById<TextView>(R.id.skip).apply {
            text = "跳过${durationSeconds}秒"
            setOnClickListener { onSkipClick() }
        }

        view.findViewById<ImageView>(R.id.notice_applogo).setOnClickListener { onActionClick() }
        view.findViewById<TextView>(R.id.notice_appname).setOnClickListener { onActionClick() }

        return view
    }
}
