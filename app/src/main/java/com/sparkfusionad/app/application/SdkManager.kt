package com.sparkfusionad.app.application

import com.sparkfusionad.sdk.SparkFusionAd


/**
 *作者：daboluo on 2025/7/8 23:03
 *Email:daboluo719@gmail.com
 * SDK· 初始化
 */
object SdkManager {
    private val TAG = "SdkManager"
    /**
     * 初始化广告
     */
    fun initAd(): Boolean {
        try {
            SparkFusionAd.initSparkFusionAd(AppContextHolder.getApplication())
            return true
        }catch (e:Exception){
            //Log.d(TAG, "initAd Error:${e} ")
            return false
        }
    }
}