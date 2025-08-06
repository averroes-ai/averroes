package com.rizilab.averroes.data.repository

import com.rizilab.averroes.data.api.ApiClient
import com.rizilab.averroes.data.api.ApiResult
import com.rizilab.averroes.data.api.CoinGeckoCrypto
import com.rizilab.averroes.data.api.HalalCryptoIds
import com.rizilab.averroes.data.api.safeApiCall
import com.rizilab.averroes.data.model.HalalCrypto
import com.rizilab.averroes.data.model.HalalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for cryptocurrency data
 */
class CryptoRepository {
    private val api = ApiClient.coinGeckoApi
    
    /**
     * Get halal cryptocurrencies with real market data
     */
    suspend fun getHalalCryptocurrencies(): ApiResult<List<HalalCrypto>> = withContext(Dispatchers.IO) {
        when (val result = safeApiCall { api.getCryptocurrenciesByIds(ids = HalalCryptoIds.getIdsString()) }) {
            is ApiResult.Success -> {
                val halalCryptos = result.data.mapNotNull { coinGeckoCrypto ->
                    mapCoinGeckoToHalalCrypto(coinGeckoCrypto)
                }.sortedBy { it.rank }
                
                ApiResult.Success(halalCryptos)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
            is ApiResult.Loading -> result
        }
    }
    
    /**
     * Get cryptocurrencies by halal status
     */
    suspend fun getCryptocurrenciesByStatus(status: HalalStatus): ApiResult<List<HalalCrypto>> = withContext(Dispatchers.IO) {
        val ids = when (status) {
            HalalStatus.HALAL -> HalalCryptoIds.getHalalIdsString()
            HalalStatus.QUESTIONABLE -> HalalCryptoIds.getQuestionableIdsString()
            HalalStatus.HARAM -> HalalCryptoIds.getHaramIdsString()
            HalalStatus.UNDER_REVIEW -> "" // No specific IDs for under review
        }
        
        if (ids.isEmpty()) {
            return@withContext ApiResult.Success(emptyList())
        }
        
        when (val result = safeApiCall { api.getCryptocurrenciesByIds(ids = ids) }) {
            is ApiResult.Success -> {
                val halalCryptos = result.data.mapNotNull { coinGeckoCrypto ->
                    mapCoinGeckoToHalalCrypto(coinGeckoCrypto)
                }.filter { it.halalStatus == status }
                .sortedBy { it.rank }
                
                ApiResult.Success(halalCryptos)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
            is ApiResult.Loading -> result
        }
    }
    
    /**
     * Search cryptocurrencies
     */
    suspend fun searchCryptocurrencies(query: String): ApiResult<List<HalalCrypto>> = withContext(Dispatchers.IO) {
        // First get all halal cryptos, then filter by search query
        when (val result = getHalalCryptocurrencies()) {
            is ApiResult.Success -> {
                val filteredCryptos = result.data.filter { crypto ->
                    crypto.name.contains(query, ignoreCase = true) ||
                    crypto.symbol.contains(query, ignoreCase = true)
                }
                ApiResult.Success(filteredCryptos)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
            is ApiResult.Loading -> result
        }
    }
    
    /**
     * Get specific cryptocurrency by ID
     */
    suspend fun getCryptocurrency(id: String): ApiResult<HalalCrypto?> = withContext(Dispatchers.IO) {
        when (val result = safeApiCall { api.getCryptocurrenciesByIds(ids = id) }) {
            is ApiResult.Success -> {
                val crypto = result.data.firstOrNull()?.let { mapCoinGeckoToHalalCrypto(it) }
                ApiResult.Success(crypto)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
            is ApiResult.Loading -> result
        }
    }
    
    /**
     * Map CoinGecko API response to HalalCrypto model
     */
    private fun mapCoinGeckoToHalalCrypto(coinGeckoCrypto: CoinGeckoCrypto): HalalCrypto? {
        val halalStatus = getHalalStatus(coinGeckoCrypto.id)
        val halalReason = getHalalReason(coinGeckoCrypto.id, halalStatus)
        
        return HalalCrypto(
            id = coinGeckoCrypto.id,
            symbol = coinGeckoCrypto.symbol.uppercase(),
            name = coinGeckoCrypto.name,
            currentPrice = coinGeckoCrypto.currentPrice ?: 0.0,
            priceChange24h = coinGeckoCrypto.priceChangePercentage24h ?: 0.0,
            marketCap = coinGeckoCrypto.marketCap ?: 0L,
            volume24h = coinGeckoCrypto.totalVolume ?: 0L,
            halalStatus = halalStatus,
            halalReason = halalReason,
            imageUrl = coinGeckoCrypto.image,
            rank = coinGeckoCrypto.marketCapRank ?: 999
        )
    }
    
    /**
     * Determine halal status based on cryptocurrency ID
     */
    private fun getHalalStatus(id: String): HalalStatus {
        return when (id) {
            in HalalCryptoIds.halalIds -> HalalStatus.HALAL
            in HalalCryptoIds.questionableIds -> HalalStatus.QUESTIONABLE
            in HalalCryptoIds.haramIds -> HalalStatus.HARAM
            else -> HalalStatus.UNDER_REVIEW
        }
    }
    
    /**
     * Get halal reasoning based on cryptocurrency ID and status
     */
    private fun getHalalReason(id: String, status: HalalStatus): String {
        return when (id) {
            "bitcoin" -> "Decentralized digital currency with no interest-based mechanisms"
            "ethereum" -> "Decentralized platform for smart contracts with legitimate utility"
            "cardano" -> "Research-driven blockchain platform with academic approach"
            "solana" -> "High-performance blockchain for decentralized applications"
            "polkadot" -> "Multi-chain protocol enabling blockchain interoperability"
            "chainlink" -> "Decentralized oracle network providing real-world data"
            "polygon" -> "Scaling solution for Ethereum with legitimate utility"
            "avalanche-2" -> "Platform for decentralized applications and custom blockchains"
            "cosmos" -> "Internet of blockchains enabling interoperability"
            "algorand" -> "Pure proof-of-stake blockchain with sustainable consensus"
            "stellar" -> "Payment network for cross-border transactions"
            "vechain" -> "Supply chain management and business processes"
            "tezos" -> "Self-amending blockchain with on-chain governance"
            "hedera-hashgraph" -> "Enterprise-grade distributed ledger technology"
            "iota" -> "Distributed ledger for Internet of Things"
            "zilliqa" -> "High-throughput blockchain with sharding"
            "nano" -> "Feeless, instant digital currency"
            "elrond-erd-2" -> "Internet-scale blockchain for new economy"
            "near" -> "Developer-friendly blockchain with sharding"
            "fantom" -> "High-performance smart contract platform"
            
            // Questionable
            "ripple" -> "Centralized cryptocurrency working with traditional banking"
            "litecoin" -> "Bitcoin fork with similar properties but less established"
            "dogecoin" -> "Meme-based cryptocurrency with speculative nature"
            
            // Haram
            "binancecoin" -> "Exchange token facilitating interest-based lending and margin trading"
            "usd-coin" -> "Stablecoin backed by interest-bearing assets"
            "tether" -> "Stablecoin with questionable backing and interest mechanisms"
            
            else -> when (status) {
                HalalStatus.HALAL -> "Generally compliant with Islamic finance principles"
                HalalStatus.QUESTIONABLE -> "Requires further analysis for Islamic compliance"
                HalalStatus.HARAM -> "Contains elements prohibited in Islamic finance"
                HalalStatus.UNDER_REVIEW -> "Currently under review by Islamic finance scholars"
            }
        }
    }
}
