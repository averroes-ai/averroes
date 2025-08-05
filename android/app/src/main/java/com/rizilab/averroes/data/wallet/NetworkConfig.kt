package com.rizilab.averroes.data.wallet

import com.rizilab.averroes.BuildConfig
import com.solana.mobilewalletadapter.clientlib.RpcCluster

/**
 * Network configuration utility for Solana networks
 * Automatically selects network based on build type:
 * - Debug builds: Devnet
 * - Release builds: Mainnet
 */
object NetworkConfig {
    
    /**
     * Get the default RPC cluster based on build configuration
     */
    fun getDefaultRpcCluster(): RpcCluster {
        return when (BuildConfig.SOLANA_NETWORK) {
            "mainnet" -> RpcCluster.MainnetBeta
            "testnet" -> RpcCluster.Testnet
            "devnet" -> RpcCluster.Devnet
            else -> RpcCluster.Devnet // Default fallback
        }
    }
    
    /**
     * Get the RPC URL for the current network
     */
    fun getRpcUrl(): String {
        return BuildConfig.RPC_URL
    }
    
    /**
     * Get network display name
     */
    fun getNetworkDisplayName(): String {
        return when (BuildConfig.SOLANA_NETWORK) {
            "mainnet" -> "Mainnet Beta"
            "testnet" -> "Testnet"
            "devnet" -> "Devnet"
            else -> "Devnet"
        }
    }
    
    /**
     * Convert user selection to RpcCluster
     */
    fun getRpcClusterFromString(network: String): RpcCluster {
        return when (network.lowercase()) {
            "mainnet", "mainnet-beta" -> RpcCluster.MainnetBeta
            "testnet" -> RpcCluster.Testnet
            "devnet" -> RpcCluster.Devnet
            else -> getDefaultRpcCluster() // Use build-based default
        }
    }
    
    /**
     * Check if current build is using devnet
     */
    fun isDevnet(): Boolean {
        return BuildConfig.SOLANA_NETWORK == "devnet"
    }
    
    /**
     * Check if current build is using mainnet
     */
    fun isMainnet(): Boolean {
        return BuildConfig.SOLANA_NETWORK == "mainnet"
    }
}
