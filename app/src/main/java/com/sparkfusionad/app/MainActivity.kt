package com.sparkfusionad.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sparkfusionad.app.config.Common
import com.sparkfusionad.app.databinding.ActivityMainBinding
import com.sparkfusionad.sdk.SparkFusionAd
import com.sparkfusionad.sdk.SparkFusionAdLoadListener
import com.sparkfusionad.sdk.SparkFusionRewardAdShowListener
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
            loadThirdPartyAd = {
                //加载其他广告方法
                Toast.makeText(this, "加载第三方banner广告", Toast.LENGTH_SHORT).show()
                Log.d("SparkFusionAd", "加载第三方banner广告")
            },
            listener = SparkFusionAdLoadListener(
                onAdLoadSuccess = {
                    Log.d("SparkFusionAd", "banner广告加载成功")
                    SparkFusionAd.showSFBannerAd(
                        binding.fl,
                        SparkFusionAdShowListener(
                            onAdShowSuccess = {
                                Log.d("SparkFusionAd", "banner广告显示成功")
                            },
                            onAdShowFailure = {
                                Log.d("SparkFusionAd", "banner广告显示失败")
                            },
                            onAdClick = {
                                Log.d("SparkFusionAd", "banner广告点击")
                            },
                            onAdClose = {
                                Log.d("SparkFusionAd", "banner广告关闭")
                            }
                        )
                    )
                },
                onAdLoadFailure = {
                    Log.d("SparkFusionAd", "banner广告加载失败")
                }
            )
        )
        binding.button.setOnClickListener {
            SparkFusionAd.loadSFInterstitialAd(
                context = this,
                adId = Common.POS_ID_Insert,
                loadThirdPartyAd = {
                    //加载其他广告方法
                    Toast.makeText(this, "加载第三方插屏广告", Toast.LENGTH_SHORT).show()
                    Log.d("SparkFusionAd", "加载第三方插屏广告")
                },
                listener = SparkFusionAdLoadListener(
                    onAdLoadSuccess = {
                        Log.d("SparkFusionAd", "插屏广告加载成功")
                        SparkFusionAd.showSFInterstitialAd(
                            this,
                            SparkFusionAdShowListener(
                                onAdShowSuccess = {
                                    Log.d("SparkFusionAd", "插屏广告显示成功")
                                },
                                onAdShowFailure = {
                                    Log.d("SparkFusionAd", "插屏广告显示失败")
                                },
                                onAdClick = {
                                    Log.d("SparkFusionAd", "插屏广告点击")
                                },
                                onAdClose = {
                                    Log.d("SparkFusionAd", "插屏广告关闭")
                                }
                            )
                        )
                    },
                    onAdLoadFailure = {
                        Log.d("SparkFusionAd", "插屏广告加载失败")
                    }
                )
            )
        }
        binding.button2.setOnClickListener {
            SparkFusionAd.loadSFVideoAd(
                context = this,
                adId = Common.POS_ID_REWARD,
                loadThirdPartyAd = {
                    Toast.makeText(this, "加载第三方激励视频广告", Toast.LENGTH_SHORT).show()
                    Log.d("SparkFusionAd", "加载第三方激励视频广告")
                },
                listener = SparkFusionAdLoadListener(
                    onAdLoadSuccess = {
                        Log.d("SparkFusionAd", "激励视频广告加载成功")
                        SparkFusionAd.showSFVideoAd(
                            activity = this,
                            listener = SparkFusionRewardAdShowListener(
                                onAdShowSuccess = {
                                    Log.d("SparkFusionAd", "激励视频广告显示成功")
                                },
                                onAdShowFailure = {
                                    Log.d("SparkFusionAd", "激励视频广告显示失败")
                                },
                                onAdClick = {
                                    Log.d("SparkFusionAd", "激励视频广告点击")
                                },
                                onAdClose = {
                                    Log.d("SparkFusionAd", "激励视频广告关闭")
                                },
                                onReward = {
                                    Log.d("SparkFusionAd", "激励视频广告奖励成功")
                                }
                            )
                        )
                    },
                    onAdLoadFailure = {
                        Log.d("SparkFusionAd", "激励视频广告加载失败")
                    }
                )
            )
        }

    }
}
