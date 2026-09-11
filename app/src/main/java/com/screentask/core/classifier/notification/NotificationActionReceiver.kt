package com.screentask.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val imageUri = Uri.parse(uriString)

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_KEEP -> {
                        // Metadata is already stored in Room; no further action required.
                    }
                    ACTION_REMIND -> {
                        val timestamp = intent.getLongExtra(EXTRA_REMINDER_TIMESTAMP, 0L)
                        // TODO: Dispatch to WorkManager / AlarmManager for scheduled alert
                    }
                    ACTION_DISCARD -> {
                        deleteImage(context, imageUri)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun deleteImage(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.delete(uri, null, null)
        }
    }

    companion object {
        const val ACTION_KEEP = "com.screentask.action.KEEP"
        const val ACTION_REMIND = "com.screentask.action.REMIND"
        const val ACTION_DISCARD = "com.screentask.action.DISCARD"

        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_REMINDER_TIMESTAMP = "extra_reminder_timestamp"
    }
}