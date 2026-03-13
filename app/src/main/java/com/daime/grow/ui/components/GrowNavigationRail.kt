package com.daime.grow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Delete

@Composable
fun GrowNavigationRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDeleting: Boolean = false
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
        containerColor = Color.White,
        header = {
            FloatingActionButton(
                onClick = { if (!isDeleting) onAddClick() },
                containerColor = if (isDeleting) Color(0xFFC62828) else MaterialTheme.colorScheme.tertiary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = if (isDeleting) Icons.Rounded.Delete else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isDeleting) Color.White else Color(0xFF1B5E20)
                )
            }
        }
    ) {
        Spacer(Modifier.height(16.dp))
        
        BottomNavItem.entries.forEach { item ->
            val selected = currentRoute == item.route
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.hasBadge) {
                                Badge(
                                    modifier = Modifier.size(6.dp),
                                    containerColor = Color.Red
                                )
                            }
                        }
                    ) {
                        if (item.iconRes != null) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
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
                label = { Text(item.title) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent // Removido fundo de seleção para visual mais limpo no branco
                )
            )
        }
    }
}
