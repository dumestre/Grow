package com.daime.grow.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.daime.grow.R

object NotificationHelper {
    const val CHANNEL_ID = "grow_reminders"

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Grow lembretes",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lembretes de rega e nutrientes"
        }
        manager.createNotificationChannel(channel)
    }

    fun showNotification(context: Context, id: Int, title: String, body: String) {
        val allowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU

        if (!allowed) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

