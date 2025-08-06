package com.rizilab.averroes.presentation.splash

import androidx.lifecycle.viewModelScope
import com.rizilab.averroes.presentation.base.MviViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Screen State
 */
data class SplashState(
    val isLoading: Boolean = true,
    val loadingProgress: Float = 0f,
    val loadingMessage: String = "Initializing Averroes..."
)

/**
 * Splash Screen Intents
 */
sealed class SplashIntent {
    object Initialize : SplashIntent()
    object CheckAuthentication : SplashIntent()
}

/**
 * Splash Screen Effects
 */
sealed class SplashEffect {
    object NavigateToAuth : SplashEffect()
    object NavigateToMain : SplashEffect()
    data class ShowError(val message: String) : SplashEffect()
}

/**
 * Splash Screen ViewModel using MVI pattern
 */
class SplashViewModel : MviViewModel<SplashState, SplashIntent, SplashEffect>(SplashState()) {

    init {
        handleIntent(SplashIntent.Initialize)
    }

    override fun handleIntent(intent: SplashIntent) {
        when (intent) {
            is SplashIntent.Initialize -> initialize()
            is SplashIntent.CheckAuthentication -> checkAuthentication()
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                // Simulate initialization steps
                val steps = listOf(
                    "Loading Islamic Finance AI..." to 0.2f,
                    "Initializing Fiqh Core..." to 0.4f,
                    "Loading Halal Crypto Database..." to 0.6f,
                    "Preparing Wallet Integration..." to 0.8f,
                    "Ready!" to 1.0f
                )

                for ((message, progress) in steps) {
                    updateState {
                        copy(
                            loadingMessage = message,
                            loadingProgress = progress
                        )
                    }
                    delay(800) // Simulate loading time
                }

                // Check authentication after initialization
                handleIntent(SplashIntent.CheckAuthentication)

            } catch (e: Exception) {
                sendEffect(SplashEffect.ShowError("Failed to initialize: ${e.message}"))
            }
        }
    }

    private fun checkAuthentication() {
        viewModelScope.launch {
            delay(500) // Simulate auth check
            
            // For now, always navigate to auth screen
            // In the future, check if user is already authenticated
            updateState { copy(isLoading = false) }
            sendEffect(SplashEffect.NavigateToAuth)
        }
    }
}
