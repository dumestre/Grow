package com.daime.grow.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Notificações",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // Mock data for notifications
            val notifications = listOf(
                NotificationItemData(1, "mariagreen", "curtiu sua planta 'Skunk #1'", "há 2 min", Icons.Default.Favorite, Color.Red),
                NotificationItemData(2, "bob_grower", "respondeu seu comentário: 'Ficou top!'", "há 15 min", Icons.AutoMirrored.Filled.Reply, Color(0xFF1976D2)),
                NotificationItemData(3, "alice_weed", "compartilhou uma nova planta: 'Northern Lights'", "há 1h", Icons.Default.Grass, Color(0xFF388E3C)),
                NotificationItemData(4, "cannabis_king", "comentou no seu post: 'Quanto tempo de vega?'", "há 3h", Icons.AutoMirrored.Filled.Comment, Color(0xFFF57C00)),
                NotificationItemData(5, "grower_master", "curtiu seu comentário no mural", "há 5h", Icons.Default.Favorite, Color.Red),
                NotificationItemData(6, "nature_lover", "compartilhou 'Lemon Haze'", "há 1d", Icons.Default.Grass, Color(0xFF388E3C))
            )
            
            LazyColumn {
                items(notifications) { item ->
                    NotificationRow(item)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

data class NotificationItemData(
    val id: Long,
    val username: String,
    val action: String,
    val time: String,
    val icon: ImageVector,
    val iconColor: Color
)

@Composable
fun NotificationRow(item: NotificationItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = item.iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = "@${item.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.action,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = item.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
