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
    private const val REWARD_VIDEO_DURATION_SECONDS = 30
    private const val REWARD_EARNED_SECONDS = 20

    private var isInitAd = false
    private var splashTimer: CountDownTimer? = null
    private var splashSensorManager: SensorManager? = null
    private var splashShakeListener: SensorEventListener? = null
    private var lastShakeTriggerAt = 0L
    private var splashActionTriggered = false

    private var currentSplashAd: LoadedAd? = null
    private var currentBannerAd: LoadedAd? = null
    private var currentInsertAd: LoadedAd? = null
    private var currentRewardAd: LoadedRewardAd? = null

    private enum class AdSource {
        SELF,
        THIRD_PARTY
    }

    private var splashAdSource: AdSource = AdSource.SELF
    private var bannerAdSource: AdSource = AdSource.SELF
    private var insertAdSource: AdSource = AdSource.SELF
    private var rewardAdSource: AdSource = AdSource.SELF

    private var thirdPartyAdLoader: SparkFusionThirdPartyAdLoader? = null

    private val splashRenderer = SparkFusionSplashAD()
    private val bannerRenderer = SparkFusionBannerAD()
    private val insertRenderer = SparkFusionInsertAD()

    private var pendingRewardSession: RewardVideoSession? = null

    internal data class RewardVideoSession(
        val adData: SparkFusionAdData,
        val videoUrl: String,
        val durationSeconds: Int,
        val rewardEligibleSeconds: Int,
        val onReward: () -> Unit,
        val onClose: () -> Unit,
        val onClick: () -> Unit
    )

    internal fun consumeRewardSession(): RewardVideoSession? {
        val session = pendingRewardSession
        pendingRewardSession = null
        return session
    }

    internal fun performRewardAdClick(context: Context, adData: SparkFusionAdData) {
        handleAdClick(context, adData)
    }

    fun initSparkFusionAd(context: Context, appKey: String) {
        Freelybase.initialize(context.applicationContext, appKey, false)
        isInitAd = true
        clearLoadedAds()

        Log.d(TAG, "initSparkFusionAd success, appKey=$appKey")
        Toast.makeText(context, "initSparkFusionAd success", Toast.LENGTH_SHORT).show()
    }

    fun loadThirdPartyAd(loader: SparkFusionThirdPartyAdLoader) {
        thirdPartyAdLoader = loader
    }

    fun loadSFSplashAd(
        context: Context,
        adId: String,
        loadThirdPartyAd: (() -> Unit)? = null,
        listener: SparkFusionAdLoadListener = SparkFusionAdLoadListener()
    ) {
        if (!checkInit(context, listener.onAdLoadFailure)) {
            return
        }

        SparkFusingAdDataRep.getSplashAdSpaceResult(
            splashId = adId,
            onSuccess = { result ->
                if (!result.enableSelfAd) {
                    loadThirdPartyAd?.invoke()
                    val loader = thirdPartyAdLoader
                    if (loader == null) {
                        listener.onAdLoadFailure(IllegalStateException("未设置第三方广告加载器"))
                        return@getSplashAdSpaceResult
                    }
                    loader.loadSplashAd(
                        context = context,
                        adId = adId,
                        listener = SparkFusionAdLoadListener(
                            onAdLoadSuccess = {
                                splashAdSource = AdSource.THIRD_PARTY
                                currentSplashAd = null
                                listener.onAdLoadSuccess()
                            },
                            onAdLoadFailure = { error ->
                                splashAdSource = AdSource.SELF
                                currentSplashAd = null
                                listener.onAdLoadFailure(error)
                            }
                        )
                    )
                    return@getSplashAdSpaceResult
                }

                splashAdSource = AdSource.SELF
                currentSplashAd = null
                loadAd(
                    adType = "splash",
                    adId = adId,
                    onSuccess = { loadedAd ->
                        splashAdSource = AdSource.SELF
                        currentSplashAd = loadedAd
                        Log.d(TAG, "loadSFSplashAd success: ${loadedAd.adData.appName}")
                        listener.onAdLoadSuccess()
                    },
                    onFailure = { error ->
                        splashAdSource = AdSource.SELF
                        currentSplashAd = null
                        listener.onAdLoadFailure(error)
                    },
                    fetch = { _, onSuccess, _ ->
                        onSuccess(result.ads)
                    }
                )
            },
            onFailure = listener.onAdLoadFailure
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

        SparkFusingAdDataRep.getBannerAdSpaceResult(
            bannerId = adId,
            onSuccess = { result ->
                if (!result.enableSelfAd) {
                    val loader = thirdPartyAdLoader
                    if (loader == null) {
                        listener.onAdLoadFailure(IllegalStateException("未设置第三方广告加载器"))
                        return@getBannerAdSpaceResult
                    }
                    loader.loadBannerAd(
                        context = context,
                        adId = adId,
                        listener = SparkFusionAdLoadListener(
                            onAdLoadSuccess = {
                                bannerAdSource = AdSource.THIRD_PARTY
                                currentBannerAd = null
                                listener.onAdLoadSuccess()
                            },
                            onAdLoadFailure = { error ->
                                bannerAdSource = AdSource.SELF
                                currentBannerAd = null
                                listener.onAdLoadFailure(error)
                            }
                        )
                    )
                    return@getBannerAdSpaceResult
                }

                bannerAdSource = AdSource.SELF
                currentBannerAd = null
                loadAd(
                    adType = "banner",
                    adId = adId,
                    onSuccess = { loadedAd ->
                        bannerAdSource = AdSource.SELF
                        currentBannerAd = loadedAd
                        Log.d(TAG, "loadSFBannerAd success: ${loadedAd.adData.appName}")
                        listener.onAdLoadSuccess()
                    },
                    onFailure = { error ->
                        bannerAdSource = AdSource.SELF
                        currentBannerAd = null
                        listener.onAdLoadFailure(error)
                    },
                    fetch = { _, onSuccess, _ ->
                        onSuccess(result.ads)
                    }
                )
            },
            onFailure = listener.onAdLoadFailure
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

        SparkFusingAdDataRep.getInsertAdSpaceResult(
            insertId = adId,
            onSuccess = { result ->
                if (!result.enableSelfAd) {
                    val loader = thirdPartyAdLoader
                    if (loader == null) {
                        listener.onAdLoadFailure(IllegalStateException("未设置第三方广告加载器"))
                        return@getInsertAdSpaceResult
                    }
                    loader.loadInterstitialAd(
                        context = context,
                        adId = adId,
                        listener = SparkFusionAdLoadListener(
                            onAdLoadSuccess = {
                                insertAdSource = AdSource.THIRD_PARTY
                                currentInsertAd = null
                                listener.onAdLoadSuccess()
                            },
                            onAdLoadFailure = { error ->
                                insertAdSource = AdSource.SELF
                                currentInsertAd = null
                                listener.onAdLoadFailure(error)
                            }
                        )
                    )
                    return@getInsertAdSpaceResult
                }

                insertAdSource = AdSource.SELF
                currentInsertAd = null
                loadAd(
                    adType = "insert",
                    adId = adId,
                    onSuccess = { loadedAd ->
                        insertAdSource = AdSource.SELF
                        currentInsertAd = loadedAd
                        Log.d(TAG, "loadSFInterstitialAd success: ${loadedAd.adData.appName}")
                        listener.onAdLoadSuccess()
                    },
                    onFailure = { error ->
                        insertAdSource = AdSource.SELF
                        currentInsertAd = null
                        listener.onAdLoadFailure(error)
                    },
                    fetch = { _, onSuccess, _ ->
                        onSuccess(result.ads)
                    }
                )
            },
            onFailure = listener.onAdLoadFailure
        )
    }

    fun loadSFVideoAd(
        context: Context,
        adId: String,
        listener: SparkFusionAdLoadListener = SparkFusionAdLoadListener()
    ) {
        if (!checkInit(context, listener.onAdLoadFailure)) {
            return
        }
        if (adId.isBlank()) {
            listener.onAdLoadFailure(IllegalArgumentException("广告 id 不能为空"))
            return
        }

        SparkFusingAdDataRep.getRewardAdSpaceResult(
            rewardId = adId,
            onSuccess = { result ->
                if (!result.enableSelfAd) {
                    val loader = thirdPartyAdLoader
                    if (loader == null) {
                        listener.onAdLoadFailure(IllegalStateException("未设置第三方广告加载器"))
                        return@getRewardAdSpaceResult
                    }
                    loader.loadRewardAd(
                        context = context,
                        adId = adId,
                        listener = SparkFusionAdLoadListener(
                            onAdLoadSuccess = {
                                rewardAdSource = AdSource.THIRD_PARTY
                                currentRewardAd = null
                                listener.onAdLoadSuccess()
                            },
                            onAdLoadFailure = { error ->
                                rewardAdSource = AdSource.SELF
                                currentRewardAd = null
                                listener.onAdLoadFailure(error)
                            }
                        )
                    )
                    return@getRewardAdSpaceResult
                }

                rewardAdSource = AdSource.SELF
                currentRewardAd = null

                val adList = result.ads
                if (adList.isEmpty()) {
                    listener.onAdLoadFailure(IllegalStateException("激励视频广告位没有可展示的数据"))
                    return@getRewardAdSpaceResult
                }

                val selectedRawAd = selectRawAdData(adList)
                if (selectedRawAd == null) {
                    listener.onAdLoadFailure(IllegalStateException("激励视频广告位没有权重大于0的可展示广告"))
                    return@getRewardAdSpaceResult
                }

                val videoUrl = selectedRawAd.video?.url?.takeIf { it.isNotBlank() }
                if (videoUrl.isNullOrBlank()) {
                    listener.onAdLoadFailure(IllegalStateException("激励视频广告视频地址为空"))
                    return@getRewardAdSpaceResult
                }

                val adData = mapToSparkFusionAdData(selectedRawAd)
                rewardAdSource = AdSource.SELF
                currentRewardAd = LoadedRewardAd(
                    adId = adId,
                    adData = adData,
                    videoUrl = videoUrl
                )
                listener.onAdLoadSuccess()
            },
            onFailure = { error ->
                listener.onAdLoadFailure(error)
            }
        )
    }

    fun showSFSplashAd(
        view: ViewGroup,
        listener: SparkFusionAdShowListener = SparkFusionAdShowListener()
    ) {
        if (!checkInit(view.context, listener.onAdShowFailure)) {
            return
        }
        if (splashAdSource == AdSource.THIRD_PARTY) {
            val loader = thirdPartyAdLoader
            if (loader == null) {
                listener.onAdShowFailure(IllegalStateException("未设置第三方广告加载器"))
                return
            }
            loader.showSplashAd(view, listener)
            return
        }

        val loadedAd = currentSplashAd
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
        if (!checkInit(view.context, listener.onAdShowFailure)) {
            return
        }
        if (bannerAdSource == AdSource.THIRD_PARTY) {
            val loader = thirdPartyAdLoader
            if (loader == null) {
                listener.onAdShowFailure(IllegalStateException("未设置第三方广告加载器"))
                return
            }
            loader.showBannerAd(view, listener)
            return
        }

        val loadedAd = currentBannerAd
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
        if (!checkInit(activity, listener.onAdShowFailure)) {
            return
        }
        if (insertAdSource == AdSource.THIRD_PARTY) {
            val loader = thirdPartyAdLoader
            if (loader == null) {
                listener.onAdShowFailure(IllegalStateException("未设置第三方广告加载器"))
                return
            }
            loader.showInterstitialAd(activity, listener)
            return
        }

        val loadedAd = currentInsertAd
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
        listener: SparkFusionRewardAdShowListener = SparkFusionRewardAdShowListener()
    ) {
        if (!checkInit(activity, listener.onAdShowFailure)) {
            return
        }
        if (rewardAdSource == AdSource.THIRD_PARTY) {
            val loader = thirdPartyAdLoader
            if (loader == null) {
                listener.onAdShowFailure(IllegalStateException("未设置第三方广告加载器"))
                return
            }
            loader.showRewardAd(activity, listener)
            return
        }

        val loadedAd = currentRewardAd
        if (loadedAd == null) {
            listener.onAdShowFailure(IllegalStateException("激励视频广告未加载，请先调用 loadSFVideoAd"))
            return
        }

        pendingRewardSession = RewardVideoSession(
            adData = loadedAd.adData,
            videoUrl = loadedAd.videoUrl,
            durationSeconds = REWARD_VIDEO_DURATION_SECONDS,
            rewardEligibleSeconds = REWARD_EARNED_SECONDS,
            onReward = listener.onReward,
            onClose = listener.onAdClose,
            onClick = {
                listener.onAdClick()
                handleAdClick(activity, loadedAd.adData)
            }
        )

        try {
            activity.startActivity(Intent(activity, SparkFusionRewardVideoActivity::class.java))
            listener.onAdShowSuccess()
        } catch (e: Exception) {
            pendingRewardSession = null
            listener.onAdShowFailure(e)
        }
    }

    fun showSFVideoAd(
        activity: Activity,
        adId: String,
        showAd: Boolean = true,
        onAdLoadSuccess: () -> Unit = {},
        onAdLoadError: () -> Unit = {},
        onAdClose: () -> Unit = {},
        onReward: () -> Unit = {}
    ) {
        loadSFVideoAd(
            context = activity,
            adId = adId,
            listener = SparkFusionAdLoadListener(
                onAdLoadSuccess = {
                    onAdLoadSuccess()
                    if (!showAd) {
                        onAdClose()
                        return@SparkFusionAdLoadListener
                    }
                    showSFVideoAd(
                        activity = activity,
                        listener = SparkFusionRewardAdShowListener(
                            onAdShowSuccess = {},
                            onAdShowFailure = { onAdLoadError() },
                            onAdClick = {},
                            onAdClose = onAdClose,
                            onReward = onReward
                        )
                    )
                },
                onAdLoadFailure = { onAdLoadError() }
            )
        )
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
        currentRewardAd = null
        splashAdSource = AdSource.SELF
        bannerAdSource = AdSource.SELF
        insertAdSource = AdSource.SELF
        rewardAdSource = AdSource.SELF
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
        val selected = selectRawAdData(adList) ?: return null
        return mapToSparkFusionAdData(selected)
    }

    private fun selectRawAdData(adList: List<Addata>): Addata? {
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
                return ad
            }
        }

        Log.d(
            TAG,
            "selectAdData fallback selectedApp=${weightedAds.last().first.appname.orEmpty()}"
        )
        return weightedAds.last().first
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

    private data class LoadedRewardAd(
        val adId: String,
        val adData: SparkFusionAdData,
        val videoUrl: String
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
