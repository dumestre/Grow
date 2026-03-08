package com.daime.grow.ui.screen.mural

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.daime.grow.data.local.dao.CommentWithUser
import com.daime.grow.data.local.dao.MuralPostWithPlant
import com.daime.grow.ui.viewmodel.MuralViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuralScreen(
    innerPadding: PaddingValues,
    viewModel: MuralViewModel,
    onPostClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    var expandedPostId by remember { mutableLongStateOf(-1L) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var pendingComment by remember { mutableStateOf("") }
    var pendingReplyToId by remember { mutableStateOf<Long?>(null) }

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
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
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
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.posts, key = { it.id }) { post ->
                        val isExpanded = expandedPostId == post.id
                        
                        MuralPostItem(
                            post = post,
                            isExpanded = isExpanded,
                            viewModel = viewModel,
                            currentUserId = currentUserId,
                            onExpandToggle = {
                                expandedPostId = if (isExpanded) -1L else post.id
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
        UsernameDialog(
            reason = "Escolha um nome rápido para interagir:",
            onDismiss = { showUsernameDialog = false },
            onConfirm = { username ->
                viewModel.createOrGetUser(username) { userId ->
                    if (pendingComment.isNotBlank() && expandedPostId != -1L) {
                        viewModel.addComment(expandedPostId, userId, pendingComment, pendingReplyToId)
                        pendingComment = ""
                        pendingReplyToId = null
                    }
                    showUsernameDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuralPostItem(
    post: MuralPostWithPlant,
    isExpanded: Boolean,
    viewModel: MuralViewModel,
    currentUserId: Long?,
    onExpandToggle: () -> Unit,
    onRequestUsername: (String, Long?) -> Unit
) {
    val commentsFlow = remember(post.id) { viewModel.getCommentsFlow(post.id) }
    val allComments by commentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var replyToComment by remember { mutableStateOf<CommentWithUser?>(null) }

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
            // Header
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = post.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF01264))
                Text(text = "Compartilhado em ${formatDate(post.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }

            if (post.photoUri != null) {
                AsyncImage(model = post.photoUri, contentDescription = null, modifier = Modifier.fillMaxWidth().height(240.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Detalhes do Cultivo (Text Based - Professional Look)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "DADOS TÉCNICOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    BadgeText(post.stage)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                MetaLine("Dias", "${post.days} dias de cultivo")
                MetaLine("Strain", post.strain)
                MetaLine("Substrato", post.medium)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preview Section
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

            // Expanded Conversations Area
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
                        allComments.forEach { comment ->
                            MuralCommentItem(
                                comment = comment,
                                onReplyClick = { replyToComment = it },
                                isReply = comment.parentId != null
                            )
                        }
                        if (allComments.isEmpty()) {
                            Text(text = "Nenhum comentário ainda.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(20.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }

                    // Reply Feedback
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
                        currentUserId = currentUserId,
                        onSendComment = { content -> 
                            viewModel.addComment(post.id, currentUserId!!, content, replyToComment?.id)
                            replyToComment = null
                        },
                        onRequestUsername = { content -> onRequestUsername(content, replyToComment?.id) }
                    )
                }
            }

            // Expand/Collapse Button (ALWAYS AT THE BOTTOM)
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

@Composable
fun MuralCommentItem(
    comment: CommentWithUser,
    onReplyClick: (CommentWithUser) -> Unit,
    isReply: Boolean = false
) {
    var isLiked by remember { mutableStateOf(false) }
    val userColor = remember(comment.username) { getUserColor(comment.username) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 40.dp else 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${comment.username}", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = userColor, 
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = formatDate(comment.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            Text(text = comment.content, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.onSurface)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Responder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { onReplyClick(comment) }.padding(top = 4.dp, bottom = 4.dp, end = 12.dp)
                )
            }
        }
        
        IconButton(
            onClick = { isLiked = !isLiked },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CommentInput(
    currentUserId: Long?,
    onSendComment: (String) -> Unit,
    onRequestUsername: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Escreva algo...", style = MaterialTheme.typography.bodySmall) },
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(modifier = Modifier.width(10.dp))
        IconButton(
            onClick = {
                if (commentText.isNotBlank()) {
                    if (currentUserId == null) {
                        onRequestUsername(commentText)
                    } else {
                        onSendComment(commentText)
                        commentText = ""
                    }
                }
            },
            enabled = commentText.isNotBlank(),
            modifier = Modifier.size(40.dp).background(if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EmptyMuralState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(imageVector = Icons.Default.Grass, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "O Mural está vazio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Seja o primeiro a compartilhar seu cultivo!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

private fun getUserColor(username: String): Color {
    val colors = listOf(
        Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFD32F2F), 
        Color(0xFFF57C00), Color(0xFF7B1FA2), Color(0xFF00796B),
        Color(0xFFC2185B), Color(0xFF5D4037), Color(0xFF455A64)
    )
    val index = username.hashCode().let { if (it < 0) -it else it } % colors.size
    return colors[index]
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
