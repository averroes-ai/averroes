package com.rizilab.fiqhadvisor.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rizilab.fiqhadvisor.ui.screens.SplashScreen
import com.rizilab.fiqhadvisor.ui.screens.AuthScreen
import com.rizilab.fiqhadvisor.ui.screens.ChatScreen
import com.rizilab.fiqhadvisor.ui.viewmodel.FiqhAIViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Chat : Screen("chat")
}

@Composable
fun FiqhAdvisorNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Chat.route) {
            val viewModel: FiqhAIViewModel = hiltViewModel()
            ChatScreen(viewModel = viewModel)
        }
    }
}