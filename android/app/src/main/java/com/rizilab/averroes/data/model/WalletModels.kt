package com.rizilab.averroes.data.model

import kotlinx.serialization.Serializable
import java.util.Date

/**
 * Wallet information stored locally
 */
@Serializable
data class StoredWallet(
    val id: String,
    val name: String,
    val publicKey: String,
    val walletType: WalletType,
    val isConnected: Boolean = false,
    val balance: Double = 0.0,
    val network: String = "testnet", // "mainnet", "testnet", "devnet"
    val lastUpdated: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Wallet types supported by the app
 */
enum class WalletType {
    PHANTOM,
    SOLFLARE,
    BACKPACK,
    GLOW,
    INTERNAL // Our own managed wallet
}

/**
 * Transaction record
 */
@Serializable
data class WalletTransaction(
    val id: String,
    val walletId: String,
    val type: TransactionType,
    val amount: Double,
    val tokenSymbol: String = "SOL",
    val toAddress: String? = null,
    val fromAddress: String? = null,
    val signature: String? = null,
    val status: TransactionStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val fee: Double = 0.0,
    val memo: String? = null
)

/**
 * Transaction types
 */
enum class TransactionType {
    SEND,
    RECEIVE,
    SWAP,
    BUY,
    SELL,
    STAKE,
    UNSTAKE
}

/**
 * Transaction status
 */
enum class TransactionStatus {
    PENDING,
    CONFIRMED,
    FAILED,
    CANCELLED
}

/**
 * Token balance in wallet
 */
@Serializable
data class TokenBalance(
    val mintAddress: String,
    val symbol: String,
    val name: String,
    val balance: Double,
    val decimals: Int,
    val usdValue: Double? = null,
    val logoUri: String? = null
)

/**
 * Complete wallet state
 */
data class WalletState(
    val wallet: StoredWallet?,
    val tokenBalances: List<TokenBalance> = emptyList(),
    val recentTransactions: List<WalletTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Wallet connection result
 */
sealed class WalletConnectionResult {
    data class Success(val wallet: StoredWallet) : WalletConnectionResult()
    data class Error(val message: String) : WalletConnectionResult()
    object NoWalletFound : WalletConnectionResult()
    object UserCancelled : WalletConnectionResult()
}
