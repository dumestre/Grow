package com.daime.grow.ui.screen.mural

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.data.local.dao.CommentWithUser
import com.daime.grow.data.local.dao.MuralPostWithPlant
import com.daime.grow.ui.components.RoundedBackButton
import com.daime.grow.ui.viewmodel.MuralViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuralPostScreen(
    postId: Long,
    innerPadding: PaddingValues,
    viewModel: MuralViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.postUiState.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    var showUsernameDialog by remember { mutableStateOf(false) }
    var editingComment by remember { mutableStateOf<CommentWithUser?>(null) }
    var pendingComment by remember { mutableStateOf("") }
    var pendingReplyToId by remember { mutableStateOf<Long?>(null) }
    var replyToComment by remember { mutableStateOf<CommentWithUser?>(null) }

    // Estado local para o like (para feedback instantâneo)
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = state.post?.name ?: "Planta",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    RoundedBackButton(onClick = onBack)
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { 
                                isLiked = !isLiked
                                if (isLiked) likeCount++ else if (likeCount > 0) likeCount--
                                // viewModel.toggleLike(postId) 
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        if (likeCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$likeCount",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
            if (state.isLoading || state.post == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                state.post?.let { post ->
                    Column(modifier = Modifier.weight(1f)) {
                        PostHeader(post = post)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Conversa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        val tree = remember(state.comments) { buildCommentTree(state.comments) }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 16.dp + innerPadding.calculateBottomPadding())
                        ) {
                            if (tree.isEmpty()) {
                                item {
                                    Text(
                                        text = "Seja o primeiro a comentar!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(tree, key = { it.first.id }) { (comment, depth) ->
                                    MuralCommentItem(
                                        comment = comment,
                                        currentUserId = currentUserId,
                                        onReplyClick = { 
                                            replyToComment = it
                                            editingComment = null
                                        },
                                        onDeleteClick = { viewModel.deleteComment(it.id) },
                                        onEditClick = { 
                                            editingComment = it
                                            replyToComment = null
                                        },
                                        depth = depth
                                    )
                                }
                            }
                        }

                        // Reply Feedback Area
                        AnimatedVisibility(
                            visible = replyToComment != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.weight(1f)) {
                                    Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Respondendo a @${replyToComment?.username}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { replyToComment = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Surface(
                            tonalElevation = 2.dp,
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            CommentInput(
                                currentUserId = currentUserId,
                                editingComment = editingComment,
                                onCancelEdit = { editingComment = null },
                                onSendComment = { content ->
                                    editingComment?.let {
                                        viewModel.editComment(it.id, content)
                                        editingComment = null
                                    } ?: run {
                                        currentUserId?.let { id ->
                                            viewModel.addComment(postId, id, content, replyToComment?.id)
                                            replyToComment = null
                                        }
                                    }
                                },
                                onRequestUsername = { content ->
                                    pendingComment = content
                                    pendingReplyToId = replyToComment?.id
                                    showUsernameDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUsernameDialog) {
        UsernameDialog(
            reason = "Escolha um nome para comentar:",
            onDismiss = { showUsernameDialog = false },
            onConfirm = { username ->
                viewModel.createOrGetUser(username) { userId ->
                    if (pendingComment.isNotBlank()) {
                        viewModel.addComment(postId, userId, pendingComment, pendingReplyToId)
                        pendingComment = ""
                        pendingReplyToId = null
                    }
                    showUsernameDialog = false
                    replyToComment = null
                }
            }
        )
    }
}

@Composable
private fun PostHeader(post: MuralPostWithPlant) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.planta),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = post.strain,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Estágio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = post.stage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Dias",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${post.days}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Meio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = post.medium,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
