package com.rizilab.averroes.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * CoinGecko API response models
 */
data class CoinGeckoCrypto(
    @SerializedName("id") val id: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String?,
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("market_cap") val marketCap: Long?,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    @SerializedName("fully_diluted_valuation") val fullyDilutedValuation: Long?,
    @SerializedName("total_volume") val totalVolume: Long?,
    @SerializedName("high_24h") val high24h: Double?,
    @SerializedName("low_24h") val low24h: Double?,
    @SerializedName("price_change_24h") val priceChange24h: Double?,
    @SerializedName("price_change_percentage_24h") val priceChangePercentage24h: Double?,
    @SerializedName("market_cap_change_24h") val marketCapChange24h: Double?,
    @SerializedName("market_cap_change_percentage_24h") val marketCapChangePercentage24h: Double?,
    @SerializedName("circulating_supply") val circulatingSupply: Double?,
    @SerializedName("total_supply") val totalSupply: Double?,
    @SerializedName("max_supply") val maxSupply: Double?,
    @SerializedName("ath") val ath: Double?,
    @SerializedName("ath_change_percentage") val athChangePercentage: Double?,
    @SerializedName("ath_date") val athDate: String?,
    @SerializedName("atl") val atl: Double?,
    @SerializedName("atl_change_percentage") val atlChangePercentage: Double?,
    @SerializedName("atl_date") val atlDate: String?,
    @SerializedName("roi") val roi: Any?,
    @SerializedName("last_updated") val lastUpdated: String?
)

/**
 * CoinGecko API interface
 * Free tier allows 10-30 calls per minute
 * No API key required for basic endpoints
 */
interface CoinGeckoApi {
    
    /**
     * Get cryptocurrency market data
     * Free endpoint - no API key required
     */
    @GET("coins/markets")
    suspend fun getMarketData(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("ids") ids: String? = null,
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false,
        @Query("price_change_percentage") priceChangePercentage: String = "24h"
    ): Response<List<CoinGeckoCrypto>>

    /**
     * Get specific cryptocurrencies by IDs
     */
    @GET("coins/markets")
    suspend fun getCryptocurrenciesByIds(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("ids") ids: String,
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 250,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false,
        @Query("price_change_percentage") priceChangePercentage: String = "24h"
    ): Response<List<CoinGeckoCrypto>>

    /**
     * Search for cryptocurrencies
     */
    @GET("search")
    suspend fun searchCryptocurrencies(
        @Query("query") query: String
    ): Response<SearchResponse>

    /**
     * Get trending cryptocurrencies
     */
    @GET("search/trending")
    suspend fun getTrendingCryptocurrencies(): Response<TrendingResponse>
}

/**
 * Search response model
 */
data class SearchResponse(
    @SerializedName("coins") val coins: List<SearchCoin>
)

data class SearchCoin(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    @SerializedName("thumb") val thumb: String?,
    @SerializedName("large") val large: String?
)

/**
 * Trending response model
 */
data class TrendingResponse(
    @SerializedName("coins") val coins: List<TrendingCoin>
)

data class TrendingCoin(
    @SerializedName("item") val item: TrendingCoinItem
)

data class TrendingCoinItem(
    @SerializedName("id") val id: String,
    @SerializedName("coin_id") val coinId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    @SerializedName("thumb") val thumb: String?,
    @SerializedName("small") val small: String?,
    @SerializedName("large") val large: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("price_btc") val priceBtc: Double?,
    @SerializedName("score") val score: Int?
)

/**
 * Predefined list of halal cryptocurrency IDs for CoinGecko API
 * Based on Sharlife.my halal crypto list
 */
object HalalCryptoIds {
    val halalIds = listOf(
        "bitcoin",
        "ethereum", 
        "cardano",
        "solana",
        "polkadot",
        "chainlink",
        "polygon",
        "avalanche-2",
        "cosmos",
        "algorand",
        "stellar",
        "vechain",
        "tezos",
        "hedera-hashgraph",
        "iota",
        "zilliqa",
        "nano",
        "elrond-erd-2",
        "near",
        "fantom"
    )
    
    val questionableIds = listOf(
        "ripple",
        "litecoin",
        "dogecoin"
    )
    
    val haramIds = listOf(
        "binancecoin",
        "usd-coin",
        "tether"
    )
    
    val allIds = halalIds + questionableIds + haramIds
    
    fun getIdsString(): String = allIds.joinToString(",")
    fun getHalalIdsString(): String = halalIds.joinToString(",")
    fun getQuestionableIdsString(): String = questionableIds.joinToString(",")
    fun getHaramIdsString(): String = haramIds.joinToString(",")
}
