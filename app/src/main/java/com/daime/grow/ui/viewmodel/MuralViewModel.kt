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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _isAuthResolved = MutableStateFlow(false)
    val isAuthResolved: StateFlow<Boolean> = _isAuthResolved.asStateFlow()

    private val _events = MutableSharedFlow<MuralEvent>()
    val events: SharedFlow<MuralEvent> = _events.asSharedFlow()

    private val supabase = SupabaseClient.clientOrNull

    private var currentPostJob: kotlinx.coroutines.Job? = null
    private var commentsCollectionJob: kotlinx.coroutines.Job? = null
    private var commentsSubscriptionJob: kotlinx.coroutines.Job? = null
    private var likesSubscriptionJob: kotlinx.coroutines.Job? = null

    init {
        loadPosts()
        observeStoredUser()
    }

    private fun observeStoredUser() {
        viewModelScope.launch {
            combine(
                preferencesRepository.currentUserUuid,
                preferencesRepository.currentUsername
            ) { uuid, username ->
                uuid to username
            }.collect { (uuid, username) ->
                _currentUserUuid.value = uuid
                _currentUsername.value = username

                if (uuid != null && username == null) {
                    // Temos ID mas não nome nas prefs, tenta buscar no banco local
                    val user = muralDao.getUserByRemoteId(uuid)
                    if (user != null) {
                        preferencesRepository.saveUsername(user.username)
                    } else {
                        // Se não tem local, tenta buscar do remote
                        fetchUserFromRemote(uuid)
                    }
                }
                _isAuthResolved.value = true
            }
        }
        viewModelScope.launch {
            preferencesRepository.currentUserEmail.collect { email ->
                _currentUserEmail.value = email
            }
        }
    }

    private suspend fun fetchUserFromRemote(uuid: String): MuralUserEntity? {
        val supabase = this@MuralViewModel.supabase ?: return null
        return try {
            val remoteUser = supabase.from("mural_users")
                .select { filter { eq("id", uuid) } }
                .decodeSingleOrNull<MuralUserDto>()

            if (remoteUser != null) {
                val entity = MuralUserEntity(
                    remoteId = remoteUser.id,
                    username = remoteUser.username,
                    email = remoteUser.email,
                    createdAt = System.currentTimeMillis()
                )
                muralDao.insertUser(entity)
                
                // Garante que o username está salvo nas preferências se este for o usuário atual
                if (uuid == _currentUserUuid.value) {
                    preferencesRepository.saveUsername(remoteUser.username)
                }
                
                entity
            } else null
        } catch (e: Exception) {
            null
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
        withContext(Dispatchers.IO) {
            val supabase = this@MuralViewModel.supabase ?: return@withContext
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
                                    email = userDto.email,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            android.util.Log.d("MuralViewModel", "Usuario criado localmente: ${userDto.username}")
                        } else if (existingUser.username != userDto.username || existingUser.email != userDto.email) {
                            // Atualiza se houver mudanca
                            muralDao.insertUser(
                                existingUser.copy(
                                    username = userDto.username,
                                    email = userDto.email
                                )
                            )
                            // Se for o usuario atual, atualiza as preferencias para manter consistencia
                            if (userDto.id == _currentUserUuid.value) {
                                preferencesRepository.saveUsername(userDto.username)
                                userDto.email?.let { preferencesRepository.saveUserEmail(it) }
                            }
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
                                    plantId = null,
                                    userId = postDto.user_id,
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
                        } else if (existingPost.userId != postDto.user_id) {
                            // Atualiza userId se estiver vazio
                            muralDao.insertPost(existingPost.copy(userId = postDto.user_id))
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
    }

    fun loadPost(postId: String) {
        currentPostJob?.cancel()
        currentPostJob = viewModelScope.launch {
            muralDao.observeMuralPostsWithPlants().collect { posts ->
                val post = posts.find { it.remoteId == postId }
                
                val currentPost = _postUiState.value.post
                _postUiState.value = _postUiState.value.copy(
                    post = post,
                    isLoading = false
                )

                if (post != null && post.remoteId != null && post.remoteId != currentPost?.remoteId) {
                    loadComments(post.id)
                    loadLikes(post.remoteId)
                    subscribeToCommentsRealtime(post.remoteId)
                    subscribeToLikesRealtime(post.remoteId)
                }
            }
        }
    }

    private fun loadComments(localPostId: Long) {
        commentsCollectionJob?.cancel()
        commentsCollectionJob = viewModelScope.launch {
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

            val currentUserUuid = _currentUserUuid.value
            muralDao.insertPost(
                MuralPostEntity(
                    plantId = plantId,
                    userId = currentUserUuid,
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

            val existingUuid = _currentUserUuid.value
            if (!existingUuid.isNullOrEmpty()) {
                // Tenta associar o username ao UUID existente no remote
                val success = updateUsernameForExistingUser(existingUuid, normalizedUsername)
                if (success) {
                    _currentUsername.value = normalizedUsername
                    onComplete(existingUuid)
                    return@launch
                }
            }

            val userUuid = createUserAndPersistSession(normalizedUsername, _currentUserEmail.value)
            onComplete(userUuid)
        }
    }

    fun updateUsername(username: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val normalizedUsername = sanitizeUsername(username)
            if (!isUsernameAvailable(normalizedUsername)) {
                _events.emit(MuralEvent.UsernameTaken(normalizedUsername))
                onComplete(false)
                return@launch
            }

            val existingUuid = _currentUserUuid.value
            if (!existingUuid.isNullOrEmpty()) {
                val success = updateUsernameForExistingUser(existingUuid, normalizedUsername)
                if (success) {
                    _currentUsername.value = normalizedUsername
                }
                onComplete(success)
            } else {
                onComplete(false)
            }
        }
    }

    private suspend fun updateUsernameForExistingUser(uuid: String, username: String): Boolean {
        val supabase = supabase ?: return false
        return try {
            // Tenta inserir ou atualizar (upsert) na tabela mural_users
            supabase.from("mural_users").upsert(
                MuralUserDto(
                    id = uuid,
                    username = username,
                    email = _currentUserEmail.value
                )
            )
            
            // Atualiza localmente
            val localUser = muralDao.getUserByRemoteId(uuid)
            if (localUser != null) {
                muralDao.insertUser(localUser.copy(username = username))
            } else {
                muralDao.insertUser(
                    MuralUserEntity(
                        remoteId = uuid,
                        username = username,
                        email = _currentUserEmail.value,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            
            // Salva nas preferências
            preferencesRepository.saveUsername(username)
            true
        } catch (e: Exception) {
            android.util.Log.e("MuralViewModel", "Erro ao atualizar username para UUID existente: ${e.message}")
            false
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
                    val email = googleIdTokenCredential.id
                    
                    preferencesRepository.saveUserEmail(email)
                    _currentUserEmail.value = email

                    // Tenta encontrar usuario pelo email no Supabase
                    var userUuid: String? = null
                    var username: String? = null

                    val supabase = this@MuralViewModel.supabase
                    try {
                        val remoteUser = supabase.from("mural_users")
                            .select { filter { eq("email", email) } }
                            .decodeSingleOrNull<MuralUserDto>()

                        if (remoteUser != null) {
                            userUuid = remoteUser.id
                            username = remoteUser.username
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MuralViewModel", "Erro ao buscar usuario por email: ${e.message}")
                    }

                    if (userUuid != null && username != null) {
                        // Usuario ja existe, salva sessao
                        preferencesRepository.saveUserUuid(userUuid)
                        preferencesRepository.saveUsername(username)
                        _currentUserUuid.value = userUuid
                        _currentUsername.value = username
                        
                        // Salva localmente se nao existir
                        if (muralDao.getUserByRemoteId(userUuid) == null) {
                            muralDao.insertUser(
                                MuralUserEntity(
                                    remoteId = userUuid,
                                    username = username,
                                    email = email,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    } else {
                        // Usuario novo, resolve username baseado no displayName
                        val baseUsername = googleIdTokenCredential.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?: email.substringBefore("@")
                        val resolvedUsername = findExistingOrAvailableUsername(baseUsername)
                        userUuid = createUserAndPersistSession(resolvedUsername, email)
                    }

                    android.util.Log.d(
                        "MuralViewModel",
                        "Google login realizado para $email, UUID: $userUuid"
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
            _currentUserEmail.value = null
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

    private suspend fun createUserAndPersistSession(username: String, email: String? = null): String {
        val existingLocalUser = muralDao.getUserByUsername(username)
        val localUserId = existingLocalUser?.id ?: muralDao.insertUser(
            MuralUserEntity(
                username = username,
                email = email,
                createdAt = System.currentTimeMillis()
            )
        )

        val user = existingLocalUser ?: muralDao.getUserByUsername(username)
        var userUuid: String = user?.remoteId ?: ""

        val supabase = this@MuralViewModel.supabase
        if (supabase != null && userUuid.isEmpty()) {
            try {
                // Tenta encontrar por username primeiro no remote
                val existingRemoteUsers = supabase.from("mural_users")
                    .select { filter { eq("username", username) } }
                    .decodeList<MuralUserDto>()

                if (existingRemoteUsers.isNotEmpty() && existingRemoteUsers.first().id != null) {
                    userUuid = existingRemoteUsers.first().id!!
                } else {
                    supabase.from("mural_users").insert(
                        MuralUserDto(
                            username = username,
                            email = email
                        )
                    )
                    val newUsers = supabase.from("mural_users")
                        .select { filter { eq("username", username) } }
                        .decodeList<MuralUserDto>()
                    if (newUsers.isNotEmpty() && newUsers.first().id != null) {
                        userUuid = newUsers.first().id!!
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MuralViewModel", "Erro ao sincronizar usuario: ${e.message}")
            }
        }

        if (userUuid.isEmpty()) {
            userUuid = username
        }
        
        // Garante que o banco local tem o remoteId (UUID ou fallback username)
        muralDao.updateUserRemoteId(localUserId, userUuid)

        preferencesRepository.saveUserUuid(userUuid)
        preferencesRepository.saveUsername(username)
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

    fun deleteComment(comment: CommentWithUser) {
        viewModelScope.launch {
            if (!comment.remoteId.isNullOrBlank()) {
                val supabase = this@MuralViewModel.supabase
                if (supabase != null) {
                    try {
                        supabase.from("mural_comments")
                            .delete { filter { eq("id", comment.remoteId) } }
                    } catch (e: Exception) {
                        android.util.Log.e("MuralViewModel", "Erro ao deletar comentario remoto: ${e.message}")
                    }
                }
            }
            
            // Sempre deleta localmente pelo ID numérico
            muralDao.deleteComment(comment.id)
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

    fun deletePost(post: MuralPostWithPlant) {
        viewModelScope.launch {
            val userUuid = _currentUserUuid.value
            if (userUuid != null && post.remoteId != null && post.userId == userUuid) {
                try {
                    supabase?.from("mural_posts")?.delete {
                        filter { eq("id", post.remoteId) }
                        filter { eq("user_id", userUuid) }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MuralViewModel", "Erro ao deletar post remoto: ${e.message}")
                }
            }
            
            muralDao.deletePost(post.id)
            if (post.plantId > 0) {
                muralDao.updatePlantSharedStatus(post.plantId, false)
            }
        }
    }

    private fun subscribeToCommentsRealtime(postId: String) {
        commentsSubscriptionJob?.cancel()
        commentsSubscriptionJob = viewModelScope.launch {
            val supabase = supabase ?: return@launch
            val channel = supabase.realtime.channel("comments_$postId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "mural_comments"
            }
            channel.subscribe()
            try {
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
            } finally {
                channel.unsubscribe()
            }
        }
    }

    private fun subscribeToLikesRealtime(postId: String) {
        likesSubscriptionJob?.cancel()
        likesSubscriptionJob = viewModelScope.launch {
            val supabase = supabase ?: return@launch
            val channel = supabase.realtime.channel("likes_$postId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "mural_likes"
            }
            channel.subscribe()
            try {
                changeFlow.collect { action ->
                    loadLikes(postId)
                    
                    if (action is PostgresAction.Insert) {
                        val dto = action.decodeRecord<MuralLikeDto>()
                        val currentUserId = _currentUserUuid.value
                        val post = muralDao.getPostByRemoteId(postId)
                        
                        if (currentUserId != null && dto.user_id != currentUserId && post?.userId == currentUserId) {
                            val user = muralDao.getUserByRemoteId(dto.user_id) ?: fetchUserFromRemote(dto.user_id)
                            val username = user?.username ?: "Alguém"
                            
                            notificationDao.insertNotification(
                                com.daime.grow.data.local.entity.NotificationEntity(
                                    type = com.daime.grow.data.local.entity.NotificationType.NEW_LIKE,
                                    username = username,
                                    message = "Curtiu sua planta",
                                    time = System.currentTimeMillis()
                                )
                            )
                            
                            com.daime.grow.data.reminder.NotificationHelper.showMuralLikeNotification(
                                SupabaseClient.context ?: return@collect,
                                postId,
                                username
                            )
                        }
                    }
                }
            } finally {
                channel.unsubscribe()
            }
        }
    }

    private suspend fun syncRemoteCommentToLocal(dto: MuralCommentDto) {
        if (dto.id == null) return
        try {
            val existingComment = muralDao.getCommentByRemoteId(dto.id)
            if (existingComment != null) return

            val user = muralDao.getUserByRemoteId(dto.user_id) ?: fetchUserFromRemote(dto.user_id)
            val post = muralDao.getPostByRemoteId(dto.post_id) ?: return // Se não temos o post localmente, não sync comentário

            muralDao.insertComment(
                MuralCommentEntity(
                    remoteId = dto.id,
                    localPostId = post.id,
                    localUserId = user?.id ?: 0,
                    content = dto.content,
                    createdAt = System.currentTimeMillis(),
                    parentId = dto.parent_id
                )
            )

            // Lógica de Notificação em Tempo Real
            val currentUserId = _currentUserUuid.value
            if (currentUserId != null && dto.user_id != currentUserId) {
                // Se o comentário é no meu post
                if (post.userId == currentUserId) {
                    val username = user?.username ?: "Alguém"
                    
                    notificationDao.insertNotification(
                        com.daime.grow.data.local.entity.NotificationEntity(
                            type = com.daime.grow.data.local.entity.NotificationType.NEW_COMMENT,
                            username = username,
                            message = dto.content.take(100),
                            time = System.currentTimeMillis()
                        )
                    )
                    
                    com.daime.grow.data.reminder.NotificationHelper.showMuralCommentNotification(
                        SupabaseClient.context ?: return,
                        dto.post_id,
                        username
                    )
                } 
                // Ou se é uma resposta a um comentário meu (precisaríamos checar parent_id)
                else if (dto.parent_id != null) {
                    val parentComment = muralDao.getCommentByRemoteId(dto.parent_id)
                    val parentUser = parentComment?.let { muralDao.getUserById(it.localUserId) }
                    
                    if (parentUser?.remoteId == currentUserId) {
                        val username = user?.username ?: "Alguém"
                        
                        notificationDao.insertNotification(
                            com.daime.grow.data.local.entity.NotificationEntity(
                                type = com.daime.grow.data.local.entity.NotificationType.NEW_REPLY,
                                username = username,
                                message = dto.content.take(100),
                                time = System.currentTimeMillis()
                            )
                        )
                        
                        com.daime.grow.data.reminder.NotificationHelper.showMuralReplyNotification(
                            SupabaseClient.context ?: return,
                            dto.post_id,
                            username
                        )
                    }
                }
            }
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
