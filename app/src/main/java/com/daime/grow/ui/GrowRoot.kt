package com.daime.grow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daime.grow.core.AppContainer
import com.daime.grow.ui.components.GrowBottomNavigationBar
import com.daime.grow.ui.components.GrowNavigationRail
import com.daime.grow.ui.components.NotificationSheet
import com.daime.grow.ui.navigation.NavRoute
import com.daime.grow.ui.screen.add.NewPlantScreen
import com.daime.grow.ui.screen.detail.PlantDetailScreen
import com.daime.grow.ui.screen.home.HomeScreen
import com.daime.grow.ui.screen.lock.LockScreen
import com.daime.grow.ui.screen.mural.MuralPostScreen
import com.daime.grow.ui.screen.mural.MuralScreen
import com.daime.grow.ui.screen.settings.SettingsScreen
import com.daime.grow.ui.viewmodel.AddPlantViewModel
import com.daime.grow.ui.viewmodel.HomeViewModel
import com.daime.grow.ui.viewmodel.LockViewModel
import com.daime.grow.ui.viewmodel.MuralViewModel
import com.daime.grow.ui.viewmodel.PlantDetailViewModel
import com.daime.grow.ui.viewmodel.SettingsViewModel
import com.daime.grow.ui.viewmodel.ViewModelFactories

@Composable
fun GrowRoot(container: AppContainer) {
    val factories = ViewModelFactories(container)
    val homeViewModel: HomeViewModel = viewModel(factory = factories.home)
    val lockViewModel: LockViewModel = viewModel(factory = factories.lock)
    val addPlantViewModel: AddPlantViewModel = viewModel(factory = factories.addPlant)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factories.settings)
    val muralViewModel: MuralViewModel = viewModel(factory = factories.mural)

    val lockState by lockViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showNotificationSheet by remember { mutableStateOf(false) }
    
    // Estados para sincronizar o arrasto com a barra de navegação
    var isDraggingPlant by remember { mutableStateOf(false) }
    var trashBounds by remember { mutableStateOf<Rect?>(null) }

    if (!lockState.isReady) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    if (lockState.showLockScreen) {
        LockScreen(
            state = lockState,
            onPinChange = lockViewModel::onPinInputChange,
            onUnlockWithPin = lockViewModel::unlockWithPin,
            onTryBiometric = { lockViewModel.tryBiometric(context) }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showNavElements = currentRoute in listOf(
        NavRoute.Home.route,
        NavRoute.Mural.route,
        NavRoute.Settings.route
    )

    // Detecção de Tablet (Largura >= 600dp)
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Row(modifier = Modifier.fillMaxSize()) {
        // Se for tablet, mostra a barra lateral (Rail)
        if (isTablet && showNavElements) {
            GrowNavigationRail(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route == NavRoute.Notifications.route) {
                        showNotificationSheet = true
                    } else if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(NavRoute.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onAddClick = { navController.navigate(NavRoute.NewPlant.route) },
                isDeleting = isDraggingPlant
            )
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                // Se NÃO for tablet, mostra a barra inferior (Bottom Bar)
                if (!isTablet && showNavElements) {
                    GrowBottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (route == NavRoute.Notifications.route) {
                                showNotificationSheet = true
                            } else if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(NavRoute.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onAddClick = { navController.navigate(NavRoute.NewPlant.route) },
                        isDeleting = isDraggingPlant,
                        onFabBounds = { trashBounds = it }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = NavRoute.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(NavRoute.Home.route) {
                        HomeScreen(
                            innerPadding = innerPadding,
                            viewModel = homeViewModel,
                            onOpenDetails = { id -> navController.navigate(NavRoute.Detail.create(id)) },
                            onOpenSettings = { navController.navigate(NavRoute.Settings.route) },
                            onAddPlant = { navController.navigate(NavRoute.NewPlant.route) },
                            externalIsDragging = isDraggingPlant,
                            onDraggingChanged = { isDraggingPlant = it },
                            externalTrashBounds = trashBounds
                        )
                    }

                    composable(NavRoute.Mural.route) {
                        MuralScreen(
                            innerPadding = innerPadding,
                            viewModel = muralViewModel,
                            onPostClick = { postId -> navController.navigate(NavRoute.MuralPost.create(postId)) }
                        )
                    }

                    composable(NavRoute.NewPlant.route) {
                        NewPlantScreen(
                            innerPadding = PaddingValues(),
                            viewModel = addPlantViewModel,
                            onSaved = { id ->
                                navController.popBackStack()
                                navController.navigate(NavRoute.Detail.create(id))
                            },
                            onClose = { navController.popBackStack() },
                            onCheckUser = { username, onComplete ->
                                muralViewModel.createOrGetUser(username, onComplete)
                            }
                        )
                    }

                    composable(
                        route = NavRoute.Detail.route,
                        arguments = listOf(navArgument("plantId") { type = NavType.LongType })
                    ) { entry ->
                        val plantId = entry.arguments?.getLong("plantId") ?: return@composable
                        val detailViewModel: PlantDetailViewModel = viewModel(
                            key = "detail-$plantId",
                            factory = factories.detail.create(plantId)
                        )
                        PlantDetailScreen(
                            innerPadding = PaddingValues(),
                            viewModel = detailViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(NavRoute.Settings.route) {
                        SettingsScreen(
                            innerPadding = innerPadding,
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = NavRoute.MuralPost.route,
                        arguments = listOf(navArgument("postId") { type = NavType.LongType })
                    ) { entry ->
                        val postId = entry.arguments?.getLong("postId") ?: return@composable
                        val currentMuralViewModel: MuralViewModel = viewModel(
                            key = "mural-post-$postId",
                            factory = factories.mural
                        )
                        MuralPostScreen(
                            postId = postId,
                            innerPadding = PaddingValues(),
                            viewModel = currentMuralViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (showNotificationSheet) {
                    NotificationSheet(
                        onDismiss = { showNotificationSheet = false }
                    )
                }
            }
        }
    }
}
