package com.daime.grow.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daime.grow.R
import com.daime.grow.ui.navigation.NavRoute
import com.daime.grow.ui.theme.GrowTheme

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
        titleRes = R.string.nav_strains,
        iconRes = null,
        selectedIcon = Icons.Rounded.Forest,
        unselectedIcon = Icons.Outlined.Forest
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
    ),
    Tips(
        route = NavRoute.GrowTips.route,
        titleRes = com.daime.grow.R.string.nav_dicas,
        iconRes = null,
        selectedIcon = Icons.Rounded.Lightbulb,
        unselectedIcon = Icons.Rounded.Lightbulb
    ),
    PPFD(
        route = NavRoute.PPFD.route,
        titleRes = com.daime.grow.R.string.nav_ppfd,
        iconRes = null,
        selectedIcon = Icons.Rounded.WbSunny,
        unselectedIcon = Icons.Rounded.WbSunny
    )
}

private val tipsGridItems = listOf(
    Triple("Estágios", Icons.Rounded.Spa, Color(0xFF4CAF50)),
    Triple("Luz", Icons.Rounded.WbSunny, Color(0xFFFFB300)),
    Triple("Nutrição", Icons.Rounded.Science, Color(0xFF9C27B0)),
    Triple("Rega", Icons.Rounded.WaterDrop, Color(0xFF2196F3)),
    Triple("Clima", Icons.Rounded.Thermostat, Color(0xFFFF5722)),
    Triple("Treinos", Icons.Rounded.Construction, Color(0xFFE91E63)),
    Triple("Colheita", Icons.Rounded.Agriculture, Color(0xFF795548)),
    Triple("Pragas", Icons.Rounded.BugReport, Color(0xFFD32F2F))
)

@OptIn(ExperimentalMaterial3Api::class)
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
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .animateContentSize(
                    animationSpec = tween(durationMillis = 10)
                )
        ) {
            val dragState = rememberDraggableState { delta: Float ->
                if (delta < -10f) isExpanded = true
                else if (delta > 10f) isExpanded = false
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = !isExpanded }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Barra visual para indicar arrasto
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
            }

            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 40))
                        .togetherWith(fadeOut(animationSpec = tween(durationMillis = 40)))
                        .using(SizeTransform(clip = false))
                },
                label = "BottomBarExpansion"
            ) { expanded ->
                if (expanded) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val itemsCount = BottomNavItem.entries.size
                        val rows = (itemsCount + 3) / 4

                        items(rows) { rowIndex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(4) { colIndex ->
                                    val itemIndex = rowIndex * 4 + colIndex
                                    if (itemIndex < itemsCount) {
                                        val item = BottomNavItem.entries[itemIndex]
                                        val badgeCount = if (item == BottomNavItem.Notifications) notificationBadgeCount else 0
                                        NavIconItem(
                                            item = item,
                                            currentRoute = currentRoute,
                                            onNavigate = {
                                                onNavigate(it)
                                                isExpanded = false
                                            },
                                            maskHomeIcon = maskHomeIcon,
                                            badgeCount = badgeCount,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0, 1, 2).forEach { index ->
                            val item = BottomNavItem.entries.getOrNull(index)
                            if (item != null) {
                                val badgeCount = if (item == BottomNavItem.Notifications) notificationBadgeCount else 0
                                NavIconItem(
                                    item = item,
                                    currentRoute = currentRoute,
                                    onNavigate = onNavigate,
                                    maskHomeIcon = maskHomeIcon,
                                    modifier = Modifier.weight(1f),
                                    badgeCount = badgeCount
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

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

                        listOf(3, 4, 5).forEach { index ->
                            val item = BottomNavItem.entries.getOrNull(index)
                            if (item != null) {
                                val badgeCount = if (item == BottomNavItem.Notifications) notificationBadgeCount else 0
                                NavIconItem(
                                    item = item,
                                    currentRoute = currentRoute,
                                    onNavigate = onNavigate,
                                    maskHomeIcon = maskHomeIcon,
                                    modifier = Modifier.weight(1f),
                                    badgeCount = badgeCount
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
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
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 40.dp),
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
                        modifier = Modifier.size(24.dp),
                        tint = color
                    )

                    item.iconRes != null && useAlternativeForItem -> Icon(
                        imageVector = item.alternativeIcon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = color
                    )

                    else -> Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = color
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp
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

@Preview(showBackground = true)
@Composable
fun GrowBottomNavigationBarPreview() {
    GrowTheme {
        GrowBottomNavigationBar(
            currentRoute = NavRoute.Home.route,
            onNavigate = {},
            onAddClick = {},
            notificationBadgeCount = 5
        )
    }
}