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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Yard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Delete
import com.daime.grow.ui.navigation.NavRoute

enum class BottomNavItem(
    val route: String,
    val title: String,
    val iconRes: Int?,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val alternativeIcon: ImageVector = Icons.Outlined.Yard, // Ícone neutro (Horta/Jardim)
    val hasBadge: Boolean = false
) {
    Home(
        route = NavRoute.Home.route,
        title = "Plantas",
        iconRes = com.daime.grow.R.drawable.planta,
        selectedIcon = Icons.Outlined.Spa,
        unselectedIcon = Icons.Outlined.Spa
    ),
    PosColheta(
        route = NavRoute.PosColheta.route,
        title = "Pós",
        iconRes = null,
        selectedIcon = Icons.Outlined.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2
    ),
    Mural(
        route = NavRoute.Mural.route,
        title = "Mural",
        iconRes = null,
        selectedIcon = Icons.Outlined.Public,
        unselectedIcon = Icons.Outlined.Public
    ),
    Store(
        route = NavRoute.Store.route,
        title = "Loja",
        iconRes = null,
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart
    ),
    Notifications(
        route = NavRoute.Notifications.route,
        title = "Avisos",
        iconRes = null,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        hasBadge = true
    ),
    Settings(
        route = NavRoute.Settings.route,
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
    useAlternativeIcons: Boolean = true, // Flag de mascaramento
    onFabBounds: (androidx.compose.ui.geometry.Rect) -> Unit = {}
) {
    Surface(
        color = Color.White,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = BottomNavItem.entries
                val firstGroup = items.take(3)
                val secondGroup = items.drop(3)

                firstGroup.forEach { item ->
                    NavIconItem(item, currentRoute, onNavigate, useAlternativeIcons, Modifier.weight(1f))
                }

                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = { if (!isDeleting) onAddClick() },
                        containerColor = if (isDeleting) Color(0xFFC62828) else MaterialTheme.colorScheme.tertiary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(42.dp) 
                            .onGloballyPositioned { onFabBounds(it.boundsInRoot()) },
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isDeleting) Icons.Rounded.Delete else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isDeleting) Color.White else Color(0xFF1B5E20)
                        )
                    }
                }

                secondGroup.forEach { item ->
                    NavIconItem(item, currentRoute, onNavigate, useAlternativeIcons, Modifier.weight(1f))
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
    useAlternativeIcons: Boolean,
    modifier: Modifier = Modifier
) {
    val selected = currentRoute == item.route
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 26.dp),
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
                            modifier = Modifier.size(6.dp),
                            containerColor = Color.Red
                        )
                    }
                }
            ) {
                if (!useAlternativeIcons && item.iconRes != null) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp),
                        tint = color
                    )
                } else {
                    Icon(
                        imageVector = if (useAlternativeIcons) item.alternativeIcon else if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp),
                        tint = color
                    )
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
