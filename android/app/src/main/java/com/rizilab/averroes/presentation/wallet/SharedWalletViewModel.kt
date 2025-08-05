package com.rizilab.averroes.presentation.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizilab.averroes.data.wallet.WalletConnectionState
import com.rizilab.averroes.data.wallet.WalletPersistence
import com.rizilab.averroes.data.wallet.WalletService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for wallet state across the entire app
 * Manages wallet connection persistence and state synchronization
 */
class SharedWalletViewModel(
    private val walletPersistence: WalletPersistence,
    private val walletService: WalletService
) : ViewModel() {
    
    companion object {
        private const val TAG = "SharedWalletViewModel"
    }
    
    private val _walletState = MutableStateFlow<WalletConnectionState>(WalletConnectionState.NotConnected)
    val walletState: StateFlow<WalletConnectionState> = _walletState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        // Load persisted wallet connection on initialization
        loadPersistedConnection()
    }
    
    /**
     * Load wallet connection from persistence
     */
    private fun loadPersistedConnection() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val connection = walletPersistence.loadConnection()
                _walletState.value = connection
                
                Log.d(TAG, "Loaded wallet connection: ${connection::class.simpleName}")
                
                if (connection is WalletConnectionState.Connected) {
                    Log.d(TAG, "Wallet connected: ${connection.accountLabel} (${connection.publicKey.take(8)}...)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading persisted connection", e)
                _walletState.value = WalletConnectionState.NotConnected
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update wallet connection state
     */
    fun updateWalletConnection(connection: WalletConnectionState) {
        viewModelScope.launch {
            _walletState.value = connection
            
            if (connection is WalletConnectionState.Connected) {
                // Persist the connection
                walletPersistence.saveConnection(connection)
                Log.d(TAG, "Wallet connection updated and persisted")
            } else {
                // Clear persistence if disconnected
                walletPersistence.clearConnection()
                Log.d(TAG, "Wallet disconnected and persistence cleared")
            }
        }
    }
    
    /**
     * Refresh wallet connection from service
     */
    fun refreshConnection() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                walletService.refreshFromPersistence()
                val currentConnection = walletService.getCurrentConnection()
                _walletState.value = currentConnection
                
                Log.d(TAG, "Refreshed wallet connection: ${currentConnection::class.simpleName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing connection", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear wallet connection
     */
    fun clearConnection() {
        viewModelScope.launch {
            walletPersistence.clearConnection()
            _walletState.value = WalletConnectionState.NotConnected
            Log.d(TAG, "Wallet connection cleared")
        }
    }
    
    /**
     * Check if wallet is connected
     */
    fun isConnected(): Boolean {
        return _walletState.value is WalletConnectionState.Connected
    }
    
    /**
     * Get current wallet connection if connected
     */
    fun getConnectedWallet(): WalletConnectionState.Connected? {
        return _walletState.value as? WalletConnectionState.Connected
    }
    
    /**
     * Update wallet balance
     */
    fun updateBalance(balance: Float) {
        viewModelScope.launch {
            val currentState = _walletState.value
            if (currentState is WalletConnectionState.Connected) {
                val updatedState = currentState.copy(balance = balance)
                _walletState.value = updatedState
                walletPersistence.updateBalance(balance)
                Log.d(TAG, "Updated wallet balance: $balance")
            }
        }
    }
}
