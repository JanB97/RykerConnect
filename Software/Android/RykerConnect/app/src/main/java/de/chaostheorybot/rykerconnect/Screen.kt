package de.chaostheorybot.rykerconnect

sealed class Screen(val route: String){
    data object SetupScreen : Screen("setup_screen")
    data object HomeScreen : Screen ("home_screen")
    data object StartScreen: Screen ("start_screen")
}