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

data class MuralUiState(
    val posts: List<MuralPostWithPlant> = emptyList(),
    val isLoading: Boolean = true
)

data class MuralPostUiState(
    val post: MuralPostWithPlant? = null,
    val comments: List<CommentWithUser> = emptyList(),
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isLoading: Boolean = true
)

class MuralViewModel(
    private val muralDao: MuralDao,
    private val preferencesRepository: MuralPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MuralUiState())
    val uiState: StateFlow<MuralUiState> = _uiState.asStateFlow()

    private val _postUiState = MutableStateFlow(MuralPostUiState())
    val postUiState: StateFlow<MuralPostUiState> = _postUiState.asStateFlow()

    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val supabase = SupabaseClient.client

    // --- Placeholders para o Mural ---
    private val placeholderPosts = listOf(
        MuralPostWithPlant(
            id = -10,
            plantId = -1,
            createdAt = System.currentTimeMillis() - 3600000,
            name = "Gorilla Glue #4",
            strain = "Hybrid",
            stage = PlantStage.FLOWER,
            days = 55,
            photoUri = null,
            sharedOnMural = true,
            plantCreatedAt = 0,
            medium = "Solo"
        ),
        MuralPostWithPlant(
            id = -11,
            plantId = -2,
            createdAt = System.currentTimeMillis() - 7200000,
            name = "Purple Haze",
            strain = "Sativa",
            stage = PlantStage.VEGETATIVE,
            days = 30,
            photoUri = null,
            sharedOnMural = true,
            plantCreatedAt = 0,
            medium = "Coco"
        )
    )

    private val placeholderComments = mapOf(
        -10L to listOf(
            CommentWithUser(id = -100, postId = -10, userId = -1, content = "Essa genética é incrível! Parabéns pelo cultivo.", createdAt = System.currentTimeMillis() - 1800000, username = "Jardineiro01"),
            CommentWithUser(id = -101, postId = -10, userId = -2, content = "Valeu! Tá quase na hora da colheita.", createdAt = System.currentTimeMillis() - 900000, username = "GrowMaster")
        ),
        -11L to listOf(
            CommentWithUser(id = -102, postId = -11, userId = -3, content = "Qual fert você está usando?", createdAt = System.currentTimeMillis() - 500000, username = "BioGrower")
        )
    )

    init {
        loadPosts()
        observeStoredUser()
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
                    posts = if (posts.isEmpty()) placeholderPosts else posts,
                    isLoading = false
                )
            }
        }
    }

    fun loadPost(postId: Long) {
        viewModelScope.launch {
            muralDao.observeMuralPostsWithPlants().collect { posts ->
                val allPosts = if (posts.isEmpty()) placeholderPosts else posts
                val post = allPosts.find { it.id == postId }
                
                _postUiState.value = _postUiState.value.copy(
                    post = post,
                    isLoading = false
                )
                
                // Carregar comentários específicos (incluindo placeholders)
                if (postId < 0) {
                    _postUiState.value = _postUiState.value.copy(
                        comments = placeholderComments[postId] ?: emptyList()
                    )
                } else {
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
                // Implementação futura
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleLike(postId: Long) {
        val currentState = _postUiState.value
        val newIsLiked = !currentState.isLiked
        val newCount = if (newIsLiked) currentState.likeCount + 1 else (currentState.likeCount - 1).coerceAtLeast(0)
        
        _postUiState.value = currentState.copy(isLiked = newIsLiked, likeCount = newCount)
        
        if (postId < 0) return // Não sincroniza likes de placeholders

        viewModelScope.launch {
            try {
                // Lógica remota
            } catch (e: Exception) {
                _postUiState.value = currentState
            }
        }
    }

    private fun subscribeToCommentsRealtime(postId: Long) {
        viewModelScope.launch {
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

    private suspend fun syncRemoteCommentToLocal(dto: MuralCommentDto) {}

    fun getCommentsFlow(postId: Long): Flow<List<CommentWithUser>> {
        return muralDao.observeCommentsWithUsers(postId)
    }

    fun sharePlant(plantId: Long) {
        viewModelScope.launch {
            muralDao.updatePlantSharedStatus(plantId, true)
            muralDao.insertPost(
                MuralPostEntity(
                    plantId = plantId,
                    createdAt = System.currentTimeMillis()
                )
            )
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
            onComplete(userId)
        }
    }

    fun addComment(postId: Long, userId: Long, content: String, parentId: Long? = null) {
        if (postId < 0) return // Não permite comentar em placeholders
        viewModelScope.launch {
            muralDao.insertComment(
                MuralCommentEntity(
                    postId = postId,
                    userId = userId,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    parentId = parentId
                )
            )
        }
    }

    fun deleteComment(commentId: Long) {
        if (commentId < 0) return
        viewModelScope.launch {
            muralDao.deleteComment(commentId)
        }
    }

    fun editComment(commentId: Long, newContent: String) {
        if (commentId < 0) return
        viewModelScope.launch {
            val comment = muralDao.getComment(commentId)
            if (comment != null) {
                muralDao.updateComment(comment.copy(content = newContent))
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
