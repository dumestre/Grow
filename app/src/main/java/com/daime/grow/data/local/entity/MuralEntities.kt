package com.daime.grow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mural_posts",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId"), Index("remoteId")]
)
data class MuralPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val plantId: Long,
    val createdAt: Long
)

@Entity(tableName = "mural_users", indices = [Index("remoteId", unique = true)])
data class MuralUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val username: String,
    val createdAt: Long
)

@Entity(
    tableName = "mural_comments",
    foreignKeys = [
        ForeignKey(
            entity = MuralPostEntity::class,
            parentColumns = ["id"],
            childColumns = ["localPostId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MuralUserEntity::class,
            parentColumns = ["id"],
            childColumns = ["localUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("localPostId"), Index("localUserId"), Index("remoteId")]
)
data class MuralCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val localPostId: Long,
    val localUserId: Long,
    val content: String,
    val createdAt: Long,
    val parentId: String? = null
)
