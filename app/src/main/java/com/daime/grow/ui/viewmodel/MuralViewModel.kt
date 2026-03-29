package com.daime.grow.ui.viewmodel

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daime.grow.data.local.dao.CommentWithUser
import com.daime.grow.data.local.dao.MuralDao
import com.daime.grow.data.local.dao.MuralPostWithPlant
import com.daime.grow.data.local.dao.NotificationDao
import com.daime.grow.data.local.entity.MuralCommentEntity
import com.daime.grow.data.local.entity.MuralPostEntity
import com.daime.grow.data.local.entity.MuralUserEntity
import com.daime.grow.data.preferences.MuralPreferencesRepository
import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.MuralCommentDto
import com.daime.grow.data.remote.model.MuralLikeDto
import com.daime.grow.data.remote.model.MuralPostDto
import com.daime.grow.data.remote.model.MuralUserDto
import com.daime.grow.BuildConfig
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

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

sealed class MuralEvent {
    data class UsernameTaken(val username: String) : MuralEvent()
    data object GoogleLoginSuccess : MuralEvent()
    data class GoogleLoginError(val message: String) : MuralEvent()
    data object SignedOut : MuralEvent()
}

@HiltViewModel
class MuralViewModel @Inject constructor(
    private val muralDao: MuralDao,
    private val preferencesRepository: MuralPreferencesRepository,
    private val notificationDao: NotificationDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(MuralUiState())
    val uiState: StateFlow<MuralUiState> = _uiState.asStateFlow()

    private val _postUiState = MutableStateFlow(MuralPostUiState())
    val postUiState: StateFlow<MuralPostUiState> = _postUiState.asStateFlow()

    private val _currentUserUuid = MutableStateFlow<String?>(null)
    val currentUserUuid: StateFlow<String?> = _currentUserUuid.asStateFlow()

    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    private val _isAuthResolved = MutableStateFlow(false)
    val isAuthResolved: StateFlow<Boolean> = _isAuthResolved.asStateFlow()

    private val _events = MutableSharedFlow<MuralEvent>()
    val events: SharedFlow<MuralEvent> = _events.asSharedFlow()

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
            preferencesRepository.currentUserUuid.collect { uuid ->
                _currentUserUuid.value = uuid
                if (uuid != null) {
                    val user = muralDao.getUserByRemoteId(uuid)
                    _currentUsername.value = user?.username
                } else {
                    _currentUsername.value = null
                }
                _isAuthResolved.value = true
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
        viewModelScope.launch {
            syncWithRemote()
        }
    }

    private suspend fun syncWithRemote() {
        val supabase = this.supabase ?: return
        try {
            android.util.Log.d("MuralViewModel", "Iniciando sincronizacao com remote...")

            val remoteUsers = supabase.from("mural_users")
                .select()
                .decodeList<MuralUserDto>()

            val remotePosts = supabase.from("mural_posts")
                .select()
                .decodeList<MuralPostDto>()

            val remoteComments = supabase.from("mural_comments")
                .select()
                .decodeList<MuralCommentDto>()

            android.util.Log.d(
                "MuralViewModel",
                "Remote: ${remoteUsers.size} usuarios, ${remotePosts.size} posts, ${remoteComments.size} comentarios"
            )

            remoteUsers.forEach { userDto ->
                if (userDto.id != null && userDto.username.isNotEmpty()) {
                    val existingUser = muralDao.getUserByRemoteId(userDto.id)
                    if (existingUser == null) {
                        muralDao.insertUser(
                            MuralUserEntity(
                                remoteId = userDto.id,
                                username = userDto.username,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        android.util.Log.d("MuralViewModel", "Usuario criado localmente: ${userDto.username}")
                    }
                }
            }

            remotePosts.forEach { postDto ->
                if (postDto.id != null) {
                    val existingPost = muralDao.getPostByRemoteId(postDto.id)
                    if (existingPost == null) {
                        muralDao.insertPost(
                            MuralPostEntity(
                                remoteId = postDto.id,
                                plantId = 0,
                                createdAt = System.currentTimeMillis(),
                                plantName = postDto.plant_name,
                                strain = postDto.strain ?: "",
                                stage = postDto.stage ?: "Germinacao",
                                medium = postDto.medium ?: "",
                                days = postDto.days ?: 0,
                                photoUrl = postDto.photo_url
                            )
                        )
                        android.util.Log.d("MuralViewModel", "Post criado localmente: ${postDto.plant_name}")
                    }
                }
            }

            remoteComments.forEach { commentDto ->
                if (commentDto.id != null) {
                    val existingComment = muralDao.getCommentByRemoteId(commentDto.id)
                    if (existingComment == null) {
                        val user = muralDao.getUserByRemoteId(commentDto.user_id)
                        val post = muralDao.getPostByRemoteId(commentDto.post_id)
                        android.util.Log.d(
                            "MuralViewModel",
                            "Tentando sync comentario - user: ${user?.username}, post: ${post?.id}"
                        )
                        if (user != null && post != null) {
                            muralDao.insertComment(
                                MuralCommentEntity(
                                    remoteId = commentDto.id,
                                    localPostId = post.id,
                                    localUserId = user.id,
                                    content = commentDto.content,
                                    createdAt = System.currentTimeMillis(),
                                    parentId = commentDto.parent_id
                                )
                            )
                            android.util.Log.d("MuralViewModel", "Comentario criado localmente: ${commentDto.content}")
                        }
                    }
                }
            }
            android.util.Log.d("MuralViewModel", "Sincronizacao concluida!")
        } catch (e: Exception) {
            android.util.Log.e("MuralViewModel", "Erro ao sincronizar com remote: ${e.message}", e)
        }
    }

    fun loadPost(postId: String) {
        viewModelScope.launch {
            muralDao.observeMuralPostsWithPlants().collect { posts ->
                val post = posts.find { it.remoteId == postId }

                _postUiState.value = _postUiState.value.copy(
                    post = post,
                    isLoading = false
                )

                if (post != null && post.remoteId != null) {
                    loadComments(post.id)
                    loadLikes(post.remoteId)
                    subscribeToCommentsRealtime(post.remoteId)
                    subscribeToLikesRealtime(post.remoteId)
                }
            }
        }
    }

    private fun loadComments(localPostId: Long) {
        viewModelScope.launch {
            muralDao.observeCommentsWithUsers(localPostId).collect { comments ->
                _postUiState.value = _postUiState.value.copy(comments = comments)
            }
        }
    }

    private fun loadLikes(postId: String) {
        viewModelScope.launch {
            try {
                val supabase = this@MuralViewModel.supabase ?: return@launch
                val likes = supabase.from("mural_likes")
                    .select { filter { eq("post_id", postId) } }
                    .decodeList<MuralLikeDto>()
                _postUiState.value = _postUiState.value.copy(likeCount = likes.size)

                val currentUserUuid = _currentUserUuid.value
                val isLiked = currentUserUuid != null && likes.any { it.user_id == currentUserUuid }
                _postUiState.value = _postUiState.value.copy(isLiked = isLiked)
            } catch (e: Exception) {
                android.util.Log.e("MuralViewModel", "Erro ao carregar curtidas", e)
            }
        }
    }

    fun toggleLike(postId: String) {
        val currentState = _postUiState.value
        val newIsLiked = !currentState.isLiked
        val newCount = if (newIsLiked) currentState.likeCount + 1 else (currentState.likeCount - 1).coerceAtLeast(0)

        _postUiState.value = currentState.copy(isLiked = newIsLiked, likeCount = newCount)

        viewModelScope.launch {
            try {
                val supabase = this@MuralViewModel.supabase ?: return@launch
                val currentUserUuid = _currentUserUuid.value
                if (currentUserUuid != null) {
                    if (newIsLiked) {
                        supabase.from("mural_likes").insert(
                            MuralLikeDto(
                                post_id = postId,
                                user_id = currentUserUuid
                            )
                        )
                    } else {
                        supabase.from("mural_likes")
                            .delete { filter { eq("post_id", postId); eq("user_id", currentUserUuid) } }
                    }
                }
            } catch (e: Exception) {
                _postUiState.value = currentState
            }
        }
    }

    fun sharePlant(plantId: Long, plantName: String, strain: String, stage: String, medium: String, days: Int, photoUrl: String?) {
        viewModelScope.launch {
            muralDao.updatePlantSharedStatus(plantId, true)

            muralDao.insertPost(
                MuralPostEntity(
                    plantId = plantId,
                    createdAt = System.currentTimeMillis(),
                    plantName = plantName,
                    strain = strain,
                    stage = stage,
                    medium = medium,
                    days = days,
                    photoUrl = photoUrl
                )
            )

            val supabase = this@MuralViewModel.supabase
            val currentUserUuid = _currentUserUuid.value
            if (supabase != null && currentUserUuid != null) {
                try {
                    supabase.from("mural_posts").insert(
                        MuralPostDto(
                            user_id = currentUserUuid,
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

    fun checkUsernameAvailable(username: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(isUsernameAvailable(username))
        }
    }

    fun createOrGetUser(username: String, onComplete: (String) -> Unit, onUsernameTaken: () -> Unit) {
        viewModelScope.launch {
            val normalizedUsername = sanitizeUsername(username)
            if (!isUsernameAvailable(normalizedUsername)) {
                _events.emit(MuralEvent.UsernameTaken(normalizedUsername))
                onUsernameTaken()
                return@launch
            }

            val userUuid = createUserAndPersistSession(normalizedUsername)
            onComplete(userUuid)
        }
    }

    fun signInWithGoogle(context: android.content.Context, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            if (this@MuralViewModel.supabase == null) {
                _events.emit(MuralEvent.GoogleLoginError("Supabase nao configurado"))
                return@launch
            }

            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
                _events.emit(
                    MuralEvent.GoogleLoginError(
                        "Google login nao configurado. Defina GOOGLE_WEB_CLIENT_ID no ambiente local."
                    )
                )
                return@launch
            }

            try {
                android.util.Log.d("MuralViewModel", "Iniciando login nativo com Google...")

                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetSignInWithGoogleOption(BuildConfig.GOOGLE_WEB_CLIENT_ID)

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context
                    )

                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                    val baseUsername = googleIdTokenCredential.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: googleIdTokenCredential.id.substringBefore("@")
                    val resolvedUsername = findExistingOrAvailableUsername(baseUsername)
                    val userUuid = createUserAndPersistSession(resolvedUsername)

                    android.util.Log.d(
                        "MuralViewModel",
                        "Google ID Token obtido (${googleIdTokenCredential.idToken.take(12)}...), sessao local criada"
                    )

                    _events.emit(MuralEvent.GoogleLoginSuccess)
                    onComplete(userUuid)
                } catch (e: NoCredentialException) {
                    android.util.Log.e("MuralViewModel", "Nenhuma credencial Google disponivel: ${e.message}", e)
                    _events.emit(
                        MuralEvent.GoogleLoginError(
                            "Nenhuma conta Google disponivel neste aparelho para login. Verifique se ha uma conta adicionada e se os Servicos do Google Play estao atualizados."
                        )
                    )
                } catch (e: GetCredentialException) {
                    android.util.Log.e("MuralViewModel", "Erro ao obter credencial: ${e.message}", e)
                    _events.emit(MuralEvent.GoogleLoginError(e.message ?: "Erro ao obter credencial Google"))
                } catch (e: GoogleIdTokenParsingException) {
                    android.util.Log.e("MuralViewModel", "Erro ao processar token: ${e.message}", e)
                    _events.emit(MuralEvent.GoogleLoginError("Erro ao processar resposta do Google"))
                }
            } catch (e: Exception) {
                android.util.Log.e("MuralViewModel", "Erro no login Google: ${e.message}", e)
                _events.emit(MuralEvent.GoogleLoginError(e.message ?: "Erro desconhecido"))
            }
        }
    }

    fun signOut(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            preferencesRepository.clearUserUuid()
            _currentUserUuid.value = null
            _currentUsername.value = null
            _postUiState.value = MuralPostUiState()
            _events.emit(MuralEvent.SignedOut)
            onComplete?.invoke()
        }
    }

    private suspend fun isUsernameAvailable(username: String): Boolean {
        if (muralDao.getUserByUsername(username) != null) {
            return false
        }

        val supabase = this@MuralViewModel.supabase ?: return true
        return try {
            val result = supabase.from("mural_users")
                .select { filter { eq("username", username) } }
                .decodeList<MuralUserDto>()
            result.isEmpty()
        } catch (e: Exception) {
            true
        }
    }

    private suspend fun createUserAndPersistSession(username: String): String {
        val existingLocalUser = muralDao.getUserByUsername(username)
        val localUserId = existingLocalUser?.id ?: muralDao.insertUser(
            MuralUserEntity(
                username = username,
                createdAt = System.currentTimeMillis()
            )
        )

        val user = existingLocalUser ?: muralDao.getUserByUsername(username)
        var userUuid: String = user?.remoteId ?: ""

        val supabase = this@MuralViewModel.supabase
        if (supabase != null && userUuid.isEmpty()) {
            try {
                val existingRemoteUsers = supabase.from("mural_users")
                    .select { filter { eq("username", username) } }
                    .decodeList<MuralUserDto>()

                if (existingRemoteUsers.isNotEmpty() && existingRemoteUsers.first().id != null) {
                    userUuid = existingRemoteUsers.first().id!!
                } else {
                    supabase.from("mural_users").insert(
                        MuralUserDto(
                            username = username
                        )
                    )
                    val newUsers = supabase.from("mural_users")
                        .select { filter { eq("username", username) } }
                        .decodeList<MuralUserDto>()
                    if (newUsers.isNotEmpty() && newUsers.first().id != null) {
                        userUuid = newUsers.first().id!!
                    }
                }

                if (userUuid.isNotEmpty()) {
                    muralDao.updateUserRemoteId(localUserId, userUuid)
                }
            } catch (e: Exception) {
                android.util.Log.e("MuralViewModel", "Erro ao sincronizar usuario: ${e.message}")
            }
        }

        if (userUuid.isEmpty()) {
            userUuid = username
        }

        preferencesRepository.saveUserUuid(userUuid)
        _currentUserUuid.value = userUuid
        _currentUsername.value = username
        return userUuid
    }

    private suspend fun findExistingOrAvailableUsername(baseUsername: String): String {
        val normalized = sanitizeUsername(baseUsername)
        if (muralDao.getUserByUsername(normalized) != null) {
            return normalized
        }
        return ensureUniqueUsername(normalized)
    }

    private suspend fun ensureUniqueUsername(baseUsername: String): String {
        var username = baseUsername
        var counter = 1
        while (muralDao.getUserByUsername(username) != null) {
            username = "${baseUsername}_$counter"
            counter++
        }
        return username
    }

    private fun sanitizeUsername(rawUsername: String): String {
        return rawUsername
            .trim()
            .lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .take(20)
            .ifBlank { "grower" }
    }

    fun addComment(postId: String, content: String, parentId: String? = null) {
        viewModelScope.launch {
            android.util.Log.d("MuralViewModel", "addComment: postId=$postId, content=$content")

            val post = muralDao.getPostByRemoteId(postId)
            if (post == null) {
                android.util.Log.e("MuralViewModel", "Post nao encontrado: $postId")
                return@launch
            }

            val userUuid = _currentUserUuid.value ?: _currentUsername.value
            if (userUuid == null) {
                android.util.Log.e("MuralViewModel", "Usuario nao definido")
                return@launch
            }

            var user = muralDao.getUserByRemoteId(userUuid)
            if (user == null) {
                user = muralDao.getUserByUsername(userUuid)
            }

            if (user == null) {
                android.util.Log.e("MuralViewModel", "Usuario nao encontrado: $userUuid")
                return@launch
            }

            val localCommentId = muralDao.insertComment(
                MuralCommentEntity(
                    localPostId = post.id,
                    localUserId = user.id,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    parentId = parentId
                )
            )
            android.util.Log.d("MuralViewModel", "Comentario salvo localmente: id=$localCommentId")

            _postUiState.value = _postUiState.value.copy(
                comments = _postUiState.value.comments + CommentWithUser(
                    id = localCommentId,
                    remoteId = null,
                    localPostId = post.id,
                    localUserId = user.id,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    username = user.username,
                    parentId = parentId
                )
            )

            val supabase = this@MuralViewModel.supabase
            if (supabase != null) {
                try {
                    supabase.from("mural_comments").insert(
                        MuralCommentDto(
                            post_id = postId,
                            user_id = _currentUserUuid.value ?: userUuid,
                            content = content,
                            parent_id = parentId
                        )
                    )
                    android.util.Log.d("MuralViewModel", "Comentario sincronizado com Supabase")
                } catch (e: Exception) {
                    android.util.Log.e("MuralViewModel", "Erro ao sincronizar comentario: ${e.message}")
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            val comment = muralDao.getCommentByRemoteId(commentId)
            if (comment != null) {
                muralDao.deleteCommentByRemoteId(commentId)
            }

            val supabase = this@MuralViewModel.supabase
            if (supabase != null) {
                try {
                    supabase.from("mural_comments")
                        .delete { filter { eq("id", commentId) } }
                } catch (e: Exception) {
                    android.util.Log.e("MuralViewModel", "Erro ao deletar comentario remoto: ${e.message}")
                }
            }
        }
    }

    fun editComment(commentId: String, newContent: String) {
        viewModelScope.launch {
            val comment = muralDao.getCommentByRemoteId(commentId) ?: return@launch

            muralDao.updateComment(comment.copy(content = newContent))

            val supabase = this@MuralViewModel.supabase
            if (supabase != null) {
                try {
                    supabase.from("mural_comments")
                        .update({ set("content", newContent) }) { filter { eq("id", commentId) } }
                } catch (e: Exception) {
                    android.util.Log.e("MuralViewModel", "Erro ao editar comentario remoto: ${e.message}")
                }
            }
        }
    }

    private fun subscribeToCommentsRealtime(postId: String) {
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
                        if (dto.post_id == postId) {
                            syncRemoteCommentToLocal(dto)
                        }
                    }
                    is PostgresAction.Delete -> {
                        val oldRecord = action.oldRecord
                        val commentId = oldRecord["id"]?.jsonPrimitive?.content
                        val commentPostId = oldRecord["post_id"]?.jsonPrimitive?.content
                        if (commentId != null && commentPostId == postId) {
                            muralDao.getCommentByRemoteId(commentId)?.let {
                                muralDao.deleteCommentByRemoteId(commentId)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun subscribeToLikesRealtime(postId: String) {
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
        if (dto.id == null) return
        try {
            val existingComment = muralDao.getCommentByRemoteId(dto.id)
            if (existingComment != null) return

            val user = muralDao.getUserByRemoteId(dto.user_id) ?: return
            val post = muralDao.getPostByRemoteId(dto.post_id) ?: return

            muralDao.insertComment(
                MuralCommentEntity(
                    remoteId = dto.id,
                    localPostId = post.id,
                    localUserId = user.id,
                    content = dto.content,
                    createdAt = System.currentTimeMillis(),
                    parentId = dto.parent_id
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("MuralViewModel", "Erro ao sincronizar comentario: ${e.message}")
        }
    }

    fun getCommentsFlow(postId: Long): Flow<List<CommentWithUser>> {
        return muralDao.observeCommentsWithUsers(postId)
    }
}

class MuralViewModelFactory(
    private val muralDao: MuralDao,
    private val preferencesRepository: MuralPreferencesRepository,
    private val notificationDao: NotificationDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MuralViewModel(muralDao, preferencesRepository, notificationDao) as T
    }
}
