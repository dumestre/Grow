package com.daime.grow.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import com.daime.grow.R
import com.daime.grow.core.AppContainer
import com.daime.grow.ui.navigation.NavRoute
import com.daime.grow.ui.screen.add.NewPlantScreen
import com.daime.grow.ui.screen.detail.PlantDetailScreen
import com.daime.grow.ui.screen.home.HomeScreen
import com.daime.grow.ui.screen.lock.LockScreen
import com.daime.grow.ui.screen.settings.SettingsScreen
import com.daime.grow.ui.viewmodel.AddPlantViewModel
import com.daime.grow.ui.viewmodel.HomeViewModel
import com.daime.grow.ui.viewmodel.LockViewModel
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

    val lockState by lockViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        homeViewModel.ensureSeedData()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!lockState.isReady) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    if (lockState.showLockScreen) {
        LockScreen(
            state = lockState,
            onPinChange = lockViewModel::onPinInputChange,
            onUnlockWithPin = lockViewModel::unlockWithPin,
            onTryBiometric = {
                lockViewModel.tryBiometric(context)
            }
        )
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        GrowNavHost(
            navController = navController,
            homeViewModel = homeViewModel,
            addPlantViewModel = addPlantViewModel,
            settingsViewModel = settingsViewModel,
            detailFactory = factories.detail,
            innerPadding = PaddingValues()
        )

        if (currentRoute == NavRoute.Home.route) {
            FloatingActionButton(
                onClick = { navController.navigate(NavRoute.NewPlant.route) },
                containerColor = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarsPadding.calculateBottomPadding() + 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.fab_add_plant)
                )
            }
        }
    }
}

@Composable
private fun GrowNavHost(
    navController: androidx.navigation.NavHostController,
    homeViewModel: HomeViewModel,
    addPlantViewModel: AddPlantViewModel,
    settingsViewModel: SettingsViewModel,
    detailFactory: ViewModelFactories.DetailFactory,
    innerPadding: PaddingValues
) {
    NavHost(navController = navController, startDestination = NavRoute.Home.route) {
        composable(NavRoute.Home.route) {
            HomeScreen(
                innerPadding = innerPadding,
                viewModel = homeViewModel,
                onOpenDetails = { navController.navigate(NavRoute.Detail.create(it)) },
                onOpenSettings = { navController.navigate(NavRoute.Settings.route) }
            )
        }

        composable(NavRoute.NewPlant.route) {
            NewPlantScreen(
                innerPadding = innerPadding,
                viewModel = addPlantViewModel,
                onSaved = { id ->
                    navController.popBackStack()
                    navController.navigate(NavRoute.Detail.create(id))
                },
                onClose = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoute.Detail.route,
            arguments = listOf(navArgument("plantId") { type = NavType.LongType })
        ) { entry ->
            val plantId = entry.arguments?.getLong("plantId") ?: return@composable
            val detailViewModel: PlantDetailViewModel = viewModel(
                key = "detail-$plantId",
                factory = detailFactory.create(plantId)
            )
            PlantDetailScreen(
                innerPadding = innerPadding,
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
    }
}

