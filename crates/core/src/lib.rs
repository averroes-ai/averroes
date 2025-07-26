pub mod actors;
pub mod ai;
pub mod api;
pub mod error;
pub mod models;

use std::sync::Arc;

pub use actors::*;
pub use ai::*;
pub use api::*;
pub use error::FiqhAIError;
pub use models::{
    // History types
    AnalysisHistory,
    AnalysisStatus,

    // Actor handle types
    AnalyzerActorHandle,
    BacktestResult,
    ChatMessage,
    ConfidenceBreakdown,
    FatwaReference,
    HistoryActorHandle,
    HistoryEntry,
    HistoryQuery,

    IslamicAnalysis,
    // Islamic analysis types
    IslamicPrinciple,
    LiquidityPool,

    MaqashidPrinciple,
    // Query types
    Query,
    QueryActorHandle,
    QueryResponse,
    QueryType,

    ScraperActorHandle,
    SolanaTokenInfo,
    // Token types
    TokenAnalysis,
    UserAnalysisStats,
};
use tokio::sync::RwLock;

// Define config and error types directly here since they're not in models
#[derive(Clone, Debug, uniffi::Record)]
pub struct FiqhAIConfig {
    pub openai_api_key: String,
    pub groq_api_key: String,
    pub grok_api_key: String,
    pub model_name: String,
    pub qdrant_url: String,
    pub database_path: String,
    pub solana_rpc_url: String,
    pub enable_solana: bool,
    pub preferred_model: String, // "groq", "grok", or "openai"
}

// ============================================================================
// UNIFFI SETUP - Modern Rust annotation approach
// ============================================================================

// Setup UniFFI scaffolding (no UDL file needed)
uniffi::setup_scaffolding!();

// ============================================================================
// MAIN SYSTEM INTERFACE
// ============================================================================

/// Main `FiqhAI` system that orchestrates all components
#[derive(uniffi::Object)]
pub struct FiqhAISystem {
    query_actor: QueryActorHandle,
    #[allow(dead_code)]
    scraper_actor: ScraperActorHandle,
    analyzer_actor: AnalyzerActorHandle,
    history_actor: Option<HistoryActorHandle>,
    config: FiqhAIConfig,
}

impl Default for FiqhAIConfig {
    fn default() -> Self {
        Self {
            openai_api_key: std::env::var("OPENAI_API_KEY").ok().unwrap_or_else(|| "".to_owned()),
            groq_api_key: std::env::var("GROQ_API_KEY").ok().unwrap_or_else(|| "".to_owned()),
            grok_api_key: std::env::var("GROK_API_KEY").ok().unwrap_or_else(|| "".to_owned()),
            model_name: "gpt-4".to_owned(),
            qdrant_url: "http://localhost:6333".to_owned(),
            database_path: "".to_owned(), // Use empty path to trigger in-memory database

            solana_rpc_url: "https://api.mainnet-beta.solana.com".to_owned(),
            enable_solana: true,
            preferred_model: "groq".to_owned(),
        }
    }
}

#[uniffi::export]
impl FiqhAISystem {
    /// Initialize the complete `FiqhAI` system with mobile-friendly approach
    #[uniffi::constructor]
    pub async fn new(config: FiqhAIConfig) -> Result<Self, FiqhAIError> {
        log::info!("Starting FiqhAI system initialization...");

        // Step 1: Initialize AI service with retries and fallbacks
        let ai_service = match Self::initialize_ai_service(&config).await {
            Ok(service) => Arc::new(service),
            Err(e) => {
                log::error!("AI service initialization failed: {}", e);
                return Err(FiqhAIError::InitializationError(format!("AI service failed: {e}")));
            },
        };

        log::info!("✅ AI service initialized successfully");

        // Step 2: Create minimal mobile config - disable all optional features
        let analyzer_config = AnalyzerConfig {
            openai_api_key: None,           // Disable OpenAI for mobile to avoid API calls
            model_name: "mock".to_string(), // Use mock for mobile
            enable_vector_search: false,    // Always disable vector search for mobile
            qdrant_url: "".to_string(),     // Empty URL
            analysis_timeout_seconds: 15,   // Shorter timeout for mobile
            enable_backtest: false,         // Always disable backtest for mobile
        };

        log::info!("✅ Mobile-friendly config created");

        // Step 3: Spawn actors with error recovery
        let history_actor = None; // Always skip for mobile

        let analyzer_actor =
            match Self::spawn_analyzer_actor_safe(None, Some(analyzer_config), ai_service.clone()).await {
                Ok(actor) => actor,
                Err(e) => {
                    log::error!("Analyzer actor spawn failed: {}", e);
                    return Err(FiqhAIError::InitializationError(format!("Analyzer actor failed: {e}")));
                },
            };

        log::info!("✅ Analyzer actor spawned successfully");

        let scraper_actor = match Self::spawn_scraper_actor_safe().await {
            Ok(actor) => actor,
            Err(e) => {
                log::error!("Scraper actor spawn failed: {}", e);
                return Err(FiqhAIError::InitializationError(format!("Scraper actor failed: {e}")));
            },
        };

        log::info!("✅ Scraper actor spawned successfully");

        let query_actor =
            match Self::spawn_query_actor_safe(scraper_actor.clone(), analyzer_actor.clone(), history_actor.clone())
                .await
            {
                Ok(actor) => actor,
                Err(e) => {
                    log::error!("Query actor spawn failed: {}", e);
                    return Err(FiqhAIError::InitializationError(format!("Query actor failed: {e}")));
                },
            };

        log::info!("✅ Query actor spawned successfully");

        log::info!("🎉 FiqhAI system initialized successfully!");

        Ok(Self {
            query_actor,
            scraper_actor,
            analyzer_actor,
            history_actor,
            config,
        })
    }

    /// Safe AI service initialization with error handling
    async fn initialize_ai_service(
        config: &FiqhAIConfig
    ) -> Result<crate::ai::AIService, Box<dyn std::error::Error + Send + Sync>> {
        // For mobile, create a minimal AI service that doesn't fail
        match crate::ai::AIService::new_mobile_safe(config).await {
            Ok(service) => Ok(service),
            Err(e) => {
                log::warn!("Failed to create full AI service, creating minimal fallback: {}", e);
                // Create a minimal AI service that won't fail
                crate::ai::AIService::new_minimal_fallback(config).await
            },
        }
    }

    /// Safe analyzer actor spawning with error recovery
    async fn spawn_analyzer_actor_safe(
        solana_rpc_url: Option<String>,
        config: Option<AnalyzerConfig>,
        ai_service: Arc<crate::ai::AIService>,
    ) -> Result<crate::models::messages::AnalyzerActorHandle, Box<dyn std::error::Error + Send + Sync>> {
        // Use tokio::time::timeout to prevent hanging
        match tokio::time::timeout(
            std::time::Duration::from_secs(10),
            spawn_analyzer_actor(solana_rpc_url, config, ai_service),
        )
        .await
        {
            Ok(actor) => Ok(actor),
            Err(_) => Err("Analyzer actor spawn timed out".into()),
        }
    }

    /// Safe scraper actor spawning
    async fn spawn_scraper_actor_safe()
    -> Result<crate::models::messages::ScraperActorHandle, Box<dyn std::error::Error + Send + Sync>> {
        match tokio::time::timeout(std::time::Duration::from_secs(5), spawn_scraper_actor()).await {
            Ok(actor) => Ok(actor),
            Err(_) => Err("Scraper actor spawn timed out".into()),
        }
    }

    /// Safe query actor spawning
    async fn spawn_query_actor_safe(
        scraper_actor: crate::models::messages::ScraperActorHandle,
        analyzer_actor: crate::models::messages::AnalyzerActorHandle,
        history_actor: Option<crate::models::messages::HistoryActorHandle>,
    ) -> Result<crate::models::messages::QueryActorHandle, Box<dyn std::error::Error + Send + Sync>> {
        match tokio::time::timeout(
            std::time::Duration::from_secs(5),
            spawn_query_actor(scraper_actor, analyzer_actor, history_actor),
        )
        .await
        {
            Ok(actor) => Ok(actor),
            Err(_) => Err("Query actor spawn timed out".into()),
        }
    }

    /// Process a user query and return analysis response
    pub async fn process_query(
        &self,
        query: Query,
    ) -> Result<QueryResponse, FiqhAIError> {
        let response = self
            .query_actor
            .process_query(query)
            .await
            .map_err(|e| FiqhAIError::ActorError(e.to_string()))?;

        Ok(response)
    }

    /// Analyze a token by ticker symbol
    pub async fn analyze_token(
        &self,
        token: String,
        user_id: Option<String>,
        language: Option<String>,
    ) -> Result<QueryResponse, FiqhAIError> {
        let query = Query::new_token_ticker(token, user_id, language);
        self.process_query(query).await
    }

    pub async fn analyze_token_symbol(
        &self,
        symbol: String,
    ) -> Result<QueryResponse, FiqhAIError> {
        let analysis = self
            .analyzer_actor
            .comprehensive_analysis(&symbol)
            .await
            .map_err(|e| FiqhAIError::AnalysisError(e.to_string()))?;

        let response_text = format!(
            "Token: {} ({})\nStatus: {}\nConfidence: {:.0}%\n\nReasoning:\n{}",
            analysis.token_info.metadata.name,
            analysis.token_info.metadata.symbol,
            if analysis.islamic_analysis.is_halal {
                "HALAL ✅"
            } else {
                "HARAM ❌"
            },
            analysis.islamic_analysis.confidence * 100.0,
            analysis.islamic_analysis.reasoning.join("\n• ")
        );

        Ok(QueryResponse::new(
            analysis.analysis_id,
            response_text,
            analysis.islamic_analysis.confidence,
            analysis.islamic_analysis.scholar_references,
            vec![], // No follow-up questions for MVP
            Some(analysis.analysis_id),
        ))
    }

    /// Analyze text input
    pub async fn analyze_text(
        &self,
        text: String,
        user_id: Option<String>,
        language: Option<String>,
    ) -> Result<QueryResponse, FiqhAIError> {
        let query = Query::new_text(text, user_id, language);
        self.process_query(query).await
    }

    /// Analyze audio input (with STT processing)
    pub async fn analyze_audio(
        &self,
        audio_data: Vec<u8>,
        user_id: Option<String>,
        language: Option<String>,
    ) -> Result<QueryResponse, FiqhAIError> {
        let query = Query::new_audio(audio_data, user_id, language);
        self.process_query(query).await
    }

    /// Analyze contract address
    pub async fn analyze_contract(
        &self,
        contract_address: String,
        user_id: Option<String>,
        language: Option<String>,
    ) -> Result<QueryResponse, FiqhAIError> {
        let query = Query::new_contract_address(contract_address, user_id, language);
        self.process_query(query).await
    }

    /// Get analysis history for a user
    pub async fn get_user_history(
        &self,
        user_id: String,
        limit: Option<u32>,
    ) -> Result<AnalysisHistory, FiqhAIError> {
        // Return empty history if history actor is not available (mobile mode)
        let Some(ref history_actor) = self.history_actor else {
            return Ok(AnalysisHistory {
                entries: vec![],
                total_count: 0,
                next_cursor: None,
            });
        };

        let query = HistoryQuery::new().for_user(user_id).limit(limit.unwrap_or(20) as usize);

        let history = history_actor
            .query_analyses(query)
            .await
            .map_err(|e| FiqhAIError::ActorError(e.to_string()))?;

        Ok(history)
    }

    /// Get token analysis history
    pub async fn get_token_history(
        &self,
        token: String,
        limit: Option<u32>,
    ) -> Result<AnalysisHistory, FiqhAIError> {
        // Return empty history if history actor is not available (mobile mode)
        let Some(ref history_actor) = self.history_actor else {
            return Ok(AnalysisHistory {
                entries: vec![],
                total_count: 0,
                next_cursor: None,
            });
        };

        let query = HistoryQuery::new().for_token(token).limit(limit.unwrap_or(10) as usize);

        let history = history_actor
            .query_analyses(query)
            .await
            .map_err(|e| FiqhAIError::ActorError(e.to_string()))?;

        Ok(history)
    }

    /// Get user statistics
    pub async fn get_user_stats(
        &self,
        user_id: String,
    ) -> Result<UserAnalysisStats, FiqhAIError> {
        // Return default stats if no history actor (mobile mode)
        if let Some(ref history_actor) = self.history_actor {
            let stats = history_actor
                .get_user_stats(user_id)
                .await
                .map_err(|e| FiqhAIError::ActorError(e.to_string()))?;
            Ok(stats)
        } else {
            // Return default stats for mobile
            Ok(UserAnalysisStats {
                user_id,
                total_analyses: 0,
                halal_count: 0,
                haram_count: 0,
                mubah_count: 0,
                average_confidence: 0.0,
                first_analysis: 0,
                last_analysis: None,
                top_tokens: vec![],
            })
        }
    }

    /// Run backtest for a specific analysis
    pub async fn run_backtest(
        &self,
        analysis_id: String,
    ) -> Result<BacktestResult, FiqhAIError> {
        let analysis_uuid = uuid::Uuid::parse_str(&analysis_id)
            .map_err(|_| FiqhAIError::invalid_query("Invalid analysis ID format"))?;

        let result = self
            .analyzer_actor
            .run_backtest(analysis_uuid)
            .await
            .map_err(|e| FiqhAIError::ActorError(e.to_string()))?;

        Ok(result)
    }

    /// Get system configuration
    pub fn get_config(&self) -> FiqhAIConfig {
        self.config.clone()
    }
}

// ============================================================================
// MOBILE-SPECIFIC INTERFACES
// ============================================================================

/// Audio processing for mobile STT integration
#[derive(uniffi::Object)]
pub struct AudioProcessor {}

impl Default for AudioProcessor {
    fn default() -> Self {
        Self::new()
    }
}

#[uniffi::export]
impl AudioProcessor {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {}
    }

    /// Mock speech-to-text implementation
    pub async fn transcribe_audio(
        &self,
        audio_data: Vec<u8>,
        _language: Option<String>,
    ) -> Result<String, FiqhAIError> {
        // Mock STT implementation - in production, this would integrate with platform STT
        if audio_data.is_empty() {
            return Err(FiqhAIError::invalid_query("Audio data cannot be empty"));
        }

        // Simulate processing delay
        tokio::time::sleep(std::time::Duration::from_millis(500)).await;

        // Mock transcription based on audio data size and content
        let transcription = match audio_data.len() {
            0..=1000 => "Apakah Bitcoin halal?",
            1001..=5000 => "Saya ingin tahu apakah token SOL itu halal menurut syariat Islam?",
            5001..=10000 => "Bagaimana hukum trading cryptocurrency dalam Islam? Apakah mengandung riba?",
            _ => "Mohon analisis token ini dari perspektif maqashid syariah dan prinsip-prinsip keuangan Islam",
        };

        Ok(transcription.to_owned())
    }

    pub fn is_supported_format(
        &self,
        audio_data: Vec<u8>,
    ) -> bool {
        // Simple format detection based on file headers
        if audio_data.len() < 4 {
            return false;
        }

        // Check for common audio format headers
        matches!(
            &audio_data[0..4],
            b"RIFF" | // WAV
            b"OggS" | // OGG
            b"\xff\xfb" | // MP3
            b"fLaC" | // FLAC
            [0x4f, 0x70, 0x75, 0x73] // Opus
        )
    }

    pub fn get_supported_formats(&self) -> Vec<String> {
        vec![
            "audio/wav".to_owned(),
            "audio/mp3".to_owned(),
            "audio/ogg".to_owned(),
            "audio/flac".to_owned(),
            "audio/opus".to_owned(),
        ]
    }
}

/// Solana `DApp` connector for blockchain integration
#[derive(uniffi::Object)]
pub struct SolanaConnector {
    rpc_url: String,
}

#[uniffi::export]
impl SolanaConnector {
    #[uniffi::constructor]
    pub fn new(rpc_url: Option<String>) -> Self {
        Self {
            rpc_url: rpc_url.unwrap_or_else(|| "https://api.mainnet-beta.solana.com".to_owned()),
        }
    }

    pub async fn get_token_info(
        &self,
        mint_address: String,
    ) -> Result<SolanaTokenInfo, FiqhAIError> {
        // Mock implementation - would use actual Solana RPC in production
        let pubkey = mint_address
            .parse::<solana_program::pubkey::Pubkey>()
            .map_err(|_| FiqhAIError::invalid_query("Invalid mint address format"))?;

        let token_info = SolanaTokenInfo::from_pubkey(pubkey);
        Ok(token_info)
    }

    pub async fn get_wallet_tokens(
        &self,
        _wallet_address: String,
    ) -> Result<Vec<String>, FiqhAIError> {
        // Mock wallet tokens
        Ok(vec![
            "So11111111111111111111111111111111111111112".to_owned(), // WSOL
            "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v".to_owned(), // USDC
            "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB".to_owned(), // USDT
        ])
    }

    pub async fn simulate_transaction(
        &self,
        _transaction_data: String,
    ) -> Result<String, FiqhAIError> {
        // Mock transaction simulation
        Ok("Transaction simulation successful".to_owned())
    }

    pub async fn is_connected(&self) -> Result<bool, FiqhAIError> {
        // Mock connection check
        Ok(true)
    }

    pub fn get_network_name(&self) -> String {
        if self.rpc_url.contains("devnet") {
            "Solana Devnet".to_owned()
        } else if self.rpc_url.contains("testnet") {
            "Solana Testnet".to_owned()
        } else {
            "Solana Mainnet".to_owned()
        }
    }
}

/// Chatbot session for follow-up interactions
#[derive(uniffi::Object)]
pub struct ChatbotSession {
    #[allow(dead_code)]
    user_id: String,
    #[allow(dead_code)]
    language: String,
    session_id: String,
    conversation_history: Arc<RwLock<Vec<ChatMessage>>>,
    session_start: u64, // Unix timestamp
    is_active: bool,
}

#[uniffi::export]
impl ChatbotSession {
    #[uniffi::constructor]
    pub fn new(
        user_id: String,
        language: Option<String>,
    ) -> Self {
        Self {
            user_id,
            language: language.unwrap_or_else(|| "id".to_owned()),
            session_id: uuid::Uuid::new_v4().to_string(),
            conversation_history: Arc::new(RwLock::new(Vec::new())),
            session_start: chrono::Utc::now().timestamp_millis() as u64,
            is_active: true,
        }
    }

    pub fn start_session(&self) -> String {
        self.session_id.clone()
    }

    pub async fn send_message(
        &self,
        message: String,
        _context: Option<String>,
    ) -> Result<QueryResponse, FiqhAIError> {
        if !self.is_active {
            return Err(FiqhAIError::invalid_query("System is not active"));
        }

        // Add user message to history
        let user_message = ChatMessage {
            id: uuid::Uuid::new_v4().to_string(),
            content: message.clone(),
            is_user_message: true,
            timestamp: chrono::Utc::now().timestamp_millis() as u64,
            analysis_id: None,
            follow_up_options: None,
        };

        self.conversation_history.write().await.push(user_message);

        // Mock response generation
        let response_text = format!(
            "Terima kasih atas pertanyaan Anda tentang '{message}'. Berdasarkan prinsip maqashid syariah, saya akan \
             menganalisis hal ini lebih lanjut."
        );

        let follow_up_questions = vec![
            "Apakah Anda ingin tahu lebih detail tentang aspek riba?".to_owned(),
            "Bagaimana pendapat ulama lain tentang hal ini?".to_owned(),
            "Apakah ada alternatif yang lebih sesuai syariah?".to_owned(),
        ];

        // Add bot response to history
        let bot_message = ChatMessage {
            id: uuid::Uuid::new_v4().to_string(),
            content: response_text.clone(),
            is_user_message: false,
            timestamp: chrono::Utc::now().timestamp_millis() as u64,
            analysis_id: Some(uuid::Uuid::new_v4().to_string()),
            follow_up_options: Some(follow_up_questions.clone()),
        };

        self.conversation_history.write().await.push(bot_message);

        Ok(QueryResponse::new(
            uuid::Uuid::new_v4(),
            response_text,
            0.85,
            vec!["Al-Quran dan Hadits".to_owned(), "Fatwa MUI".to_owned()],
            follow_up_questions,
            Some(uuid::Uuid::new_v4()),
        ))
    }

    pub async fn get_conversation_history(&self) -> Vec<ChatMessage> {
        self.conversation_history.read().await.clone()
    }

    pub async fn clear_session(&self) {
        self.conversation_history.write().await.clear();
    }

    pub fn is_active(&self) -> bool {
        self.is_active
    }

    pub fn get_session_start_time(&self) -> u64 {
        self.session_start
    }
}

// ============================================================================
// UTILITY FUNCTIONS FOR MOBILE
// ============================================================================

/// Get display text for Islamic ruling
#[uniffi::export]
pub fn get_ruling_display_text(ruling: IslamicPrinciple) -> String {
    match ruling {
        IslamicPrinciple::Halal => "Halal (Diperbolehkan)".to_owned(),
        IslamicPrinciple::Haram => "Haram (Dilarang)".to_owned(),
        IslamicPrinciple::Makruh => "Makruh (Tidak Disukai)".to_owned(),
        IslamicPrinciple::Mustahab => "Mustahab (Dianjurkan)".to_owned(),
        IslamicPrinciple::Mubah => "Mubah (Boleh)".to_owned(),
        IslamicPrinciple::Riba => "Riba (Haram - Bunga)".to_owned(),
        IslamicPrinciple::Gharar => "Gharar (Syubhat - Ketidakpastian Berlebihan)".to_owned(),
        IslamicPrinciple::Maysir => "Maysir (Haram - Perjudian)".to_owned(),
        IslamicPrinciple::Syubhat => "Syubhat (Meragukan)".to_owned(),
    }
}

/// Get emoji for Islamic ruling
#[uniffi::export]
pub fn get_ruling_emoji(ruling: IslamicPrinciple) -> String {
    match ruling {
        IslamicPrinciple::Halal => "✅".to_owned(),
        IslamicPrinciple::Haram => "❌".to_owned(),
        IslamicPrinciple::Makruh => "⚠️".to_owned(),
        IslamicPrinciple::Mustahab => "⭐".to_owned(),
        IslamicPrinciple::Mubah => "✔️".to_owned(),
        IslamicPrinciple::Riba => "🚫".to_owned(),
        IslamicPrinciple::Gharar => "⚡".to_owned(),
        IslamicPrinciple::Maysir => "🎰".to_owned(),
        IslamicPrinciple::Syubhat => "❓".to_owned(),
    }
}

/// Check if ruling is permissible
#[uniffi::export]
pub fn is_ruling_permissible(ruling: IslamicPrinciple) -> bool {
    matches!(ruling, IslamicPrinciple::Halal | IslamicPrinciple::Mubah | IslamicPrinciple::Mustahab)
}

// ============================================================================
// HELPER CONSTRUCTORS FOR MOBILE
// ============================================================================

/// Create text query
#[uniffi::export]
pub fn create_text_query(
    text: String,
    user_id: Option<String>,
    language: Option<String>,
) -> Query {
    Query::new_text(text, user_id, language)
}

/// Create token query
#[uniffi::export]
pub fn create_token_query(
    token: String,
    user_id: Option<String>,
    language: Option<String>,
) -> Query {
    Query::new_token_ticker(token, user_id, language)
}

/// Create contract address query
#[uniffi::export]
pub fn create_contract_query(
    address: String,
    user_id: Option<String>,
    language: Option<String>,
) -> Query {
    Query::new_contract_address(address, user_id, language)
}

/// Create audio query
#[uniffi::export]
pub fn create_audio_query(
    audio_data: Vec<u8>,
    user_id: Option<String>,
    language: Option<String>,
) -> Query {
    Query::new_audio(audio_data, user_id, language)
}

/// Create default configuration
#[uniffi::export]
pub fn create_default_config() -> FiqhAIConfig {
    FiqhAIConfig::default()
}

/// Create mobile-optimized configuration
#[uniffi::export]
pub fn create_mobile_config(
    groq_api_key: Option<String>,
    grok_api_key: Option<String>,
    openai_api_key: Option<String>,
    enable_vector_search: bool,
) -> FiqhAIConfig {
    let groq_available = groq_api_key.is_some();
    let grok_available = grok_api_key.is_some();

    FiqhAIConfig {
        openai_api_key: openai_api_key.unwrap_or_else(|| "".to_owned()),
        groq_api_key: groq_api_key.unwrap_or_else(|| "".to_owned()),
        grok_api_key: grok_api_key.unwrap_or_else(|| "".to_owned()),
        enable_solana: enable_vector_search,
        preferred_model: if groq_available {
            "groq".to_owned()
        } else if grok_available {
            "grok".to_owned()
        } else {
            "openai".to_owned()
        },
        // Use empty database path for mobile platforms to trigger in-memory database
        database_path: "".to_owned(),
        model_name: "gpt-4".to_owned(),
        qdrant_url: "http://localhost:6333".to_owned(),
        solana_rpc_url: "https://api.mainnet-beta.solana.com".to_owned(),
    }
}

// ============================================================================
// NON-UNIFFI INTERNAL IMPLEMENTATION
// ============================================================================

impl FiqhAISystem {
    // Internal method to create HTTP API router (not exposed to mobile)
    // pub fn create_api_router(&self) -> axum::Router {
    //     // Only create router if history actor is available (non-mobile mode)
    //     if let Some(ref history_actor) = self.history_actor {
    //         let _app_state = AppState {
    //             query_actor: self.query_actor.clone(),
    //             analyzer_actor: self.analyzer_actor.clone(),
    //             history_actor: history_actor.clone(),
    //         };

    //         // Create middleware state
    //         // ... rest of router creation
    //         // todo("Complete router implementation for non-mobile mode")
    //     } else {
    //         // Return empty router for mobile mode
    //         axum::Router::new()
    //     }
    // }
}

// ============================================================================
// TESTS
// ============================================================================

#[cfg(test)]
mod mobile_tests {
    use super::*;

    #[tokio::test]
    async fn test_audio_processor() {
        let processor = AudioProcessor::new();
        let result = processor.transcribe_audio(vec![1, 2, 3, 4], Some("id".to_owned())).await;
        assert!(result.is_ok());
        assert!(!result.unwrap().is_empty());
    }

    #[tokio::test]
    async fn test_solana_connector() {
        let connector = SolanaConnector::new(None);
        assert_eq!(connector.get_network_name(), "Solana Mainnet");

        let is_connected = connector.is_connected().await.unwrap();
        assert!(is_connected);
    }

    #[test]
    fn test_ruling_helpers() {
        assert_eq!(get_ruling_display_text(IslamicPrinciple::Halal), "Halal (Diperbolehkan)");
        assert!(is_ruling_permissible(IslamicPrinciple::Halal));
        assert!(!is_ruling_permissible(IslamicPrinciple::Haram));
        assert_eq!(get_ruling_emoji(IslamicPrinciple::Halal), "✅");
    }

    #[test]
    fn test_config_helpers() {
        let default_config = create_default_config();
        assert_eq!(default_config.model_name, "gpt-4");

        let mobile_config = create_mobile_config(
            Some("groq_key".to_owned()),
            Some("grok_key".to_owned()),
            Some("test_key".to_owned()),
            true,
        );
        assert_eq!(mobile_config.openai_api_key, "test_key");
        assert_eq!(mobile_config.groq_api_key, "groq_key");
        assert_eq!(mobile_config.grok_api_key, "grok_key");
        assert!(mobile_config.enable_solana);
    }
}
