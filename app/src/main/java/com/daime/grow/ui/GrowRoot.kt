package com.daime.grow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    if (!lockState.isReady) {
        Box(modifier = Modifier.fillMaxSize())
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

    val showBottomBar = currentRoute in listOf(
        NavRoute.Home.route,
        NavRoute.Mural.route,
        NavRoute.Settings.route
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = NavRoute.Home.route,
            modifier = Modifier.padding(bottom = if (showBottomBar) 80.dp else 0.dp)
        ) {
            composable(NavRoute.Home.route) {
                HomeScreen(
                    innerPadding = PaddingValues(),
                    viewModel = homeViewModel,
                    onOpenDetails = { id -> navController.navigate(NavRoute.Detail.create(id)) },
                    onOpenSettings = { navController.navigate(NavRoute.Settings.route) },
                    onAddPlant = { navController.navigate(NavRoute.NewPlant.route) }
                )
            }

            composable(NavRoute.Mural.route) {
                MuralScreen(
                    innerPadding = PaddingValues(),
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
                    innerPadding = PaddingValues(),
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

        if (showBottomBar) {
            GrowBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route == NavRoute.Notifications.route) {
                        showNotificationSheet = true
                    } else if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(NavRoute.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (showNotificationSheet) {
            NotificationSheet(
                onDismiss = { showNotificationSheet = false }
            )
        }
    }
}
