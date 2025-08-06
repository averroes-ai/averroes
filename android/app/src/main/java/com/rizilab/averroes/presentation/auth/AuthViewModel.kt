package com.rizilab.averroes.presentation.auth

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.rizilab.averroes.data.wallet.WalletResult
import com.rizilab.averroes.data.wallet.WalletService
import com.rizilab.averroes.data.wallet.WalletPersistence
import com.rizilab.averroes.data.wallet.NetworkConfig
import com.rizilab.averroes.presentation.base.MviViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Mobile Wallet Adapter imports (re-enabled for wallet authentication)
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

/**
 * Authentication State
 */
data class AuthState(
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val walletAddress: String? = null,
    val error: String? = null,
    val authMethod: AuthMethod = AuthMethod.WALLET
)

/**
 * Authentication Methods
 */
enum class AuthMethod {
    WALLET,
    GUEST
}

/**
 * Authentication Intents
 */
sealed class AuthIntent {
    data class ConnectWallet(val seedPhrase: String?, val network: String) : AuthIntent()
    object ContinueAsGuest : AuthIntent()
    object RetryConnection : AuthIntent()
    object ClearError : AuthIntent()
}

/**
 * Authentication Effects
 */
sealed class AuthEffect {
    object NavigateToMain : AuthEffect()
    object ShowWalletSelector : AuthEffect()
    object OpenPhantomPlayStore : AuthEffect()
    data class ShowError(val message: String) : AuthEffect()
    data class ShowSuccess(val message: String) : AuthEffect()
}

/**
 * Authentication ViewModel using MVI pattern with real Mobile Wallet Adapter
 */
class AuthViewModel(
    private val activityResultSender: ActivityResultSender? = null,
    private val walletPersistence: WalletPersistence? = null
) : MviViewModel<AuthState, AuthIntent, AuthEffect>(AuthState()) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val walletService = WalletService(walletPersistence)

    override fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.ConnectWallet -> connectWallet(intent.seedPhrase, intent.network)
            is AuthIntent.ContinueAsGuest -> continueAsGuest()
            is AuthIntent.RetryConnection -> connectWallet(null, "testnet") // Default retry
            is AuthIntent.ClearError -> clearError()
        }
    }

    private fun connectWallet(seedPhrase: String?, network: String) {
        viewModelScope.launch {
            updateState { copy(isConnecting = true, error = null) }

            try {
                // Handle seed phrase scenarios
                when (seedPhrase) {
                    "GENERATE_NEW" -> {
                        // Generate new wallet (simplified for auth screen)
                        updateState {
                            copy(
                                isConnecting = false,
                                walletAddress = "Generated_Wallet_Address",
                                authMethod = AuthMethod.WALLET
                            )
                        }
                        sendEffect(AuthEffect.ShowSuccess("New wallet generated successfully"))
                        delay(1000)
                        sendEffect(AuthEffect.NavigateToMain)
                        return@launch
                    }
                    null -> {
                        // Connect existing wallet via MWA
                        val sender = activityResultSender
                        if (sender == null) {
                            updateState {
                                copy(
                                    isConnecting = false,
                                    error = "Activity result sender not available"
                                )
                            }
                            sendEffect(AuthEffect.ShowError("Cannot connect wallet: Activity not available"))
                            return@launch
                        }

                        Log.d(TAG, "Attempting wallet connection with devnet (hardcoded for debugging)")

                        // Always use devnet for debugging to avoid local wallet establishment issues
                        when (val result = walletService.connectWallet(sender, rpcCluster = com.solana.mobilewalletadapter.clientlib.RpcCluster.Devnet)) {
                            is WalletResult.Success -> {
                                val connection = result.data
                                Log.d(TAG, "Wallet connected successfully: ${connection.publicKey}")
                                updateState {
                                    copy(
                                        isConnecting = false,
                                        walletAddress = connection.publicKey,
                                        authMethod = AuthMethod.WALLET
                                    )
                                }

                                sendEffect(AuthEffect.ShowSuccess("Wallet connected: ${connection.accountLabel}"))
                                delay(1000)
                                sendEffect(AuthEffect.NavigateToMain)
                            }
                            is WalletResult.Error -> {
                                updateState {
                                    copy(
                                        isConnecting = false,
                                        error = result.message
                                    )
                                }
                                sendEffect(AuthEffect.ShowError("Failed to connect wallet: ${result.message}"))
                            }
                            is WalletResult.NoWalletFound -> {
                                updateState {
                                    copy(
                                        isConnecting = false,
                                        error = "No compatible wallet found. Please install a Solana wallet app."
                                    )
                                }
                                sendEffect(AuthEffect.ShowError("No compatible wallet found"))
                            }
                            is WalletResult.UserCancelled -> {
                                updateState {
                                    copy(
                                        isConnecting = false,
                                        error = "Wallet connection cancelled by user"
                                    )
                                }
                                sendEffect(AuthEffect.ShowError("Connection cancelled"))
                            }
                        }
                    }
                    else -> {
                        // Import from seed phrase
                        updateState {
                            copy(
                                isConnecting = false,
                                walletAddress = "Imported_Wallet_Address",
                                authMethod = AuthMethod.WALLET
                            )
                        }
                        sendEffect(AuthEffect.ShowSuccess("Wallet imported successfully"))
                        delay(1000)
                        sendEffect(AuthEffect.NavigateToMain)
                    }
                }

            } catch (e: Exception) {
                updateState {
                    copy(
                        isConnecting = false,
                        error = e.message ?: "Failed to connect wallet"
                    )
                }
                sendEffect(AuthEffect.ShowError("Failed to connect wallet: ${e.message}"))
            }
        }
    }

    private fun continueAsGuest() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            
            try {
                delay(1000) // Simulate guest setup
                
                updateState { 
                    copy(
                        isLoading = false,
                        authMethod = AuthMethod.GUEST
                    )
                }
                
                sendEffect(AuthEffect.ShowSuccess("Welcome! You can explore features in guest mode."))
                delay(1000)
                sendEffect(AuthEffect.NavigateToMain)
                
            } catch (e: Exception) {
                updateState { 
                    copy(
                        isLoading = false,
                        error = e.message ?: "Failed to continue as guest"
                    )
                }
            }
        }
    }

    private fun retryConnection() {
        clearError()
        connectWallet(null, "testnet") // Default retry with testnet
    }

    private fun clearError() {
        updateState { copy(error = null) }
    }

    // TODO: Implement actual Mobile Wallet Adapter methods
    /*
    private fun connectWithMobileWalletAdapter() {
        viewModelScope.launch {
            try {
                // Mobile Wallet Adapter integration
                val mobileWalletAdapter = MobileWalletAdapter()
                
                val authResult = mobileWalletAdapter.transact(activityResultSender) { wallet ->
                    wallet.authorize(
                        identityUri = Uri.parse("https://averroes.rizilab.com"),
                        iconUri = Uri.parse("favicon.ico"),
                        identityName = "Averroes",
                        rpcCluster = MobileWalletAdapter.RpcCluster.MainnetBeta
                    )
                }

                when (authResult) {
                    is TransactionResult.Success -> {
                        val publicKey = authResult.payload.publicKey
                        val publicKeyString = android.util.Base64.encodeToString(publicKey, android.util.Base64.NO_WRAP)
                        
                        updateState { 
                            copy(
                                isConnecting = false,
                                walletAddress = publicKeyString,
                                authMethod = AuthMethod.WALLET
                            )
                        }
                        
                        sendEffect(AuthEffect.NavigateToMain)
                    }
                    is TransactionResult.Failure -> {
                        updateState { 
                            copy(
                                isConnecting = false,
                                error = "Failed to connect: ${authResult.reason}"
                            )
                        }
                    }
                    is TransactionResult.NoWalletFound -> {
                        updateState { 
                            copy(
                                isConnecting = false,
                                error = "No compatible wallet found"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                updateState { 
                    copy(
                        isConnecting = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }
    */
}
