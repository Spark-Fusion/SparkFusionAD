package com.sparkfusionad.sdk.freelybase

import android.util.Log
import com.sparkfusionad.sdk.freelybase.bean.Addata
import com.sparkfusionad.sdk.freelybase.bean.Adspace
import io.freelybase.android.CachePolicy
import io.freelybase.android.FreelyQuery

/**
 *作者：daboluo on 2026/7/13 23:54
 *Email:daboluo719@gmail.com
 */
object SparkFusingAdDataRep {

    private const val TAG = "SparkFusingAdDataRep"

    //获取开屏广告数据
    fun getSplashAdMutableList(
        splashId: String,
        onSuccess: (MutableList<Addata>) -> Unit,
        onFailure: (Throwable) -> Unit = {}
    ) {
        getAdMutableList(splashId, onSuccess, onFailure)
    }

    //获取banner广告数据
    fun getBannerAdMutableList(
        bannerId: String,
        onSuccess: (MutableList<Addata>) -> Unit,
        onFailure: (Throwable) -> Unit = {}
    ) {
        getAdMutableList(bannerId, onSuccess, onFailure)
    }

    //获取插屏广告数据
    fun getInsertAdMutableList(
        insertId: String,
        onSuccess: (MutableList<Addata>) -> Unit,
        onFailure: (Throwable) -> Unit = {}
    ) {
        getAdMutableList(insertId, onSuccess, onFailure)
    }

    private fun getAdMutableList(
        adSpaceId: String,
        onSuccess: (MutableList<Addata>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        if (adSpaceId.isBlank()) {
            val error = IllegalArgumentException("adSpaceId 不能为空")
            Log.e(TAG, "getAdMutableList: adSpaceId is blank")
            onFailure(error)
            return
        }

        Log.d(TAG, "getAdMutableList start, adSpaceId=$adSpaceId")

        FreelyQuery<Adspace>()
            .cachePolicy(CachePolicy.NETWORK_ELSE_CACHE)
            .include("adcontent")
            .getObject(adSpaceId)
            .onSuccess { adSpace ->
                val adList = adSpace.adcontent.getItems().toMutableList()
                Log.d(
                    TAG,
                    "getAdMutableList success, adSpaceId=$adSpaceId, adCount=${adList.size}, ads=${summarizeAds(adList)}"
                )
                onSuccess(adList)
            }
            .onFailure { error ->
                Log.e(TAG, "getAdMutableList failed, adSpaceId=$adSpaceId", error)
                onFailure(error)
            }
    }

    private fun summarizeAds(adList: List<Addata>): String {
        if (adList.isEmpty()) {
            return "[]"
        }
        return adList.joinToString(
            prefix = "[",
            postfix = "]"
        ) { ad ->
            "{app=${ad.appname.orEmpty()}, weight=${ad.weight}, url=${ad.url.orEmpty()}, download=${ad.isDownload}}"
        }
    }
}
