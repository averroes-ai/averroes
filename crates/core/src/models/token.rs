use chrono::DateTime;
use chrono::Utc;
use serde::Deserialize;
use serde::Serialize;
use solana_pubkey::Pubkey;

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Enum)]
pub enum TokenStandard {
    SPL,
    ERC20,
    Other {
        name: String,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Enum)]
pub enum BlockchainNetwork {
    Solana,
    Ethereum,
    BSC,
    Polygon,
    Other {
        name: String,
    },
}

/// Universal token metadata that works across different blockchains
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct TokenMetadata {
    pub name: String,
    pub symbol: String,
    pub contract_address: String, // Renamed from mint_address for chain-agnostic use
    pub decimals: u32,
    pub description: Option<String>,
    pub image_url: Option<String>,
    pub creator: Option<String>,
    pub verified: bool,
    pub token_standard: TokenStandard,
    pub blockchain: BlockchainNetwork, // NEW: Identifies the source chain
}

impl TokenMetadata {
    pub fn new(
        name: String,
        symbol: String,
        contract_address: String,
        decimals: u32,
        blockchain: BlockchainNetwork,
    ) -> Self {
        Self {
            name,
            symbol,
            contract_address,
            decimals,
            description: None,
            image_url: None,
            creator: None,
            verified: false,
            token_standard: match blockchain {
                BlockchainNetwork::Solana => TokenStandard::SPL,
                BlockchainNetwork::Ethereum | BlockchainNetwork::BSC | BlockchainNetwork::Polygon => {
                    TokenStandard::ERC20
                },
                BlockchainNetwork::Other {
                    ..
                } => TokenStandard::Other {
                    name: "Unknown".to_owned(),
                },
            },
            blockchain,
        }
    }
}

/// Token price and market data
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct TokenPriceData {
    pub price_usd: f64,
    pub price_change_24h: f64,
    pub volume_24h: u64,
    pub market_cap: u64,
    pub total_supply: Option<f64>, // NEW: Added for comprehensive market data
    pub last_updated: u64,         // Unix timestamp for UniFFI
}

/// Liquidity pool information
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct LiquidityPool {
    pub dex_name: String,
    pub pool_address: String,
    pub liquidity_usd: f64,
    pub volume_24h: f64,
}

/// Universal token information that works across different blockchains
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct UniversalTokenInfo {
    pub address: String, // Contract/mint address as String for UniFFI
    pub metadata: TokenMetadata,
    pub price_data: Option<TokenPriceData>,
    pub holders: Option<u64>,
    pub liquidity_pools: Vec<LiquidityPool>,
    pub is_verified: bool,
    pub risk_score: Option<f64>,
    pub blockchain: BlockchainNetwork, // NEW: Explicit chain identification
}

/// Solana-specific token information (for backward compatibility and Solana-specific features)
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct SolanaTokenInfo {
    pub pubkey: String, // Pubkey as String for UniFFI
    pub metadata: TokenMetadata,
    pub price_data: Option<TokenPriceData>,
    pub holders: Option<u64>,
    pub liquidity_pools: Vec<LiquidityPool>,
    pub is_verified: bool,
    pub risk_score: Option<f64>,
}

// Internal structs (not exposed to UniFFI)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenMetadataInternal {
    pub mint_address: String,
    pub name: String,
    pub symbol: String,
    pub decimals: u8,
    pub total_supply: Option<u64>,
    pub token_standard: TokenStandard,
    pub logo_uri: Option<String>,
    pub description: Option<String>,
    pub website: Option<String>,
    pub twitter: Option<String>,
    pub telegram: Option<String>,
    pub discord: Option<String>,
    pub coingecko_id: Option<String>,
    pub coinmarketcap_id: Option<String>,
    pub created_at: DateTime<Utc>,
    pub last_updated: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenPriceDataInternal {
    pub mint_address: String,
    pub price_usd: f64,
    pub price_change_24h: f64,
    pub volume_24h: f64,
    pub market_cap: Option<f64>,
    pub circulating_supply: Option<f64>,
    pub fully_diluted_valuation: Option<f64>,
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenHolders {
    pub mint_address: String,
    pub total_holders: u32,
    pub top_10_holders_percentage: f64,
    pub distribution_score: f64, // 0-100, higher is better distribution
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]

pub struct SolanaTokenInfoInternal {
    pub pubkey: Pubkey,
    pub metadata: TokenMetadataInternal,
    pub price_data: Option<TokenPriceDataInternal>,
    pub holders: Option<TokenHolders>,
    pub liquidity_pools: Vec<String>, // Pool addresses
    pub is_verified: bool,
    pub risk_score: Option<f64>, // 0-100, lower is safer
}

impl TokenMetadataInternal {
    pub fn new(
        mint_address: String,
        name: String,
        symbol: String,
        decimals: u8,
    ) -> Self {
        let now = Utc::now();
        Self {
            mint_address,
            name,
            symbol,
            decimals,
            total_supply: None,
            token_standard: TokenStandard::SPL,
            logo_uri: None,
            description: None,
            website: None,
            twitter: None,
            telegram: None,
            discord: None,
            coingecko_id: None,
            coinmarketcap_id: None,
            created_at: now,
            last_updated: now,
        }
    }
}

impl SolanaTokenInfoInternal {
    pub fn from_metadata(metadata: TokenMetadataInternal) -> Self {
        let pubkey = metadata.mint_address.parse::<Pubkey>().unwrap_or_else(|_| Pubkey::default());

        Self {
            pubkey,
            metadata,
            price_data: None,
            holders: None,
            liquidity_pools: Vec::new(),
            is_verified: false,
            risk_score: None,
        }
    }
}

impl SolanaTokenInfo {
    pub fn from_pubkey(pubkey: Pubkey) -> Self {
        Self {
            pubkey: pubkey.to_string(),
            metadata: TokenMetadata {
                name: "Unknown Token".to_owned(),
                symbol: "UNKNOWN".to_owned(),
                contract_address: pubkey.to_string(),
                decimals: 9,
                description: None,
                image_url: None,
                creator: None,
                verified: false,
                token_standard: TokenStandard::SPL,
                blockchain: BlockchainNetwork::Solana,
            },
            price_data: None,
            holders: None,
            liquidity_pools: vec![],
            is_verified: false,
            risk_score: None,
        }
    }

    pub fn from_internal(internal: SolanaTokenInfoInternal) -> Self {
        Self {
            pubkey: internal.pubkey.to_string(),
            metadata: TokenMetadata {
                name: internal.metadata.name,
                symbol: internal.metadata.symbol,
                contract_address: internal.metadata.mint_address,
                decimals: internal.metadata.decimals as u32,
                description: internal.metadata.description,
                image_url: internal.metadata.logo_uri,
                creator: None,
                verified: internal.is_verified,
                token_standard: internal.metadata.token_standard,
                blockchain: BlockchainNetwork::Solana,
            },
            price_data: internal.price_data.map(|pd| TokenPriceData {
                price_usd: pd.price_usd,
                price_change_24h: pd.price_change_24h,
                volume_24h: pd.volume_24h as u64,
                market_cap: pd.market_cap.unwrap_or(0.0) as u64,
                total_supply: pd.circulating_supply,
                last_updated: pd.timestamp.timestamp_millis() as u64,
            }),
            holders: internal.holders.map(|h| h.total_holders as u64),
            liquidity_pools: internal
                .liquidity_pools
                .into_iter()
                .map(|pool_address| LiquidityPool {
                    dex_name: "Unknown DEX".to_owned(),
                    pool_address,
                    liquidity_usd: 0.0,
                    volume_24h: 0.0,
                })
                .collect(),
            is_verified: internal.is_verified,
            risk_score: internal.risk_score,
        }
    }
}
