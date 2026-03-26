package com.daime.grow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daime.grow.data.local.dao.CommentWithUser
import com.daime.grow.data.local.dao.MuralDao
import com.daime.grow.data.local.dao.MuralPostWithPlant
import com.daime.grow.data.local.entity.MuralCommentEntity
import com.daime.grow.data.local.entity.MuralPostEntity
import com.daime.grow.data.local.entity.MuralUserEntity
import com.daime.grow.data.preferences.MuralPreferencesRepository
import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.MuralCommentDto
import com.daime.grow.data.remote.model.MuralLikeDto
import com.daime.grow.data.remote.model.MuralPostDto
import com.daime.grow.data.remote.model.MuralUserDto
import com.daime.grow.domain.model.Plant
import com.daime.grow.domain.model.PlantStage
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

data class MuralUiState(
    val posts: List<MuralPostWithPlant> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class MuralPostUiState(
    val post: MuralPostWithPlant? = null,
    val comments: List<CommentWithUser> = emptyList(),
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class MuralViewModel @Inject constructor(
    private val muralDao: MuralDao,
    private val preferencesRepository: MuralPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MuralUiState())
    val uiState: StateFlow<MuralUiState> = _uiState.asStateFlow()

    private val _postUiState = MutableStateFlow(MuralPostUiState())
    val postUiState: StateFlow<MuralPostUiState> = _postUiState.asStateFlow()

    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val supabase = SupabaseClient.clientOrNull

    init {
        loadPosts()
        observeStoredUser()
        viewModelScope.launch {
            syncWithRemote()
        }
    }

    private fun observeStoredUser() {
        viewModelScope.launch {
            preferencesRepository.currentUserId.collect { id ->
                _currentUserId.value = id
            }
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            muralDao.observeMuralPostsWithPlants().collect { posts ->
                _uiState.value = _uiState.value.copy(
                    posts = posts,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun syncWithRemote() {
        val supabase = this.supabase ?: return
        try {
            val remotePosts = supabase.from("mural_posts")
                .select()
                .decodeList<MuralPostDto>()
            
            val remoteUsers = supabase.from("mural_users")
                .select()
                .decodeList<MuralUserDto>()
            
            val remoteComments = supabase.from("mural_comments")
                .select()
                .decodeList<MuralCommentDto>()

            remoteUsers.forEach { userDto ->
                if (userDto.id != null) {
                    muralDao.insertUser(
                        MuralUserEntity(
                            id = userDto.id.toLongOrNull() ?: 0,
                            username = userDto.username,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            remotePosts.forEach { postDto ->
                if (postDto.id != null) {
                    muralDao.insertPost(
                        MuralPostEntity(
                            id = postDto.id.toLongOrNull() ?: 0,
                            plantId = 0,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            remoteComments.forEach { commentDto ->
                if (commentDto.id != null) {
                    muralDao.insertComment(
                        MuralCommentEntity(
                            id = commentDto.id.toLongOrNull() ?: 0,
                            postId = commentDto.post_id.toLongOrNull() ?: 0,
                            userId = commentDto.user_id.toLongOrNull() ?: 0,
                            content = commentDto.content,
                            createdAt = System.currentTimeMillis(),
                            parentId = commentDto.parent_id?.toLongOrNull()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MuralViewModel", "Erro ao sincronizar com remote: ${e.message}")
        }
    }

    fun loadPost(postId: Long) {
        viewModelScope.launch {
            muralDao.observeMuralPostsWithPlants().collect { posts ->
                val post = posts.find { it.id == postId }
                
                _postUiState.value = _postUiState.value.copy(
                    post = post,
                    isLoading = false
                )
                
                if (post != null) {
                    loadComments(postId)
                }
            }
        }
        if (postId >= 0) {
            loadLikes(postId)
            subscribeToCommentsRealtime(postId)
            subscribeToLikesRealtime(postId)
        }
    }

    private fun loadComments(postId: Long) {
        viewModelScope.launch {
            muralDao.observeCommentsWithUsers(postId).collect { comments ->
                _postUiState.value = _postUiState.value.copy(comments = comments)
            }
        }
    }

    private fun loadLikes(postId: Long) {
        viewModelScope.launch {
            try {
                val supabase = this@MuralViewModel.supabase ?: return@launch
                val likes = supabase.from("mural_likes")
                    .select { filter { eq("post_id", postId.toString()) } }
                    .decodeList<MuralLikeDto>()
                _postUiState.value = _postUiState.value.copy(likeCount = likes.size)
            } catch (e: Exception) {
                android.util.Log.e("MuralViewModel", "Erro ao carregar curtidas", e)
            }
        }
    }

    fun toggleLike(postId: Long) {
        val currentState = _postUiState.value
        val newIsLiked = !currentState.isLiked
        val newCount = if (newIsLiked) currentState.likeCount + 1 else (currentState.likeCount - 1).coerceAtLeast(0)
        
        _postUiState.value = currentState.copy(isLiked = newIsLiked, likeCount = newCount)
        
        viewModelScope.launch {
            try {
                val supabase = this@MuralViewModel.supabase ?: return@launch
                val currentUserId = _currentUserId.value
                if (currentUserId != null) {
                    if (newIsLiked) {
                        supabase.from("mural_likes").insert(
                            MuralLikeDto(
                                post_id = postId.toString(),
                                user_id = currentUserId.toString()
                            )
                        )
                    } else {
                        supabase.from("mural_likes")
                            .delete { filter { eq("post_id", postId.toString()); eq("user_id", currentUserId.toString()) } }
                    }
                }
            } catch (e: Exception) {
                _postUiState.value = currentState
            }
        }
    }

    private fun subscribeToCommentsRealtime(postId: Long) {
        viewModelScope.launch {
            val supabase = supabase ?: return@launch
            val channel = supabase.realtime.channel("comments_$postId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "mural_comments"
            }
            channel.subscribe()
            changeFlow.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        val dto = action.decodeRecord<MuralCommentDto>()
                        if (dto.post_id == postId.toString()) {
                             syncRemoteCommentToLocal(dto)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun subscribeToLikesRealtime(postId: Long) {
        viewModelScope.launch {
            val supabase = supabase ?: return@launch
            val channel = supabase.realtime.channel("likes_$postId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "mural_likes"
            }
            channel.subscribe()
            changeFlow.collect {
                loadLikes(postId)
            }
        }
    }

    private suspend fun syncRemoteCommentToLocal(dto: MuralCommentDto) {
        if (dto.id == null || dto.user_id == null) return
        try {
            val userId = dto.user_id.toLongOrNull() ?: return
            muralDao.insertComment(
                MuralCommentEntity(
                    id = dto.id.toLongOrNull() ?: 0,
                    postId = dto.post_id.toLongOrNull() ?: 0,
                    userId = userId,
                    content = dto.content,
                    createdAt = System.currentTimeMillis(),
                    parentId = dto.parent_id?.toLongOrNull()
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("MuralViewModel", "Erro ao sincronizar comentário: ${e.message}")
        }
    }

    fun getCommentsFlow(postId: Long): Flow<List<CommentWithUser>> {
        return muralDao.observeCommentsWithUsers(postId)
    }

    fun sharePlant(plantId: Long, plantName: String, strain: String, stage: String, medium: String, days: Int, photoUrl: String?) {
        viewModelScope.launch {
            muralDao.updatePlantSharedStatus(plantId, true)
            val localPostId = muralDao.insertPost(
                MuralPostEntity(
                    plantId = plantId,
                    createdAt = System.currentTimeMillis()
                )
            )
            
            val supabase = this@MuralViewModel.supabase
            if (supabase != null) {
                try {
                    supabase.from("mural_posts").insert(
                        MuralPostDto(
                            id = localPostId.toString(),
                            user_id = _currentUserId.value?.toString() ?: "",
                            plant_name = plantName,
                            strain = strain,
                            stage = stage,
                            medium = medium,
                            days = days,
                            photo_url = photoUrl
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MuralViewModel", "Erro ao sincronizar post: ${e.message}")
                }
            }
        }
    }

    fun createOrGetUser(username: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            var user = muralDao.getUserByUsername(username)
            val userId = if (user == null) {
                muralDao.insertUser(
                    MuralUserEntity(
                        username = username,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                user.id
            }
            preferencesRepository.saveUserId(userId)
            
            val supabase = this@MuralViewModel.supabase
            if (supabase != null) {
                try {
                    supabase.from("mural_users").insert(
                        MuralUserDto(
                            id = userId.toString(),
                            username = username
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MuralViewModel", "Erro ao sincronizar usuário: ${e.message}")
                }
            }
            
            onComplete(userId)
        }
    }

    fun addComment(postId: Long, userId: Long, content: String, parentId: Long? = null) {
        viewModelScope.launch {
            val localCommentId = muralDao.insertComment(
                MuralCommentEntity(
                    postId = postId,
                    userId = userId,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    parentId = parentId
                )
            )
            
            val supabase = this@MuralViewModel.supabase
            if (supabase != null) {
                try {
                    supabase.from("mural_comments").insert(
                        MuralCommentDto(
                            id = localCommentId.toString(),
                            post_id = postId.toString(),
                            user_id = userId.toString(),
                            content = content,
                            parent_id = parentId?.toString()
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MuralViewModel", "Erro ao sincronizar comentário: ${e.message}")
                }
            }
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            muralDao.deleteComment(commentId)
            
            val supabase = this@MuralViewModel.supabase
            if (supabase != null) {
                try {
                    supabase.from("mural_comments")
                        .delete { filter { eq("id", commentId.toString()) } }
                } catch (e: Exception) {
                    android.util.Log.e("MuralViewModel", "Erro ao deletar comentário remoto: ${e.message}")
                }
            }
        }
    }

    fun editComment(commentId: Long, newContent: String) {
        viewModelScope.launch {
            val comment = muralDao.getComment(commentId)
            if (comment != null) {
                muralDao.updateComment(comment.copy(content = newContent))
                
                val supabase = this@MuralViewModel.supabase
                if (supabase != null) {
                    try {
                        supabase.from("mural_comments")
                            .update({ set("content", newContent) }) { filter { eq("id", commentId.toString()) } }
                    } catch (e: Exception) {
                        android.util.Log.e("MuralViewModel", "Erro ao editar comentário remoto: ${e.message}")
                    }
                }
            }
        }
    }
}

class MuralViewModelFactory(
    private val muralDao: MuralDao,
    private val preferencesRepository: MuralPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MuralViewModel(muralDao, preferencesRepository) as T
    }
}
