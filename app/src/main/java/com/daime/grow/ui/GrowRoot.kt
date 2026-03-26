package com.daime.grow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daime.grow.core.AppContainer
import com.daime.grow.domain.model.DarkThemeMode
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
import com.daime.grow.ui.screen.poscolheta.PosColhetaScreen
import com.daime.grow.ui.screen.settings.SettingsScreen
import com.daime.grow.ui.screen.store.StoreScreen
import com.daime.grow.ui.viewmodel.*

@Composable
fun GrowRoot(container: AppContainer) {
    val factories = ViewModelFactories(container)
    val homeViewModel: HomeViewModel = viewModel(factory = factories.home)
    val lockViewModel: LockViewModel = viewModel(factory = factories.lock)
    val addPlantViewModel: AddPlantViewModel = viewModel(factory = factories.addPlant)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factories.settings)
    val muralViewModel: MuralViewModel = viewModel(factory = factories.mural)
    val notificationViewModel: NotificationViewModel = viewModel(factory = factories.notifications)
    val posColhetaViewModel: PosColhetaViewModel = viewModel(factory = factories.posColheta)

    val lockState by lockViewModel.uiState.collectAsStateWithLifecycle()
    val securityPrefs by settingsViewModel.security.collectAsStateWithLifecycle()
    
    val context = LocalContext.current

    var showNotificationSheet by remember { mutableStateOf(false) }
    
    var isDraggingPlant by remember { mutableStateOf(false) }
    var trashBounds by remember { mutableStateOf<Rect?>(null) }

    // Estado global para esconder a barra de navegação no scroll
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1) {
                    isBottomBarVisible = false
                } else if (available.y > 1) {
                    isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

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
        NavRoute.PosColheta.route,
        NavRoute.Mural.route,
        NavRoute.Store.route,
        NavRoute.Notifications.route,
        NavRoute.Settings.route
    )

    val configuration = LocalConfiguration.current
    // Detectar tablet de forma mais precisa (não apenas pela largura em landscape)
    val isTablet = configuration.smallestScreenWidthDp >= 600

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
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
                    onAddClick = { navController.navigate(NavRoute.NewPlant.route) }
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (!isTablet && showNavElements) {
                        AnimatedVisibility(
                            visible = isBottomBarVisible,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
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
                                maskHomeIcon = securityPrefs.maskHomeIcon,
                                onFabBounds = { trashBounds = it }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = NavRoute.Home.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(NavRoute.Home.route) {
                            HomeScreen(
                                innerPadding = innerPadding,
                                viewModel = homeViewModel,
                                settingsViewModel = settingsViewModel,
                                onOpenDetails = { id -> navController.navigate(NavRoute.Detail.create(id)) },
                                onOpenSettings = { navController.navigate(NavRoute.Settings.route) },
                                onAddPlant = { navController.navigate(NavRoute.NewPlant.route) },
                                externalIsDragging = isDraggingPlant,
                                onDraggingChanged = { isDraggingPlant = it },
                                externalTrashBounds = trashBounds
                            )
                        }

                        composable(NavRoute.PosColheta.route) {
                            PosColhetaScreen(
                                innerPadding = innerPadding,
                                viewModel = posColhetaViewModel
                            )
                        }

                        composable(NavRoute.Mural.route) {
                            MuralScreen(
                                innerPadding = innerPadding,
                                viewModel = muralViewModel,
                                onPostClick = { postId -> navController.navigate(NavRoute.MuralPost.create(postId)) }
                            )
                        }

                        composable(NavRoute.Store.route) {
                            StoreScreen(
                                innerPadding = innerPadding,
                                maskStoreCatalog = securityPrefs.maskStoreCatalog
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

                        composable(NavRoute.Detail.route,
                            arguments = listOf(navArgument("plantId") { type = NavType.LongType })
                        ) {
                            val detailViewModel: PlantDetailViewModel = hiltViewModel()
                            PlantDetailScreen(
                                innerPadding = PaddingValues(),
                                viewModel = detailViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToPosColheta = { navController.navigate(NavRoute.PosColheta.route) }
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
                        ) {
                            val currentMuralViewModel: MuralViewModel = hiltViewModel()
                            MuralPostScreen(
                                postId = checkNotNull(it.arguments?.getLong("postId")),
                                innerPadding = PaddingValues(),
                                viewModel = currentMuralViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    if (showNotificationSheet) {
                        NotificationSheet(
                            viewModel = notificationViewModel,
                            onDismiss = { showNotificationSheet = false }
                        )
                    }
                }
            }
        }

        if (isDraggingPlant) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                FloatingActionButton(
                    onClick = { /* Trigger delete - handled by HomeScreen */ },
                    containerColor = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier
                        .offset(y = (-96).dp)
                        .size(56.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Excluir plantas",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}
