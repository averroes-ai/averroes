package com.rizilab.averroes.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.rizilab.averroes.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Repository for managing wallet data and storage
 */
class WalletRepository(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "averroes_wallet_prefs"
        private const val KEY_CURRENT_WALLET = "current_wallet"
        private const val KEY_WALLET_LIST = "wallet_list"
        private const val KEY_TRANSACTIONS = "transactions"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Save wallet information after connection
     */
    suspend fun saveWallet(
        publicKey: String,
        walletType: WalletType,
        accountLabel: String? = null,
        network: String = "testnet"
    ): StoredWallet = withContext(Dispatchers.IO) {
        val walletId = UUID.randomUUID().toString()
        val wallet = StoredWallet(
            id = walletId,
            name = accountLabel ?: "${walletType.name} Wallet",
            publicKey = publicKey,
            walletType = walletType,
            isConnected = true,
            balance = 0.0, // Will be updated when we fetch balance
            network = network,
            lastUpdated = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        
        // Save as current wallet
        prefs.edit()
            .putString(KEY_CURRENT_WALLET, json.encodeToString(wallet))
            .apply()
        
        // Add to wallet list
        val walletList = getStoredWallets().toMutableList()
        walletList.removeAll { it.publicKey == publicKey } // Remove duplicates
        walletList.add(wallet)
        
        prefs.edit()
            .putString(KEY_WALLET_LIST, json.encodeToString(walletList))
            .apply()
        
        wallet
    }
    
    /**
     * Get current connected wallet
     */
    suspend fun getCurrentWallet(): StoredWallet? = withContext(Dispatchers.IO) {
        val walletJson = prefs.getString(KEY_CURRENT_WALLET, null)
        walletJson?.let { 
            try {
                json.decodeFromString<StoredWallet>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get all stored wallets
     */
    suspend fun getStoredWallets(): List<StoredWallet> = withContext(Dispatchers.IO) {
        val walletsJson = prefs.getString(KEY_WALLET_LIST, null)
        walletsJson?.let {
            try {
                json.decodeFromString<List<StoredWallet>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
    
    /**
     * Update wallet balance
     */
    suspend fun updateWalletBalance(walletId: String, balance: Double) = withContext(Dispatchers.IO) {
        val currentWallet = getCurrentWallet()
        if (currentWallet?.id == walletId) {
            val updatedWallet = currentWallet.copy(
                balance = balance,
                lastUpdated = System.currentTimeMillis()
            )
            prefs.edit()
                .putString(KEY_CURRENT_WALLET, json.encodeToString(updatedWallet))
                .apply()
        }
        
        // Update in wallet list too
        val walletList = getStoredWallets().toMutableList()
        val index = walletList.indexOfFirst { it.id == walletId }
        if (index != -1) {
            walletList[index] = walletList[index].copy(
                balance = balance,
                lastUpdated = System.currentTimeMillis()
            )
            prefs.edit()
                .putString(KEY_WALLET_LIST, json.encodeToString(walletList))
                .apply()
        }
    }
    
    /**
     * Disconnect current wallet
     */
    suspend fun disconnectWallet() = withContext(Dispatchers.IO) {
        val currentWallet = getCurrentWallet()
        if (currentWallet != null) {
            val disconnectedWallet = currentWallet.copy(
                isConnected = false,
                lastUpdated = System.currentTimeMillis()
            )
            
            // Update in wallet list
            val walletList = getStoredWallets().toMutableList()
            val index = walletList.indexOfFirst { it.id == currentWallet.id }
            if (index != -1) {
                walletList[index] = disconnectedWallet
                prefs.edit()
                    .putString(KEY_WALLET_LIST, json.encodeToString(walletList))
                    .apply()
            }
        }
        
        // Clear current wallet
        prefs.edit()
            .remove(KEY_CURRENT_WALLET)
            .apply()
    }
    
    /**
     * Save transaction record
     */
    suspend fun saveTransaction(transaction: WalletTransaction) = withContext(Dispatchers.IO) {
        val transactions = getTransactions().toMutableList()
        transactions.add(0, transaction) // Add to beginning
        
        // Keep only last 100 transactions
        if (transactions.size > 100) {
            transactions.subList(100, transactions.size).clear()
        }
        
        prefs.edit()
            .putString(KEY_TRANSACTIONS, json.encodeToString(transactions))
            .apply()
    }
    
    /**
     * Get transaction history
     */
    suspend fun getTransactions(): List<WalletTransaction> = withContext(Dispatchers.IO) {
        val transactionsJson = prefs.getString(KEY_TRANSACTIONS, null)
        transactionsJson?.let {
            try {
                json.decodeFromString<List<WalletTransaction>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
    
    /**
     * Get transactions for specific wallet
     */
    suspend fun getTransactionsForWallet(walletId: String): List<WalletTransaction> = withContext(Dispatchers.IO) {
        getTransactions().filter { it.walletId == walletId }
    }
    
    /**
     * Clear all wallet data (for testing/reset)
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}
