package com.daime.grow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object NotificationType {
    const val PLANT_SHARED = "PLANT_SHARED"     // Usuario compartilhou sua planta
    const val NEW_COMMENT = "NEW_COMMENT"       // Usuario comentou na sua planta compartilhada
    const val NEW_REPLY = "NEW_REPLY"           // Usuario respondeu seu comentario
    const val NEW_LIKE = "NEW_LIKE"            // Usuario deu like
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val username: String,
    val message: String,
    val time: Long,
    val relatedId: Long? = null,
    val isRead: Boolean = false,
    val userId: Long? = null
)
