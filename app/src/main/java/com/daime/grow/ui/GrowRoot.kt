package com.daime.grow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

    LaunchedEffect(Unit) {
        homeViewModel.ensureSeedData()
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
            onTryBiometric = { lockViewModel.tryBiometric(context) }
        )
        return
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = NavRoute.Home.route) {
        composable(NavRoute.Home.route) {
            HomeScreen(
                innerPadding = PaddingValues(),
                viewModel = homeViewModel,
                onOpenDetails = { id -> navController.navigate(NavRoute.Detail.create(id)) },
                onOpenSettings = { navController.navigate(NavRoute.Settings.route) },
                onAddPlant = { navController.navigate(NavRoute.NewPlant.route) }
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
    }
}
