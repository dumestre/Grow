package com.daime.grow.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.daime.grow.MainActivity
import com.daime.grow.R

object NotificationHelper {
    const val CHANNEL_WATERING_ID = "grow_reminders_watering"
    const val CHANNEL_NUTRIENT_ID = "grow_reminders_nutrient"
    private const val TYPE_WATERING = 1
    private const val TYPE_NUTRIENT = 2

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val wateringChannel = NotificationChannel(
            CHANNEL_WATERING_ID,
            context.getString(R.string.notification_channel_watering_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_watering_description)
            enableVibration(true)
            enableLights(true)
        }
        val nutrientChannel = NotificationChannel(
            CHANNEL_NUTRIENT_ID,
            context.getString(R.string.notification_channel_nutrient_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_nutrient_description)
            enableVibration(true)
            enableLights(true)
        }
        manager.createNotificationChannel(wateringChannel)
        manager.createNotificationChannel(nutrientChannel)
    }

    fun showWateringReminder(context: Context, plantId: Long, plantName: String) {
        showReminderNotification(
            context = context,
            type = TYPE_WATERING,
            channelId = CHANNEL_WATERING_ID,
            plantId = plantId,
            plantName = plantName,
            title = context.getString(R.string.notification_watering_title),
            body = context.getString(R.string.notification_watering_body, plantName)
        )
    }

    fun showNutrientReminder(context: Context, plantId: Long, plantName: String) {
        showReminderNotification(
            context = context,
            type = TYPE_NUTRIENT,
            channelId = CHANNEL_NUTRIENT_ID,
            plantId = plantId,
            plantName = plantName,
            title = context.getString(R.string.notification_nutrient_title),
            body = context.getString(R.string.notification_nutrient_body, plantName)
        )
    }

    private fun showReminderNotification(
        context: Context,
        type: Int,
        channelId: String,
        plantId: Long,
        plantName: String,
        title: String,
        body: String
    ) {
        val allowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU

        if (!allowed) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.daime.grow.OPEN_FROM_NOTIFICATION"
            putExtra("plantId", plantId)
            putExtra("plantName", plantName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(type, plantId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(
                if (type == TYPE_WATERING) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .addAction(
                R.mipmap.ic_launcher_round,
                context.getString(R.string.notification_action_open),
                pendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(type, plantId), notification)
    }

    private fun notificationId(type: Int, plantId: Long): Int {
        val normalizedPlant = (plantId and 0x7FFF_FFFFL).toInt()
        return (normalizedPlant * 10) + type
    }
}

