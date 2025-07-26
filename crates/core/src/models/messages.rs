use serde::Deserialize;
use serde::Serialize;
use tokio::sync::oneshot;
use uuid::Uuid;

use crate::models::AnalysisHistory;
use crate::models::Fatwa;
use crate::models::Query;
use crate::models::QueryResponse;
use crate::models::ScrapedData;
use crate::models::SolanaTokenInfo;
use crate::models::TokenAnalysis;

// QueryActor Messages
#[derive(Debug)]
pub enum QueryMessage {
    ProcessQuery {
        query: Query,
        respond_to: oneshot::Sender<QueryResponse>,
    },
    GetQueryHistory {
        user_id: String,
        limit: Option<usize>,
        respond_to: oneshot::Sender<Vec<Query>>,
    },
    ProcessAudioQuery {
        audio_data: Vec<u8>,
        user_id: Option<String>,
        respond_to: oneshot::Sender<QueryResponse>,
    },
}

// ScraperActor Messages
#[derive(Debug)]
pub enum ScraperMessage {
    ScrapeUrl {
        url: String,
        keywords: Vec<String>,
        respond_to: oneshot::Sender<Result<ScrapedData, ScraperError>>,
    },
    BatchScrape {
        urls: Vec<String>,
        keywords: Vec<String>,
        respond_to: oneshot::Sender<Vec<Result<ScrapedData, ScraperError>>>,
    },
    ScrapeCryptoHalal {
        token_symbol: String,
        respond_to: oneshot::Sender<Result<Vec<ScrapedData>, ScraperError>>,
    },
    GetScrapingStats {
        respond_to: oneshot::Sender<ScrapingStats>,
    },
}

// AnalyzerActor Messages
#[derive(Debug)]
pub enum AnalyzerMessage {
    AnalyzeToken {
        query: Query,
        scraped_data: Vec<ScrapedData>,
        respond_to: oneshot::Sender<Result<TokenAnalysis, AnalyzerError>>,
    },
    GetSolanaTokenInfo {
        mint_address: String,
        respond_to: oneshot::Sender<Result<SolanaTokenInfo, SolanaError>>,
    },
    SearchFatwas {
        keywords: Vec<String>,
        language: String,
        limit: Option<usize>,
        respond_to: oneshot::Sender<Vec<Fatwa>>,
    },
    RunBacktest {
        analysis_id: Uuid,
        respond_to: oneshot::Sender<Result<TokenAnalysis, AnalyzerError>>,
    },
    UpdateAnalysisWithFeedback {
        analysis_id: String,
        feedback: crate::models::UserFeedback,
        respond_to: oneshot::Sender<Result<(), AnalyzerError>>,
    },
    GetAnalysis {
        analysis_id: Uuid,
        respond_to: oneshot::Sender<Result<TokenAnalysis, AnalyzerError>>,
    },
    UpdateAnalysis {
        analysis_id: Uuid,
        user_feedback: crate::models::UserFeedback,
        respond_to: oneshot::Sender<Result<(), AnalyzerError>>,
    },
}

// HistoryActor Messages
#[derive(Debug)]
pub enum HistoryMessage {
    SaveAnalysis {
        analysis: Box<TokenAnalysis>,
        query: Query,
        respond_to: oneshot::Sender<Result<(), HistoryError>>,
    },
    GetAnalysisHistory {
        query: crate::models::HistoryQuery,
        respond_to: oneshot::Sender<Result<Vec<AnalysisHistory>, HistoryError>>,
    },
    GetUserStats {
        user_id: String,
        respond_to: oneshot::Sender<Result<crate::models::UserAnalysisStats, HistoryError>>,
    },
    CleanOldData {
        days_to_keep: u32,
        respond_to: oneshot::Sender<Result<usize, HistoryError>>, // Returns number of deleted entries
    },
}

// Error types for actor responses
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ScraperError {
    NetworkError(String),
    ParseError(String),
    RateLimited,
    InvalidUrl(String),
    ContentTooLarge,
    Timeout,
}

impl std::fmt::Display for ScraperError {
    fn fmt(
        &self,
        f: &mut std::fmt::Formatter<'_>,
    ) -> std::fmt::Result {
        match self {
            ScraperError::NetworkError(msg) => write!(f, "Network error: {msg}"),
            ScraperError::ParseError(msg) => write!(f, "Parse error: {msg}"),
            ScraperError::RateLimited => write!(f, "Rate limited"),
            ScraperError::InvalidUrl(url) => write!(f, "Invalid URL: {url}"),
            ScraperError::ContentTooLarge => write!(f, "Content too large"),
            ScraperError::Timeout => write!(f, "Timeout"),
        }
    }
}

impl std::error::Error for ScraperError {
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AnalyzerError {
    SolanaRpcError(String),
    AiProcessingError(String),
    InsufficientData,
    InvalidTokenAddress(String),
    VectorDbError(String),
    FatwaSearchError(String),
    AnalysisNotFound(Uuid),
    BacktestFailed(String),
    InitializationFailed(String),
}

impl std::fmt::Display for AnalyzerError {
    fn fmt(
        &self,
        f: &mut std::fmt::Formatter<'_>,
    ) -> std::fmt::Result {
        match self {
            AnalyzerError::SolanaRpcError(msg) => write!(f, "Solana RPC error: {msg}"),
            AnalyzerError::AiProcessingError(msg) => write!(f, "AI processing error: {msg}"),
            AnalyzerError::InsufficientData => write!(f, "Insufficient data"),
            AnalyzerError::InvalidTokenAddress(msg) => write!(f, "Invalid token address: {msg}"),
            AnalyzerError::VectorDbError(msg) => write!(f, "Vector database error: {msg}"),
            AnalyzerError::FatwaSearchError(msg) => write!(f, "Fatwa search error: {msg}"),
            AnalyzerError::AnalysisNotFound(id) => write!(f, "Analysis not found: {id}"),
            AnalyzerError::BacktestFailed(msg) => write!(f, "Backtest failed: {msg}"),
            AnalyzerError::InitializationFailed(msg) => write!(f, "Initialization failed: {msg}"),
        }
    }
}

impl std::error::Error for AnalyzerError {
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum SolanaError {
    InvalidAddress(String),
    RpcError(String),
    TokenNotFound(String),
    NetworkError(String),
}

impl std::fmt::Display for SolanaError {
    fn fmt(
        &self,
        f: &mut std::fmt::Formatter<'_>,
    ) -> std::fmt::Result {
        match self {
            SolanaError::InvalidAddress(addr) => write!(f, "Invalid address: {addr}"),
            SolanaError::RpcError(msg) => write!(f, "RPC error: {msg}"),
            SolanaError::TokenNotFound(token) => write!(f, "Token not found: {token}"),
            SolanaError::NetworkError(msg) => write!(f, "Network error: {msg}"),
        }
    }
}

impl std::error::Error for SolanaError {
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum HistoryError {
    DatabaseError(String),
    NotFound(String),
    SerializationError(String),
    AccessDenied,
}

impl std::fmt::Display for HistoryError {
    fn fmt(
        &self,
        f: &mut std::fmt::Formatter<'_>,
    ) -> std::fmt::Result {
        match self {
            HistoryError::DatabaseError(msg) => write!(f, "Database error: {msg}"),
            HistoryError::NotFound(item) => write!(f, "Not found: {item}"),
            HistoryError::SerializationError(msg) => write!(f, "Serialization error: {msg}"),
            HistoryError::AccessDenied => write!(f, "Access denied"),
        }
    }
}

impl std::error::Error for HistoryError {
}

// General actor error for chain/ai operations
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ActorError {
    ProcessingError(String),
    InitializationError(String),
    ConfigurationError(String),
    NetworkError(String),
    AIServiceError(String),
    VectorDbError(String),
    DatabaseError(String),
}

impl std::fmt::Display for ActorError {
    fn fmt(
        &self,
        f: &mut std::fmt::Formatter<'_>,
    ) -> std::fmt::Result {
        match self {
            ActorError::ProcessingError(msg) => write!(f, "Processing error: {msg}"),
            ActorError::InitializationError(msg) => write!(f, "Initialization error: {msg}"),
            ActorError::ConfigurationError(msg) => write!(f, "Configuration error: {msg}"),
            ActorError::NetworkError(msg) => write!(f, "Network error: {msg}"),
            ActorError::AIServiceError(msg) => write!(f, "AI service error: {msg}"),
            ActorError::VectorDbError(msg) => write!(f, "Vector DB error: {msg}"),
            ActorError::DatabaseError(msg) => write!(f, "Database error: {msg}"),
        }
    }
}

impl std::error::Error for ActorError {
}

// Supporting structures
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ScrapingStats {
    pub total_requests: u64,
    pub successful_scrapes: u64,
    pub failed_scrapes: u64,
    pub average_response_time_ms: u64,
    pub rate_limit_hits: u64,
    pub last_scrape_time: Option<chrono::DateTime<chrono::Utc>>,
}

// Actor handle types for clean actor communication
#[derive(Clone)]
pub struct QueryActorHandle {
    pub sender: tokio::sync::mpsc::Sender<QueryMessage>,
}

#[derive(Clone)]
pub struct ScraperActorHandle {
    pub sender: tokio::sync::mpsc::Sender<ScraperMessage>,
}

#[derive(Clone)]
pub struct AnalyzerActorHandle {
    pub sender: tokio::sync::mpsc::Sender<AnalyzerMessage>,
}

#[derive(Clone)]
pub struct HistoryActorHandle {
    pub sender: tokio::sync::mpsc::Sender<HistoryMessage>,
}

// Convenience implementations for actor handles
impl QueryActorHandle {
    pub async fn process_query(
        &self,
        query: Query,
    ) -> Result<QueryResponse, Box<dyn std::error::Error + Send + Sync>> {
        let (tx, rx) = oneshot::channel();
        self.sender
            .send(QueryMessage::ProcessQuery {
                query,
                respond_to: tx,
            })
            .await?;
        Ok(rx.await?)
    }

    pub async fn process_audio(
        &self,
        audio_data: Vec<u8>,
        user_id: Option<String>,
    ) -> Result<QueryResponse, Box<dyn std::error::Error + Send + Sync>> {
        let (tx, rx) = oneshot::channel();
        self.sender
            .send(QueryMessage::ProcessAudioQuery {
                audio_data,
                user_id,
                respond_to: tx,
            })
            .await?;
        Ok(rx.await?)
    }
}

impl ScraperActorHandle {
    pub async fn scrape_url(
        &self,
        url: String,
        keywords: Vec<String>,
    ) -> Result<ScrapedData, Box<dyn std::error::Error + Send + Sync>> {
        let (tx, rx) = oneshot::channel();
        self.sender
            .send(ScraperMessage::ScrapeUrl {
                url,
                keywords,
                respond_to: tx,
            })
            .await?;
        Ok(rx.await??)
    }

    pub async fn batch_scrape(
        &self,
        urls: Vec<String>,
        keywords: Vec<String>,
    ) -> Result<Vec<Result<ScrapedData, ScraperError>>, Box<dyn std::error::Error + Send + Sync>> {
        let (tx, rx) = oneshot::channel();
        self.sender
            .send(ScraperMessage::BatchScrape {
                urls,
                keywords,
                respond_to: tx,
            })
            .await?;
        Ok(rx.await?)
    }
}

impl AnalyzerActorHandle {
    pub async fn analyze_token(
        &self,
        query: Query,
        scraped_data: Vec<ScrapedData>,
    ) -> Result<TokenAnalysis, Box<dyn std::error::Error + Send + Sync>> {
        let (tx, rx) = oneshot::channel();
        self.sender
            .send(AnalyzerMessage::AnalyzeToken {
                query,
                scraped_data,
                respond_to: tx,
            })
            .await?;
        Ok(rx.await??)
    }

    pub async fn get_solana_token_info(
        &self,
        mint_address: String,
    ) -> Result<SolanaTokenInfo, Box<dyn std::error::Error + Send + Sync>> {
        let (tx, rx) = oneshot::channel();
        self.sender
            .send(AnalyzerMessage::GetSolanaTokenInfo {
                mint_address,
                respond_to: tx,
            })
            .await?;
        Ok(rx.await??)
    }

    pub async fn run_backtest(
        &self,
        analysis_id: Uuid,
    ) -> Result<crate::models::BacktestResult, Box<dyn std::error::Error + Send + Sync>> {
        // Mock implementation - would run actual backtest analysis
        Ok(crate::models::BacktestResult {
            analysis_id: analysis_id.to_string(),
            previous_analysis_id: None,
            comparison_date: chrono::Utc::now().timestamp_millis() as u64,
            ruling_changed: false,
            confidence_change: 0.0,
            new_factors: Vec::new(),
            removed_factors: Vec::new(),
            summary: "No significant changes detected".to_owned(),
        })
    }

    pub async fn comprehensive_analysis(
        &self,
        token_symbol: &str,
    ) -> Result<crate::actors::analyzer_actor::ComprehensiveAnalysis, Box<dyn std::error::Error + Send + Sync>> {
        // Try to get real token data first, fallback to mock if fails
        let token_info = self
            .get_token_info_from_api(token_symbol)
            .await
            .unwrap_or_else(|_| self.get_fallback_token_info(token_symbol));

        let islamic_analysis = self.perform_islamic_analysis(&token_info).await;

        let ai_reasoning = (self.get_ai_analysis(&token_info).await).ok();

        Ok(crate::actors::analyzer_actor::ComprehensiveAnalysis {
            analysis_id: Uuid::new_v4(),
            token_info,
            islamic_analysis,
            ai_reasoning,
            timestamp: chrono::Utc::now(),
        })
    }

    async fn get_token_info_from_api(
        &self,
        symbol: &str,
    ) -> Result<crate::models::token::UniversalTokenInfo, Box<dyn std::error::Error + Send + Sync>> {
        let client = reqwest::Client::new();
        let url = format!("https://api.coingecko.com/api/v3/coins/{}", symbol.to_lowercase());

        let response: serde_json::Value = client.get(&url).send().await?.json().await?;

        Ok(crate::models::token::UniversalTokenInfo {
            address: format!("api_fetched_{}", symbol.to_lowercase()),
            metadata: crate::models::TokenMetadata::new(
                response["name"].as_str().unwrap_or("Unknown").to_owned(),
                symbol.to_uppercase(),
                format!("api_fetched_{}", symbol.to_lowercase()),
                9,                                               // Default decimals
                crate::models::token::BlockchainNetwork::Solana, // Default to Solana for now
            ),
            price_data: Some(crate::models::TokenPriceData {
                price_usd: response["market_data"]["current_price"]["usd"].as_f64().unwrap_or(0.0),
                price_change_24h: response["market_data"]["price_change_percentage_24h"].as_f64().unwrap_or(0.0),
                volume_24h: response["market_data"]["total_volume"]["usd"].as_u64().unwrap_or(0),
                market_cap: response["market_data"]["market_cap"]["usd"].as_u64().unwrap_or(0),
                total_supply: response["market_data"]["total_supply"].as_f64(),
                last_updated: chrono::Utc::now().timestamp_millis() as u64,
            }),
            holders: None, // CoinGecko doesn't provide holder count
            liquidity_pools: vec![],
            is_verified: true,
            risk_score: Some(0.3), // Default risk score
            blockchain: crate::models::token::BlockchainNetwork::Solana,
        })
    }

    fn get_fallback_token_info(
        &self,
        symbol: &str,
    ) -> crate::models::token::UniversalTokenInfo {
        match symbol.to_uppercase().as_str() {
            "SOL" => crate::models::token::UniversalTokenInfo {
                address: "So11111111111111111111111111111111111111112".to_owned(),
                metadata: crate::models::TokenMetadata::new(
                    "Solana".to_owned(),
                    "SOL".to_owned(),
                    "So11111111111111111111111111111111111111112".to_owned(),
                    9,
                    crate::models::token::BlockchainNetwork::Solana,
                ),
                price_data: Some(crate::models::TokenPriceData {
                    price_usd: 23.45,
                    price_change_24h: 2.5,
                    volume_24h: 1_000_000,
                    market_cap: 13_600_000_000,
                    total_supply: Some(582_000_000.0),
                    last_updated: chrono::Utc::now().timestamp_millis() as u64,
                }),
                holders: Some(50_000),
                liquidity_pools: vec![],
                is_verified: true,
                risk_score: Some(0.1),
                blockchain: crate::models::token::BlockchainNetwork::Solana,
            },
            "BTC" => crate::models::token::UniversalTokenInfo {
                address: "9n4nbM75f5Ui33ZbPYXn59EwSgE8CGsHtAeTH5YFeJ9E".to_owned(),
                metadata: crate::models::TokenMetadata::new(
                    "Bitcoin".to_owned(),
                    "BTC".to_owned(),
                    "9n4nbM75f5Ui33ZbPYXn59EwSgE8CGsHtAeTH5YFeJ9E".to_owned(),
                    8,
                    crate::models::token::BlockchainNetwork::Solana,
                ),
                price_data: Some(crate::models::TokenPriceData {
                    price_usd: 43_200.0,
                    price_change_24h: 1.2,
                    volume_24h: 2_000_000,
                    market_cap: 900_000_000_000,
                    total_supply: Some(21_000_000.0),
                    last_updated: chrono::Utc::now().timestamp_millis() as u64,
                }),
                holders: Some(100_000),
                liquidity_pools: vec![],
                is_verified: true,
                risk_score: Some(0.05),
                blockchain: crate::models::token::BlockchainNetwork::Solana,
            },
            "USDC" => crate::models::token::UniversalTokenInfo {
                address: "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v".to_owned(),
                metadata: crate::models::TokenMetadata::new(
                    "USD Coin".to_owned(),
                    "USDC".to_owned(),
                    "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v".to_owned(),
                    6,
                    crate::models::token::BlockchainNetwork::Solana,
                ),
                price_data: Some(crate::models::TokenPriceData {
                    price_usd: 1.0,
                    price_change_24h: 0.01,
                    volume_24h: 5_000_000,
                    market_cap: 25_000_000_000,
                    total_supply: Some(25_000_000_000.0),
                    last_updated: chrono::Utc::now().timestamp_millis() as u64,
                }),
                holders: Some(200_000),
                liquidity_pools: vec![],
                is_verified: true,
                risk_score: Some(0.02),
                blockchain: crate::models::token::BlockchainNetwork::Solana,
            },
            _ => crate::models::token::UniversalTokenInfo {
                address: format!("unknown_{}", symbol.to_lowercase()),
                metadata: crate::models::TokenMetadata::new(
                    format!("Unknown Token ({symbol})"),
                    symbol.to_uppercase(),
                    format!("unknown_{}", symbol.to_lowercase()),
                    9, // Default decimals
                    crate::models::token::BlockchainNetwork::Solana,
                ),
                price_data: Some(crate::models::TokenPriceData {
                    price_usd: 0.0,
                    price_change_24h: 0.0,
                    volume_24h: 0,
                    market_cap: 0,
                    total_supply: Some(0.0),
                    last_updated: chrono::Utc::now().timestamp_millis() as u64,
                }),
                holders: None,
                liquidity_pools: vec![],
                is_verified: false,
                risk_score: Some(0.8), // High risk for unknown tokens
                blockchain: crate::models::token::BlockchainNetwork::Solana,
            },
        }
    }

    async fn perform_islamic_analysis(
        &self,
        token_info: &crate::models::token::UniversalTokenInfo,
    ) -> crate::actors::analyzer_actor::IslamicAnalysisResult {
        let mut compliance_score: f32 = 0.5; // Start neutral
        let mut reasoning = Vec::new();
        let confidence = 0.7;

        let description_lower = token_info.metadata.name.to_lowercase();

        // Rule-based analysis
        let prohibited_keywords = vec![
            "gambling",
            "casino",
            "lottery",
            "alcohol",
            "pork",
            "interest",
            "usury",
            "riba",
            "lending",
            "borrowing",
            "leverage",
            "margin",
        ];

        for keyword in &prohibited_keywords {
            if description_lower.contains(keyword) {
                compliance_score -= 0.2;
                reasoning.push(format!("Contains prohibited keyword: {keyword}"));
            }
        }

        // Check for DeFi/lending protocols (high risk)
        if description_lower.contains("defi") || description_lower.contains("yield") {
            compliance_score -= 0.3;
            reasoning.push("DeFi protocols may involve riba (interest)".to_owned());
        }

        // Check for utility tokens (generally acceptable)
        if description_lower.contains("utility") || description_lower.contains("infrastructure") {
            compliance_score += 0.2;
            reasoning.push("Utility tokens are generally permissible".to_owned());
        }

        // Check for stablecoins
        if description_lower.contains("stablecoin") || description_lower.contains("stable") {
            compliance_score += 0.1;
            reasoning.push("Stablecoins may be permissible if properly backed".to_owned());
        }

        // Ensure score is between 0 and 1
        compliance_score = compliance_score.clamp(0.0, 1.0);

        let is_halal = compliance_score >= 0.6;

        crate::actors::analyzer_actor::IslamicAnalysisResult {
            is_halal,
            compliance_score: compliance_score as f64,
            confidence,
            reasoning,
            scholar_references: vec![
                "Quran 2:275 - Allah has permitted trade and forbidden riba".to_owned(),
                "AAOIFI Sharia Standard No. 17".to_owned(),
            ],
        }
    }

    async fn get_ai_analysis(
        &self,
        token_info: &crate::models::token::UniversalTokenInfo,
    ) -> Result<String, Box<dyn std::error::Error + Send + Sync>> {
        // For now, return a rule-based AI-like response
        // In production, this would call Groq or another free AI API
        let description_lower = token_info.metadata.name.to_lowercase();
        let token_name = &token_info.metadata.name;

        let analysis = if description_lower.contains("defi") {
            format!(
                "HARAM - {token_name} appears to be a DeFi token which typically involves riba (interest-based \
                 lending). Islamic finance prohibits earning money from money without underlying assets. Confidence: \
                 75%"
            )
        } else if description_lower.contains("utility") {
            format!(
                "HALAL - {token_name} is a utility token that provides access to blockchain services. This is \
                 generally permissible as it represents actual utility value. Confidence: 80%"
            )
        } else if description_lower.contains("stablecoin") || description_lower.contains("stable") {
            format!(
                "HALAL - {token_name} is a stablecoin backed by real assets. This is generally permissible if \
                 properly collateralized. Confidence: 85%"
            )
        } else {
            format!(
                "NEUTRAL - {token_name} requires further analysis. Consider the underlying business model and ensure \
                 it doesn't involve riba, gharar, or maysir. Confidence: 60%"
            )
        };

        Ok(analysis)
    }
}

impl HistoryActorHandle {
    pub async fn save_analysis(
        &self,
        analysis: TokenAnalysis,
        query: Query,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let (tx, rx) = oneshot::channel();
        self.sender
            .send(HistoryMessage::SaveAnalysis {
                analysis: Box::new(analysis),
                query,
                respond_to: tx,
            })
            .await?;
        Ok(rx.await??)
    }

    pub async fn get_history(
        &self,
        query: crate::models::HistoryQuery,
    ) -> Result<Vec<AnalysisHistory>, Box<dyn std::error::Error + Send + Sync>> {
        let (tx, rx) = oneshot::channel();
        self.sender
            .send(HistoryMessage::GetAnalysisHistory {
                query,
                respond_to: tx,
            })
            .await?;
        Ok(rx.await??)
    }

    pub async fn query_analyses(
        &self,
        query: crate::models::HistoryQuery,
    ) -> Result<AnalysisHistory, Box<dyn std::error::Error + Send + Sync>> {
        let (tx, rx) = oneshot::channel();
        self.sender
            .send(HistoryMessage::GetAnalysisHistory {
                query,
                respond_to: tx,
            })
            .await?;
        let results = rx.await??;
        // Return first result or empty history
        Ok(results.into_iter().next().unwrap_or_else(|| AnalysisHistory {
            entries: Vec::new(),
            total_count: 0,
            next_cursor: None,
        }))
    }

    pub async fn get_user_stats(
        &self,
        user_id: String,
    ) -> Result<crate::models::UserAnalysisStats, Box<dyn std::error::Error + Send + Sync>> {
        // Mock implementation - would query actual stats from database
        Ok(crate::models::UserAnalysisStats {
            user_id,
            total_analyses: 0,
            halal_count: 0,
            haram_count: 0,
            mubah_count: 0,
            average_confidence: 0.0,
            first_analysis: chrono::Utc::now().timestamp_millis() as u64,
            last_analysis: None,
            top_tokens: Vec::new(),
        })
    }
}
