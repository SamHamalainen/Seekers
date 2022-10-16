package com.example.seekers.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.example.seekers.R
import com.example.seekers.ui.theme.Emerald

/**
 * NotificationHelper: Contains the functions to create a notification and it's channel
 */

object NotificationHelper {
    private const val CHANNEL_ID = "FOREGROUND_SERVICE"

    fun createNotificationChannel(
        context: Context,
        channelId: String = CHANNEL_ID,
        importanceLevel: Int = NotificationManager.IMPORTANCE_NONE,
        lockscreenVisibility: Int = Notification.VISIBILITY_PUBLIC,
    ) {
        val channel = NotificationChannel(
            channelId,
            "Notification",
            importanceLevel
        ).apply {
            description = "description"
        }
        channel.lockscreenVisibility = lockscreenVisibility
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun createNotification(
        context: Context,
        title: String,
        content: String,
        channelId: String = CHANNEL_ID,
        priority: Int = NotificationManager.IMPORTANCE_MIN,
        category: String = Notification.CATEGORY_SERVICE,
        pendingIntent: PendingIntent? = null,
        autoCancel: Boolean = false
    ): Notification {
        val builder = NotificationCompat.Builder(context, channelId)
        return builder
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.seekers_notif)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,
                R.drawable.chick_with_background
            ))
            .setPriority(priority)
            .setCategory(category)
            .setContentIntent(pendingIntent)
            .setAutoCancel(autoCancel)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setColor(Emerald.toArgb())
            .setColorized(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
    }
}