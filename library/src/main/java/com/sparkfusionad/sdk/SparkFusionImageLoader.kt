package com.sparkfusionad.sdk

import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

internal object SparkFusionImageLoader {

    private const val TAG = "SparkFusionImageLoader"
    private val executor = Executors.newCachedThreadPool()

    fun load(imageView: ImageView, url: String?, fallbackResId: Int) {
        imageView.setImageResource(fallbackResId)
        if (url.isNullOrBlank()) {
            return
        }

        imageView.tag = url
        executor.execute {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                    doInput = true
                }
                connection.connect()
                connection.inputStream.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input) ?: return@use
                    imageView.post {
                        if (imageView.tag == url) {
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "load image failed: $url", e)
            }
        }
    }
}
