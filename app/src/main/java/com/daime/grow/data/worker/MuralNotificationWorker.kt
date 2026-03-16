package com.daime.grow.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daime.grow.R
import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.MuralCommentDto
import com.daime.grow.data.remote.model.MuralPostDto
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.Instant

class MuralNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val supabase = SupabaseClient.clientOrNull ?: return Result.success()
        val sharedPrefs = applicationContext.getSharedPreferences("mural_prefs", Context.MODE_PRIVATE)
        val lastCheck = sharedPrefs.getLong("last_mural_check", 0L)
        val userId = sharedPrefs.getString("remote_user_id", null) ?: return Result.success()

        try {
            // 1. Verificar novos comentários em seus posts
            val myPosts = supabase.from("mural_posts")
                .select { 
                    filter { 
                        eq("user_id", userId) 
                    } 
                }
                .decodeList<MuralPostDto>()

            val postIds = myPosts.mapNotNull { it.id }
            if (postIds.isNotEmpty()) {
                val newComments = supabase.from("mural_comments")
                    .select {
                        filter {
                            isIn("post_id", postIds)
                            gt("created_at", Instant.fromEpochMilliseconds(lastCheck))
                            neq("user_id", userId) // Não notificar meus próprios comentários
                        }
                    }
                    .decodeList<MuralCommentDto>()

                if (newComments.isNotEmpty()) {
                    showNotification(
                        "Novo comentário!",
                        "Alguém comentou no seu post no Mural."
                    )
                }
            }

            // Atualizar o timestamp da última verificação
            sharedPrefs.edit().putLong("last_mural_check", System.currentTimeMillis()).apply()

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "mural_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Mural", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.planta) // Use o ícone do seu app
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
