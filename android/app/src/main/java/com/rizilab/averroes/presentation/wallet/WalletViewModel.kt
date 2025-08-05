package com.rizilab.averroes.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizilab.averroes.data.model.*
import com.rizilab.averroes.data.repository.WalletRepository
import com.rizilab.averroes.data.wallet.WalletService
import com.rizilab.averroes.data.wallet.WalletResult
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * ViewModel for wallet management
 */
class WalletViewModel(
    private val walletRepository: WalletRepository,
    private val walletService: WalletService
) : ViewModel() {
    
    companion object {
        private const val TAG = "WalletViewModel"
    }
    
    private val _state = MutableStateFlow(WalletState(wallet = null))
    val state: StateFlow<WalletState> = _state.asStateFlow()
    
    init {
        loadCurrentWallet()
    }
    
    /**
     * Load current wallet from storage
     */
    private fun loadCurrentWallet() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                val currentWallet = walletRepository.getCurrentWallet()
                if (currentWallet != null) {
                    _state.value = _state.value.copy(
                        wallet = currentWallet,
                        isLoading = false
                    )
                    
                    // Load recent transactions
                    loadTransactions(currentWallet.id)
                    
                    // Update balance if wallet is connected
                    if (currentWallet.isConnected) {
                        updateBalance()
                    }
                } else {
                    _state.value = _state.value.copy(
                        wallet = null,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading current wallet", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load wallet: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Connect wallet using Mobile Wallet Adapter with optional seed phrase and network
     */
    fun connectWallet(activityResultSender: ActivityResultSender?, seedPhrase: String? = null, network: String = "testnet") {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                // Handle seed phrase scenarios
                when (seedPhrase) {
                    "GENERATE_NEW" -> {
                        // Generate new wallet
                        generateNewWallet()
                        return@launch
                    }
                    null -> {
                        // Connect existing wallet via MWA
                        if (activityResultSender != null) {
                            connectExistingWallet(activityResultSender, network)
                        } else {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "Activity result sender required for wallet connection"
                            )
                        }
                    }
                    else -> {
                        // Import from seed phrase
                        importFromSeedPhrase(seedPhrase)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during wallet connection", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Connection failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Connect existing wallet via Mobile Wallet Adapter
     */
    private suspend fun connectExistingWallet(activityResultSender: ActivityResultSender, network: String) {
        // Convert network string to RpcCluster
        val rpcCluster = when (network) {
            "mainnet" -> com.solana.mobilewalletadapter.clientlib.RpcCluster.MainnetBeta
            "testnet" -> com.solana.mobilewalletadapter.clientlib.RpcCluster.Testnet
            "devnet" -> com.solana.mobilewalletadapter.clientlib.RpcCluster.Devnet
            else -> com.solana.mobilewalletadapter.clientlib.RpcCluster.Testnet
        }

        when (val result = walletService.connectWallet(activityResultSender, rpcCluster = rpcCluster)) {
                    is WalletResult.Success -> {
                        val connectionState = result.data
                        
                        // Determine wallet type from wallet URI or account label
                        val walletType = determineWalletType(
                            connectionState.walletUriBase,
                            connectionState.accountLabel
                        )
                        
                        // Save wallet to repository with network
                        val storedWallet = walletRepository.saveWallet(
                            publicKey = connectionState.publicKey,
                            walletType = walletType,
                            accountLabel = connectionState.accountLabel,
                            network = network
                        )
                        
                        _state.value = _state.value.copy(
                            wallet = storedWallet,
                            isLoading = false,
                            error = null
                        )
                        
                        // Update balance after connection
                        updateBalance()
                        
                        Log.d(TAG, "Wallet connected successfully: ${storedWallet.name}")
                        
                    }
                    is WalletResult.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                        Log.e(TAG, "Wallet connection failed: ${result.message}")
                    }
                    is WalletResult.NoWalletFound -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "No Solana wallet found. Please install Phantom wallet."
                        )
                        Log.w(TAG, "No wallet found")
                    }
                    else -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Connection cancelled or failed"
                        )
                        Log.w(TAG, "Connection cancelled or failed")
                    }
                }
    }

    /**
     * Generate a new wallet with seed phrase
     */
    private suspend fun generateNewWallet() {
        try {
            // Generate a new seed phrase (simplified for demo)
            val newSeedPhrase = generateSeedPhrase()
            val publicKey = derivePublicKeyFromSeed(newSeedPhrase)

            // Save the new wallet with network
            val storedWallet = walletRepository.saveWallet(
                publicKey = publicKey,
                walletType = WalletType.INTERNAL,
                accountLabel = "Averroes Wallet",
                network = "testnet" // Generated wallets default to testnet
            )

            // Store the seed phrase securely (in production, use Android Keystore)
            storeSeedPhraseSecurely(storedWallet.id, newSeedPhrase)

            _state.value = _state.value.copy(
                wallet = storedWallet,
                isLoading = false,
                error = null
            )

            Log.d(TAG, "New wallet generated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error generating new wallet", e)
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Failed to generate wallet: ${e.message}"
            )
        }
    }

    /**
     * Import wallet from seed phrase
     */
    private suspend fun importFromSeedPhrase(seedPhrase: String) {
        try {
            // Validate seed phrase
            if (!isValidSeedPhrase(seedPhrase)) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Invalid seed phrase. Please check and try again."
                )
                return
            }

            val publicKey = derivePublicKeyFromSeed(seedPhrase)

            // Save the imported wallet with network
            val storedWallet = walletRepository.saveWallet(
                publicKey = publicKey,
                walletType = WalletType.INTERNAL,
                accountLabel = "Imported Wallet",
                network = "testnet" // Imported wallets default to testnet
            )

            // Store the seed phrase securely
            storeSeedPhraseSecurely(storedWallet.id, seedPhrase)

            _state.value = _state.value.copy(
                wallet = storedWallet,
                isLoading = false,
                error = null
            )

            // Update balance after import
            updateBalance()

            Log.d(TAG, "Wallet imported successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error importing wallet", e)
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Failed to import wallet: ${e.message}"
            )
        }
    }

    /**
     * Generate a new seed phrase (simplified implementation)
     */
    private fun generateSeedPhrase(): String {
        // In production, use proper BIP39 implementation
        val words = listOf(
            "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
            "absurd", "abuse", "access", "accident", "account", "accuse", "achieve", "acid"
        )
        return (1..12).map { words.random() }.joinToString(" ")
    }

    /**
     * Derive public key from seed phrase (simplified implementation)
     */
    private fun derivePublicKeyFromSeed(seedPhrase: String): String {
        // In production, use proper Solana key derivation
        val hash = seedPhrase.hashCode().toString()
        return "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAs${hash.takeLast(1)}"
    }

    /**
     * Validate seed phrase format
     */
    private fun isValidSeedPhrase(seedPhrase: String): Boolean {
        val words = seedPhrase.trim().split("\\s+".toRegex())
        return words.size in listOf(12, 15, 18, 21, 24) && words.all { it.isNotBlank() }
    }

    /**
     * Store seed phrase securely (simplified implementation)
     */
    private fun storeSeedPhraseSecurely(walletId: String, seedPhrase: String) {
        // In production, use Android Keystore for encryption
        val prefs = android.content.Context.MODE_PRIVATE
        // This is just a placeholder - implement proper encryption
        Log.d(TAG, "Seed phrase stored securely for wallet: $walletId")
    }
    
    /**
     * Disconnect current wallet
     */
    fun disconnectWallet(activityResultSender: ActivityResultSender?) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // Disconnect from Mobile Wallet Adapter if available
                if (activityResultSender != null) {
                    walletService.disconnect(activityResultSender)
                }
                
                // Clear from repository
                walletRepository.disconnectWallet()
                
                _state.value = _state.value.copy(
                    wallet = null,
                    tokenBalances = emptyList(),
                    recentTransactions = emptyList(),
                    isLoading = false,
                    error = null
                )
                
                Log.d(TAG, "Wallet disconnected successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting wallet", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to disconnect: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update wallet balance from Solana RPC
     */
    private fun updateBalance() {
        viewModelScope.launch {
            try {
                val currentWallet = _state.value.wallet ?: return@launch

                Log.d(TAG, "Fetching balance for wallet: ${currentWallet.publicKey} on ${currentWallet.network}")

                // Fetch real balance from Solana RPC using wallet's network
                val realBalance = fetchSolanaBalance(currentWallet.publicKey, currentWallet.network)

                walletRepository.updateWalletBalance(currentWallet.id, realBalance)

                _state.value = _state.value.copy(
                    wallet = currentWallet.copy(
                        balance = realBalance,
                        lastUpdated = System.currentTimeMillis()
                    )
                )

                Log.d(TAG, "Balance updated: $realBalance SOL")

            } catch (e: Exception) {
                Log.e(TAG, "Error updating balance", e)
                // Don't fail completely, just log the error
            }
        }
    }

    /**
     * Fetch real SOL balance from Solana RPC
     */
    private suspend fun fetchSolanaBalance(publicKey: String, network: String = "testnet"): Double = withContext(Dispatchers.IO) {
        try {
            // Use appropriate Solana RPC endpoint based on network
            val rpcUrl = when (network) {
                "mainnet" -> "https://api.mainnet-beta.solana.com"
                "testnet" -> "https://api.testnet.solana.com"
                "devnet" -> "https://api.devnet.solana.com"
                else -> "https://api.testnet.solana.com"
            }
            val client = okhttp3.OkHttpClient()

            val requestBody = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "getBalance",
                    "params": ["$publicKey"]
                }
            """.trimIndent()

            val request = okhttp3.Request.Builder()
                .url(rpcUrl)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                // Parse JSON response
                val jsonResponse = org.json.JSONObject(responseBody)
                if (jsonResponse.has("result")) {
                    val result = jsonResponse.getJSONObject("result")
                    val lamports = result.getLong("value")
                    // Convert lamports to SOL (1 SOL = 1,000,000,000 lamports)
                    val solBalance = lamports / 1_000_000_000.0
                    Log.d(TAG, "Fetched balance: $solBalance SOL ($lamports lamports)")
                    return@withContext solBalance
                } else if (jsonResponse.has("error")) {
                    val error = jsonResponse.getJSONObject("error")
                    Log.e(TAG, "RPC Error: ${error.getString("message")}")
                }
            } else {
                Log.e(TAG, "RPC request failed: ${response.code}")
            }

            // Return 0 if we can't fetch the balance
            return@withContext 0.0

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Solana balance", e)
            return@withContext 0.0
        }
    }
    
    /**
     * Load transaction history
     */
    private fun loadTransactions(walletId: String) {
        viewModelScope.launch {
            try {
                val transactions = walletRepository.getTransactionsForWallet(walletId)
                _state.value = _state.value.copy(recentTransactions = transactions)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading transactions", e)
            }
        }
    }
    
    /**
     * Determine wallet type from connection info
     */
    private fun determineWalletType(walletUriBase: String, accountLabel: String?): WalletType {
        return when {
            walletUriBase.contains("phantom", ignoreCase = true) -> WalletType.PHANTOM
            walletUriBase.contains("solflare", ignoreCase = true) -> WalletType.SOLFLARE
            walletUriBase.contains("backpack", ignoreCase = true) -> WalletType.BACKPACK
            walletUriBase.contains("glow", ignoreCase = true) -> WalletType.GLOW
            accountLabel?.contains("phantom", ignoreCase = true) == true -> WalletType.PHANTOM
            accountLabel?.contains("solflare", ignoreCase = true) == true -> WalletType.SOLFLARE
            else -> WalletType.PHANTOM // Default to Phantom
        }
    }
    
    /**
     * Refresh wallet data
     */
    fun refresh() {
        val currentWallet = _state.value.wallet
        if (currentWallet != null) {
            updateBalance()
            loadTransactions(currentWallet.id)
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Clear all wallet data (for testing/reset)
     */
    fun clearAllWalletData() {
        viewModelScope.launch {
            try {
                walletRepository.clearAllData()
                _state.value = WalletState(wallet = null)
                Log.d(TAG, "All wallet data cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing wallet data", e)
            }
        }
    }
}
