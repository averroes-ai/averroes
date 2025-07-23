use std::collections::HashMap;

use chrono::Utc;
use tokio::sync::mpsc;
use tracing::debug;
use tracing::error;
use tracing::info;
use tracing::warn;
use uuid::Uuid;

use crate::models::AnalyzerActorHandle;
use crate::models::HistoryActorHandle;
use crate::models::Query;
use crate::models::QueryMessage;
use crate::models::QueryResponse;
use crate::models::QueryType;
use crate::models::ScraperActorHandle;

pub struct QueryActor {
    receiver: mpsc::Receiver<QueryMessage>,
    scraper_handle: ScraperActorHandle,
    analyzer_handle: AnalyzerActorHandle,
    history_handle: HistoryActorHandle,
    query_cache: HashMap<String, Vec<Query>>, // user_id -> queries
}

impl QueryActor {
    pub fn new(
        receiver: mpsc::Receiver<QueryMessage>,
        scraper_handle: ScraperActorHandle,
        analyzer_handle: AnalyzerActorHandle,
        history_handle: HistoryActorHandle,
    ) -> Self {
        Self {
            receiver,
            scraper_handle,
            analyzer_handle,
            history_handle,
            query_cache: HashMap::new(),
        }
    }

    pub async fn run(&mut self) {
        info!("QueryActor started");

        while let Some(msg) = self.receiver.recv().await {
            debug!("QueryActor received message: {:?}", msg);

            match msg {
                QueryMessage::ProcessQuery {
                    query,
                    respond_to,
                } => {
                    let response = self.handle_query(query).await;
                    if let Err(e) = respond_to.send(response) {
                        error!("Failed to send query response: {:?}", e);
                    }
                },
                QueryMessage::GetQueryHistory {
                    user_id,
                    limit,
                    respond_to,
                } => {
                    let history = self.get_user_history(&user_id, limit);
                    if let Err(e) = respond_to.send(history) {
                        error!("Failed to send query history: {:?}", e);
                    }
                },
                QueryMessage::ProcessAudioQuery {
                    audio_data,
                    user_id,
                    respond_to,
                } => {
                    let response = self.handle_audio_query(audio_data, user_id).await;
                    if let Err(e) = respond_to.send(response) {
                        error!("Failed to send audio query response: {:?}", e);
                    }
                },
            }
        }

        warn!("QueryActor shutting down");
    }

    async fn handle_query(
        &mut self,
        query: Query,
    ) -> QueryResponse {
        info!("Processing query: {:?}", query.query_type);
        let start_time = std::time::Instant::now();

        // Store query in cache
        if let Some(user_id) = &query.user_id {
            self.query_cache.entry(user_id.clone()).or_default().push(query.clone());
        }

        let response = match &query.query_type {
            QueryType::Text {
                text,
            } => self.process_text_query(query.clone(), text.clone()).await,
            QueryType::TokenTicker {
                ticker,
            } => self.process_token_query(query.clone(), ticker.clone()).await,
            QueryType::ContractAddress {
                address,
            } => self.process_contract_query(query.clone(), address.clone()).await,
            QueryType::FollowUp {
                original_query_id,
                question,
            } => match Uuid::parse_str(original_query_id) {
                Ok(original_id) => self.process_follow_up_query(query.clone(), original_id, question.clone()).await,
                Err(_) => QueryResponse {
                    query_id: query.id.clone(),
                    response: "Invalid follow-up query ID".to_owned(),
                    confidence: 0.0,
                    sources: vec![],
                    follow_up_questions: vec![],
                    timestamp: Utc::now().timestamp_millis() as u64,
                    analysis_id: None,
                },
            },
            QueryType::Audio {
                audio_data: _,
            } => {
                // Mock audio processing
                QueryResponse {
                    query_id: query.id.clone(),
                    response: "Audio query processing not implemented yet".to_owned(),
                    confidence: 0.5,
                    sources: vec![],
                    follow_up_questions: vec![],
                    timestamp: Utc::now().timestamp_millis() as u64,
                    analysis_id: None,
                }
            },
        };

        let processing_time = start_time.elapsed();
        info!("Query processed in {:?}", processing_time);

        response
    }

    async fn process_text_query(
        &self,
        query: Query,
        text: String,
    ) -> QueryResponse {
        // Extract keywords for scraping
        let keywords = self.extract_keywords(&text);

        // Check if this looks like a token-related query
        if self.is_token_related(&text) {
            // Try to extract token symbol or address from text
            if let Some(token_identifier) = self.extract_token_identifier(&text) {
                return if token_identifier.len() > 20 {
                    // Looks like a contract address
                    self.process_contract_query(query, token_identifier).await
                } else {
                    // Looks like a ticker symbol
                    self.process_token_query(query, token_identifier).await
                };
            }
        }

        // General text query - scrape relevant sources and analyze
        let scraping_results = self
            .scraper_handle
            .batch_scrape(
                vec![
                    "https://cryptohalal.cc".to_owned(),
                    format!("https://www.google.com/search?q={}", urlencoding::encode(&text)),
                ],
                keywords.clone(),
            )
            .await;

        let scraped_data = match scraping_results {
            Ok(results) => results.into_iter().filter_map(|r| r.ok()).collect(),
            Err(e) => {
                warn!("Failed to scrape data for text query: {:?}", e);
                Vec::new()
            },
        };

        // Analyze the scraped data
        match self.analyzer_handle.analyze_token(query.clone(), scraped_data).await {
            Ok(analysis) => {
                // Save analysis to history
                if let Err(e) = self.history_handle.save_analysis(analysis.clone(), query.clone()).await {
                    warn!("Failed to save analysis to history: {:?}", e);
                }

                QueryResponse {
                    query_id: query.id.clone(),
                    response: analysis.islamic_analysis.reasoning.clone(),
                    confidence: analysis.islamic_analysis.confidence,
                    sources: analysis.scraped_data.iter().map(|s| s.source_url.clone()).collect(),
                    follow_up_questions: self.generate_follow_up_questions(&analysis.islamic_analysis.ruling),
                    timestamp: Utc::now().timestamp_millis() as u64,
                    analysis_id: Some(analysis.id),
                }
            },
            Err(e) => {
                error!("Failed to analyze text query: {:?}", e);
                QueryResponse {
                    query_id: query.id.clone(),
                    response: "Maaf, terjadi kesalahan dalam menganalisis permintaan Anda. Silakan coba lagi."
                        .to_owned(),
                    confidence: 0.0,
                    sources: Vec::new(),
                    follow_up_questions: Vec::new(),
                    timestamp: Utc::now().timestamp_millis() as u64,
                    analysis_id: None,
                }
            },
        }
    }

    async fn process_token_query(
        &self,
        query: Query,
        ticker: String,
    ) -> QueryResponse {
        info!("Processing token ticker query: {}", ticker);

        // Scrape data from multiple sources
        let scraping_results = self
            .scraper_handle
            .batch_scrape(
                vec![
                    format!("https://cryptohalal.cc/search?q={}", ticker),
                    format!("https://www.coingecko.com/en/coins/{}", ticker.to_lowercase()),
                    format!("https://coinmarketcap.com/currencies/{}/", ticker.to_lowercase()),
                ],
                vec![ticker.clone(), "halal".to_owned(), "haram".to_owned(), "islamic".to_owned()],
            )
            .await;

        let scraped_data = match scraping_results {
            Ok(results) => results.into_iter().filter_map(|r| r.ok()).collect(),
            Err(e) => {
                warn!("Failed to scrape data for token {}: {:?}", ticker, e);
                Vec::new()
            },
        };

        // Analyze the token
        match self.analyzer_handle.analyze_token(query.clone(), scraped_data).await {
            Ok(analysis) => {
                // Save to history
                if let Err(e) = self.history_handle.save_analysis(analysis.clone(), query.clone()).await {
                    warn!("Failed to save token analysis to history: {:?}", e);
                }

                let ruling_text = self.format_islamic_ruling(&analysis.islamic_analysis);

                QueryResponse {
                    query_id: query.id.clone(),
                    response: format!(
                        "Analisis token {}: {}\n\nAlasan: {}",
                        ticker.to_uppercase(),
                        ruling_text,
                        analysis.islamic_analysis.reasoning
                    ),
                    confidence: analysis.islamic_analysis.confidence,
                    sources: analysis.scraped_data.iter().map(|s| s.source_url.clone()).collect(),
                    follow_up_questions: self.generate_follow_up_questions(&analysis.islamic_analysis.ruling),
                    timestamp: Utc::now().timestamp_millis() as u64,
                    analysis_id: Some(analysis.id),
                }
            },
            Err(e) => {
                error!("Failed to analyze token {}: {:?}", ticker, e);
                QueryResponse {
                    query_id: query.id.clone(),
                    response: format!(
                        "Maaf, tidak dapat menganalisis token {ticker}. Pastikan ticker benar dan coba lagi."
                    ),
                    confidence: 0.0,
                    sources: Vec::new(),
                    follow_up_questions: vec![
                        "Apakah Anda ingin mencari token dengan nama yang berbeda?".to_owned(),
                        "Apakah Anda memiliki alamat kontrak token ini?".to_owned(),
                    ],
                    timestamp: Utc::now().timestamp_millis() as u64,
                    analysis_id: None,
                }
            },
        }
    }

    async fn process_contract_query(
        &self,
        query: Query,
        contract_address: String,
    ) -> QueryResponse {
        info!("Processing contract address query: {}", contract_address);

        // Get Solana token info
        let token_info_result = self.analyzer_handle.get_solana_token_info(contract_address.clone()).await;

        let token_info = match token_info_result {
            Ok(info) => Some(info),
            Err(e) => {
                warn!("Failed to get Solana token info for {}: {:?}", contract_address, e);
                None
            },
        };

        // Scrape data using token name if available
        let search_terms = if let Some(ref info) = token_info {
            vec![info.metadata.name.clone(), info.metadata.symbol.clone()]
        } else {
            vec![contract_address.clone()]
        };

        let scraping_results = self
            .scraper_handle
            .batch_scrape(
                vec![
                    format!("https://cryptohalal.cc/search?q={}", contract_address),
                    format!("https://solscan.io/token/{}", contract_address),
                ],
                search_terms,
            )
            .await;

        let scraped_data = match scraping_results {
            Ok(results) => results.into_iter().filter_map(|r| r.ok()).collect(),
            Err(e) => {
                warn!("Failed to scrape data for contract {}: {:?}", contract_address, e);
                Vec::new()
            },
        };

        // Analyze the token
        match self.analyzer_handle.analyze_token(query.clone(), scraped_data).await {
            Ok(analysis) => {
                // Save to history
                if let Err(e) = self.history_handle.save_analysis(analysis.clone(), query.clone()).await {
                    warn!("Failed to save contract analysis to history: {:?}", e);
                }

                let token_name = token_info
                    .as_ref()
                    .map(|t| format!("{} ({})", t.metadata.name, t.metadata.symbol))
                    .unwrap_or_else(|| "Token".to_owned());

                let ruling_text = self.format_islamic_ruling(&analysis.islamic_analysis);

                QueryResponse {
                    query_id: query.id.clone(),
                    response: format!(
                        "Analisis {} ({}): {}\n\nAlasan: {}",
                        token_name, contract_address, ruling_text, analysis.islamic_analysis.reasoning
                    ),
                    confidence: analysis.islamic_analysis.confidence,
                    sources: analysis.scraped_data.iter().map(|s| s.source_url.clone()).collect(),
                    follow_up_questions: self.generate_follow_up_questions(&analysis.islamic_analysis.ruling),
                    timestamp: Utc::now().timestamp_millis() as u64,
                    analysis_id: Some(analysis.id),
                }
            },
            Err(e) => {
                error!("Failed to analyze contract {}: {:?}", contract_address, e);
                QueryResponse {
                    query_id: query.id.clone(),
                    response: format!("Maaf, tidak dapat menganalisis token dengan alamat kontrak {contract_address}."),
                    confidence: 0.0,
                    sources: Vec::new(),
                    follow_up_questions: vec![
                        "Apakah alamat kontrak sudah benar?".to_owned(),
                        "Apakah Anda ingin mencari dengan ticker token?".to_owned(),
                    ],
                    timestamp: Utc::now().timestamp_millis() as u64,
                    analysis_id: None,
                }
            },
        }
    }

    async fn process_follow_up_query(
        &self,
        query: Query,
        original_query_id: Uuid,
        question: String,
    ) -> QueryResponse {
        info!("Processing follow-up query for original query: {}", original_query_id);

        // For now, we'll treat follow-up questions as new text queries with context
        // In a more sophisticated implementation, we'd retrieve the original analysis
        // and provide contextual answers

        let context_aware_text = format!("Follow-up question: {question}");
        let mut context_query = query.clone();
        context_query.query_type = QueryType::Text {
            text: context_aware_text,
        };

        let context_response = self.process_text_query(context_query, question).await;

        QueryResponse {
            query_id: query.id.clone(),
            response: context_response.response,
            confidence: 0.6,
            sources: vec!["Previous context".to_owned()],
            follow_up_questions: vec!["Any other questions?".to_owned()],
            timestamp: Utc::now().timestamp_millis() as u64,
            analysis_id: None,
        }
    }

    async fn handle_audio_query(
        &self,
        audio_data: Vec<u8>,
        user_id: Option<String>,
    ) -> QueryResponse {
        info!("Processing audio query (size: {} bytes)", audio_data.len());

        // Mock STT processing - in real implementation, this would use actual STT service
        let transcribed_text = self.mock_speech_to_text(&audio_data).await;

        let query = Query::new_text(transcribed_text.clone(), user_id, Some("id".to_owned()));

        // Add context that this came from audio
        let mut audio_response = self.process_text_query(query.clone(), transcribed_text).await;
        audio_response.response = format!("(Dari audio) {}", audio_response.response);

        audio_response
    }

    fn get_user_history(
        &self,
        user_id: &str,
        limit: Option<usize>,
    ) -> Vec<Query> {
        self.query_cache
            .get(user_id)
            .map(|queries| {
                let limit = limit.unwrap_or(10);
                queries.iter().rev().take(limit).cloned().collect()
            })
            .unwrap_or_default()
    }

    // Helper functions
    fn extract_keywords(
        &self,
        text: &str,
    ) -> Vec<String> {
        let common_islamic_terms = vec![
            "halal",
            "haram",
            "riba",
            "gharar",
            "maysir",
            "syariah",
            "islamic",
            "muslim",
            "fatwa",
            "ulama",
            "fiqh",
            "maqashid",
            "crypto",
            "token",
            "blockchain",
        ];

        let mut keywords: Vec<String> = Vec::new();
        let text_lower = text.to_lowercase();

        for term in &common_islamic_terms {
            if text_lower.contains(term) {
                keywords.push((*term).to_owned());
            }
        }

        // Add first few words as potential keywords
        keywords.extend(
            text.split_whitespace()
                .take(3)
                .map(|w| w.to_lowercase())
                .filter(|w| w.len() > 2),
        );

        keywords.into_iter().take(10).collect()
    }

    fn is_token_related(
        &self,
        text: &str,
    ) -> bool {
        let token_indicators = [
            "token", "crypto", "koin", "coin", "btc", "eth", "sol", "usdt", "usdc", "$", "address", "contract", "mint",
            "ticker", "symbol",
        ];

        let text_lower = text.to_lowercase();
        token_indicators.iter().any(|indicator| text_lower.contains(indicator))
    }

    fn extract_token_identifier(
        &self,
        text: &str,
    ) -> Option<String> {
        // Look for potential token symbols (2-10 chars, alphanumeric)
        let words: Vec<&str> = text.split_whitespace().collect();
        for word in &words {
            let clean_word = word.trim_matches(|c: char| !c.is_alphanumeric());
            if clean_word.len() >= 2 && clean_word.len() <= 50 && clean_word.chars().all(|c| c.is_alphanumeric()) {
                // Check if it looks like a Solana address (base58, ~44 chars)
                if clean_word.len() > 30 {
                    return Some(clean_word.to_owned());
                }
                // Or a typical token symbol
                if clean_word.len() <= 10 {
                    return Some(clean_word.to_uppercase());
                }
            }
        }
        None
    }

    fn format_islamic_ruling(
        &self,
        analysis: &crate::models::IslamicAnalysis,
    ) -> String {
        use crate::models::IslamicPrinciple;

        match &analysis.ruling {
            IslamicPrinciple::Halal => "HALAL ✅",
            IslamicPrinciple::Haram => "HARAM ❌",
            IslamicPrinciple::Makruh => "MAKRUH ⚠️",
            IslamicPrinciple::Mustahab => "MUSTAHAB ⭐",
            IslamicPrinciple::Mubah => "MUBAH ⚪",
            IslamicPrinciple::Riba => "MENGANDUNG RIBA ❌",
            IslamicPrinciple::Gharar => "MENGANDUNG GHARAR ⚠️",
            IslamicPrinciple::Maysir => "MENGANDUNG MAYSIR 🎰",
            IslamicPrinciple::Syubhat => "SYUBHAT - MERAGUKAN ⚠️",
        }
        .to_owned()
    }

    fn generate_follow_up_questions(
        &self,
        ruling: &crate::models::IslamicPrinciple,
    ) -> Vec<String> {
        use crate::models::IslamicPrinciple;

        match ruling {
            IslamicPrinciple::Halal => vec![
                "Apakah Anda ingin mengetahui lebih lanjut tentang aspek halal dari token ini?".to_owned(),
                "Ingin melihat fatwa terkait yang mendukung analisis ini?".to_owned(),
                "Apakah Anda ingin menganalisis token lainnya?".to_owned(),
            ],
            IslamicPrinciple::Haram => vec![
                "Apakah Anda ingin mengetahui alternatif investasi yang halal?".to_owned(),
                "Ingin memahami lebih dalam mengapa token ini haram?".to_owned(),
                "Apakah Anda ingin melihat fatwa dari negara lain?".to_owned(),
            ],
            _ => vec![
                "Apakah Anda ingin penjelasan lebih detail tentang ruling ini?".to_owned(),
                "Ingin melihat pandangan ulama lain tentang topik ini?".to_owned(),
                "Apakah ada aspek lain yang ingin Anda tanyakan?".to_owned(),
            ],
        }
    }

    async fn mock_speech_to_text(
        &self,
        _audio_data: &[u8],
    ) -> String {
        // Mock implementation - in real app, this would call actual STT service
        tokio::time::sleep(std::time::Duration::from_millis(500)).await;
        "Apakah bitcoin halal menurut Islam?".to_owned() // Mock transcription
    }
}

// Actor spawner function
pub async fn spawn_query_actor(
    scraper_handle: ScraperActorHandle,
    analyzer_handle: AnalyzerActorHandle,
    history_handle: HistoryActorHandle,
) -> crate::models::QueryActorHandle {
    let (sender, receiver) = mpsc::channel(100);

    let mut actor = QueryActor::new(receiver, scraper_handle, analyzer_handle, history_handle);

    tokio::spawn(async move {
        actor.run().await;
    });

    crate::models::QueryActorHandle {
        sender,
    }
}
