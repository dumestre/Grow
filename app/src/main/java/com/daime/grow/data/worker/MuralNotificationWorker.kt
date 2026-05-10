package com.daime.grow.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.content.edit
import com.daime.grow.GrowApplication
import com.daime.grow.data.local.entity.NotificationEntity
import com.daime.grow.data.local.entity.NotificationType
import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.MuralCommentDto
import com.daime.grow.data.remote.model.MuralLikeDto
import com.daime.grow.data.remote.model.MuralPostDto
import com.daime.grow.data.remote.model.MuralUserDto
import com.daime.grow.data.reminder.NotificationHelper
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.firstOrNull
import java.time.format.DateTimeFormatter
import java.time.Instant

class MuralNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GrowApplication
        val container = app.appContainer
        val supabase = SupabaseClient.clientOrNull ?: return Result.success()
        
        val sharedPrefs = applicationContext.getSharedPreferences("mural_worker_prefs", Context.MODE_PRIVATE)
        val lastCheck = sharedPrefs.getLong("last_mural_check", 0L)
        
        // Usa o UUID real do repositório de preferências do mural
        val userId = container.muralPreferencesRepository.currentUserUuid.firstOrNull() ?: return Result.success()

        try {
            val lastCheckInstant = Instant.ofEpochMilli(lastCheck)
            val isoDate = DateTimeFormatter.ISO_INSTANT.format(lastCheckInstant)
            
            // 1. Buscar meus posts para saber o que monitorar
            val myPosts = supabase.from("mural_posts")
                .select { filter { eq("user_id", userId) } }
                .decodeList<MuralPostDto>()

            val postIds = myPosts.mapNotNull { it.id }
            
            if (postIds.isNotEmpty()) {
                // 2. Verificar Novos Comentários nos meus posts
                val newComments = supabase.from("mural_comments")
                    .select {
                        filter {
                            isIn("post_id", postIds)
                            gt("created_at", isoDate)
                            neq("user_id", userId)
                        }
                    }
                    .decodeList<MuralCommentDto>()

                for (comment in newComments) {
                    val author = fetchUser(comment.user_id)
                    val username = author?.username ?: "Alguém"
                    
                    // Salva no banco local de notificações
                    container.database.notificationDao().insertNotification(
                        NotificationEntity(
                            type = NotificationType.NEW_COMMENT,
                            username = username,
                            message = comment.content.take(100),
                            time = System.currentTimeMillis(),
                            relatedId = null // Poderia ser o ID do post
                        )
                    )
                    
                    NotificationHelper.showMuralCommentNotification(
                        applicationContext,
                        comment.post_id,
                        username
                    )
                }

                // 3. Verificar Novas Curtidas nos meus posts
                // Nota: mural_likes geralmente não tem created_at no DTO, 
                // se não tiver, teremos que buscar todos e comparar com um cache local ou ignorar check de data se for inviável.
                // Por enquanto, vamos tentar buscar e assumir que o DTO/Tabela suporte se possível, 
                // ou apenas buscar as mais recentes se houver campo.
                try {
                    val newLikes = supabase.from("mural_likes")
                        .select {
                            filter {
                                isIn("post_id", postIds)
                                neq("user_id", userId)
                            }
                        }
                        .decodeList<MuralLikeDto>()
                    
                    // Como mural_likes pode não ter timestamp, poderíamos apenas notificar se o count mudou significativamente
                    // ou se tivermos um jeito de rastrear quais já vimos.
                    // Para simplificar e evitar spam, vamos focar em comentários e respostas por enquanto, 
                    // ou implementar um check básico se houver suporte a data.
                } catch (e: Exception) {
                    android.util.Log.e("MuralWorker", "Erro ao buscar likes", e)
                }
            }
            
            // 4. Verificar Respostas aos meus comentários
            // Primeiro busca meus comentários recentes (ou todos)
            val myComments = supabase.from("mural_comments")
                .select { filter { eq("user_id", userId) } }
                .decodeList<MuralCommentDto>()
            
            val myCommentIds = myComments.mapNotNull { it.id }
            if (myCommentIds.isNotEmpty()) {
                val newReplies = supabase.from("mural_comments")
                    .select {
                        filter {
                            isIn("parent_id", myCommentIds)
                            gt("created_at", isoDate)
                            neq("user_id", userId)
                        }
                    }
                    .decodeList<MuralCommentDto>()
                
                for (reply in newReplies) {
                    val author = fetchUser(reply.user_id)
                    val username = author?.username ?: "Alguém"

                    container.database.notificationDao().insertNotification(
                        NotificationEntity(
                            type = NotificationType.NEW_REPLY,
                            username = username,
                            message = reply.content.take(100),
                            time = System.currentTimeMillis(),
                            relatedId = null
                        )
                    )

                    NotificationHelper.showMuralReplyNotification(
                        applicationContext,
                        reply.post_id,
                        username
                    )
                }
            }

            // Atualizar o timestamp da última verificação
            sharedPrefs.edit { putLong("last_mural_check", System.currentTimeMillis()) }

            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("MuralWorker", "Erro no worker", e)
            return Result.retry()
        }
    }

    private suspend fun fetchUser(userId: String): MuralUserDto? {
        val supabase = SupabaseClient.clientOrNull ?: return null
        return try {
            supabase.from("mural_users")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<MuralUserDto>()
        } catch (e: Exception) {
            null
        }
    }
}
