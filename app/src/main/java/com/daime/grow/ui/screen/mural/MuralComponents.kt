package com.daime.grow.ui.screen.mural

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import com.daime.grow.R
import com.daime.grow.data.local.dao.CommentWithUser
import java.text.SimpleDateFormat
import java.util.*

/**
 * Organiza a lista de comentários em uma estrutura plana mas ordenada para a árvore visual.
 * Cada item contém o comentário e seu nível de profundidade.
 */
fun buildCommentTree(allComments: List<CommentWithUser>): List<Pair<CommentWithUser, Int>> {
    val result = mutableListOf<Pair<CommentWithUser, Int>>()
    val roots = allComments.filter { it.parentId == null }
    val map = allComments.groupBy { it.parentId }

    fun walk(comment: CommentWithUser, depth: Int) {
        result.add(comment to depth)
        map[comment.remoteId]?.forEach { child ->
            walk(child, depth + 1)
        }
    }

    roots.forEach { walk(it, 0) }
    return result
}

@Composable
fun MuralCommentItem(
    comment: CommentWithUser,
    currentUsername: String?,
    onReplyClick: (CommentWithUser) -> Unit,
    onDeleteClick: (CommentWithUser) -> Unit,
    onEditClick: (CommentWithUser) -> Unit,
    depth: Int = 0
) {
    var isLiked by remember { mutableStateOf(false) }
    val userColor = remember(comment.username) { getUserColor(comment.username) }
    val isReply = depth > 0
    
    // Limita a indentação máxima para não espremer o texto em telas pequenas
    val indentation = (depth * 16).coerceAtMost(48).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(start = 16.dp + indentation, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (isReply) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Linha vertical de guia
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
                // Pequeno "tique" horizontal
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .height(2.dp)
                        .width(10.dp)
                        .align(Alignment.TopStart)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${comment.username}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = userColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatMuralDate(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Responder",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onReplyClick(comment) }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Botão de Curtir com ripple circular
                IconButton(
                    onClick = { isLiked = !isLiked },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (comment.username == currentUsername) {
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Botão Editar direto
                    IconButton(
                        onClick = { onEditClick(comment) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // Botão Excluir direto
                    IconButton(
                        onClick = { onDeleteClick(comment) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentInput(
    currentUserUuid: String?,
    editingComment: CommentWithUser? = null,
    onCancelEdit: () -> Unit = {},
    onSendComment: (String) -> Unit,
    onRequestUsername: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    // Quando entrar em modo de edição, preenche o texto
    LaunchedEffect(editingComment) {
        if (editingComment != null) {
            commentText = editingComment.content
        }
    }
    
    Column {
        AnimatedVisibility(visible = editingComment != null, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Editando seu comentário...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                IconButton(onClick = { 
                    commentText = ""
                    onCancelEdit() 
                }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Escreva um comentário...", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = false,
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        if (currentUserUuid == null && editingComment == null) {
                            onRequestUsername(commentText)
                        } else {
                            onSendComment(commentText)
                            commentText = ""
                        }
                    }
                },
                enabled = commentText.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant, 
                        CircleShape
                    )
            ) {
                Icon(
                    if (editingComment != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Send, 
                    null, 
                    tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.onPrimary 
                           else MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun UsernameDialog(
    reason: String,
    initialError: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onGoogleLogin: (() -> Unit)? = null
) {
    var username by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(initialError) }

    LaunchedEffect(initialError) {
        error = initialError
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entrar no Mural", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(text = reason, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botão Google
                if (onGoogleLogin != null) {
                    Button(
                        onClick = onGoogleLogin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.googleicon),
                            contentDescription = "Google",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continuar com Google")
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    Text(
                        text = "ou digite um nome:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it.replace(" ", "")
                        if (error != null) error = null
                    },
                    label = { Text("Nome") },
                    prefix = { Text("@") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        username.length < 3 -> error = "Mínimo 3 caracteres"
                        !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> error = "Apenas letras, números e _"
                        else -> onConfirm(username)
                    }
                }
            ) { Text("Continuar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

fun getUserColor(username: String): Color {
    val colors = listOf(
        Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFD32F2F), 
        Color(0xFFF57C00), Color(0xFF7B1FA2), Color(0xFF00796B),
        Color(0xFFC2185B), Color(0xFF5D4037), Color(0xFF455A64)
    )
    val index = username.hashCode().let { if (it < 0) -it else it } % colors.size
    return colors[index]
}

fun formatMuralDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
