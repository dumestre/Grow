package com.daime.grow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daime.grow.core.AppContainer
import com.daime.grow.data.preferences.AppPreferencesRepository
import com.daime.grow.ui.components.GrowBottomNavigationBar
import com.daime.grow.ui.components.GrowNavigationRail
import com.daime.grow.ui.components.NotificationSheet
import com.daime.grow.ui.navigation.NavRoute
import com.daime.grow.ui.screen.add.NewPlantScreen
import com.daime.grow.ui.screen.auth.GoogleLoginScreen
import com.daime.grow.ui.screen.detail.PlantDetailScreen
import com.daime.grow.ui.screen.home.HomeScreen
import com.daime.grow.ui.screen.lock.LockScreen
import com.daime.grow.ui.screen.mural.MuralPostScreen
import com.daime.grow.ui.screen.mural.MuralScreen
import com.daime.grow.ui.screen.onboarding.DisclaimerScreen
import com.daime.grow.ui.screen.poscolheta.PosColhetaScreen
import com.daime.grow.ui.screen.ppfd.PPFDScreen
import com.daime.grow.ui.screen.settings.SettingsScreen
import com.daime.grow.ui.screen.store.StrainDetailScreen
import com.daime.grow.ui.screen.store.StrainsScreen
import com.daime.grow.ui.screen.tips.GrowTipsScreen
import com.daime.grow.ui.viewmodel.AddPlantViewModel
import com.daime.grow.ui.viewmodel.HomeViewModel
import com.daime.grow.ui.viewmodel.LockViewModel
import com.daime.grow.ui.viewmodel.MuralEvent
import com.daime.grow.ui.viewmodel.MuralViewModel
import com.daime.grow.ui.viewmodel.NotificationViewModel
import com.daime.grow.ui.viewmodel.PlantDetailViewModel
import com.daime.grow.ui.viewmodel.PosColhetaViewModel
import com.daime.grow.ui.viewmodel.SettingsViewModel
import com.daime.grow.ui.viewmodel.ViewModelFactories
import kotlinx.coroutines.launch

@Composable
fun GrowRoot(container: AppContainer) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferencesRepository(context) }
    val disclaimerAccepted by appPreferences.observeDisclaimerAccepted().collectAsStateWithLifecycle(initialValue = null)

    if (disclaimerAccepted == null) {
        Box(modifier = Modifier.fillMaxSize()) {}
        return
    }

    if (disclaimerAccepted == false) {
        DisclaimerScreen(
            onAccept = {
                kotlinx.coroutines.MainScope().launch {
                    appPreferences.setDisclaimerAccepted(true)
                }
            }
        )
        return
    }

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
    val unreadNotificationCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()
    val currentUserUuid by muralViewModel.currentUserUuid.collectAsStateWithLifecycle()
    val currentUsername by muralViewModel.currentUsername.collectAsStateWithLifecycle()
    val currentUserEmail by muralViewModel.currentUserEmail.collectAsStateWithLifecycle()
    val isAuthResolved by muralViewModel.isAuthResolved.collectAsStateWithLifecycle()
    
    var showNotificationSheet by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isGoogleLoginLoading by remember { mutableStateOf(false) }
    
    var isDraggingPlant by remember { mutableStateOf(false) }
    var trashBounds by remember { mutableStateOf<Rect?>(null) }

    // Estado global para esconder a barra de navegação no scroll com threshold aumentado
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Threshold ajustado para melhor responsividade
                if (available.y < -8) {
                    isBottomBarVisible = false
                } else if (available.y > 8) {
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

    if (!isAuthResolved) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (currentUserUuid == null) {
        LaunchedEffect(muralViewModel) {
            muralViewModel.events.collect { event ->
                when (event) {
                    is MuralEvent.GoogleLoginError -> {
                        loginError = event.message
                        isGoogleLoginLoading = false
                    }
                    MuralEvent.GoogleLoginSuccess -> {
                        loginError = null
                        isGoogleLoginLoading = false
                    }
                    else -> Unit
                }
            }
        }

        GoogleLoginScreen(
            isLoading = isGoogleLoginLoading,
            errorMessage = loginError,
            onGoogleLogin = {
                isGoogleLoginLoading = true
                loginError = null
                muralViewModel.signInWithGoogle(context) {
                    isGoogleLoginLoading = false
                    loginError = null
                }
            }
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
        NavRoute.Settings.route,
        NavRoute.PPFD.route
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
                    onAddClick = { navController.navigate(NavRoute.NewPlant.route) },
                    notificationBadgeCount = unreadNotificationCount
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (!isTablet && showNavElements) {
                        AnimatedVisibility(
                            visible = isBottomBarVisible,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )
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
                                maskHomeIcon = securityPrefs?.maskHomeIcon ?: true,
                                onFabBounds = { trashBounds = it },
                                notificationBadgeCount = unreadNotificationCount
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
                                onOpenTips = { navController.navigate(NavRoute.GrowTips.route) },
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
                            StrainsScreen(
                                innerPadding = innerPadding,
                                onStrainClick = { strainId -> 
                                    navController.navigate(NavRoute.StrainDetail.create(strainId))
                                }
                            )
                        }

                        composable(NavRoute.StrainDetail.route,
                            arguments = listOf(navArgument("strainId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val strainId = backStackEntry.arguments?.getString("strainId") ?: return@composable
                            StrainDetailScreen(
                                strainId = strainId,
                                onBack = { navController.popBackStack() }
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
                                onCheckUser = { username, onComplete, onUsernameTaken ->
                                    muralViewModel.createOrGetUser(username, onComplete, onUsernameTaken)
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
                                accountUsername = currentUsername,
                                accountEmail = currentUserEmail,
                                onSignOut = { muralViewModel.signOut() },
                                onUpdateUsername = { username, onComplete ->
                                    muralViewModel.updateUsername(username, onComplete)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = NavRoute.MuralPost.route,
                            arguments = listOf(navArgument("postId") { type = NavType.StringType })
                        ) {
                            val currentMuralViewModel: MuralViewModel = hiltViewModel()
                            MuralPostScreen(
                                postId = checkNotNull(it.arguments?.getString("postId")),
                                innerPadding = PaddingValues(),
                                viewModel = currentMuralViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoute.GrowTips.route) {
                            GrowTipsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoute.PPFD.route) {
                            PPFDScreen(
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
