package com.sparkfusionad.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sparkfusionad.app.databinding.ActivityMainBinding
import com.sparkfusionad.sdk.SparkFusionAd

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        SparkFusionAd.showSFBannerAd(binding.fl)
        binding.button.setOnClickListener {
            SparkFusionAd.showSFInterstitialAd( this,1, true)
        }
        binding.button2.setOnClickListener {
            SparkFusionAd.showSFVideoAd( this, true, onAdLoadSuccess = {}, onAdLoadError = {}, onAdClose = {})
        }

    }
}