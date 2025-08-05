package com.rizilab.averroes.data.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.util.Log

// Mobile Wallet Adapter imports (re-enabled for wallet integration)
import android.net.Uri
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.RpcCluster
import com.solana.mobilewalletadapter.common.signin.SignInWithSolana

/**
 * Wallet connection state
 */
sealed class WalletConnectionState {
    object NotConnected : WalletConnectionState()
    data class Connected(
        val publicKey: String,
        val accountLabel: String,
        val authToken: String,
        val walletUriBase: String,
        val balance: Float = 0f,
        val network: String = "devnet",
        val isConnected: Boolean = true
    ) : WalletConnectionState()
}

/**
 * Wallet connection result
 */
sealed class WalletResult<out T> {
    data class Success<T>(val data: T) : WalletResult<T>()
    data class Error(val message: String) : WalletResult<Nothing>()
    object NoWalletFound : WalletResult<Nothing>()
    object UserCancelled : WalletResult<Nothing>()
}

/**
 * Service for handling Solana wallet connections using Mobile Wallet Adapter
 */
class WalletService(private val walletPersistence: WalletPersistence? = null) {
    companion object {
        private const val TAG = "WalletService"
    }

    private var currentConnection: WalletConnectionState = WalletConnectionState.NotConnected

    init {
        // Load persisted connection on initialization
        walletPersistence?.let { persistence ->
            currentConnection = persistence.loadConnection()
            Log.d(TAG, "Loaded persisted connection: ${currentConnection::class.simpleName}")
        }
    }

    // Singleton MobileWalletAdapter instance like reference implementation
    private val mobileWalletAdapter: MobileWalletAdapter by lazy {
        val connectionIdentity = ConnectionIdentity(
            identityUri = Uri.parse("https://solana.com"), // Use same domain as reference
            iconUri = Uri.parse("favicon.ico"),
            identityName = "Averroes"
        )
        MobileWalletAdapter(connectionIdentity)
    }

    /**
     * Connect to a Solana wallet using Mobile Wallet Adapter
     */
    /**
     * Check if wallet apps are available
     */
    fun checkWalletAvailability(context: android.content.Context) {
        val packageManager = context.packageManager
        val phantomInstalled = try {
            packageManager.getPackageInfo("app.phantom", 0)
            true
        } catch (e: Exception) {
            false
        }

        Log.d(TAG, "Phantom wallet installed: $phantomInstalled")

        // Check for MWA compatible apps
        val mwaIntent = android.content.Intent("solana.mobilewalletadapter.action.AUTHORIZE")
        val resolveInfos = packageManager.queryIntentActivities(mwaIntent, 0)
        Log.d(TAG, "MWA compatible apps found: ${resolveInfos.size}")
        resolveInfos.forEach { resolveInfo ->
            Log.d(TAG, "MWA app: ${resolveInfo.activityInfo.packageName}")
        }
    }

    suspend fun connectWallet(
        sender: ActivityResultSender,
        rpcCluster: RpcCluster = RpcCluster.Devnet // Always use devnet for debugging
    ): WalletResult<WalletConnectionState.Connected> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to connect wallet with network: $rpcCluster (Devnet for debugging)")
            Log.d(TAG, "Using reference implementation pattern with solana.com domain")

            // Follow reference implementation exactly - use SignInWithSolana for new connections
            val isNewConnection = currentConnection is WalletConnectionState.NotConnected
            val signInPayload = if (isNewConnection) {
                SignInWithSolana.Payload("solana.com", "Sign in to Averroes Islamic Finance AI")
            } else {
                null
            }

            Log.d(TAG, "Connection type: ${if (isNewConnection) "New" else "Existing"}")

            // Use singleton adapter with reference pattern
            val result = mobileWalletAdapter.transact(sender, signInPayload) { authResult ->
                Log.d(TAG, "Authorization successful, authResult received")
                // Empty block like reference - just return the authResult
            }

            Log.d(TAG, "MWA transact completed, processing result: ${result::class.simpleName}")
            when (result) {
                is TransactionResult.Success -> {
                    Log.d(TAG, "Wallet connection successful!")
                    val authResult = result.authResult
                    Log.d(TAG, "Auth result - Accounts: ${authResult.accounts.size}, Wallet: ${authResult.walletUriBase}")
                    val firstAccount = authResult.accounts.firstOrNull()

                    if (firstAccount != null) {
                        // Convert public key bytes to Base58 string (Solana standard)
                        val publicKeyString = encodeBase58(firstAccount.publicKey)
                        Log.d(TAG, "Connected to wallet: ${firstAccount.accountLabel}")
                        Log.d(TAG, "Public key: $publicKeyString")

                        val connection = WalletConnectionState.Connected(
                            publicKey = publicKeyString,
                            accountLabel = firstAccount.accountLabel ?: "Solana Wallet",
                            authToken = authResult.authToken,
                            walletUriBase = authResult.walletUriBase?.toString() ?: ""
                        )

                        currentConnection = connection

                        // Persist the connection for future use
                        walletPersistence?.saveConnection(connection)
                        Log.d(TAG, "Connection established and persisted: ${connection.publicKey}")

                        WalletResult.Success(connection)
                    } else {
                        Log.e(TAG, "No accounts returned from wallet")
                        WalletResult.Error("No accounts returned from wallet")
                    }
                }
                is TransactionResult.Failure -> {
                    Log.e(TAG, "Wallet connection failed: ${result.message}")
                    val errorMessage = when {
                        result.message.contains("iconRelativeUri must be a relative Uri") ->
                            "Wallet connection configuration error. Please try again."
                        result.message.contains("No wallet found") ->
                            "No Solana wallet found. Please install Phantom wallet."
                        else -> "Failed to connect: ${result.message}"
                    }
                    WalletResult.Error(errorMessage)
                }
                is TransactionResult.NoWalletFound -> {
                    Log.w(TAG, "No wallet found: ${result.message}")
                    WalletResult.NoWalletFound
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during wallet connection", e)
            WalletResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Fallback mock connection for testing
     */
    private suspend fun mockWalletConnection(): WalletResult<WalletConnectionState.Connected> {
        delay(2000)

        val connection = WalletConnectionState.Connected(
            publicKey = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU",
            accountLabel = "Mock Solana Wallet (MWA not available)",
            authToken = "mock_auth_token_${System.currentTimeMillis()}",
            walletUriBase = "https://phantom.app"
        )

        currentConnection = connection
        return WalletResult.Success(connection)
    }

    /**
     * Get current wallet connection state
     */
    fun getCurrentConnection(): WalletConnectionState {
        return currentConnection
    }

    /**
     * Check if wallet is currently connected
     */
    fun isConnected(): Boolean {
        return currentConnection is WalletConnectionState.Connected
    }

    /**
     * Refresh connection from persistence
     */
    fun refreshFromPersistence() {
        walletPersistence?.let { persistence ->
            currentConnection = persistence.loadConnection()
            Log.d(TAG, "Refreshed connection from persistence: ${currentConnection::class.simpleName}")
        }
    }



    /**
     * Disconnect from wallet
     */
    suspend fun disconnect(sender: ActivityResultSender): WalletResult<Unit> {
        return try {
            // Use singleton adapter for disconnection like reference
            mobileWalletAdapter.disconnect(sender)
            currentConnection = WalletConnectionState.NotConnected

            // Clear persisted connection
            walletPersistence?.clearConnection()
            Log.d(TAG, "Wallet disconnected and persistence cleared")

            WalletResult.Success(Unit)
        } catch (e: Exception) {
            WalletResult.Error(e.message ?: "Failed to disconnect")
        }
    }

    /**
     * Get current connection state
     */
    fun getConnectionState(): WalletConnectionState = currentConnection

    // Removed duplicate isConnected method

    /**
     * Get connected wallet address
     */
    fun getWalletAddress(): String? {
        return when (val connection = currentConnection) {
            is WalletConnectionState.Connected -> connection.publicKey
            is WalletConnectionState.NotConnected -> null
        }
    }

    /**
     * Get wallet label
     */
    fun getWalletLabel(): String? {
        return when (val connection = currentConnection) {
            is WalletConnectionState.Connected -> connection.accountLabel
            is WalletConnectionState.NotConnected -> null
        }
    }

    /**
     * Encode byte array to Base58 string (Solana standard)
     */
    private fun encodeBase58(bytes: ByteArray): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        var num = java.math.BigInteger(1, bytes)
        val sb = StringBuilder()

        while (num > java.math.BigInteger.ZERO) {
            val remainder = num.remainder(java.math.BigInteger.valueOf(58))
            sb.append(alphabet[remainder.toInt()])
            num = num.divide(java.math.BigInteger.valueOf(58))
        }

        // Add leading zeros
        for (b in bytes) {
            if (b.toInt() == 0) {
                sb.append(alphabet[0])
            } else {
                break
            }
        }

        return sb.reverse().toString()
    }
}
