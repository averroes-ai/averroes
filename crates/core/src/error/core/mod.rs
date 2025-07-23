use thiserror::Error;

/// Core `FiqhAI` system errors
#[derive(Debug, Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum FiqhAIError {
    #[error("Initialization failed: {0}")]
    InitializationError(String),
    #[error("Actor error: {0}")]
    ActorError(String),
    #[error("Configuration error: {0}")]
    ConfigurationError(String),
    #[error("Database error: {0}")]
    DatabaseError(String),
    #[error("Network error: {0}")]
    NetworkError(String),
    #[error("AI service error: {0}")]
    AIServiceError(String),
    #[error("Solana RPC error: {0}")]
    SolanaError(String),
    #[error("Audio processing error: {0}")]
    AudioError(String),
    #[error("Vector database error: {0}")]
    VectorDatabaseError(String),
    #[error("Scraping error: {0}")]
    ScrapingError(String),
    #[error("Analysis error: {0}")]
    AnalysisError(String),
    #[error("History storage error: {0}")]
    HistoryError(String),
    #[error("Invalid query: {0}")]
    InvalidQuery(String),
    #[error("Timeout error: {0}")]
    TimeoutError(String),
    #[error("Permission denied: {0}")]
    PermissionDenied(String),
}

impl FiqhAIError {
    pub fn initialization(msg: impl Into<String>) -> Self {
        Self::InitializationError(msg.into())
    }

    pub fn actor(msg: impl Into<String>) -> Self {
        Self::ActorError(msg.into())
    }

    pub fn config(msg: impl Into<String>) -> Self {
        Self::ConfigurationError(msg.into())
    }

    pub fn database(msg: impl Into<String>) -> Self {
        Self::DatabaseError(msg.into())
    }

    pub fn network(msg: impl Into<String>) -> Self {
        Self::NetworkError(msg.into())
    }

    pub fn ai_service(msg: impl Into<String>) -> Self {
        Self::AIServiceError(msg.into())
    }

    pub fn solana(msg: impl Into<String>) -> Self {
        Self::SolanaError(msg.into())
    }

    pub fn audio(msg: impl Into<String>) -> Self {
        Self::AudioError(msg.into())
    }

    pub fn vector_db(msg: impl Into<String>) -> Self {
        Self::VectorDatabaseError(msg.into())
    }

    pub fn scraping(msg: impl Into<String>) -> Self {
        Self::ScrapingError(msg.into())
    }

    pub fn analysis(msg: impl Into<String>) -> Self {
        Self::AnalysisError(msg.into())
    }

    pub fn history(msg: impl Into<String>) -> Self {
        Self::HistoryError(msg.into())
    }

    pub fn invalid_query(msg: impl Into<String>) -> Self {
        Self::InvalidQuery(msg.into())
    }

    pub fn timeout(msg: impl Into<String>) -> Self {
        Self::TimeoutError(msg.into())
    }

    pub fn permission_denied(msg: impl Into<String>) -> Self {
        Self::PermissionDenied(msg.into())
    }
}

/// Actor system specific errors
#[derive(Error, Debug)]
pub enum ActorSystemError {
    #[error("Actor spawn failed: {0}")]
    SpawnError(String),
    #[error("Actor communication failed: {0}")]
    CommunicationError(String),
    #[error("Actor shutdown failed: {0}")]
    ShutdownError(String),
    #[error("Channel error: {0}")]
    ChannelError(String),
    #[error("Actor timeout: {0}")]
    TimeoutError(String),
    #[error("Actor overload: {0}")]
    OverloadError(String),
}

/// AI service errors
#[derive(Error, Debug)]
pub enum AIError {
    #[error("OpenAI API error: {0}")]
    OpenAIError(String),
    #[error("Model loading error: {0}")]
    ModelLoadError(String),
    #[error("Inference error: {0}")]
    InferenceError(String),
    #[error("Embedding error: {0}")]
    EmbeddingError(String),
    #[error("Token limit exceeded: {0}")]
    TokenLimitError(String),
    #[error("Rate limit exceeded: {0}")]
    RateLimitError(String),
}

/// Vector database errors
#[derive(Error, Debug)]
pub enum VectorDatabaseError {
    #[error("Qdrant connection error: {0}")]
    ConnectionError(String),
    #[error("Collection error: {0}")]
    CollectionError(String),
    #[error("Search error: {0}")]
    SearchError(String),
    #[error("Indexing error: {0}")]
    IndexingError(String),
    #[error("Vector dimension mismatch: {0}")]
    DimensionMismatch(String),
}
