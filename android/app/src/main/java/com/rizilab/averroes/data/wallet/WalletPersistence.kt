package com.rizilab.averroes.data.wallet

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log

/**
 * Handles persistence of wallet connection data using SharedPreferences
 * Based on the Mobile Wallet Adapter reference implementation
 */
class WalletPersistence(context: Context) {
    
    companion object {
        private const val TAG = "WalletPersistence"
        private const val PREFS_NAME = "wallet_prefs"
        private const val PUBKEY_KEY = "stored_pubkey"
        private const val ACCOUNT_LABEL_KEY = "stored_account_label"
        private const val AUTH_TOKEN_KEY = "stored_auth_token"
        private const val WALLET_URI_BASE_KEY = "stored_wallet_uri_base"
        private const val BALANCE_KEY = "stored_balance"
        private const val NETWORK_KEY = "stored_network"
        private const val CONNECTION_TIME_KEY = "stored_connection_time"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Save wallet connection data to persistent storage
     */
    fun saveConnection(connection: WalletConnectionState.Connected) {
        Log.d(TAG, "Saving wallet connection: ${connection.publicKey}")
        
        sharedPreferences.edit().apply {
            putString(PUBKEY_KEY, connection.publicKey)
            putString(ACCOUNT_LABEL_KEY, connection.accountLabel)
            putString(AUTH_TOKEN_KEY, connection.authToken)
            putString(WALLET_URI_BASE_KEY, connection.walletUriBase?.toString())
            putFloat(BALANCE_KEY, connection.balance)
            putString(NETWORK_KEY, connection.network)
            putLong(CONNECTION_TIME_KEY, System.currentTimeMillis())
        }.apply()
        
        Log.d(TAG, "Wallet connection saved successfully")
    }
    
    /**
     * Load wallet connection data from persistent storage
     */
    fun loadConnection(): WalletConnectionState {
        val publicKey = sharedPreferences.getString(PUBKEY_KEY, null)
        val authToken = sharedPreferences.getString(AUTH_TOKEN_KEY, null)
        
        return if (publicKey.isNullOrEmpty() || authToken.isNullOrEmpty()) {
            Log.d(TAG, "No valid wallet connection found in storage")
            WalletConnectionState.NotConnected
        } else {
            val accountLabel = sharedPreferences.getString(ACCOUNT_LABEL_KEY, "") ?: ""
            val walletUriBase = sharedPreferences.getString(WALLET_URI_BASE_KEY, null)?.let { 
                Uri.parse(it) 
            }
            val balance = sharedPreferences.getFloat(BALANCE_KEY, 0f)
            val network = sharedPreferences.getString(NETWORK_KEY, "devnet") ?: "devnet"
            val connectionTime = sharedPreferences.getLong(CONNECTION_TIME_KEY, 0)
            
            Log.d(TAG, "Loaded wallet connection: $publicKey")
            
            WalletConnectionState.Connected(
                publicKey = publicKey,
                accountLabel = accountLabel,
                authToken = authToken,
                walletUriBase = walletUriBase?.toString() ?: "",
                balance = balance,
                network = network,
                isConnected = true
            )
        }
    }
    
    /**
     * Clear all wallet connection data
     */
    fun clearConnection() {
        Log.d(TAG, "Clearing wallet connection data")
        
        sharedPreferences.edit().apply {
            remove(PUBKEY_KEY)
            remove(ACCOUNT_LABEL_KEY)
            remove(AUTH_TOKEN_KEY)
            remove(WALLET_URI_BASE_KEY)
            remove(BALANCE_KEY)
            remove(NETWORK_KEY)
            remove(CONNECTION_TIME_KEY)
        }.apply()
        
        Log.d(TAG, "Wallet connection data cleared")
    }
    
    /**
     * Update wallet balance
     */
    fun updateBalance(balance: Float) {
        sharedPreferences.edit().apply {
            putFloat(BALANCE_KEY, balance)
        }.apply()
        
        Log.d(TAG, "Updated wallet balance: $balance")
    }
    
    /**
     * Check if we have a valid stored connection
     */
    fun hasValidConnection(): Boolean {
        val publicKey = sharedPreferences.getString(PUBKEY_KEY, null)
        val authToken = sharedPreferences.getString(AUTH_TOKEN_KEY, null)
        return !publicKey.isNullOrEmpty() && !authToken.isNullOrEmpty()
    }
    
    /**
     * Get connection age in milliseconds
     */
    fun getConnectionAge(): Long {
        val connectionTime = sharedPreferences.getLong(CONNECTION_TIME_KEY, 0)
        return if (connectionTime > 0) {
            System.currentTimeMillis() - connectionTime
        } else {
            Long.MAX_VALUE
        }
    }
}
