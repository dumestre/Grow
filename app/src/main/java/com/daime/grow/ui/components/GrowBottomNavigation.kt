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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.daime.grow.ui.navigation.NavRoute

enum class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val iconRes: Int?,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val alternativeIcon: ImageVector = Icons.Outlined.Yard,
    val showBadge: Boolean = false
) {
    Home(
        route = NavRoute.Home.route,
        titleRes = com.daime.grow.R.string.nav_plantas,
        iconRes = null,
        selectedIcon = Icons.Outlined.Spa,
        unselectedIcon = Icons.Outlined.Spa
    ),
    PosColheta(
        route = NavRoute.PosColheta.route,
        titleRes = com.daime.grow.R.string.nav_pos,
        iconRes = null,
        selectedIcon = Icons.Outlined.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2
    ),
    Mural(
        route = NavRoute.Mural.route,
        titleRes = com.daime.grow.R.string.nav_mural,
        iconRes = null,
        selectedIcon = Icons.Outlined.Public,
        unselectedIcon = Icons.Outlined.Public
    ),
    Store(
        route = NavRoute.Store.route,
        titleRes = com.daime.grow.R.string.nav_loja,
        iconRes = null,
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart
    ),
    Notifications(
        route = NavRoute.Notifications.route,
        titleRes = com.daime.grow.R.string.nav_avisos,
        iconRes = null,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        showBadge = true
    ),
    Settings(
        route = NavRoute.Settings.route,
        titleRes = com.daime.grow.R.string.nav_ajustes,
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
    maskHomeIcon: Boolean = true,
    onFabBounds: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    notificationBadgeCount: Int = 0
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = BottomNavItem.entries
            val firstGroup = items.take(3)
            val secondGroup = items.drop(3)

            firstGroup.forEach { item ->
                NavIconItem(item, currentRoute, onNavigate, maskHomeIcon, Modifier.weight(1f).height(52.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .height(52.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(42.dp)
                        .onGloballyPositioned { onFabBounds(it.boundsInRoot()) },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF1B5E20)
                    )
                }
            }

            secondGroup.forEach { item ->
                val badgeCount = if (item == BottomNavItem.Notifications) notificationBadgeCount else 0
                NavIconItem(item, currentRoute, onNavigate, maskHomeIcon, Modifier.weight(1f).height(52.dp), badgeCount)
            }
        }
    }
}

@Composable
private fun NavIconItem(
    item: BottomNavItem,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    maskHomeIcon: Boolean,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val selected = currentRoute == item.route
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val useAlternativeForItem = maskHomeIcon && item == BottomNavItem.Home
    val title = getStringResource(item.titleRes)

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 20.dp),
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
                    if (item.showBadge && badgeCount > 0) {
                        Badge(
                            modifier = Modifier.size(10.dp),
                            containerColor = Color.Red
                        )
                    }
                }
            ) {
                when {
                    item.iconRes != null && !useAlternativeForItem -> Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(28.dp),
                        tint = color
                    )

                    item.iconRes != null && useAlternativeForItem -> Icon(
                        imageVector = item.alternativeIcon,
                        contentDescription = title,
                        modifier = Modifier.size(28.dp),
                        tint = color
                    )

                    else -> Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = title,
                        modifier = Modifier.size(28.dp),
                        tint = color
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
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

@Composable
private fun getStringResource(@StringRes resId: Int): String {
    return stringResource(resId)
}
