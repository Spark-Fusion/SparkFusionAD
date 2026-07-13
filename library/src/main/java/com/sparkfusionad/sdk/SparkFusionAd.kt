package com.sparkfusionad.sdk

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.sparkfusionad.sdk.freelybase.SparkFusingAdDataRep
import com.sparkfusionad.sdk.freelybase.bean.Addata
import io.freelybase.android.Freelybase
import kotlin.math.ceil
import kotlin.random.Random

object SparkFusionAd {

    private const val TAG = "SparkFusionAd"
    private const val SHAKE_THRESHOLD_GRAVITY = 2.2f
    private const val SHAKE_DEBOUNCE_MILLIS = 1000L

    private var isInitAd = false
    private var splashTimer: CountDownTimer? = null
    private var splashSensorManager: SensorManager? = null
    private var splashShakeListener: SensorEventListener? = null
    private var lastShakeTriggerAt = 0L
    private var splashActionTriggered = false

    private var currentSplashAd: LoadedAd? = null
    private var currentBannerAd: LoadedAd? = null
    private var currentInsertAd: LoadedAd? = null

    private val splashRenderer = SparkFusionSplashAD()
    private val bannerRenderer = SparkFusionBannerAD()
    private val insertRenderer = SparkFusionInsertAD()

    fun initSparkFusionAd(context: Context, appKey: String) {
        Freelybase.initialize(context.applicationContext, appKey, false)
        isInitAd = true
        clearLoadedAds()

        Log.d(TAG, "initSparkFusionAd success, appKey=$appKey")
        Toast.makeText(context, "initSparkFusionAd success", Toast.LENGTH_SHORT).show()
    }

    fun loadSFSplashAd(
        context: Context,
        adId: String,
        listener: SparkFusionAdLoadListener = SparkFusionAdLoadListener()
    ) {
        if (!checkInit(context, listener.onAdLoadFailure)) {
            return
        }

        loadAd(
            adType = "splash",
            adId = adId,
            onSuccess = { loadedAd ->
                currentSplashAd = loadedAd
                Log.d(TAG, "loadSFSplashAd success: ${loadedAd.adData.appName}")
                listener.onAdLoadSuccess()
            },
            onFailure = listener.onAdLoadFailure,
            fetch = { targetId, onSuccess, onFailure ->
                SparkFusingAdDataRep.getSplashAdMutableList(targetId, onSuccess, onFailure)
            }
        )
    }

    fun loadSFBannerAd(
        context: Context,
        adId: String,
        listener: SparkFusionAdLoadListener = SparkFusionAdLoadListener()
    ) {
        if (!checkInit(context, listener.onAdLoadFailure)) {
            return
        }

        loadAd(
            adType = "banner",
            adId = adId,
            onSuccess = { loadedAd ->
                currentBannerAd = loadedAd
                Log.d(TAG, "loadSFBannerAd success: ${loadedAd.adData.appName}")
                listener.onAdLoadSuccess()
            },
            onFailure = listener.onAdLoadFailure,
            fetch = { targetId, onSuccess, onFailure ->
                SparkFusingAdDataRep.getBannerAdMutableList(targetId, onSuccess, onFailure)
            }
        )
    }

    fun loadSFInterstitialAd(
        context: Context,
        adId: String,
        listener: SparkFusionAdLoadListener = SparkFusionAdLoadListener()
    ) {
        if (!checkInit(context, listener.onAdLoadFailure)) {
            return
        }

        loadAd(
            adType = "insert",
            adId = adId,
            onSuccess = { loadedAd ->
                currentInsertAd = loadedAd
                Log.d(TAG, "loadSFInterstitialAd success: ${loadedAd.adData.appName}")
                listener.onAdLoadSuccess()
            },
            onFailure = listener.onAdLoadFailure,
            fetch = { targetId, onSuccess, onFailure ->
                SparkFusingAdDataRep.getInsertAdMutableList(targetId, onSuccess, onFailure)
            }
        )
    }

    fun showSFSplashAd(
        view: ViewGroup,
        listener: SparkFusionAdShowListener = SparkFusionAdShowListener()
    ) {
        val loadedAd = currentSplashAd
        if (!checkInit(view.context, listener.onAdShowFailure)) {
            return
        }
        if (loadedAd == null) {
            listener.onAdShowFailure(IllegalStateException("开屏广告未加载，请先调用 loadSFSplashAd"))
            return
        }

        splashTimer?.cancel()
        unregisterSplashShakeListener()
        splashActionTriggered = false

        try {
            val splashView = splashRenderer.createView(
                context = view.context,
                adData = loadedAd.adData,
                onSkipClick = { dismissSplash(view, listener.onAdClose) },
                onActionClick = {
                    if (!splashActionTriggered) {
                        splashActionTriggered = true
                        listener.onAdClick()
                        handleAdClick(view.context, loadedAd.adData)
                    }
                }
            )

            view.removeAllViews()
            view.addView(
                splashView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            registerSplashShakeListener(
                context = view.context,
                onShake = {
                    if (!splashActionTriggered) {
                        splashActionTriggered = true
                        listener.onAdClick()
                        handleAdClick(view.context, loadedAd.adData)
                    }
                }
            )

            val skipView = splashView.findViewById<TextView>(R.id.skip)
            val totalMillis = loadedAd.adData.splashDurationSeconds * 1000L
            splashTimer = object : CountDownTimer(totalMillis, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    val seconds = ceil(millisUntilFinished / 1000.0).toInt().coerceAtLeast(1)
                    skipView.text = "跳过${seconds}秒"
                }

                override fun onFinish() {
                    dismissSplash(view, listener.onAdClose)
                }
            }.also { it.start() }

            listener.onAdShowSuccess()
            Log.d(TAG, "showSFSplashAd success")
        } catch (e: Exception) {
            Log.e(TAG, "showSFSplashAd failed", e)
            listener.onAdShowFailure(e)
        }
    }

    fun showSFBannerAd(
        view: ViewGroup,
        listener: SparkFusionAdShowListener = SparkFusionAdShowListener()
    ) {
        val loadedAd = currentBannerAd
        if (!checkInit(view.context, listener.onAdShowFailure)) {
            return
        }
        if (loadedAd == null) {
            listener.onAdShowFailure(IllegalStateException("Banner 广告未加载，请先调用 loadSFBannerAd"))
            return
        }

        try {
            val bannerView = bannerRenderer.createView(
                context = view.context,
                adData = loadedAd.adData,
                onClose = {
                    removeSFBannerAd(view)
                    listener.onAdClose()
                },
                onActionClick = {
                    listener.onAdClick()
                    handleAdClick(view.context, loadedAd.adData)
                }
            )

            view.removeAllViews()
            view.addView(bannerView)
            listener.onAdShowSuccess()
            Log.d(TAG, "showSFBannerAd success")
        } catch (e: Exception) {
            Log.e(TAG, "showSFBannerAd failed", e)
            listener.onAdShowFailure(e)
        }
    }

    fun showSFInterstitialAd(
        activity: Activity,
        listener: SparkFusionAdShowListener = SparkFusionAdShowListener()
    ) {
        val loadedAd = currentInsertAd
        if (!checkInit(activity, listener.onAdShowFailure)) {
            return
        }
        if (loadedAd == null) {
            listener.onAdShowFailure(IllegalStateException("插屏广告未加载，请先调用 loadSFInterstitialAd"))
            return
        }

        try {
            var dialog: AlertDialog? = null
            val adView = insertRenderer.createView(
                context = activity,
                adData = loadedAd.adData,
                onClose = {
                    val currentDialog = dialog
                    if (currentDialog?.isShowing == true) {
                        currentDialog.dismiss()
                    }
                },
                onActionClick = {
                    listener.onAdClick()
                    handleAdClick(activity, loadedAd.adData)
                }
            )

            val dialogContainer = FrameLayout(activity).apply {
                setBackgroundColor(Color.parseColor("#66000000"))
                addView(
                    adView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                )
            }

            dialog = AlertDialog.Builder(activity)
                .setView(dialogContainer)
                .setCancelable(true)
                .create()

            dialog.window?.apply {
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            dialog.setOnDismissListener { listener.onAdClose() }
            dialog.show()
            listener.onAdShowSuccess()

            Handler(Looper.getMainLooper()).postDelayed({
                val currentDialog = dialog
                if (currentDialog?.isShowing == true) {
                    currentDialog.dismiss()
                }
            }, 3000)

            Log.d(TAG, "showSFInterstitialAd success")
        } catch (e: Exception) {
            Log.e(TAG, "showSFInterstitialAd failed", e)
            listener.onAdShowFailure(e)
        }
    }

    fun showSFVideoAd(
        activity: Activity,
        showAd: Boolean = true,
        onAdLoadSuccess: () -> Unit = {},
        onAdLoadError: () -> Unit = {},
        onAdClose: () -> Unit = {}
    ) {
        if (!checkInit(activity)) {
            onAdLoadError()
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (Random.nextInt(10) < 9) {
                onAdLoadSuccess()
                Log.d(TAG, "showSFVideoAd: 广告加载成功")

                if (!showAd) {
                    onAdClose()
                    return@postDelayed
                }

                val dialogView = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.parseColor("#9B59B6"))
                    setPadding(40, 40, 40, 40)
                }

                val titleView = TextView(activity).apply {
                    text = "激励视频广告"
                    textSize = 24f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                }

                val contentView = TextView(activity).apply {
                    text = "观看完整视频可获得奖励！"
                    textSize = 18f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                }

                dialogView.addView(titleView)
                dialogView.addView(contentView)

                val dialog = AlertDialog.Builder(activity)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create()

                dialog.window?.apply {
                    setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundDrawableResource(android.R.color.transparent)
                }

                dialog.setOnDismissListener { onAdClose() }
                dialog.show()

                Handler(Looper.getMainLooper()).postDelayed({
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }, 5000)

                Log.d(TAG, "showSFVideoAd success")
            } else {
                onAdLoadError()
                Log.d(TAG, "showSFVideoAd: 广告加载失败")
            }
        }, 500)
    }

    fun removeSFBannerAd(view: ViewGroup) {
        view.removeAllViews()
        Log.d(TAG, "removeSFBannerAd success")
    }

    fun destroy() {
        splashTimer?.cancel()
        splashTimer = null
        unregisterSplashShakeListener()
        isInitAd = false
        clearLoadedAds()
        Log.d(TAG, "destroy success")
    }

    private fun loadAd(
        adType: String,
        adId: String,
        onSuccess: (LoadedAd) -> Unit,
        onFailure: (Throwable) -> Unit,
        fetch: (
            adId: String,
            onSuccess: (MutableList<Addata>) -> Unit,
            onFailure: (Throwable) -> Unit
        ) -> Unit
    ) {
        if (adId.isBlank()) {
            val error = IllegalArgumentException("广告 id 不能为空")
            Log.e(TAG, "loadAd blocked, type=$adType, reason=${error.message}")
            onFailure(error)
            return
        }

        Log.d(TAG, "loadAd start, type=$adType, adId=$adId")

        fetch(
            adId,
            { adList ->
                Log.d(
                    TAG,
                    "loadAd fetched, type=$adType, adId=$adId, adCount=${adList.size}, ads=${summarizeAds(adList)}"
                )
                if (adList.isEmpty()) {
                    val error = IllegalStateException("${adType} 广告位没有可展示的数据")
                    Log.w(TAG, "loadAd empty, type=$adType, adId=$adId")
                    onFailure(error)
                    return@fetch
                }
                val selectedAd = selectAdData(adList)
                if (selectedAd == null) {
                    val error = IllegalStateException("${adType} 广告位没有权重大于0的可展示广告")
                    Log.w(TAG, "loadAd filtered, type=$adType, adId=$adId, all weights <= 0")
                    onFailure(error)
                    return@fetch
                }
                Log.d(
                    TAG,
                    "loadAd selected, type=$adType, adId=$adId, appName=${selectedAd.appName}, clickUrl=${selectedAd.clickUrl.orEmpty()}"
                )
                onSuccess(
                    LoadedAd(
                        adId = adId,
                        adData = selectedAd
                    )
                )
            },
            { error ->
                Log.e(TAG, "loadAd failed, type=$adType, adId=$adId", error)
                onFailure(error)
            }
        )
    }

    private fun clearLoadedAds() {
        currentSplashAd = null
        currentBannerAd = null
        currentInsertAd = null
    }

    private fun dismissSplash(view: ViewGroup, onAdClose: () -> Unit) {
        splashTimer?.cancel()
        splashTimer = null
        unregisterSplashShakeListener()
        splashActionTriggered = false
        view.removeAllViews()
        onAdClose()
    }

    private fun registerSplashShakeListener(
        context: Context,
        onShake: () -> Unit
    ) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        unregisterSplashShakeListener()
        splashSensorManager = sensorManager
        lastShakeTriggerAt = 0L
        splashShakeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val values = event.values ?: return
                if (values.size < 3) {
                    return
                }

                val x = values[0] / SensorManager.GRAVITY_EARTH
                val y = values[1] / SensorManager.GRAVITY_EARTH
                val z = values[2] / SensorManager.GRAVITY_EARTH
                val gForce = kotlin.math.sqrt(x * x + y * y + z * z)

                if (gForce < SHAKE_THRESHOLD_GRAVITY) {
                    return
                }

                val now = SystemClock.elapsedRealtime()
                if (now - lastShakeTriggerAt < SHAKE_DEBOUNCE_MILLIS) {
                    return
                }
                lastShakeTriggerAt = now
                onShake()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }.also {
            sensorManager.registerListener(it, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun unregisterSplashShakeListener() {
        val sensorManager = splashSensorManager
        val listener = splashShakeListener
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener)
        }
        splashShakeListener = null
        splashSensorManager = null
        lastShakeTriggerAt = 0L
    }

    private fun checkInit(context: Context, onFailure: ((Throwable) -> Unit)? = null): Boolean {
        if (isInitAd) {
            return true
        }

        val error = IllegalStateException("请先初始化")
        Log.d(TAG, error.message.orEmpty())
        Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
        onFailure?.invoke(error)
        return false
    }

    private fun checkInit(context: Context): Boolean {
        return checkInit(context, null)
    }

    private fun handleAdClick(context: Context, adData: SparkFusionAdData) {
        val clickUrl = adData.clickUrl
        if (clickUrl.isNullOrBlank()) {
            Toast.makeText(context, "${adData.callToAction}：${adData.appName}", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = buildAdIntent(context, clickUrl)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "handleAdClick failed: $clickUrl", e)
            Toast.makeText(context, "暂时无法打开广告目标", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "handleAdClick unexpected error: $clickUrl", e)
            Toast.makeText(context, "广告跳转失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildAdIntent(context: Context, clickUrl: String): Intent {
        val isWebUrl = clickUrl.startsWith("http://", true) || clickUrl.startsWith("https://", true)
        val intent = if (isWebUrl) {
            Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl))
        } else {
            context.packageManager.getLaunchIntentForPackage(clickUrl)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl))
        }

        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return intent
    }

    private fun selectAdData(adList: List<Addata>): SparkFusionAdData? {
        val weightedAds = adList
            .map { it to it.weight.coerceIn(0, 100) }
            .filter { (_, weight) -> weight > 0 }
        if (weightedAds.isEmpty()) {
            Log.w(TAG, "selectAdData skipped, no ad has weight > 0")
            return null
        }

        val totalWeight = weightedAds.sumOf { (_, weight) -> weight }
        val weightedSummary = weightedAds.joinToString(
            prefix = "[",
            postfix = "]"
        ) { (ad, weight) ->
            "{app=${ad.appname.orEmpty()}, rawWeight=${ad.weight}, usedWeight=$weight}"
        }
        Log.d(
            TAG,
            "selectAdData candidates=$weightedSummary, totalWeight=$totalWeight"
        )

        var hit = Random.nextInt(totalWeight)
        val originalHit = hit
        weightedAds.forEach { (ad, weight) ->
            hit -= weight
            if (hit < 0) {
                Log.d(
                    TAG,
                    "selectAdData hit=$originalHit, selectedApp=${ad.appname.orEmpty()}, selectedWeight=$weight"
                )
                return mapToSparkFusionAdData(ad)
            }
        }

        Log.d(
            TAG,
            "selectAdData fallback selectedApp=${weightedAds.last().first.appname.orEmpty()}"
        )
        return mapToSparkFusionAdData(weightedAds.last().first)
    }

    private fun mapToSparkFusionAdData(adData: Addata): SparkFusionAdData {
        val clickUrl = adData.url?.takeIf { it.isNotBlank() }
        return SparkFusionAdData(
            appName = adData.appname?.takeIf { it.isNotBlank() } ?: "SparkFusion Ad",
            description = adData.content?.takeIf { it.isNotBlank() } ?: "广告内容加载中",
            callToAction = when {
                clickUrl.isNullOrBlank() -> "查看详情"
                adData.isDownload == true -> "立即下载"
                else -> "立即打开"
            },
            versionName = adData.version?.takeIf { it.isNotBlank() } ?: "v1.0.0",
            clickUrl = clickUrl,
            appLogoUrl = adData.applogo?.url,
            promoImageUrl = adData.promotional?.url,
            appLogoRes = R.drawable.applogo,
            promoImageRes = R.drawable.a
        )
    }

    private data class LoadedAd(
        val adId: String,
        val adData: SparkFusionAdData
    )

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
