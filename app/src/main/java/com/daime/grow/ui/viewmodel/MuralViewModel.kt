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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MuralUiState(
    val posts: List<MuralPostWithPlant> = emptyList(),
    val isLoading: Boolean = true
)

data class MuralPostUiState(
    val post: MuralPostWithPlant? = null,
    val comments: List<CommentWithUser> = emptyList(),
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
                    posts = posts,
                    isLoading = false
                )
            }
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
            }
        }
        loadComments(postId)
    }

    private fun loadComments(postId: Long) {
        viewModelScope.launch {
            muralDao.observeCommentsWithUsers(postId).collect { comments ->
                _postUiState.value = _postUiState.value.copy(comments = comments)
            }
        }
    }

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
        viewModelScope.launch {
            muralDao.deleteComment(commentId)
        }
    }

    fun editComment(commentId: Long, newContent: String) {
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
