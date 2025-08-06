package com.rizilab.averroes.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizilab.averroes.data.wallet.WalletPersistence
import com.rizilab.averroes.data.wallet.WalletService
import com.rizilab.averroes.presentation.auth.AuthScreen
import com.rizilab.averroes.presentation.chat.ChatScreen
import com.rizilab.averroes.presentation.crypto.CryptoScreen
import com.rizilab.averroes.presentation.navigation.Screen
import com.rizilab.averroes.presentation.screens.MainScreen
import com.rizilab.averroes.presentation.splash.SplashScreen
import com.rizilab.averroes.presentation.theme.AverroesTheme
import com.rizilab.averroes.presentation.wallet.SharedWalletViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

/**
 * Main app composable for Android with wallet integration
 */
@Composable
fun App(activityResultSender: ActivityResultSender? = null) {
    val context = LocalContext.current

    // Create wallet persistence and shared wallet view model
    val walletPersistence = remember { WalletPersistence(context) }
    val walletService = remember { WalletService(walletPersistence) }
    val sharedWalletViewModel: SharedWalletViewModel = viewModel {
        SharedWalletViewModel(walletPersistence, walletService)
    }

    AverroesTheme {
        var currentScreen by remember { mutableStateOf("splash") }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                "splash" -> {
                    SplashScreen(
                        onNavigateToAuth = { currentScreen = "auth" },
                        onNavigateToMain = { currentScreen = "main" }
                    )
                }
                "auth" -> {
                    AuthScreen(
                        onAuthSuccess = { currentScreen = "main" },
                        onSkipAuth = { currentScreen = "main" },
                        activityResultSender = activityResultSender,
                        walletPersistence = walletPersistence,
                        sharedWalletViewModel = sharedWalletViewModel
                    )
                }
                "main" -> {
                    MainScreen(
                        onBack = { currentScreen = "auth" },
                        activityResultSender = activityResultSender,
                        sharedWalletViewModel = sharedWalletViewModel
                    )
                }
            }
        }
    }
}
