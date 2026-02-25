package com.daime.grow.ui.navigation

sealed class NavRoute(val route: String) {
    data object Home : NavRoute("home")
    data object NewPlant : NavRoute("new_plant")
    data object Settings : NavRoute("settings")
    data object Detail : NavRoute("detail/{plantId}") {
        fun create(plantId: Long) = "detail/$plantId"
    }
}

