package com.sparkfusionad.sdk

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random

object SparkFusionAd {

    private val TAG = "SparkFusionAd"
    private var isInitAd: Boolean = false
    private var context: Context? = null
    
    private var splashAd: FrameLayout? = null
    private var bannerAd: FrameLayout? = null
    private var splashTimer: CountDownTimer? = null

    /**
     * 初始化SparkFusionAd
     */
    fun initSparkFusionAd(context: Context) {
        this.context = context.applicationContext
        isInitAd = true
        
        // 初始化开屏广告ImageView
        splashAd = createSplashAdView(context)
        
        // 初始化Banner广告ImageView
        bannerAd = createBannerAdView(context)
        
        Log.d(TAG, "initSparkFusionAd success")
        Toast.makeText(context, "initSparkFusionAd success", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 创建开屏广告视图
     */
    private fun createSplashAdView(context: Context): FrameLayout {
        val container = FrameLayout(context)
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.setBackgroundColor(Color.parseColor("#4A90E2")) // 蓝色背景
        
        // 添加广告文字
        val textView = TextView(context)
        textView.text = "开屏广告\nSparkFusion Ad"
        textView.textSize = 24f
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        container.addView(textView)
        
        return container
    }
    
    /**
     * 创建Banner广告视图
     */
    private fun createBannerAdView(context: Context): FrameLayout {
        val container = FrameLayout(context)
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        container.setBackgroundColor(Color.parseColor("#50C878")) // 绿色背景
        container.minimumHeight = 50.dpToPx(context)
        
        // 添加广告文字
        val textView = TextView(context)
        textView.text = "Banner广告 - SparkFusion Ad"
        textView.textSize = 18f
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        container.addView(textView)
        
        return container
    }
    
    /**
     * dp转px
     */
    private fun Int.dpToPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (this * density + 0.5f).toInt()
    }
    /**
     * 加载开屏广告
     */
    fun loadSFSplashAd(context: Context) {
        if (!isInitAd) {
            Log.d(TAG, "请先初始化")
            Toast.makeText(context, "请先初始化", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 重新创建开屏广告视图（模拟加载新广告）
        splashAd = createSplashAdView(context)
        
        Log.d(TAG, "loadSFSplashAd success")
        Toast.makeText(context, "loadSFSplashAd success", Toast.LENGTH_SHORT).show()
    }
    /**
     * 显示开屏广告
     */
    fun showSFSplashAd(view: ViewGroup, onAdClose: () -> Unit) {
        if (!isInitAd) {
            Log.d(TAG, "请先初始化")
            Toast.makeText(view.context, "请先初始化", Toast.LENGTH_SHORT).show()
            onAdClose()
            return
        }
        
        if (splashAd == null) {
            splashAd = createSplashAdView(view.context)
        }
        
        // 取消之前的定时器
        splashTimer?.cancel()
        
        view.removeAllViews()
        view.addView(splashAd)
        
        Log.d(TAG, "showSFSplashAd success")
        
        // 创建倒计时定时器
        splashTimer = object : CountDownTimer(2500, 100) {
            override fun onTick(millisUntilFinished: Long) {
                // 可以在这里更新倒计时显示
            }

            override fun onFinish() {
                view.removeAllViews()
                onAdClose()
            }
        }
        splashTimer?.start()
    }
    /**
     * 显示插屏广告
     * @param activity Activity上下文
     * @param probability 显示概率，1/probability (例如：5表示1/5的概率)
     * @param showAd 是否显示广告
     * @param onAdClose 广告关闭回调
     */
    fun showSFInterstitialAd(
        activity: Activity,
        probability: Int = 5,
        showAd: Boolean = true,
        onAdClose: () -> Unit = {}
    ) {
        if (!isInitAd) {
            Log.d(TAG, "请先初始化")
            Toast.makeText(activity, "请先初始化", Toast.LENGTH_SHORT).show()
            onAdClose()
            return
        }
        
        // 概率判断：如果随机数不为0，则不显示广告
        if (Random.nextInt(probability) != 0) {
            Log.d(TAG, "showSFInterstitialAd: 概率未命中，不显示广告")
            onAdClose()
            return
        }
        
        if (!showAd) {
            Log.d(TAG, "showSFInterstitialAd: showAd为false，不显示广告")
            onAdClose()
            return
        }
        
        // 创建插屏广告对话框
        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#FF6B6B")) // 红色背景
            setPadding(40, 40, 40, 40)
        }
        
        val titleView = TextView(activity).apply {
            text = "插屏广告"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20.dpToPx(activity)
            }
        }
        
        val contentView = TextView(activity).apply {
            text = "SparkFusion Ad\n插屏广告展示中..."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
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
        
        dialog.setOnDismissListener {
            onAdClose()
        }
        
        dialog.show()
        
        // 3秒后自动关闭
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 3000)
        
        Log.d(TAG, "showSFInterstitialAd success")
    }
    /**
     * 显示激励视频广告
     * @param activity Activity上下文
     * @param showAd 是否显示广告
     * @param onAdLoadSuccess 广告加载成功回调
     * @param onAdLoadError 广告加载失败回调
     * @param onAdClose 广告关闭回调
     */
    fun showSFVideoAd(
        activity: Activity,
        showAd: Boolean = true,
        onAdLoadSuccess: () -> Unit = {},
        onAdLoadError: () -> Unit = {},
        onAdClose: () -> Unit = {}
    ) {
        if (!isInitAd) {
            Log.d(TAG, "请先初始化")
            Toast.makeText(activity, "请先初始化", Toast.LENGTH_SHORT).show()
            onAdLoadError()
            return
        }
        
        // 模拟加载过程
        Handler(Looper.getMainLooper()).postDelayed({
            // 模拟90%的成功率
            if (Random.nextInt(10) < 9) {
                onAdLoadSuccess()
                Log.d(TAG, "showSFVideoAd: 广告加载成功")
                
                if (showAd) {
                    // 创建激励视频广告对话框
                    val dialogView = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.parseColor("#9B59B6")) // 紫色背景
                        setPadding(40, 40, 40, 40)
                    }
                    
                    val titleView = TextView(activity).apply {
                        text = "激励视频广告"
                        textSize = 24f
                        setTextColor(Color.WHITE)
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 20.dpToPx(activity)
                        }
                    }
                    
                    val contentView = TextView(activity).apply {
                        text = "SparkFusion Ad\n观看完整视频可获得奖励！"
                        textSize = 18f
                        setTextColor(Color.WHITE)
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
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
                    
                    dialog.setOnDismissListener {
                        onAdClose()
                    }
                    
                    dialog.show()
                    
                    // 5秒后自动关闭（模拟视频播放完成）
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }, 5000)
                    
                    Log.d(TAG, "showSFVideoAd success")
                } else {
                    onAdClose()
                }
            } else {
                onAdLoadError()
                Log.d(TAG, "showSFVideoAd: 广告加载失败")
            }
        }, 500) // 模拟500ms的加载延迟
    }

    /**
     * 显示Banner广告
     */
    fun showSFBannerAd(view: ViewGroup) {
        if (!isInitAd) {
            Log.d(TAG, "请先初始化")
            Toast.makeText(view.context, "请先初始化", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (bannerAd == null) {
            bannerAd = createBannerAdView(view.context)
        }
        
        view.removeAllViews()
        view.addView(bannerAd)
        
        Log.d(TAG, "showSFBannerAd success")
    }
    
    /**
     * 移除Banner广告
     */
    fun removeSFBannerAd(view: ViewGroup) {
        view.removeAllViews()
        Log.d(TAG, "removeSFBannerAd success")
    }
    
    /**
     * 销毁资源
     */
    fun destroy() {
        splashTimer?.cancel()
        splashTimer = null
        splashAd = null
        bannerAd = null
        isInitAd = false
        context = null
        Log.d(TAG, "destroy success")
    }
}