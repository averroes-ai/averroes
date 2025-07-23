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
