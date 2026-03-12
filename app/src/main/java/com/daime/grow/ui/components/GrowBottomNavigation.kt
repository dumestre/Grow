package com.daime.grow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Delete

enum class BottomNavItem(
    val route: String,
    val title: String,
    val iconRes: Int?,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasBadge: Boolean = false
) {
    Home(
        route = "home",
        title = "Plantas",
        iconRes = com.daime.grow.R.drawable.planta,
        selectedIcon = Icons.Outlined.Settings,
        unselectedIcon = Icons.Outlined.Settings
    ),
    Mural(
        route = "mural",
        title = "Mural",
        iconRes = null,
        selectedIcon = Icons.Outlined.Public,
        unselectedIcon = Icons.Outlined.Public
    ),
    Notifications(
        route = "notifications",
        title = "Avisos",
        iconRes = null,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        hasBadge = true
    ),
    Settings(
        route = "settings",
        title = "Ajustes",
        iconRes = null,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

@Composable
fun GrowBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDeleting: Boolean = false,
    onFabBounds: (androidx.compose.ui.geometry.Rect) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp, // Volta para a cor original (sem o tom de elevação)
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Aplica o padding APENAS embaixo para os gestos/botões do Android
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp), // Altura slim real para ser bem fina
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = BottomNavItem.entries
                
                // Primeiro par de ícones
                items.take(2).forEach { item ->
                    NavIconItem(item, currentRoute, onNavigate, Modifier.weight(1f))
                }

                // FAB Centralizado - Tamanho reduzido para a barra slim
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = { if (!isDeleting) onAddClick() },
                        containerColor = if (isDeleting) Color(0xFFC62828) else MaterialTheme.colorScheme.tertiary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp) 
                            .onGloballyPositioned { onFabBounds(it.boundsInRoot()) },
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isDeleting) Icons.Rounded.Delete else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isDeleting) Color(0xFF333333) else Color(0xFF1B5E20)
                        )
                    }
                }

                // Segundo par de ícones
                items.drop(2).forEach { item ->
                    NavIconItem(item, currentRoute, onNavigate, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NavIconItem(
    item: BottomNavItem,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = currentRoute == item.route
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = { onNavigate(item.route) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                badge = {
                    if (item.hasBadge) {
                        Badge(
                            modifier = Modifier.size(5.dp),
                            containerColor = Color.Red
                        )
                    }
                }
            ) {
                if (item.iconRes != null) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.title,
                        modifier = Modifier.size(18.dp),
                        tint = color
                    )
                } else {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        modifier = Modifier.size(18.dp),
                        tint = color
                    )
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
