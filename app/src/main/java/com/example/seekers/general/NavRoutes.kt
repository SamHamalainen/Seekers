package com.example.seekers.general

/**
 * Navigation routes for compose navigation
 */
sealed class NavRoutes(val route: String) {
    object StartGame: NavRoutes("StartGame")
    object MainScreen: NavRoutes("MainScreen")
    object LobbyCreation: NavRoutes("LobbyCreation")
    object AvatarPicker: NavRoutes("AvatarPicker")
    object LobbyQR: NavRoutes("LobbyQR")
    object Scanner: NavRoutes("Scanner")
    object Countdown: NavRoutes("Countdown")
    object Heatmap: NavRoutes("Heatmap")
    object CreateAccount : NavRoutes("CreateAccount")
    object EndGame : NavRoutes("EndGame")
}
