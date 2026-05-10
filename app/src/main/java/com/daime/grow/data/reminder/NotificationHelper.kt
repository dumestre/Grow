package com.daime.grow.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.daime.grow.MainActivity
import com.daime.grow.R

object NotificationHelper {
    const val CHANNEL_WATERING_ID = "grow_reminders_watering"
    const val CHANNEL_NUTRIENT_ID = "grow_reminders_nutrient"
    const val CHANNEL_BURP_DRYING_ID = "grow_reminders_burp_drying"
    const val CHANNEL_BURP_CURING_ID = "grow_reminders_burp_curing"
    const val CHANNEL_HYDROponic_ID = "grow_reminders_hydroponic"
    const val CHANNEL_MURAL_ID = "grow_mural_notifications"
    
    private const val TYPE_WATERING = 1
    private const val TYPE_NUTRIENT = 2
    private const val TYPE_BURP_DRYING = 3
    private const val TYPE_BURP_CURING = 4
    private const val TYPE_HYDROponic = 5
    private const val TYPE_MURAL_COMMENT = 6
    private const val TYPE_MURAL_LIKE = 7
    private const val TYPE_MURAL_REPLY = 8

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
        
        val burpDryingChannel = NotificationChannel(
            CHANNEL_BURP_DRYING_ID,
            context.getString(R.string.notification_channel_burp_drying_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_burp_drying_description)
            enableVibration(true)
            enableLights(true)
        }
        
        val burpCuringChannel = NotificationChannel(
            CHANNEL_BURP_CURING_ID,
            context.getString(R.string.notification_channel_burp_curing_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_burp_curing_description)
            enableVibration(true)
            enableLights(true)
        }
        
        val hydroponicChannel = NotificationChannel(
            CHANNEL_HYDROponic_ID,
            context.getString(R.string.notification_channel_hydroponic_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_hydroponic_description)
            enableVibration(true)
            enableLights(true)
        }

        val muralChannel = NotificationChannel(
            CHANNEL_MURAL_ID,
            context.getString(R.string.notification_channel_mural_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_mural_description)
            enableVibration(true)
            enableLights(true)
        }
        
        manager.createNotificationChannel(wateringChannel)
        manager.createNotificationChannel(nutrientChannel)
        manager.createNotificationChannel(burpDryingChannel)
        manager.createNotificationChannel(burpCuringChannel)
        manager.createNotificationChannel(hydroponicChannel)
        manager.createNotificationChannel(muralChannel)
    }

    fun showMuralCommentNotification(context: Context, postId: String, username: String) {
        showMuralNotification(
            context = context,
            type = TYPE_MURAL_COMMENT,
            postId = postId,
            title = context.getString(R.string.notification_mural_comment_title),
            body = context.getString(R.string.notification_mural_comment_body, username)
        )
    }

    fun showMuralLikeNotification(context: Context, postId: String, username: String) {
        showMuralNotification(
            context = context,
            type = TYPE_MURAL_LIKE,
            postId = postId,
            title = context.getString(R.string.notification_mural_like_title),
            body = context.getString(R.string.notification_mural_like_body, username)
        )
    }

    fun showMuralReplyNotification(context: Context, postId: String, username: String) {
        showMuralNotification(
            context = context,
            type = TYPE_MURAL_REPLY,
            postId = postId,
            title = context.getString(R.string.notification_mural_reply_title),
            body = context.getString(R.string.notification_mural_reply_body, username)
        )
    }

    private fun showMuralNotification(
        context: Context,
        type: Int,
        postId: String,
        title: String,
        body: String
    ) {
        val allowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU

        if (!allowed) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.daime.grow.OPEN_MURAL_POST"
            putExtra("postId", postId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            postId.hashCode() + type,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, CHANNEL_MURAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(appIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(postId.hashCode() + type, notification)
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

    fun showHydroponicReminder(context: Context, plantId: Long, plantName: String) {
        showReminderNotification(
            context = context,
            type = TYPE_HYDROponic,
            channelId = CHANNEL_HYDROponic_ID,
            plantId = plantId,
            plantName = plantName,
            title = context.getString(R.string.notification_hydroponic_title),
            body = context.getString(R.string.notification_hydroponic_body, plantName)
        )
    }

    fun showBurpReminder(context: Context, batchId: Long, title: String, body: String, isDrying: Boolean) {
        val channelId = if (isDrying) CHANNEL_BURP_DRYING_ID else CHANNEL_BURP_CURING_ID
        val type = if (isDrying) TYPE_BURP_DRYING else TYPE_BURP_CURING
        
        showBurpNotification(
            context = context,
            type = type,
            channelId = channelId,
            batchId = batchId,
            title = title,
            body = body
        )
    }

    private fun showBurpNotification(
        context: Context,
        type: Int,
        channelId: String,
        batchId: Long,
        title: String,
        body: String
    ) {
        val allowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU

        if (!allowed) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.daime.grow.OPEN_POSCOLHEITA"
            putExtra("batchId", batchId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(type, batchId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(appIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.notification_action_open),
                pendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(type, batchId), notification)
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

        val appIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(appIcon)
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
                R.drawable.ic_launcher_foreground,
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
