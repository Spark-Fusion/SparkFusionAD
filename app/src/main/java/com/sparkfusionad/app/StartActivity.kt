package com.sparkfusionad.app

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sparkfusionad.app.application.SdkManager
import com.sparkfusionad.app.config.Common
import com.sparkfusionad.app.databinding.ActivityStartBinding
import com.sparkfusionad.sdk.SparkFusionAd
import com.sparkfusionad.sdk.SparkFusionAdLoadListener
import com.sparkfusionad.sdk.SparkFusionAdShowListener
import com.wdit.shrmtfx.dcllk.kllcd.config.AppConfig
import com.sparkfusionad.app.R
import com.sparkfusionad.app.dialog.DialogHelper

class StartActivity : AppCompatActivity(){
    private lateinit var binding: ActivityStartBinding
    private lateinit var appConfig: AppConfig
    var canJumpImmediately: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)


        appConfig = AppConfig()
        if (appConfig.getFirst()) {
            //第一次进入
            this.let {
                DialogHelper.showLaunchAgreementDialog(
                    it,
                    getString(R.string.welcome_use),
                    onClickWeb = { title, url ->
                        WebViewActivity.start(this, title, url)
                    },
                    onAgree = {
                        if(SdkManager.initAd()){
                            loadAndShowSplashAd()
                        }else{
                            countdown()
                        }
                        appConfig.setFirst(false)
                    },
                    onCancel = {
                        finish()
                    })
            }
            //}
        } else {
            if(SdkManager.initAd()){
                loadAndShowSplashAd()
            }else{
                countdown()
            }
        }




    }

    private fun loadAndShowSplashAd() {
        SparkFusionAd.loadSFSplashAd(
            context = this,
            adId = Common.POS_ID_Splash,
            loadThirdPartyAd = {
                //加载其他广告方法
                Toast.makeText(this, "加载第三方开屏广告", Toast.LENGTH_SHORT).show()
                Log.d("SparkFusionAd", "加载第三方开屏广告")
                countdown()
            },
            listener = SparkFusionAdLoadListener(
                onAdLoadSuccess = {
                    Log.d("SparkFusionAd", "开屏广告加载成功")
                    SparkFusionAd.showSFSplashAd(
                        binding.fl,
                        SparkFusionAdShowListener(
                            onAdShowSuccess = {
                                Log.d("SparkFusionAd", "开屏广告显示成功")
                            },
                            onAdShowFailure = {
                                countdown()
                                Log.d("SparkFusionAd", "开屏广告显示失败")
                            },
                            onAdClick = {
                                Log.d("SparkFusionAd", "开屏广告被点击")
                            },
                            onAdClose = {
                                startActivity()
                                Log.d("SparkFusionAd", "开屏广告被关闭")
                            }
                        )
                    )
                },
                onAdLoadFailure = {
                    countdown()
                    Log.d("SparkFusionAd", "开屏广告加载失败")
                }
            )
        )
    }

    //倒计时
    private fun countdown() {
        val timer: CountDownTimer = object : CountDownTimer(
            2500, 10
        ) {
            override fun onTick(millisUntilFinished: Long) {
                //tv_time.setText(millisUntilFinished/1000+"秒");
            }

            override fun onFinish() {
                startActivity()

            }
        }
        timer.start()
    }
    //跳转到主页
    private fun startActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun jumpWhenCanClick() {
        if (canJumpImmediately) {
            startActivity()
        } else {
            canJumpImmediately = true
        }
    }

    override fun onPause() {
        super.onPause()
        canJumpImmediately = false;
    }

    override fun onResume() {
        super.onResume()
        if (canJumpImmediately) {
            jumpWhenCanClick();
        }
        canJumpImmediately = true;
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
