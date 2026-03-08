package com.daime.grow.ui.navigation

sealed class NavRoute(val route: String) {
    data object Home : NavRoute("home")
    data object NewPlant : NavRoute("new_plant")
    data object Settings : NavRoute("settings")
    data object Mural : NavRoute("mural")
    data object Notifications : NavRoute("notifications")
    data object Detail : NavRoute("detail/{plantId}") {
        fun create(plantId: Long) = "detail/$plantId"
    }
    data object MuralPost : NavRoute("mural_post/{postId}") {
        fun create(postId: Long) = "mural_post/$postId"
    }
}
