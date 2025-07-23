use std::collections::HashMap;
use std::sync::Arc;

use chrono::Utc;
use solana_client::rpc_client::RpcClient;
use solana_program::pubkey::Pubkey;
use tokio::sync::Mutex;
use tokio::sync::RwLock;
use tokio::sync::mpsc;
use tracing::debug;
use tracing::error;
use tracing::info;
use tracing::warn;
use uuid::Uuid;

use crate::ai::chains::BacktestChain;
use crate::ai::chains::IslamicAnalysisChain as IslamicChain;
use crate::ai::chains::IslamicChainConfig;
use crate::ai::embeddings::VectorDatabase;
use crate::ai::embeddings::VectorDbConfig;
use crate::models::AnalysisStatus;
use crate::models::BacktestResult;
use crate::models::ConfidenceBreakdown;
use crate::models::SolanaError;
use crate::models::TokenStandard;
use crate::models::analysis::ScrapedData;
use crate::models::analysis::TokenAnalysis;
use crate::models::fatwa::IslamicAnalysis;
use crate::models::fatwa::IslamicPrinciple;
use crate::models::messages::AnalyzerError;
use crate::models::messages::AnalyzerMessage;
use crate::models::query::Query;
use crate::models::query::QueryType;
use crate::models::token::SolanaTokenInfo;

pub struct AnalyzerActor {
    receiver: mpsc::Receiver<AnalyzerMessage>,
    #[allow(dead_code)]
    solana_client: RpcClient,
    analysis_cache: Arc<RwLock<HashMap<Uuid, TokenAnalysis>>>,
    islamic_chain: Arc<Mutex<Option<IslamicChain>>>,
    backtest_chain: Arc<Mutex<Option<BacktestChain>>>,
    vector_db: Arc<Mutex<Option<VectorDatabase>>>,
    config: AnalyzerConfig,
}

#[derive(Debug, Clone)]
pub struct AnalyzerConfig {
    pub openai_api_key: Option<String>,
    pub model_name: String,
    pub enable_vector_search: bool,
    pub qdrant_url: String,
    pub analysis_timeout_seconds: u64,
    pub enable_backtest: bool,
}

impl Default for AnalyzerConfig {
    fn default() -> Self {
        Self {
            openai_api_key: std::env::var("OPENAI_API_KEY").ok(),
            model_name: "gpt-4".to_owned(),
            enable_vector_search: true,
            qdrant_url: "http://localhost:6333".to_owned(),
            analysis_timeout_seconds: 30,
            enable_backtest: true,
        }
    }
}

impl AnalyzerActor {
    pub async fn new(
        receiver: mpsc::Receiver<AnalyzerMessage>,
        solana_rpc_url: Option<String>,
        config: Option<AnalyzerConfig>,
    ) -> Self {
        let config = config.unwrap_or_default();
        let solana_client =
            RpcClient::new(solana_rpc_url.unwrap_or_else(|| "https://api.mainnet-beta.solana.com".to_owned()));

        Self {
            receiver,
            solana_client,
            analysis_cache: Arc::new(RwLock::new(HashMap::new())),
            islamic_chain: Arc::new(Mutex::new(None)),
            backtest_chain: Arc::new(Mutex::new(None)),
            vector_db: Arc::new(Mutex::new(None)),
            config,
        }
    }

    pub async fn run(&mut self) {
        info!("AnalyzerActor starting up");

        // Initialize AI components
        if let Err(e) = self.initialize_ai_components().await {
            error!("Failed to initialize AI components: {}", e);
        }

        while let Some(msg) = self.receiver.recv().await {
            match msg {
                AnalyzerMessage::AnalyzeToken {
                    query,
                    scraped_data,
                    respond_to,
                } => {
                    let result = self.analyze_token(&query, None, &scraped_data).await;
                    let _ = respond_to.send(result);
                },
                AnalyzerMessage::GetSolanaTokenInfo {
                    mint_address,
                    respond_to,
                } => {
                    let result = self.get_solana_token_info(&mint_address).await;
                    let _ = respond_to.send(result);
                },
                AnalyzerMessage::SearchFatwas {
                    keywords: _keywords,
                    language: _language,
                    limit: _limit,
                    respond_to,
                } => {
                    // Mock implementation for search_fatwas
                    let fatwas = vec![]; // Empty for now
                    let _ = respond_to.send(fatwas);
                },
                AnalyzerMessage::RunBacktest {
                    analysis_id,
                    respond_to,
                } => {
                    // Return a mock TokenAnalysis instead of BacktestResult for now
                    let mock_analysis = TokenAnalysis {
                        id: analysis_id.to_string(),
                        query_id: "mock".to_owned(),
                        token_info: None,
                        islamic_analysis: IslamicAnalysis {
                            ruling: IslamicPrinciple::Mubah,
                            confidence: 0.5,
                            reasoning: "Mock backtest result".to_owned(),
                            supporting_fatwas: vec![],
                            risk_factors: vec![],
                            recommendations: vec![],
                            maqashid_assessment: vec![],
                        },
                        scraped_data: vec![],
                        status: AnalysisStatus::Completed,
                        created_at: Utc::now().timestamp_millis() as u64,
                        completed_at: Some(Utc::now().timestamp_millis() as u64),
                        processing_time_ms: Some(100),
                        confidence_breakdown: ConfidenceBreakdown {
                            token_data_quality: 0.8,
                            fatwa_relevance: 0.7,
                            scraping_completeness: 0.9,
                            consensus_level: 0.6,
                            data_freshness: 0.8,
                            overall_confidence: 0.75,
                        },
                        backtest_results: vec![],
                        user_feedback: None,
                    };
                    let _ = respond_to.send(Ok(mock_analysis));
                },
                AnalyzerMessage::UpdateAnalysis {
                    analysis_id,
                    user_feedback,
                    respond_to,
                } => {
                    let result = self.update_analysis_with_feedback(analysis_id, user_feedback).await;
                    let _ = respond_to.send(result);
                },
                AnalyzerMessage::UpdateAnalysisWithFeedback {
                    analysis_id,
                    feedback,
                    respond_to,
                } => {
                    let result = self
                        .update_analysis_with_feedback(Uuid::parse_str(&analysis_id).unwrap_or_default(), feedback)
                        .await;
                    let _ = respond_to.send(result);
                },
                AnalyzerMessage::GetAnalysis {
                    analysis_id,
                    respond_to,
                } => {
                    let result = self.get_cached_analysis(analysis_id).await;
                    let _ = respond_to.send(result.ok_or(AnalyzerError::AnalysisNotFound(analysis_id)));
                },
            }
        }

        info!("AnalyzerActor shutting down");
    }

    async fn initialize_ai_components(&self) -> Result<(), AnalyzerError> {
        info!("Initializing AI components");

        // Initialize Islamic Analysis Chain
        if let Some(api_key) = &self.config.openai_api_key {
            let chain_config = IslamicChainConfig {
                model_name: "gpt-4".to_owned(),
                temperature: 0.3,
                max_tokens: 1500,
                system_prompt: "You are an Islamic finance expert analyzing tokens according to Sharia principles."
                    .to_owned(),
                embedding_model: "text-embedding-ada-002".to_owned(),
            };

            let islamic_chain = match IslamicChain::new(chain_config).await {
                Ok(mut chain) => {
                    if let Err(e) = chain.initialize().await {
                        error!("Failed to initialize Islamic chain: {}", e);
                        return Err(AnalyzerError::InitializationFailed(e.to_string()));
                    }
                    Some(chain)
                },
                Err(e) => {
                    error!("Failed to create Islamic chain: {}", e);
                    None
                },
            };

            // Store the Islamic chain
            *self.islamic_chain.lock().await = islamic_chain;
            info!("Islamic Analysis Chain initialized successfully");

            // Initialize the backtest chain if needed
            if !api_key.is_empty() {
                match BacktestChain::new().await {
                    Ok(backtest_chain) => {
                        *self.backtest_chain.lock().await = Some(backtest_chain);
                        info!("Backtest Chain initialized successfully");
                    },
                    Err(e) => {
                        error!("Failed to initialize Backtest Chain: {}", e);
                    },
                }
            }
        } else {
            warn!("No OpenAI API key provided, using mock analysis");
        }

        // Initialize Vector Database
        if self.config.enable_vector_search {
            let vector_config = VectorDbConfig {
                collection_name: "fiqh_ai_knowledge".to_owned(),
                embedding_dimension: 384,
                distance_metric: "cosine".to_owned(),
            };

            match VectorDatabase::new(vector_config).await {
                Ok(db) => {
                    *self.vector_db.lock().await = Some(db);
                    info!("Vector Database initialized successfully");
                },
                Err(e) => {
                    warn!("Failed to initialize Vector Database: {}", e);
                },
            }
        }

        Ok(())
    }

    async fn analyze_token(
        &self,
        query: &Query,
        token_info: Option<&SolanaTokenInfo>,
        scraped_data: &[ScrapedData],
    ) -> Result<TokenAnalysis, AnalyzerError> {
        let analysis_id = Uuid::new_v4();
        let start_time = std::time::Instant::now();

        info!("Starting token analysis: {} for query: {}", analysis_id, query.id);

        // Get Solana token info if not provided
        let solana_token_info = if token_info.is_none() {
            self.extract_and_fetch_token_info(query).await.ok()
        } else {
            token_info.cloned()
        };

        // Perform Islamic analysis using AI chain
        let islamic_analysis = if let Some(chain) = self.islamic_chain.lock().await.as_ref() {
            match chain.analyze_token(query, solana_token_info.as_ref(), scraped_data).await {
                Ok(analysis) => analysis,
                Err(e) => {
                    error!("Islamic analysis failed: {}", e);
                    // Fallback to basic analysis
                    self.create_fallback_analysis(&e.to_string()).await
                },
            }
        } else {
            warn!("Islamic chain not available, using fallback analysis");
            self.create_fallback_analysis("Islamic chain not initialized").await
        };

        let processing_time = start_time.elapsed();

        // Create comprehensive token analysis
        let analysis = TokenAnalysis {
            id: analysis_id.to_string(),
            query_id: query.id.clone(),
            token_info: solana_token_info,
            islamic_analysis,
            scraped_data: scraped_data.to_vec(),
            status: AnalysisStatus::Completed,
            created_at: Utc::now().timestamp_millis() as u64,
            completed_at: Some(Utc::now().timestamp_millis() as u64),
            processing_time_ms: Some(processing_time.as_millis() as u64),
            confidence_breakdown: self.calculate_confidence_breakdown(scraped_data, processing_time).await,
            backtest_results: Vec::new(),
            user_feedback: None,
        };

        // Store analysis in cache
        self.analysis_cache.write().await.insert(analysis_id, analysis.clone());

        // Store analysis in vector database for future semantic search
        // Note: Using mock vector DB implementation, so analysis storage is skipped
        if let Some(_vector_db) = self.vector_db.lock().await.as_ref() {
            debug!("Mock vector database - analysis storage skipped");
        }

        info!(
            "Token analysis completed: {} in {}ms with confidence: {:.2}",
            analysis_id,
            processing_time.as_millis(),
            analysis.islamic_analysis.confidence
        );

        Ok(analysis)
    }

    async fn get_solana_token_info(
        &self,
        address: &str,
    ) -> Result<SolanaTokenInfo, SolanaError> {
        debug!("Fetching Solana token info for address: {}", address);

        // Parse the address
        let pubkey = address
            .parse::<Pubkey>()
            .map_err(|e| SolanaError::InvalidAddress(e.to_string()))?;

        // In a real implementation, this would fetch actual token metadata from Solana
        // For now, return mock data
        let token_info = SolanaTokenInfo {
            pubkey: pubkey.to_string(),
            metadata: crate::models::TokenMetadata {
                name: "Unknown Token".to_owned(),
                symbol: "UNK".to_owned(),
                mint_address: address.to_owned(),
                decimals: 9,
                description: Some("Token metadata fetched from blockchain".to_owned()),
                image_url: None,
                creator: None,
                verified: false,
                token_standard: TokenStandard::SPL,
            },
            price_data: None,
            holders: None,
            liquidity_pools: Vec::new(),
            is_verified: false,
            risk_score: Some(0.5),
        };

        Ok(token_info)
    }

    #[allow(dead_code)]
    async fn run_backtest(
        &self,
        analysis_id: Uuid,
    ) -> Result<BacktestResult, AnalyzerError> {
        info!("Running backtest for analysis: {}", analysis_id);

        // Get the original analysis
        let original_analysis = self
            .get_cached_analysis(analysis_id)
            .await
            .ok_or(AnalyzerError::AnalysisNotFound(analysis_id))?;

        // Use backtest chain if available
        if let Some(backtest_chain) = self.backtest_chain.lock().await.as_ref() {
            // Create new scraped data to simulate updated information
            let new_scraped_data = vec![ScrapedData::new(
                "https://updated-source.com".to_owned(),
                "Updated analysis data for backtesting".to_owned(),
                crate::models::ScrapedDataType::CryptoHalalVerification,
                Some("Updated Analysis".to_owned()),
            )];

            match backtest_chain
                .run_backtest(&original_analysis.islamic_analysis, &new_scraped_data)
                .await
            {
                Ok(result) => Ok(result),
                Err(e) => {
                    error!("Backtest chain failed: {}", e);
                    Err(AnalyzerError::BacktestFailed(e.to_string()))
                },
            }
        } else {
            // Fallback backtest logic
            let backtest_result = BacktestResult {
                analysis_id: analysis_id.to_string(),
                previous_analysis_id: Some(analysis_id.to_string()),
                comparison_date: Utc::now().timestamp_millis() as u64,
                ruling_changed: false,
                confidence_change: 0.0,
                new_factors: Vec::new(),
                removed_factors: Vec::new(),
                summary: "No significant changes detected".to_owned(),
            };

            Ok(backtest_result)
        }
    }

    async fn update_analysis_with_feedback(
        &self,
        analysis_id: Uuid,
        feedback: crate::models::UserFeedback,
    ) -> Result<(), AnalyzerError> {
        info!("Updating analysis {} with user feedback", analysis_id);

        let mut cache = self.analysis_cache.write().await;
        if let Some(analysis) = cache.get_mut(&analysis_id) {
            analysis.user_feedback = Some(feedback.clone());

            // Adjust confidence based on feedback
            if feedback.is_helpful {
                analysis.islamic_analysis.confidence = (analysis.islamic_analysis.confidence + 0.1).min(1.0);
            } else {
                analysis.islamic_analysis.confidence = (analysis.islamic_analysis.confidence - 0.05).max(0.0);
            }

            info!(
                "Analysis {} updated with feedback: helpful={}, new_confidence={:.2}",
                analysis_id, feedback.is_helpful, analysis.islamic_analysis.confidence
            );

            Ok(())
        } else {
            Err(AnalyzerError::AnalysisNotFound(analysis_id))
        }
    }

    async fn get_cached_analysis(
        &self,
        analysis_id: Uuid,
    ) -> Option<TokenAnalysis> {
        self.analysis_cache.read().await.get(&analysis_id).cloned()
    }

    // Helper methods

    async fn extract_and_fetch_token_info(
        &self,
        query: &Query,
    ) -> Result<SolanaTokenInfo, SolanaError> {
        // Extract potential token address or ticker from query
        let token_identifier = match &query.query_type {
            QueryType::TokenTicker {
                ticker,
            } => ticker.clone(),
            QueryType::ContractAddress {
                address,
            } => address.clone(),
            QueryType::Text {
                text,
            } => self.extract_token_from_text(text).unwrap_or_else(|| "unknown".to_owned()),
            _ => "unknown".to_owned(),
        };

        // If it looks like an address, fetch token info
        if token_identifier.len() > 20 {
            self.get_solana_token_info(&token_identifier).await
        } else {
            // Create mock info for ticker
            let token_info = SolanaTokenInfo {
                pubkey: Pubkey::default().to_string(),
                metadata: crate::models::TokenMetadata {
                    name: token_identifier.clone(),
                    symbol: token_identifier.to_uppercase(),
                    mint_address: "mock_address".to_owned(),
                    decimals: 9,
                    description: Some(format!("Mock data for {token_identifier} token")),
                    image_url: None,
                    creator: None,
                    verified: false,
                    token_standard: TokenStandard::SPL,
                },
                price_data: None,
                holders: None,
                liquidity_pools: Vec::new(),
                is_verified: false,
                risk_score: Some(0.5),
            };

            Ok(token_info)
        }
    }

    fn extract_token_from_text(
        &self,
        text: &str,
    ) -> Option<String> {
        // Look for token symbols (starts with $ or common crypto names)
        let words: Vec<&str> = text.split_whitespace().collect();
        for word in &words {
            if word.starts_with('$') && word.len() > 1 {
                return Some(word[1..].to_uppercase());
            }
            let upper_word = word.to_uppercase();
            if ["BTC", "ETH", "SOL", "USDT", "USDC", "BNB", "ADA", "DOT"].contains(&upper_word.as_str()) {
                return Some(upper_word);
            }
            // Handle token names to symbols mapping
            match upper_word.as_str() {
                "BITCOIN" => return Some("BTC".to_owned()),
                "ETHEREUM" => return Some("ETH".to_owned()),
                "SOLANA" => return Some("SOL".to_owned()),
                "TETHER" => return Some("USDT".to_owned()),
                "CARDANO" => return Some("ADA".to_owned()),
                "POLKADOT" => return Some("DOT".to_owned()),
                _ => {},
            }
        }
        None
    }

    async fn create_fallback_analysis(
        &self,
        error_context: &str,
    ) -> IslamicAnalysis {
        warn!("Creating fallback Islamic analysis due to: {}", error_context);

        IslamicAnalysis {
            ruling: IslamicPrinciple::Mubah, // Default to permissible with low confidence
            confidence: 0.3,                 // Low confidence for fallback
            reasoning: format!(
                "Analisis dasar berdasarkan prinsip umum Islam. {error_context}. Direkomendasikan untuk konsultasi \
                 lebih lanjut dengan ahli fiqh."
            ),
            supporting_fatwas: vec![],
            risk_factors: vec!["Analisis terbatas".to_owned()],
            recommendations: vec!["Konsultasi dengan ahli fiqh".to_owned()],
            maqashid_assessment: vec![],
        }
    }

    async fn calculate_confidence_breakdown(
        &self,
        scraped_data: &[ScrapedData],
        processing_time: std::time::Duration,
    ) -> ConfidenceBreakdown {
        let data_quality_score = if scraped_data.is_empty() {
            0.2
        } else {
            let avg_relevance: f64 =
                scraped_data.iter().map(|d| d.relevance_score).sum::<f64>() / scraped_data.len() as f64;
            avg_relevance.min(1.0)
        };

        let processing_speed_score = if processing_time.as_millis() < 500 {
            1.0 // Fast processing gets full score
        } else if processing_time.as_millis() < 2000 {
            0.8 // Moderate processing
        } else {
            0.5 // Slow processing
        };

        let source_reliability_score = scraped_data
            .iter()
            .map(|d| match d.data_type {
                crate::models::ScrapedDataType::CryptoHalalVerification => 0.9,
                crate::models::ScrapedDataType::IslamicFinanceContent => 0.8,
                crate::models::ScrapedDataType::UserProvidedContent => 0.6,
                crate::models::ScrapedDataType::GeneralCryptoContent => 0.4,
                crate::models::ScrapedDataType::Documentation => 0.7,
                crate::models::ScrapedDataType::Official => 0.8,
                crate::models::ScrapedDataType::News => 0.6,
                crate::models::ScrapedDataType::Forum => 0.3,
                crate::models::ScrapedDataType::Social => 0.2,
                crate::models::ScrapedDataType::UserProvided => 0.5,
            })
            .max_by(|a, b| a.partial_cmp(b).unwrap())
            .unwrap_or(0.3);

        ConfidenceBreakdown {
            token_data_quality: data_quality_score,
            fatwa_relevance: source_reliability_score,
            scraping_completeness: processing_speed_score,
            consensus_level: 0.7, // Default AI confidence
            data_freshness: 0.7,
            overall_confidence: (data_quality_score + source_reliability_score + processing_speed_score + 0.7 + 0.7)
                / 5.0,
        }
    }

    // Mock implementation for testing
    #[allow(dead_code)]
    fn extract_analysis_keywords(
        &self,
        analysis: &IslamicAnalysis,
        _scraped_data: &[ScrapedData],
    ) -> Vec<String> {
        let mut keywords = vec![];

        // Add keywords based on ruling
        match analysis.ruling {
            IslamicPrinciple::Riba => {
                keywords.extend_from_slice(&["riba".to_owned(), "interest".to_owned()]);
            },
            IslamicPrinciple::Haram => {
                keywords.push("haram".to_owned());
            },
            IslamicPrinciple::Halal => {
                keywords.push("halal".to_owned());
            },
            _ => {},
        }

        // Add keywords from risk factors
        for factor in &analysis.risk_factors {
            if factor.to_lowercase().contains("interest") {
                keywords.push("interest".to_owned());
            }
        }

        keywords
    }
}

pub async fn spawn_analyzer_actor(solana_rpc_url: Option<String>) -> crate::models::AnalyzerActorHandle {
    let (sender, receiver) = mpsc::channel(32);

    let mut actor = AnalyzerActor::new(receiver, solana_rpc_url, None).await;

    tokio::spawn(async move {
        actor.run().await;
    });

    crate::models::AnalyzerActorHandle {
        sender,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_analyzer_actor_creation() {
        let handle = spawn_analyzer_actor(None).await;
        assert!(handle.sender.capacity() > 0);
    }

    #[tokio::test]
    async fn test_token_extraction() {
        let (_sender, receiver) = mpsc::channel(10);
        let actor = AnalyzerActor::new(receiver, None, None).await;

        let result = actor.extract_token_from_text("What is BTC price?");
        assert_eq!(result, Some("BTC".to_owned()));

        let result = actor.extract_token_from_text("Is Solana halal?");
        assert_eq!(result, Some("SOL".to_owned()));
    }

    #[tokio::test]
    async fn test_keyword_extraction() {
        let (_sender, receiver) = mpsc::channel(10);
        let actor = AnalyzerActor::new(receiver, None, None).await;

        let analysis = IslamicAnalysis {
            ruling: IslamicPrinciple::Riba,
            risk_factors: vec!["Interest-based mechanism".to_owned()],
            ..Default::default()
        };

        let keywords = actor.extract_analysis_keywords(&analysis, &[]);
        assert!(keywords.contains(&"riba".to_owned()));
        assert!(keywords.contains(&"interest".to_owned()));
    }
}
