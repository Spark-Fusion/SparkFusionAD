package com.sparkfusionad.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sparkfusionad.app.config.Common
import com.sparkfusionad.app.databinding.ActivityMainBinding
import com.sparkfusionad.sdk.SparkFusionAd
import com.sparkfusionad.sdk.SparkFusionAdLoadListener
import com.sparkfusionad.sdk.SparkFusionAdShowListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        SparkFusionAd.loadSFBannerAd(
            context = this,
            adId = Common.POS_ID_BANNER,
            listener = SparkFusionAdLoadListener(
                onAdLoadSuccess = {
                    SparkFusionAd.showSFBannerAd(
                        binding.fl,
                        SparkFusionAdShowListener(
                            onAdShowSuccess = {},
                            onAdShowFailure = {},
                            onAdClick = {},
                            onAdClose = {}
                        )
                    )
                },
                onAdLoadFailure = {}
            )
        )
        binding.button.setOnClickListener {
            SparkFusionAd.loadSFInterstitialAd(
                context = this,
                adId = Common.POS_ID_Insert,
                listener = SparkFusionAdLoadListener(
                    onAdLoadSuccess = {
                        SparkFusionAd.showSFInterstitialAd(
                            this,
                            SparkFusionAdShowListener(
                                onAdShowSuccess = {},
                                onAdShowFailure = {},
                                onAdClick = {},
                                onAdClose = {}
                            )
                        )
                    },
                    onAdLoadFailure = {}
                )
            )
        }
        binding.button2.setOnClickListener {
            SparkFusionAd.showSFVideoAd( this, true, onAdLoadSuccess = {}, onAdLoadError = {}, onAdClose = {})
        }

    }
}
