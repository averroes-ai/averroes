package com.rizilab.averroes.data.model

// TODO: Add kotlinx.serialization dependency when needed
// import kotlinx.serialization.Serializable

/**
 * Halal cryptocurrency data model
 */
// @Serializable // TODO: Enable when kotlinx.serialization is added
data class HalalCrypto(
    val id: String,
    val symbol: String,
    val name: String,
    val currentPrice: Double = 0.0,
    val priceChange24h: Double = 0.0,
    val marketCap: Long = 0L,
    val volume24h: Long = 0L,
    val halalStatus: HalalStatus,
    val halalReason: String,
    val imageUrl: String? = null,
    val rank: Int = 0
)

/**
 * Halal status enum
 */
enum class HalalStatus {
    HALAL,
    HARAM,
    QUESTIONABLE,
    UNDER_REVIEW
}

/**
 * Predefined list of halal cryptocurrencies based on Sharlife.my
 */
object HalalCryptoList {
    val halalCryptos = listOf(
        HalalCrypto("bitcoin", "BTC", "Bitcoin", halalStatus = HalalStatus.HALAL, 
            halalReason = "Decentralized digital currency with no interest-based mechanisms", rank = 1),
        HalalCrypto("ethereum", "ETH", "Ethereum", halalStatus = HalalStatus.HALAL,
            halalReason = "Decentralized platform for smart contracts with legitimate utility", rank = 2),
        HalalCrypto("cardano", "ADA", "Cardano", halalStatus = HalalStatus.HALAL,
            halalReason = "Research-driven blockchain platform with academic approach", rank = 3),
        HalalCrypto("solana", "SOL", "Solana", halalStatus = HalalStatus.HALAL,
            halalReason = "High-performance blockchain for decentralized applications", rank = 4),
        HalalCrypto("polkadot", "DOT", "Polkadot", halalStatus = HalalStatus.HALAL,
            halalReason = "Multi-chain protocol enabling blockchain interoperability", rank = 5),
        HalalCrypto("chainlink", "LINK", "Chainlink", halalStatus = HalalStatus.HALAL,
            halalReason = "Decentralized oracle network providing real-world data", rank = 6),
        HalalCrypto("polygon", "MATIC", "Polygon", halalStatus = HalalStatus.HALAL,
            halalReason = "Scaling solution for Ethereum with legitimate utility", rank = 7),
        HalalCrypto("avalanche-2", "AVAX", "Avalanche", halalStatus = HalalStatus.HALAL,
            halalReason = "Platform for decentralized applications and custom blockchains", rank = 8),
        HalalCrypto("cosmos", "ATOM", "Cosmos", halalStatus = HalalStatus.HALAL,
            halalReason = "Internet of blockchains enabling interoperability", rank = 9),
        HalalCrypto("algorand", "ALGO", "Algorand", halalStatus = HalalStatus.HALAL,
            halalReason = "Pure proof-of-stake blockchain with sustainable consensus", rank = 10),
        HalalCrypto("stellar", "XLM", "Stellar", halalStatus = HalalStatus.HALAL,
            halalReason = "Payment network for cross-border transactions", rank = 11),
        HalalCrypto("vechain", "VET", "VeChain", halalStatus = HalalStatus.HALAL,
            halalReason = "Supply chain management and business processes", rank = 12),
        HalalCrypto("tezos", "XTZ", "Tezos", halalStatus = HalalStatus.HALAL,
            halalReason = "Self-amending blockchain with on-chain governance", rank = 13),
        HalalCrypto("hedera-hashgraph", "HBAR", "Hedera", halalStatus = HalalStatus.HALAL,
            halalReason = "Enterprise-grade distributed ledger technology", rank = 14),
        HalalCrypto("iota", "MIOTA", "IOTA", halalStatus = HalalStatus.HALAL,
            halalReason = "Distributed ledger for Internet of Things", rank = 15),
        HalalCrypto("zilliqa", "ZIL", "Zilliqa", halalStatus = HalalStatus.HALAL,
            halalReason = "High-throughput blockchain with sharding", rank = 16),
        HalalCrypto("nano", "XNO", "Nano", halalStatus = HalalStatus.HALAL,
            halalReason = "Feeless, instant digital currency", rank = 17),
        HalalCrypto("elrond-erd-2", "EGLD", "MultiversX", halalStatus = HalalStatus.HALAL,
            halalReason = "Internet-scale blockchain for new economy", rank = 18),
        HalalCrypto("near", "NEAR", "NEAR Protocol", halalStatus = HalalStatus.HALAL,
            halalReason = "Developer-friendly blockchain with sharding", rank = 19),
        HalalCrypto("fantom", "FTM", "Fantom", halalStatus = HalalStatus.HALAL,
            halalReason = "High-performance smart contract platform", rank = 20),
        
        // Questionable cryptos
        HalalCrypto("ripple", "XRP", "XRP", halalStatus = HalalStatus.QUESTIONABLE,
            halalReason = "Centralized cryptocurrency working with traditional banking", rank = 21),
        HalalCrypto("litecoin", "LTC", "Litecoin", halalStatus = HalalStatus.QUESTIONABLE,
            halalReason = "Bitcoin fork with similar properties but less established", rank = 22),
        HalalCrypto("dogecoin", "DOGE", "Dogecoin", halalStatus = HalalStatus.QUESTIONABLE,
            halalReason = "Meme-based cryptocurrency with speculative nature", rank = 23),
        
        // Haram cryptos
        HalalCrypto("binancecoin", "BNB", "BNB", halalStatus = HalalStatus.HARAM,
            halalReason = "Exchange token facilitating interest-based lending and margin trading", rank = 24),
        HalalCrypto("usd-coin", "USDC", "USD Coin", halalStatus = HalalStatus.HARAM,
            halalReason = "Stablecoin backed by interest-bearing assets", rank = 25),
        HalalCrypto("tether", "USDT", "Tether", halalStatus = HalalStatus.HARAM,
            halalReason = "Stablecoin with questionable backing and interest mechanisms", rank = 26)
    )
    
    fun getHalalOnly() = halalCryptos.filter { it.halalStatus == HalalStatus.HALAL }
    fun getByStatus(status: HalalStatus) = halalCryptos.filter { it.halalStatus == status }
}
