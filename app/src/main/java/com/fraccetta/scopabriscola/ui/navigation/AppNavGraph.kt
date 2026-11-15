package com.fraccetta.scopabriscola.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fraccetta.scopabriscola.ui.lobby.LobbyScreen
import com.fraccetta.scopabriscola.ui.game.briscola.BriscolaGameScreen
import com.fraccetta.scopabriscola.ui.screens.HomeScreen
import com.fraccetta.scopabriscola.ui.screens.ResultScreen
import com.fraccetta.scopabriscola.ui.game.scopa.ScopaGameScreen
import com.fraccetta.scopabriscola.ui.screens.StatsScreen
import com.fraccetta.scopabriscola.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("lobby/{gameType}") { backStackEntry ->
            val gameType = backStackEntry.arguments?.getString("gameType") ?: ""
            LobbyScreen(gameType = gameType, navController = navController)
        }
        composable("game/scopa") {
            ScopaGameScreen(navController = navController)
        }
        composable("game/briscola") {
            BriscolaGameScreen(navController = navController)
        }
        composable("result/{winner}/{gameType}") { backStackEntry ->
            val winner = backStackEntry.arguments?.getString("winner") ?: ""
            val gameType = backStackEntry.arguments?.getString("gameType") ?: ""
            ResultScreen(winner = winner, gameType = gameType, navController = navController)
        }
        composable("stats") {
            StatsScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}
