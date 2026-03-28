package com.daime.grow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daime.grow.data.local.entity.MuralCommentEntity
import com.daime.grow.data.local.entity.MuralPostEntity
import com.daime.grow.data.local.entity.MuralUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MuralDao {
    @Query("SELECT * FROM mural_posts ORDER BY createdAt DESC")
    fun observeMuralPosts(): Flow<List<MuralPostEntity>>

    @Query("""
        SELECT mp.*, p.name, p.strain, p.stage, p.days, p.photoUri, p.sharedOnMural, p.createdAt as plantCreatedAt, p.medium
        FROM mural_posts mp
        INNER JOIN plants p ON mp.plantId = p.id
        WHERE p.sharedOnMural = 1
        ORDER BY mp.createdAt DESC
    """)
    fun observeMuralPostsWithPlants(): Flow<List<MuralPostWithPlant>>

    @Query("SELECT * FROM mural_posts WHERE remoteId = :postId LIMIT 1")
    fun observePost(postId: String): Flow<MuralPostEntity?>

    @Query("SELECT * FROM mural_posts WHERE remoteId = :postId LIMIT 1")
    suspend fun getPostByRemoteId(postId: String): MuralPostEntity?

    @Query("SELECT * FROM mural_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: Long): MuralPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: MuralPostEntity): Long

    @Query("UPDATE mural_posts SET remoteId = :remoteId WHERE id = :localId")
    suspend fun updatePostRemoteId(localId: Long, remoteId: String)

    @Query("DELETE FROM mural_posts WHERE remoteId = :postId")
    suspend fun deletePostByRemoteId(postId: String)

    @Query("DELETE FROM mural_posts WHERE id = :postId")
    suspend fun deletePost(postId: Long)

    @Query("UPDATE plants SET sharedOnMural = :shared WHERE id = :plantId")
    suspend fun updatePlantSharedStatus(plantId: Long, shared: Boolean)

    @Query("SELECT * FROM mural_users WHERE remoteId = :userUuid LIMIT 1")
    suspend fun getUserByRemoteId(userUuid: String): MuralUserEntity?

    @Query("SELECT * FROM mural_users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): MuralUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: MuralUserEntity): Long

    @Query("UPDATE mural_users SET remoteId = :remoteId WHERE id = :localId")
    suspend fun updateUserRemoteId(localId: Long, remoteId: String)

    @Query("SELECT * FROM mural_comments WHERE remoteId = :commentId LIMIT 1")
    suspend fun getCommentByRemoteId(commentId: String): MuralCommentEntity?

    @Query("SELECT * FROM mural_comments WHERE localPostId = :postId ORDER BY createdAt ASC")
    fun observeComments(postId: Long): Flow<List<MuralCommentEntity>>

    @Query("""
        SELECT mc.*, mu.username
        FROM mural_comments mc
        INNER JOIN mural_users mu ON mc.localUserId = mu.id
        WHERE mc.localPostId = :postId
        ORDER BY mc.createdAt ASC
    """)
    fun observeCommentsWithUsers(postId: Long): Flow<List<CommentWithUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: MuralCommentEntity): Long

    @Query("UPDATE mural_comments SET remoteId = :remoteId WHERE id = :localId")
    suspend fun updateCommentRemoteId(localId: Long, remoteId: String)

    @Update
    suspend fun updateComment(comment: MuralCommentEntity)

    @Query("DELETE FROM mural_comments WHERE remoteId = :commentId")
    suspend fun deleteCommentByRemoteId(commentId: String)

    @Query("DELETE FROM mural_comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Long)
}

data class MuralPostWithPlant(
    val id: Long,
    val remoteId: String?,
    val plantId: Long,
    val createdAt: Long,
    val name: String,
    val strain: String,
    val stage: String,
    val days: Int,
    val photoUri: String?,
    val sharedOnMural: Boolean,
    val plantCreatedAt: Long,
    val medium: String
)

data class CommentWithUser(
    val id: Long,
    val remoteId: String?,
    val localPostId: Long,
    val localUserId: Long,
    val content: String,
    val createdAt: Long,
    val username: String,
    val parentId: String? = null
)
