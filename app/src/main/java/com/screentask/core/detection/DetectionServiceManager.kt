package com.screentask.core.detection

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

class DetectionServiceManager(private val context: Context) {

    private var observer: ScreenshotObserver? = null

    fun startListening() {
        if (observer != null) return

        val handler = Handler(Looper.getMainLooper())
        observer = ScreenshotObserver(context, handler).also {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                it
            )
        }
    }

    fun stopListening() {
        observer?.let {
            context.contentResolver.unregisterContentObserver(it)
            observer = null
        }
    }
}