package com.sparkfusionad.sdk

import android.content.Context
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
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
        applyThemeColor(view, adData.themeColor)

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
        startShakeAnimation(view.findViewById(R.id.shakeIcon))

        return view
    }

    private fun applyThemeColor(view: View, themeColor: String?) {
        val raw = themeColor?.trim().orEmpty()
        if (raw.isBlank()) {
            return
        }
        val normalized = if (raw.startsWith("#")) raw else "#$raw"
        val color = runCatching { Color.parseColor(normalized) }.getOrNull() ?: return
        view.setBackgroundColor(color)
    }

    private fun startShakeAnimation(imageView: ImageView?) {
        if (imageView == null) {
            return
        }
        ObjectAnimator.ofFloat(imageView, View.TRANSLATION_X, 0f, -12f, 12f, -10f, 10f, 0f).apply {
            duration = 600L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
    }
}
