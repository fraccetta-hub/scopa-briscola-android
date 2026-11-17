package com.fraccetta.scopabriscola.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fraccetta.scopabriscola.ui.lobby.LobbyScreen
import com.fraccetta.scopabriscola.ui.game.briscola.BriscolaGameScreen
import com.fraccetta.scopabriscola.ui.screens.GameModeScreen
import com.fraccetta.scopabriscola.ui.screens.HomeScreen
import com.fraccetta.scopabriscola.ui.result.ResultScreen
import com.fraccetta.scopabriscola.ui.game.scopa.ScopaGameScreen
import com.fraccetta.scopabriscola.ui.stats.StatsScreen
import com.fraccetta.scopabriscola.ui.screens.guide.GuideScreen
import com.fraccetta.scopabriscola.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("gamemode/{gameType}") { backStackEntry ->
            val gameType = backStackEntry.arguments?.getString("gameType") ?: "scopa"
            GameModeScreen(gameType = gameType, navController = navController)
        }
        composable("lobby/{gameType}/{role}") { backStackEntry ->
            val gameType = backStackEntry.arguments?.getString("gameType") ?: ""
            val role = backStackEntry.arguments?.getString("role") ?: "HOST"
            LobbyScreen(gameType = gameType, role = role, navController = navController)
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
        composable("guide") {
            GuideScreen(navController = navController)
        }
    }
}
