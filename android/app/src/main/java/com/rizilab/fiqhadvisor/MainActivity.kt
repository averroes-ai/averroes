package com.rizilab.fiqhadvisor

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizilab.fiqhadvisor.ui.screens.AuthScreen
import com.rizilab.fiqhadvisor.ui.screens.ChatScreen
import com.rizilab.fiqhadvisor.ui.screens.SplashScreen
import com.rizilab.fiqhadvisor.ui.viewmodel.FiqhAIViewModel

/** Navigation state for the app */
enum class AppScreen {
    SPLASH,
    AUTH,
    CHAT
}

/** Main Activity for FiqhAdvisor - Islamic Finance AI Assistant */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "🚀 Starting FiqhAdvisor App")

        setContent {
            MaterialTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) { FiqhAdvisorApp() }
            }
        }
    }
}

@Composable
fun FiqhAdvisorApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    val viewModel: FiqhAIViewModel = viewModel()

    // Initialize AI system when app starts
    LaunchedEffect(Unit) { viewModel.initializeAI() }

    when (currentScreen) {
        AppScreen.SPLASH -> {
            SplashScreen(
                    onNavigateToAuth = { currentScreen = AppScreen.AUTH },
                    onNavigateToHome = { currentScreen = AppScreen.CHAT }
            )
        }
        AppScreen.AUTH -> {
            AuthScreen(
                    onAuthSuccess = { currentScreen = AppScreen.CHAT },
                    onSkipAuth = { currentScreen = AppScreen.CHAT }
            )
        }
        AppScreen.CHAT -> {
            ChatScreen(
                    onBack = { currentScreen = AppScreen.AUTH },
                    onAnalyzeToken = { token, callbacks ->
                        // Use streaming analysis from ViewModel with proper callbacks
                        viewModel.analyzeTokenStream(
                                token = token,
                                onChunk = callbacks.onChunk,
                                onComplete = callbacks.onComplete,
                                onError = callbacks.onError
                        )
                    },
                    onQueryStream = { question, callbacks ->
                        // Use streaming query from ViewModel with proper callbacks
                        viewModel.queryStream(
                                question = question,
                                onChunk = callbacks.onChunk,
                                onComplete = callbacks.onComplete,
                                onError = callbacks.onError
                        )
                    }
            )
        }
    }
}
