package com.sparkfusionad.sdk

import android.app.Activity
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.ceil

class SparkFusionRewardVideoActivity : Activity() {

    private var player: ExoPlayer? = null
    private var timer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var rewardRunnable: Runnable? = null
    private var rewardSent = false
    private var onAdClose: (() -> Unit)? = null
    private var noticeView: ConstraintLayout? = null
    private var noticeShown = false
    private var downloadView: ImageView? = null
    private var downloadAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val session = SparkFusionAd.consumeRewardSession()
        if (session == null) {
            finish()
            return
        }

        onAdClose = session.onClose

        val rewardView = SparkFusionRewardAD().createView(
            context = this,
            adData = session.adData,
            durationSeconds = session.durationSeconds,
            onSkipClick = { finish() },
            onActionClick = { session.onClick() }
        )

        setContentView(rewardView)

        noticeView = rewardView.findViewById(R.id.notice)
        downloadView = rewardView.findViewById(R.id.notice_download)
        noticeView?.post {
            val view = noticeView ?: return@post
            view.translationY = -view.height.toFloat()
        }
        handler.postDelayed({ showNotice() }, 10_000L)

        val playerView = rewardView.findViewById<PlayerView>(R.id.player_view)
        playerView.useController = false
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(session.videoUrl)))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }

        val skipView = rewardView.findViewById<TextView>(R.id.skip)
        val startTime = SystemClock.elapsedRealtime()

        rewardRunnable = object : Runnable {
            override fun run() {
                if (!rewardSent) {
                    val elapsedSeconds = ((SystemClock.elapsedRealtime() - startTime) / 1000L).toInt()
                    if (elapsedSeconds >= session.rewardEligibleSeconds) {
                        rewardSent = true
                        skipView.text = "已获取奖励|关闭"
                        session.onReward()
                    }
                }
                handler.postDelayed(this, 500L)
            }
        }.also { handler.post(it) }

        timer = object : CountDownTimer(session.durationSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (rewardSent) {
                    return
                }
                val seconds = ceil(millisUntilFinished / 1000.0).toInt().coerceAtLeast(1)
                skipView.text = "跳过${seconds}秒"
            }

            override fun onFinish() {
                if (rewardSent) {
                    return
                }
                skipView.text = "关闭"
            }
        }.also { it.start() }
    }

    override fun onDestroy() {
        if (isFinishing) {
            onAdClose?.invoke()
            onAdClose = null
        }

        val currentAnimator = downloadAnimator
        if (currentAnimator != null) {
            currentAnimator.cancel()
        }
        downloadAnimator = null

        timer?.cancel()
        timer = null
        rewardRunnable?.let { handler.removeCallbacks(it) }
        rewardRunnable = null

        val currentPlayer = player
        if (currentPlayer != null) {
            currentPlayer.release()
        }
        player = null

        super.onDestroy()
    }

    private fun showNotice() {
        if (noticeShown) {
            return
        }
        noticeShown = true

        val view = noticeView ?: return
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(450L)
            .setInterpolator(OvershootInterpolator())
            .start()

        startDownloadAnimation()
    }

    private fun startDownloadAnimation() {
        val view = downloadView ?: return
        val travel = dp(14f)
        view.translationY = -travel
        downloadAnimator = ValueAnimator.ofFloat(-travel, travel).apply {
            duration = 700L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { animator ->
                view.translationY = animator.animatedValue as Float
            }
            start()
        }
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        )
    }
}
