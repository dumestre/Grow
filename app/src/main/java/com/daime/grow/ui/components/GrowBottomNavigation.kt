package com.daime.grow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

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
        selectedIcon = Icons.Outlined.Settings, // Fallback
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
    NavigationBar(
        modifier = modifier.height(80.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        val items = BottomNavItem.entries
        
        // Primeiros dois itens
        items.take(2).forEach { item ->
            NavIconItem(item, currentRoute, onNavigate)
        }

        // FAB Centralizado dentro da barra (sem sair para fora)
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
                    .size(48.dp) // Um pouco menor para caber bem dentro da barra de 80dp
                    .onGloballyPositioned { onFabBounds(it.boundsInRoot()) },
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = if (isDeleting) Icons.Rounded.Delete else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isDeleting) Color.White else Color(0xFF1B5E20)
                )
            }
        }

        // Últimos dois itens
        items.drop(2).forEach { item ->
            NavIconItem(item, currentRoute, onNavigate)
        }
    }
}

@Composable
private fun RowScope.NavIconItem(
    item: BottomNavItem,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val selected = currentRoute == item.route
    NavigationBarItem(
        selected = selected,
        onClick = { onNavigate(item.route) },
        icon = {
            BadgedBox(
                badge = {
                    if (item.hasBadge) {
                        Badge(
                            modifier = Modifier.size(8.dp),
                            containerColor = Color.Red
                        )
                    }
                }
            ) {
                if (item.iconRes != null) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = Color.Transparent
        )
    )
}
