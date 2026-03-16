package com.daime.grow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "LIKE", "COMMENT", "HARVEST_REMINDER", etc.
    val username: String?,
    val action: String,
    val time: Long,
    val relatedId: Long? = null, // ID do post, planta ou lote relacionado
    val isRead: Boolean = false
)
