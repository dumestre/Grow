package com.daime.grow.ui.screen.mural

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.daime.grow.R
import com.daime.grow.data.local.dao.CommentWithUser
import com.daime.grow.data.local.dao.MuralPostWithPlant
import com.daime.grow.ui.viewmodel.MuralEvent
import com.daime.grow.ui.viewmodel.MuralViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuralScreen(
    innerPadding: PaddingValues,
    viewModel: MuralViewModel,
    onPostClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserUuid by viewModel.currentUserUuid.collectAsStateWithLifecycle()
    val currentUsername by viewModel.currentUsername.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var expandedPostId by remember { mutableStateOf<Long?>(null) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var pendingComment by remember { mutableStateOf("") }
    var pendingReplyToId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Mural da Comunidade",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            } else if (state.posts.isEmpty()) {
                EmptyMuralState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isTablet) 2 else 1),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        top = 8.dp, 
                        bottom = 8.dp + innerPadding.calculateBottomPadding()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.posts, key = { it.id }) { post ->
                        val isExpanded = expandedPostId == post.id
                        
                        MuralPostItem(
                            post = post,
                            isExpanded = isExpanded,
                            viewModel = viewModel,
                            currentUsername = currentUsername,
                            onExpandToggle = {
                                expandedPostId = if (isExpanded) null else post.id
                                if (!isExpanded && post.remoteId != null) {
                                    viewModel.loadPost(post.remoteId)
                                }
                            },
                            onRequestUsername = { text, replyId ->
                                pendingComment = text
                                pendingReplyToId = replyId
                                showUsernameDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showUsernameDialog) {
        var usernameError by remember { mutableStateOf<String?>(null) }
        var isLoggingIn by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is MuralEvent.UsernameTaken -> {
                        usernameError = "Nome de usuário já está em uso"
                        isLoggingIn = false
                    }
                    is MuralEvent.GoogleLoginSuccess -> {
                        if (pendingComment.isNotBlank() && expandedPostId != null) {
                            val post = state.posts.find { it.id == expandedPostId }
                            if (post?.remoteId != null) {
                                viewModel.addComment(post.remoteId, pendingComment, pendingReplyToId)
                            }
                            pendingComment = ""
                            pendingReplyToId = null
                        }
                        showUsernameDialog = false
                        isLoggingIn = false
                    }
                    is MuralEvent.GoogleLoginError -> {
                        usernameError = event.message
                        isLoggingIn = false
                    }
                    MuralEvent.SignedOut -> {
                        pendingComment = ""
                        pendingReplyToId = null
                        usernameError = null
                        showUsernameDialog = false
                        isLoggingIn = false
                    }
                }
            }
        }
        
        UsernameDialog(
            reason = "Escolha um nome rápido para interagir:",
            initialError = usernameError,
            onDismiss = { showUsernameDialog = false },
            onConfirm = { username ->
                usernameError = null
                viewModel.createOrGetUser(
                    username = username,
                    onComplete = { userUuid ->
                        if (pendingComment.isNotBlank() && expandedPostId != null) {
                            val post = state.posts.find { it.id == expandedPostId }
                            if (post?.remoteId != null) {
                                viewModel.addComment(post.remoteId, pendingComment, pendingReplyToId)
                            }
                            pendingComment = ""
                            pendingReplyToId = null
                        }
                        showUsernameDialog = false
                    },
                    onUsernameTaken = {
                        usernameError = "Nome de usuário já está em uso"
                    }
                )
            },
            onGoogleLogin = if (!isLoggingIn) {
                {
                    isLoggingIn = true
                    usernameError = null
                    viewModel.signInWithGoogle(context) { _ ->
                        if (pendingComment.isNotBlank() && expandedPostId != null) {
                            val post = state.posts.find { it.id == expandedPostId }
                            if (post?.remoteId != null) {
                                viewModel.addComment(post.remoteId, pendingComment, pendingReplyToId)
                            }
                            pendingComment = ""
                            pendingReplyToId = null
                        }
                        showUsernameDialog = false
                    }
                }
            } else null
        )
    }
}

@Composable
fun MuralPostItem(
    post: MuralPostWithPlant,
    isExpanded: Boolean,
    viewModel: MuralViewModel,
    currentUsername: String?,
    onExpandToggle: () -> Unit,
    onRequestUsername: (String, String?) -> Unit
) {
    val commentsFlow = remember(post.id) { viewModel.getCommentsFlow(post.id) }
    val allComments by commentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var replyToComment by remember { mutableStateOf<CommentWithUser?>(null) }
    var editingComment by remember { mutableStateOf<CommentWithUser?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = post.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF01264))
                Text(text = "Compartilhado em ${formatMuralDate(post.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }

            if (post.photoUri != null) {
                AsyncImage(model = post.photoUri, contentDescription = null, modifier = Modifier.fillMaxWidth().height(240.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "DADOS TÉCNICOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    BadgeText(post.stage)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                MetaLine("Ciclo", formatCultivoTime(post.days))
                MetaLine("Strain", post.strain)
                MetaLine("Substrato", post.medium)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isExpanded && allComments.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allComments.take(2).forEach { comment ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "@${comment.username}: ", 
                                style = MaterialTheme.typography.labelMedium, 
                                fontWeight = FontWeight.Bold, 
                                color = getUserColor(comment.username)
                            )
                            Text(text = comment.content, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(scrollState)
                            .padding(top = 12.dp)
                    ) {
                        val tree = remember(allComments) { buildCommentTree(allComments) }

                        tree.forEach { (comment, depth) ->
                            MuralCommentItem(
                                comment = comment,
                                currentUsername = currentUsername,
                                onReplyClick = { 
                                    replyToComment = it
                                    editingComment = null 
                                },
                                onDeleteClick = { viewModel.deleteComment(it.remoteId ?: "") },
                                onEditClick = { 
                                    editingComment = it
                                    replyToComment = null 
                                },
                                depth = depth
                            )
                        }
                        
                        if (allComments.isEmpty()) {
                            Text(text = "Nenhum comentário ainda.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(20.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }

                    if (replyToComment != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Respondendo a @${replyToComment?.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { replyToComment = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    CommentInput(
                        currentUserUuid = currentUsername,
                        editingComment = editingComment,
                        onCancelEdit = { editingComment = null },
                        onSendComment = { content -> 
                            editingComment?.let { 
                                viewModel.editComment(it.remoteId ?: "", content)
                                editingComment = null
                            } ?: run {
                                currentUsername?.let { _ ->
                                    post.remoteId?.let { remoteId ->
                                        viewModel.addComment(remoteId, content, replyToComment?.remoteId)
                                        replyToComment = null
                                    }
                                }
                            }
                        },
                        onRequestUsername = { content -> onRequestUsername(content, replyToComment?.remoteId) }
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = onExpandToggle,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp 
                                         else if (allComments.size > 2) Icons.Default.MoreHoriz 
                                         else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isExpanded && allComments.size > 2) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Ver mais ${allComments.size - 2} comentários", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        } else if (isExpanded) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Recolher conversa", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1B5E20)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF3A3A3A)
        )
    }
}

private fun formatCultivoTime(days: Int): String {
    return when {
        days < 7 -> "$days dias"
        days < 14 -> "1 semana"
        days % 7 == 0 -> "${days / 7} semanas"
        else -> "${days / 7}s ${days % 7}d"
    }
}

@Composable
private fun EmptyMuralState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(painter = painterResource(id = R.drawable.planta), contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "O Mural está vazio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Seja o primeiro a compartilhar seu cultivo!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}
