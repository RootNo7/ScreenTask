package com.screentask.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.screentask.R
import com.screentask.core.classifier.model.ClassificationResult
import com.screentask.core.classifier.util.BitmapScaler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class NotificationDispatcher(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationIdGenerator = AtomicInteger(1000)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screenshot Classification",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for screenshot classification actions"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    suspend fun dispatchClassificationNotification(
        imageUri: Uri,
        result: ClassificationResult
    ) = withContext(Dispatchers.IO) {
        val notificationId = notificationIdGenerator.incrementAndGet()
        val thumbnailBitmap = BitmapScaler.scaleImageForClassification(context, imageUri)

        val keepPendingIntent = createActionPendingIntent(
            NotificationActionReceiver.ACTION_KEEP,
            imageUri,
            notificationId
        )

        val discardPendingIntent = createActionPendingIntent(
            NotificationActionReceiver.ACTION_DISCARD,
            imageUri,
            notificationId
        )

        val remindPendingIntent = createActionPendingIntent(
            NotificationActionReceiver.ACTION_REMIND,
            imageUri,
            notificationId,
            result.reminderTimestamp ?: 0L
        )

        val title = "Screenshot Analyzed: ${formatCategoryName(result.category.name)}"
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(title)
            .setContentText(result.summary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${result.summary}\n\n${result.extractedText ?: ""}".trim())
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "Keep", keepPendingIntent)
            .addAction(0, "Remind", remindPendingIntent)
            .addAction(0, "Discard", discardPendingIntent)

        thumbnailBitmap?.let {
            builder.setLargeIcon(it)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(it)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(result.summary)
            )
        }

        runCatching {
            notificationManager.notify(notificationId, builder.build())
        }
    }

    private fun createActionPendingIntent(
        action: String,
        imageUri: Uri,
        notificationId: Int,
        reminderTimestamp: Long = 0L
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_IMAGE_URI, imageUri.toString())
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            if (reminderTimestamp > 0L) {
                putExtra(NotificationActionReceiver.EXTRA_REMINDER_TIMESTAMP, reminderTimestamp)
            }
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getBroadcast(
            context,
            (notificationId.toString() + action).hashCode(),
            intent,
            flags
        )
    }

    private fun formatCategoryName(category: String): String {
        return category.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    companion object {
        const val CHANNEL_ID = "screentask_classification_channel"
    }
}