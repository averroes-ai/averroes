use std::time::Instant;

use axum::extract::Path;
use axum::extract::Query;
use axum::extract::State;
use axum::http::HeaderMap;
use axum::http::StatusCode;
use axum::response::Json;
use serde::Deserialize;
use serde::Serialize;
use tracing::debug;
use tracing::error;
use tracing::info;
use tracing::instrument;
use tracing::warn;
use uuid::Uuid;

use crate::api::AppState;
use crate::error::ErrorResponse;
use crate::models::Query as FiqhQuery;
use crate::models::QueryType;

/// Handler for the main token analysis endpoint based on Gherkin scenarios
#[instrument(skip(state), fields(token = %token))]
pub async fn handle_token_analysis(
    Path(token): Path<String>,
    Query(params): Query<AnalysisQueryParams>,
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<TokenAnalysisResponse>, (StatusCode, Json<ErrorResponse>)> {
    let start_time = Instant::now();
    info!("Processing token analysis request for: {}", token);

    // Validate token format
    if token.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse::new("invalid_token", "Token identifier cannot be empty")),
        ));
    }

    // Create appropriate query based on input format
    let query = if token.starts_with("0x") || token.len() > 20 {
        // Likely a contract address
        FiqhQuery::new_contract_address(
            token.clone(),
            params.user_id.clone(),
            Some(params.language.unwrap_or_else(|| "id".to_owned())),
        )
    } else if let Some(stripped) = token.strip_prefix('$') {
        // Token ticker with $ prefix
        FiqhQuery::new_token_ticker(
            stripped.to_owned(),
            params.user_id.clone(),
            Some(params.language.unwrap_or_else(|| "id".to_owned())),
        )
    } else {
        // Regular token ticker
        FiqhQuery::new_token_ticker(
            token.clone(),
            params.user_id.clone(),
            Some(params.language.unwrap_or_else(|| "id".to_owned())),
        )
    };

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            let processing_time = start_time.elapsed();

            // Log performance metrics
            if processing_time.as_millis() > 500 {
                warn!("Analysis took {}ms, exceeding 500ms target", processing_time.as_millis());
            } else {
                debug!("Analysis completed in {}ms", processing_time.as_millis());
            }

            let analysis_response = TokenAnalysisResponse {
                token: token.clone(),
                original_query: token.clone(),
                ruling: extract_ruling_from_response(&response.response),
                confidence: response.confidence,
                reasoning: response.response.clone(),
                summary: generate_summary(&response.response, &token),
                sources: response.sources,
                follow_up_questions: response.follow_up_questions,
                analysis_id: response.analysis_id.and_then(|id| Uuid::parse_str(&id).ok()),
                timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                    .unwrap_or_else(chrono::Utc::now),
                processing_time_ms: processing_time.as_millis() as u64,
                maqashid_assessment: generate_maqashid_summary(&response.response),
            };

            Ok(Json(analysis_response))
        },
        Err(e) => {
            error!("Token analysis failed for {token}: {e}");
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "analysis_failed".to_owned(),
                    message: format!("Failed to analyze token: {e}"),
                    details: None,
                    timestamp: Some(chrono::Utc::now().to_rfc3339()),
                }),
            ))
        },
    }
}

/// Handler for text-based queries (implementing Gherkin text scenario)
#[instrument(skip(state, request))]
pub async fn handle_text_query(
    State(state): State<AppState>,
    Json(request): Json<TextQueryRequest>,
) -> Result<Json<TextQueryResponse>, (StatusCode, Json<ErrorResponse>)> {
    let start_time = Instant::now();
    info!("Processing text query: {}", &request.query[..50.min(request.query.len())]);

    // Validate query content
    if request.query.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "empty_query".to_owned(),
                message: "Query text cannot be empty".to_owned(),
                details: None,
                timestamp: Some(chrono::Utc::now().to_rfc3339()),
            }),
        ));
    }

    let query = FiqhQuery::new_text(
        request.query.clone(),
        request.user_id.clone(),
        Some(request.language.unwrap_or_else(|| "id".to_owned())),
    );

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            let processing_time = start_time.elapsed();

            // Extract token from query if possible
            let extracted_token = extract_token_from_text(&request.query);

            let text_response = TextQueryResponse {
                original_query: request.query,
                extracted_token,
                ruling: extract_ruling_from_response(&response.response),
                confidence: response.confidence,
                reasoning: response.response,
                sources: response.sources,
                follow_up_questions: response.follow_up_questions,
                analysis_id: response.analysis_id.and_then(|id| Uuid::parse_str(&id).ok()),
                timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                    .unwrap_or_else(chrono::Utc::now),
                processing_time_ms: processing_time.as_millis() as u64,
            };

            Ok(Json(text_response))
        },
        Err(e) => {
            error!("Text query failed: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "text_query_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                    timestamp: Some(chrono::Utc::now().to_rfc3339()),
                }),
            ))
        },
    }
}

/// Handler for audio queries (implementing Gherkin audio scenario)
#[instrument(skip(state, request), fields(audio_size = request.audio_data.len()))]
pub async fn handle_audio_query(
    State(state): State<AppState>,
    Json(request): Json<AudioQueryRequest>,
) -> Result<Json<AudioQueryResponse>, (StatusCode, Json<ErrorResponse>)> {
    let start_time = Instant::now();
    info!("Processing audio query of {} bytes", request.audio_data.len());

    // Validate audio data
    if request.audio_data.is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "empty_audio".to_owned(),
                message: "Audio data cannot be empty".to_owned(),
                details: None,
                timestamp: Some(chrono::Utc::now().to_rfc3339()),
            }),
        ));
    }

    if request.audio_data.len() > 10_000_000 {
        // 10MB limit
        return Err((
            StatusCode::PAYLOAD_TOO_LARGE,
            Json(ErrorResponse {
                error: "audio_too_large".to_owned(),
                message: "Audio file exceeds 10MB limit".to_owned(),
                details: Some(serde_json::json!({
                    "max_size_bytes": 10_000_000,
                    "received_size_bytes": request.audio_data.len()
                })),
                timestamp: Some(chrono::Utc::now().to_rfc3339()),
            }),
        ));
    }

    let query = FiqhQuery::new_audio(
        request.audio_data,
        request.user_id.clone(),
        Some(request.language.unwrap_or_else(|| "id".to_owned())),
    );

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            let processing_time = start_time.elapsed();

            let audio_response = AudioQueryResponse {
                transcribed_text: extract_transcription(&response.response),
                extracted_token: extract_token_from_text(&response.response),
                ruling: extract_ruling_from_response(&response.response),
                confidence: response.confidence,
                reasoning: response.response,
                sources: response.sources,
                follow_up_questions: response.follow_up_questions,
                analysis_id: response.analysis_id.and_then(|id| Uuid::parse_str(&id).ok()),
                timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                    .unwrap_or_else(chrono::Utc::now),
                processing_time_ms: processing_time.as_millis() as u64,
                audio_processing_time_ms: 0, // Would be actual STT time
            };

            Ok(Json(audio_response))
        },
        Err(e) => {
            error!("Audio query failed: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "audio_query_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                    timestamp: Some(chrono::Utc::now().to_rfc3339()),
                }),
            ))
        },
    }
}

/// Handler for contract address analysis (implementing Gherkin contract scenario)
#[instrument(skip(state), fields(contract = %contract_address))]
pub async fn handle_contract_analysis(
    Path(contract_address): Path<String>,
    Query(params): Query<ContractAnalysisParams>,
    State(state): State<AppState>,
) -> Result<Json<ContractAnalysisResponse>, (StatusCode, Json<ErrorResponse>)> {
    let start_time = Instant::now();
    info!("Analyzing contract address: {}", contract_address);

    // Validate contract address format
    if !is_valid_contract_address(&contract_address) {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "invalid_contract_address".to_owned(),
                message: "Invalid contract address format".to_owned(),
                details: Some(serde_json::json!({
                    "address": contract_address,
                    "expected_formats": ["Solana: 32-44 characters base58", "Ethereum: 42 characters hex (0x prefix)"]
                })),
                timestamp: Some(chrono::Utc::now().to_rfc3339()),
            }),
        ));
    }

    let query = FiqhQuery::new_contract_address(
        contract_address.clone(),
        params.user_id.clone(),
        Some(params.language.unwrap_or_else(|| "id".to_owned())),
    );

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            let processing_time = start_time.elapsed();

            let contract_response = ContractAnalysisResponse {
                contract_address: contract_address.clone(),
                blockchain: detect_blockchain(&contract_address),
                token_metadata: None, // Would be populated from blockchain query
                ruling: extract_ruling_from_response(&response.response),
                confidence: response.confidence,
                reasoning: response.response.clone(),
                risk_assessment: extract_risk_factors(&response.response),
                compliance_issues: extract_compliance_issues(&response.response),
                sources: response.sources,
                analysis_id: response.analysis_id.and_then(|id| Uuid::parse_str(&id).ok()),
                timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                    .unwrap_or_else(chrono::Utc::now),
                processing_time_ms: processing_time.as_millis() as u64,
            };

            Ok(Json(contract_response))
        },
        Err(e) => {
            error!("Contract analysis failed for {contract_address}: {e}");
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "contract_analysis_failed".to_owned(),
                    message: format!("Failed to analyze contract: {e}"),
                    details: Some(serde_json::json!({
                        "contract_address": contract_address
                    })),
                    timestamp: Some(chrono::Utc::now().to_rfc3339()),
                }),
            ))
        },
    }
}

/// Handler for follow-up questions (implementing interactive flow)
#[instrument(skip(state, request))]
pub async fn handle_follow_up_query(
    State(state): State<AppState>,
    Json(request): Json<FollowUpRequest>,
) -> Result<Json<FollowUpResponse>, (StatusCode, Json<ErrorResponse>)> {
    let start_time = Instant::now();
    info!("Processing follow-up query for analysis: {:?}", request.original_analysis_id);

    let query = FiqhQuery {
        id: Uuid::new_v4().to_string(),
        query_type: QueryType::FollowUp {
            original_query_id: request.original_analysis_id.unwrap_or_else(Uuid::new_v4).to_string(),
            question: request.question.clone(),
        },
        user_id: request.user_id.clone(),
        timestamp: chrono::Utc::now().timestamp_millis() as u64,
        language: request.language.unwrap_or_else(|| "id".to_owned()),
        context: Some(request.context.unwrap_or_default()),
    };

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            let processing_time = start_time.elapsed();

            let follow_up_response = FollowUpResponse {
                original_analysis_id: request.original_analysis_id,
                question: request.question,
                answer: response.response,
                confidence: response.confidence,
                additional_sources: response.sources,
                related_questions: response.follow_up_questions,
                timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                    .unwrap_or_else(chrono::Utc::now),
                processing_time_ms: processing_time.as_millis() as u64,
            };

            Ok(Json(follow_up_response))
        },
        Err(e) => {
            error!("Follow-up query failed: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "follow_up_failed".to_owned(),
                    message: format!("Failed to process follow-up query: {e}"),
                    details: None,
                    timestamp: Some(chrono::Utc::now().to_rfc3339()),
                }),
            ))
        },
    }
}

/// Handler for batch analysis requests
#[instrument(skip(state, request), fields(batch_size = request.tokens.len()))]
pub async fn handle_batch_analysis(
    State(state): State<AppState>,
    Json(request): Json<BatchAnalysisRequest>,
) -> Result<Json<BatchAnalysisResponse>, (StatusCode, Json<ErrorResponse>)> {
    let start_time = Instant::now();
    info!("Processing batch analysis for {} tokens", request.tokens.len());

    // Validate batch size
    if request.tokens.is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "empty_batch".to_owned(),
                message: "Token list cannot be empty".to_owned(),
                details: None,
                timestamp: Some(chrono::Utc::now().to_rfc3339()),
            }),
        ));
    }

    if request.tokens.len() > 10 {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "batch_too_large".to_owned(),
                message: "Batch size exceeds maximum of 10 tokens".to_owned(),
                details: Some(serde_json::json!({
                    "max_batch_size": 10,
                    "requested_size": request.tokens.len()
                })),
                timestamp: Some(chrono::Utc::now().to_rfc3339()),
            }),
        ));
    }

    let mut results = Vec::new();
    let mut errors = Vec::new();

    // Process each token in the batch
    for token in &request.tokens {
        let query = FiqhQuery::new_token_ticker(
            token.clone(),
            request.user_id.clone(),
            Some(request.language.clone().unwrap_or_else(|| "id".to_owned())),
        );

        match state.query_actor.process_query(query).await {
            Ok(response) => {
                results.push(BatchAnalysisResult {
                    token: token.clone(),
                    ruling: extract_ruling_from_response(&response.response),
                    confidence: response.confidence,
                    summary: generate_summary(&response.response, token),
                    analysis_id: response.analysis_id.and_then(|id| Uuid::parse_str(&id).ok()),
                });
            },
            Err(e) => {
                errors.push(BatchAnalysisError {
                    token: token.clone(),
                    error: e.to_string(),
                });
            },
        }
    }

    let processing_time = start_time.elapsed();

    let errors_count = errors.len();
    let total_requested = request.tokens.len();

    let batch_response = BatchAnalysisResponse {
        successful_analyses: results,
        failed_analyses: errors,
        total_requested,
        total_successful: total_requested - errors_count,
        total_failed: errors_count,
        processing_time_ms: processing_time.as_millis() as u64,
        timestamp: chrono::DateTime::from_timestamp_millis(chrono::Utc::now().timestamp_millis())
            .unwrap_or_else(chrono::Utc::now),
    };

    Ok(Json(batch_response))
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

fn extract_ruling_from_response(response: &str) -> String {
    let response_lower = response.to_lowercase();

    if response_lower.contains("halal") && !response_lower.contains("haram") {
        "HALAL".to_owned()
    } else if response_lower.contains("haram") {
        "HARAM".to_owned()
    } else if response_lower.contains("riba") {
        "RIBA (HARAM)".to_owned()
    } else if response_lower.contains("gharar") {
        "GHARAR (SYUBHAT)".to_owned()
    } else if response_lower.contains("maysir") || response_lower.contains("gambling") {
        "MAYSIR (HARAM)".to_owned()
    } else if response_lower.contains("makruh") {
        "MAKRUH".to_owned()
    } else if response_lower.contains("mustahab") {
        "MUSTAHAB".to_owned()
    } else {
        "MUBAH".to_owned()
    }
}

fn generate_summary(
    response: &str,
    token: &str,
) -> String {
    let ruling = extract_ruling_from_response(response);
    match ruling.as_str() {
        "HALAL" => format!("Token {token} dinilai halal berdasarkan analisis syariah"),
        "HARAM" => format!("Token {token} dinilai haram karena mengandung unsur yang dilarang dalam Islam"),
        "RIBA (HARAM)" => format!("Token {token} mengandung unsur riba yang diharamkan dalam Islam"),
        "GHARAR (SYUBHAT)" => format!("Token {token} mengandung unsur gharar (ketidakpastian berlebihan)"),
        "MAYSIR (HARAM)" => format!("Token {token} terkait dengan aktivitas maysir (perjudian)"),
        _ => format!("Status token {token} perlu kajian lebih lanjut"),
    }
}

fn extract_token_from_text(text: &str) -> Option<String> {
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
    }
    None
}

fn extract_transcription(response: &str) -> String {
    // In a real implementation, this would extract the transcribed text
    // For now, return a placeholder
    response.lines().next().unwrap_or(response).to_owned()
}

fn generate_maqashid_summary(response: &str) -> Vec<MaqashidAssessment> {
    vec![
        MaqashidAssessment {
            principle: "Hifz al-Mal (Menjaga Harta)".to_owned(),
            score: if response.contains("halal") {
                0.9
            } else {
                0.3
            },
            description: "Penilaian terhadap pelestarian dan perlindungan harta".to_owned(),
        },
        MaqashidAssessment {
            principle: "Hifz al-Din (Menjaga Agama)".to_owned(),
            score: if response.contains("haram") {
                0.1
            } else {
                0.8
            },
            description: "Kesesuaian dengan nilai-nilai agama Islam".to_owned(),
        },
    ]
}

fn is_valid_contract_address(address: &str) -> bool {
    // Ethereum address: 0x followed by 40 hex characters
    if address.starts_with("0x") && address.len() == 42 {
        return address[2..].chars().all(|c| c.is_ascii_hexdigit());
    }

    // Solana address: 32-44 base58 characters
    if address.len() >= 32 && address.len() <= 44 {
        return address
            .chars()
            .all(|c| "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".contains(c));
    }

    false
}

fn detect_blockchain(address: &str) -> String {
    if address.starts_with("0x") && address.len() == 42 {
        "Ethereum".to_owned()
    } else if address.len() >= 32 && address.len() <= 44 {
        "Solana".to_owned()
    } else {
        "Unknown".to_owned()
    }
}

fn extract_risk_factors(response: &str) -> Vec<String> {
    let mut risk_factors = Vec::new();

    if response.to_lowercase().contains("riba") {
        risk_factors.push("Mengandung unsur riba (bunga)".to_owned());
    }
    if response.to_lowercase().contains("gharar") {
        risk_factors.push("Mengandung gharar (ketidakpastian berlebihan)".to_owned());
    }
    if response.to_lowercase().contains("maysir") {
        risk_factors.push("Terkait dengan maysir (perjudian)".to_owned());
    }

    if risk_factors.is_empty() {
        risk_factors.push("Tidak ditemukan faktor risiko syariah yang signifikan".to_owned());
    }

    risk_factors
}

fn extract_compliance_issues(response: &str) -> Vec<String> {
    let mut issues = Vec::new();

    if response.to_lowercase().contains("haram") {
        issues.push("Aktivitas yang diharamkan dalam Islam".to_owned());
    }
    if response.to_lowercase().contains("speculation") || response.to_lowercase().contains("spekulasi") {
        issues.push("Potensi spekulasi berlebihan".to_owned());
    }

    issues
}

// ============================================================================
// REQUEST/RESPONSE TYPES
// ============================================================================

#[derive(Debug, Deserialize)]
pub struct AnalysisQueryParams {
    pub user_id: Option<String>,
    pub language: Option<String>,
    pub include_sources: Option<bool>,
}

#[derive(Debug, Deserialize)]
pub struct TextQueryRequest {
    pub query: String,
    pub user_id: Option<String>,
    pub language: Option<String>,
    pub context: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct AudioQueryRequest {
    pub audio_data: Vec<u8>,
    pub user_id: Option<String>,
    pub language: Option<String>,
    pub audio_format: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct ContractAnalysisParams {
    pub user_id: Option<String>,
    pub language: Option<String>,
    pub include_metadata: Option<bool>,
}

#[derive(Debug, Deserialize)]
pub struct FollowUpRequest {
    pub original_analysis_id: Option<Uuid>,
    pub question: String,
    pub user_id: Option<String>,
    pub language: Option<String>,
    pub context: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct BatchAnalysisRequest {
    pub tokens: Vec<String>,
    pub user_id: Option<String>,
    pub language: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct TokenAnalysisResponse {
    pub token: String,
    pub original_query: String,
    pub ruling: String,
    pub confidence: f64,
    pub reasoning: String,
    pub summary: String,
    pub sources: Vec<String>,
    pub follow_up_questions: Vec<String>,
    pub analysis_id: Option<Uuid>,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub processing_time_ms: u64,
    pub maqashid_assessment: Vec<MaqashidAssessment>,
}

#[derive(Debug, Serialize)]
pub struct TextQueryResponse {
    pub original_query: String,
    pub extracted_token: Option<String>,
    pub ruling: String,
    pub confidence: f64,
    pub reasoning: String,
    pub sources: Vec<String>,
    pub follow_up_questions: Vec<String>,
    pub analysis_id: Option<Uuid>,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub processing_time_ms: u64,
}

#[derive(Debug, Serialize)]
pub struct AudioQueryResponse {
    pub transcribed_text: String,
    pub extracted_token: Option<String>,
    pub ruling: String,
    pub confidence: f64,
    pub reasoning: String,
    pub sources: Vec<String>,
    pub follow_up_questions: Vec<String>,
    pub analysis_id: Option<Uuid>,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub processing_time_ms: u64,
    pub audio_processing_time_ms: u64,
}

#[derive(Debug, Serialize)]
pub struct ContractAnalysisResponse {
    pub contract_address: String,
    pub blockchain: String,
    pub token_metadata: Option<serde_json::Value>,
    pub ruling: String,
    pub confidence: f64,
    pub reasoning: String,
    pub risk_assessment: Vec<String>,
    pub compliance_issues: Vec<String>,
    pub sources: Vec<String>,
    pub analysis_id: Option<Uuid>,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub processing_time_ms: u64,
}

#[derive(Debug, Serialize)]
pub struct FollowUpResponse {
    pub original_analysis_id: Option<Uuid>,
    pub question: String,
    pub answer: String,
    pub confidence: f64,
    pub additional_sources: Vec<String>,
    pub related_questions: Vec<String>,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub processing_time_ms: u64,
}

#[derive(Debug, Serialize)]
pub struct BatchAnalysisResponse {
    pub successful_analyses: Vec<BatchAnalysisResult>,
    pub failed_analyses: Vec<BatchAnalysisError>,
    pub total_requested: usize,
    pub total_successful: usize,
    pub total_failed: usize,
    pub processing_time_ms: u64,
    pub timestamp: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Serialize)]
pub struct BatchAnalysisResult {
    pub token: String,
    pub ruling: String,
    pub confidence: f64,
    pub summary: String,
    pub analysis_id: Option<Uuid>,
}

#[derive(Debug, Serialize)]
pub struct BatchAnalysisError {
    pub token: String,
    pub error: String,
}

#[derive(Debug, Serialize)]
pub struct MaqashidAssessment {
    pub principle: String,
    pub score: f64,
    pub description: String,
}
