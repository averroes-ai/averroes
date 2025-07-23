use std::collections::HashMap;

use axum::Router;
use axum::extract::Path;
use axum::extract::Query;
use axum::extract::State;
use axum::http::StatusCode;
use axum::response::Json;
use axum::routing::get;
use axum::routing::post;
use serde::Deserialize;
use serde::Serialize;
use tracing::error;
use tracing::info;
use uuid::Uuid;

use super::AppState;
use crate::models::BacktestResult;
use crate::models::HistoryQuery;
use crate::models::IslamicPrinciple;
use crate::models::Query as FiqhQuery;
use crate::models::SolanaTokenInfo;
use crate::models::TokenAnalysis;
use crate::models::UserAnalysisStats;

/// Shared application state
/// Create the main API router
pub fn create_router(state: AppState) -> Router {
    Router::new()
        // Token Analysis endpoints
        .route("/analyze/:token", get(analyze_token))
        .route("/analyze", post(analyze_token_detailed))
        // Halal Index endpoints
        .route("/index/:token", get(get_token_index))
        .route("/index", get(list_halal_index))
        .route("/index", post(update_token_index))
        // History endpoints
        .route("/history/user/:user_id", get(get_user_history))
        .route("/history/token/:token", get(get_token_history))
        .route("/history/stats/:user_id", get(get_user_stats))
        // Audio analysis endpoint
        .route("/analyze/audio", post(analyze_audio))
        // Contract address analysis
        .route("/analyze/contract/:address", get(analyze_contract_address))
        // Backtest endpoints
        .route("/backtest/:analysis_id", post(run_backtest))
        .route("/backtest/results/:analysis_id", get(get_backtest_results))
        // Health and status
        .route("/health", get(health_check))
        .route("/status", get(get_system_status))
        .with_state(state)
}

// ============================================================================
// TOKEN ANALYSIS ENDPOINTS
// ============================================================================

/// Analyze a token by ticker symbol or name
async fn analyze_token(
    Path(token): Path<String>,
    Query(params): Query<AnalysisParams>,
    State(state): State<AppState>,
) -> Result<Json<AnalysisResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Analyzing token: {}", token);

    let query = FiqhQuery::new_token_ticker(
        token.clone(),
        params.user_id.clone(),
        Some(params.language.unwrap_or_else(|| "id".to_owned())),
    );

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            let analysis_response = AnalysisResponse {
                token,
                ruling: format!(
                    "{:?}",
                    response
                        .analysis_id
                        .as_ref()
                        .map(|_| IslamicPrinciple::Halal)
                        .unwrap_or(IslamicPrinciple::Mubah)
                ),
                confidence: response.confidence,
                reasoning: response.response,
                sources: response.sources,
                follow_up_questions: response.follow_up_questions,
                analysis_id: response.analysis_id.clone().and_then(|id| Uuid::parse_str(&id).ok()),
                timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                    .unwrap_or_else(chrono::Utc::now),
            };
            Ok(Json(analysis_response))
        },
        Err(e) => {
            error!("Token analysis failed: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "analysis_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

/// Detailed token analysis with additional parameters
async fn analyze_token_detailed(
    State(state): State<AppState>,
    Json(request): Json<DetailedAnalysisRequest>,
) -> Result<Json<DetailedAnalysisResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Performing detailed analysis for token: {}", request.token);

    let query = match request.input_type.as_str() {
        "ticker" => {
            FiqhQuery::new_token_ticker(request.token.clone(), request.user_id.clone(), Some(request.language.clone()))
        },
        "contract" => FiqhQuery::new_contract_address(
            request.token.clone(),
            request.user_id.clone(),
            Some(request.language.clone()),
        ),
        "text" => FiqhQuery::new_text(request.token.clone(), request.user_id.clone(), Some(request.language.clone())),
        _ => {
            return Err((
                StatusCode::BAD_REQUEST,
                Json(ErrorResponse {
                    error: "invalid_input_type".to_owned(),
                    message: "Input type must be 'ticker', 'contract', or 'text'".to_owned(),
                    details: None,
                }),
            ));
        },
    };

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            // Get detailed analysis if available
            // Mock implementation - detailed analysis not available yet
            let detailed_analysis: Option<TokenAnalysis> = None;

            let detailed_response = DetailedAnalysisResponse {
                basic: AnalysisResponse {
                    token: request.token,
                    ruling: format!(
                        "{:?}",
                        response
                            .analysis_id
                            .as_ref()
                            .map(|_| IslamicPrinciple::Halal)
                            .unwrap_or(IslamicPrinciple::Mubah)
                    ),
                    confidence: response.confidence,
                    reasoning: response.response,
                    sources: response.sources,
                    follow_up_questions: response.follow_up_questions,
                    analysis_id: response.analysis_id.clone().and_then(|id| Uuid::parse_str(&id).ok()),
                    timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                        .unwrap_or_else(chrono::Utc::now),
                },
                detailed_analysis: detailed_analysis.clone(),
                maqashid_assessment: detailed_analysis
                    .as_ref()
                    .map(|a| a.islamic_analysis.maqashid_assessment.clone())
                    .unwrap_or_default(),
                risk_factors: detailed_analysis
                    .as_ref()
                    .map(|a| a.islamic_analysis.risk_factors.clone())
                    .unwrap_or_default(),
                recommendations: detailed_analysis
                    .as_ref()
                    .map(|a| a.islamic_analysis.recommendations.clone())
                    .unwrap_or_default(),
            };

            Ok(Json(detailed_response))
        },
        Err(e) => {
            error!("Detailed token analysis failed: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "detailed_analysis_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

// ============================================================================
// HALAL INDEX ENDPOINTS
// ============================================================================

/// Get halal index status for a specific token
async fn get_token_index(
    Path(token): Path<String>,
    State(state): State<AppState>,
) -> Result<Json<TokenIndexResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Getting halal index for token: {}", token);

    // Query recent analyses for this token
    let history_query = HistoryQuery::new().for_token(token.clone()).limit(5);

    match state.history_actor.query_analyses(history_query).await {
        Ok(history) => {
            let latest_analysis = history.entries.first();

            let index_response = TokenIndexResponse {
                token: token.clone(),
                halal_status: latest_analysis
                    .map(|entry| format!("{:?}", entry.ruling))
                    .unwrap_or_else(|| "UNKNOWN".to_owned()),
                confidence: latest_analysis.map(|entry| entry.confidence).unwrap_or(0.0),
                last_updated: latest_analysis
                    .and_then(|entry| chrono::DateTime::from_timestamp_millis(entry.analyzed_at as i64))
                    .unwrap_or_else(chrono::Utc::now),
                analysis_count: history.entries.len() as u32,
                consensus_score: calculate_consensus_score(&history.entries),
            };

            Ok(Json(index_response))
        },
        Err(e) => {
            error!("Failed to get token index: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "index_retrieval_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

/// List all tokens in the halal index
async fn list_halal_index(
    Query(params): Query<IndexListParams>,
    State(state): State<AppState>,
) -> Result<Json<IndexListResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Listing halal index with filters: {:?}", params);

    let history_query = HistoryQuery::new().limit(params.limit.unwrap_or(50));

    match state.history_actor.query_analyses(history_query).await {
        Ok(history) => {
            let total_entries_count = history.entries.len() as u32;
            let mut token_summaries: HashMap<String, Vec<_>> = HashMap::new();

            // Group by token
            for entry in history.entries {
                if let Some(token_info) = &entry.token_info {
                    let token_name = token_info.metadata.symbol.clone();
                    token_summaries.entry(token_name).or_default().push(entry);
                }
            }

            let mut index_entries: Vec<IndexEntry> = token_summaries
                .into_iter()
                .map(|(token, analyses)| {
                    let latest = analyses.first().unwrap();
                    IndexEntry {
                        token: token.clone(),
                        symbol: latest.token_info.as_ref().map(|t| t.metadata.symbol.clone()).unwrap_or(token),
                        halal_status: format!("{:?}", latest.ruling),
                        confidence: latest.confidence,
                        last_updated: chrono::DateTime::from_timestamp_millis(latest.analyzed_at as i64)
                            .unwrap_or_else(chrono::Utc::now),
                        analysis_count: analyses.len() as u32,
                        consensus_score: calculate_consensus_score(&analyses),
                    }
                })
                .collect();

            // Apply filters
            if let Some(ref status_filter) = params.status {
                index_entries.retain(|entry| entry.halal_status == *status_filter);
            }

            if let Some(min_confidence) = params.min_confidence {
                index_entries.retain(|entry| entry.confidence >= min_confidence);
            }

            // Sort by confidence or last updated
            match params.sort_by.as_deref() {
                Some("confidence") => index_entries.sort_by(|a, b| b.confidence.partial_cmp(&a.confidence).unwrap()),
                Some("updated") => index_entries.sort_by(|a, b| b.last_updated.cmp(&a.last_updated)),
                _ => index_entries.sort_by(|a, b| b.last_updated.cmp(&a.last_updated)),
            }

            let filtered_count = index_entries.len() as u32;
            let response = IndexListResponse {
                entries: index_entries,
                total_count: total_entries_count,
                filtered_count,
            };

            Ok(Json(response))
        },
        Err(e) => {
            error!("Failed to list halal index: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "index_listing_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

/// Update token index (trigger re-analysis)
async fn update_token_index(
    State(state): State<AppState>,
    Json(request): Json<IndexUpdateRequest>,
) -> Result<Json<IndexUpdateResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Updating halal index for token: {}", request.token);

    let query = FiqhQuery::new_token_ticker(request.token.clone(), request.user_id.clone(), Some("id".to_owned()));

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            let update_response = IndexUpdateResponse {
                token: request.token,
                updated: true,
                analysis_id: response.analysis_id.clone().and_then(|id| Uuid::parse_str(&id).ok()),
                new_status: format!(
                    "{:?}",
                    response
                        .analysis_id
                        .as_ref()
                        .map(|_| IslamicPrinciple::Halal)
                        .unwrap_or(IslamicPrinciple::Mubah)
                ),
                timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                    .unwrap_or_else(chrono::Utc::now),
            };
            Ok(Json(update_response))
        },
        Err(e) => {
            error!("Failed to update token index: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "index_update_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

// ============================================================================
// SPECIALIZED ANALYSIS ENDPOINTS
// ============================================================================

/// Analyze audio input (voice query)
async fn analyze_audio(
    State(state): State<AppState>,
    Json(request): Json<AudioAnalysisRequest>,
) -> Result<Json<AnalysisResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Analyzing audio input of {} bytes", request.audio_data.len());

    let query = FiqhQuery::new_audio(
        request.audio_data,
        request.user_id.clone(),
        Some(request.language.unwrap_or_else(|| "id".to_owned())),
    );

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            let analysis_response = AnalysisResponse {
                token: "Audio Query".to_owned(),
                ruling: format!(
                    "{:?}",
                    response
                        .analysis_id
                        .as_ref()
                        .map(|_| IslamicPrinciple::Halal)
                        .unwrap_or(IslamicPrinciple::Mubah)
                ),
                confidence: response.confidence,
                reasoning: response.response,
                sources: response.sources,
                follow_up_questions: response.follow_up_questions,
                analysis_id: response.analysis_id.clone().and_then(|id| Uuid::parse_str(&id).ok()),
                timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                    .unwrap_or_else(chrono::Utc::now),
            };
            Ok(Json(analysis_response))
        },
        Err(e) => {
            error!("Audio analysis failed: {}", e);
            Err((
                StatusCode::BAD_REQUEST,
                Json(ErrorResponse {
                    error: "audio_analysis_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

/// Analyze by smart contract address
async fn analyze_contract_address(
    Path(address): Path<String>,
    Query(params): Query<AnalysisParams>,
    State(state): State<AppState>,
) -> Result<Json<ContractAnalysisResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Analyzing contract address: {}", address);

    let query = FiqhQuery::new_contract_address(
        address.clone(),
        params.user_id.clone(),
        Some(params.language.unwrap_or_else(|| "id".to_owned())),
    );

    match state.query_actor.process_query(query).await {
        Ok(response) => {
            let contract_response = ContractAnalysisResponse {
                contract_address: address,
                token_info: None, // Would be populated from blockchain data
                ruling: format!(
                    "{:?}",
                    response
                        .analysis_id
                        .as_ref()
                        .map(|_| IslamicPrinciple::Halal)
                        .unwrap_or(IslamicPrinciple::Mubah)
                ),
                confidence: response.confidence,
                reasoning: response.response,
                sources: response.sources,
                blockchain_data: None, // Would include metadata, holders, etc.
                analysis_id: response.analysis_id.clone().and_then(|id| Uuid::parse_str(&id).ok()),
                timestamp: chrono::DateTime::from_timestamp_millis(response.timestamp as i64)
                    .unwrap_or_else(chrono::Utc::now),
            };
            Ok(Json(contract_response))
        },
        Err(e) => {
            error!("Contract analysis failed: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "contract_analysis_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

// ============================================================================
// BACKTEST ENDPOINTS
// ============================================================================

/// Run backtest for a specific analysis
async fn run_backtest(
    Path(analysis_id): Path<Uuid>,
    State(state): State<AppState>,
    Json(_request): Json<BacktestRequest>,
) -> Result<Json<BacktestResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Running backtest for analysis: {}", analysis_id);

    match state.analyzer_actor.run_backtest(analysis_id).await {
        Ok(result) => {
            let backtest_response = BacktestResponse {
                analysis_id,
                backtest_id: Uuid::new_v4(),
                result,
                timestamp: chrono::Utc::now(),
            };
            Ok(Json(backtest_response))
        },
        Err(e) => {
            error!("Backtest failed: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "backtest_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

/// Get backtest results for an analysis
async fn get_backtest_results(
    Path(analysis_id): Path<Uuid>,
    State(_state): State<AppState>,
) -> Result<Json<BacktestResultsResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Getting backtest results for analysis: {}", analysis_id);

    // This would query stored backtest results
    // For now, return a placeholder
    let results_response = BacktestResultsResponse {
        analysis_id,
        results: vec![], // Would contain actual BacktestResult entries
        summary: BacktestSummary {
            total_backtests: 0,
            accuracy_score: 0.0,
            consistency_score: 0.0,
            improvement_trend: "stable".to_owned(),
        },
    };

    Ok(Json(results_response))
}

// ============================================================================
// HISTORY AND STATS ENDPOINTS
// ============================================================================

/// Get analysis history for a user
async fn get_user_history(
    Path(user_id): Path<String>,
    Query(params): Query<HistoryParams>,
    State(state): State<AppState>,
) -> Result<Json<HistoryResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Getting history for user: {}", user_id);

    let mut history_query = HistoryQuery::new().for_user(user_id.clone()).limit(params.limit.unwrap_or(20));

    if let Some(min_confidence) = params.min_confidence {
        history_query = history_query.with_min_confidence(min_confidence);
    }

    match state.history_actor.query_analyses(history_query).await {
        Ok(history) => {
            let total_count = history.entries.len() as u32;
            let response = HistoryResponse {
                user_id,
                entries: history.entries,
                total_count,
            };
            Ok(Json(response))
        },
        Err(e) => {
            error!("Failed to get user history: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "history_retrieval_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

/// Get analysis history for a token
async fn get_token_history(
    Path(token): Path<String>,
    Query(params): Query<HistoryParams>,
    State(state): State<AppState>,
) -> Result<Json<TokenHistoryResponse>, (StatusCode, Json<ErrorResponse>)> {
    info!("Getting history for token: {}", token);

    let history_query = HistoryQuery::new().for_token(token.clone()).limit(params.limit.unwrap_or(10));

    match state.history_actor.query_analyses(history_query).await {
        Ok(history) => {
            let trend_analysis = calculate_trend_analysis(&history.entries);
            let response = TokenHistoryResponse {
                token,
                entries: history.entries,
                trend_analysis,
            };
            Ok(Json(response))
        },
        Err(e) => {
            error!("Failed to get token history: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "token_history_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

/// Get user statistics
async fn get_user_stats(
    Path(user_id): Path<String>,
    State(state): State<AppState>,
) -> Result<Json<UserAnalysisStats>, (StatusCode, Json<ErrorResponse>)> {
    info!("Getting stats for user: {}", user_id);

    match state.history_actor.get_user_stats(user_id.clone()).await {
        Ok(stats) => Ok(Json(stats)),
        Err(e) => {
            error!("Failed to get user stats: {}", e);
            Err((
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(ErrorResponse {
                    error: "stats_retrieval_failed".to_owned(),
                    message: e.to_string(),
                    details: None,
                }),
            ))
        },
    }
}

// ============================================================================
// HEALTH AND STATUS ENDPOINTS
// ============================================================================

/// Health check endpoint
async fn health_check() -> Json<HealthResponse> {
    Json(HealthResponse {
        status: "healthy".to_owned(),
        timestamp: chrono::Utc::now(),
        version: env!("CARGO_PKG_VERSION").to_owned(),
    })
}

/// Get system status
async fn get_system_status(
    State(_state): State<AppState>
) -> Result<Json<SystemStatus>, (StatusCode, Json<ErrorResponse>)> {
    let status = SystemStatus {
        actors_running: true, // Would check actual actor health
        database_connected: true,
        ai_service_available: true,
        vector_db_connected: true,
        last_analysis: chrono::Utc::now(),
        total_analyses: 0, // Would get from actual stats
        uptime_seconds: 0,
    };

    Ok(Json(status))
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

fn calculate_consensus_score(entries: &[crate::models::HistoryEntry]) -> f64 {
    if entries.is_empty() {
        return 0.0;
    }

    // Calculate how consistent the rulings are
    let rulings: Vec<_> = entries.iter().map(|e| &e.ruling).collect();
    let most_common = rulings
        .iter()
        .max_by_key(|&ruling| rulings.iter().filter(|&r| r == ruling).count());

    if let Some(most_common_ruling) = most_common {
        let count = rulings.iter().filter(|&r| r == most_common_ruling).count();
        count as f64 / rulings.len() as f64
    } else {
        0.0
    }
}

fn calculate_trend_analysis(entries: &[crate::models::HistoryEntry]) -> TrendAnalysis {
    if entries.len() < 2 {
        return TrendAnalysis {
            direction: "stable".to_owned(),
            confidence_trend: 0.0,
            ruling_changes: 0,
            latest_change: None,
        };
    }

    let ruling_changes = entries
        .windows(2)
        .filter(|pair| std::mem::discriminant(&pair[0].ruling) != std::mem::discriminant(&pair[1].ruling))
        .count();

    let confidence_trend = if entries.len() >= 2 {
        entries.first().unwrap().confidence - entries.last().unwrap().confidence
    } else {
        0.0
    };

    let direction = if confidence_trend > 0.1 {
        "improving".to_owned()
    } else if confidence_trend < -0.1 {
        "declining".to_owned()
    } else {
        "stable".to_owned()
    };

    TrendAnalysis {
        direction,
        confidence_trend,
        ruling_changes,
        latest_change: entries
            .first()
            .and_then(|e| chrono::DateTime::from_timestamp_millis(e.analyzed_at as i64)),
    }
}

// ============================================================================
// REQUEST/RESPONSE TYPES
// ============================================================================

#[derive(Debug, Deserialize)]
pub struct AnalysisParams {
    pub user_id: Option<String>,
    pub language: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct DetailedAnalysisRequest {
    pub token: String,
    pub input_type: String, // "ticker", "contract", "text"
    pub user_id: Option<String>,
    pub language: String,
    pub additional_sources: Option<Vec<String>>,
}

#[derive(Debug, Deserialize)]
pub struct AudioAnalysisRequest {
    pub audio_data: Vec<u8>,
    pub user_id: Option<String>,
    pub language: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct IndexListParams {
    pub status: Option<String>,
    pub min_confidence: Option<f64>,
    pub limit: Option<usize>,
    pub sort_by: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct IndexUpdateRequest {
    pub token: String,
    pub user_id: Option<String>,
    pub force_refresh: Option<bool>,
}

#[derive(Debug, Deserialize)]
pub struct BacktestRequest {
    pub comparison_period_days: Option<i64>,
    pub include_new_sources: Option<bool>,
}

#[derive(Debug, Deserialize)]
pub struct HistoryParams {
    pub limit: Option<usize>,
    pub min_confidence: Option<f64>,
}

#[derive(Debug, Serialize)]
pub struct AnalysisResponse {
    pub token: String,
    pub ruling: String,
    pub confidence: f64,
    pub reasoning: String,
    pub sources: Vec<String>,
    pub follow_up_questions: Vec<String>,
    pub analysis_id: Option<Uuid>,
    pub timestamp: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Serialize)]
pub struct DetailedAnalysisResponse {
    #[serde(flatten)]
    pub basic: AnalysisResponse,
    pub detailed_analysis: Option<TokenAnalysis>,
    pub maqashid_assessment: Vec<crate::models::MaqashidPrinciple>,
    pub risk_factors: Vec<String>,
    pub recommendations: Vec<String>,
}

#[derive(Debug, Serialize)]
pub struct ContractAnalysisResponse {
    pub contract_address: String,
    pub token_info: Option<SolanaTokenInfo>,
    pub ruling: String,
    pub confidence: f64,
    pub reasoning: String,
    pub sources: Vec<String>,
    pub blockchain_data: Option<serde_json::Value>,
    pub analysis_id: Option<Uuid>,
    pub timestamp: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Serialize)]
pub struct TokenIndexResponse {
    pub token: String,
    pub halal_status: String,
    pub confidence: f64,
    pub last_updated: chrono::DateTime<chrono::Utc>,
    pub analysis_count: u32,
    pub consensus_score: f64,
}

#[derive(Debug, Serialize)]
pub struct IndexListResponse {
    pub entries: Vec<IndexEntry>,
    pub total_count: u32,
    pub filtered_count: u32,
}

#[derive(Debug, Serialize)]
pub struct IndexEntry {
    pub token: String,
    pub symbol: String,
    pub halal_status: String,
    pub confidence: f64,
    pub last_updated: chrono::DateTime<chrono::Utc>,
    pub analysis_count: u32,
    pub consensus_score: f64,
}

#[derive(Debug, Serialize)]
pub struct IndexUpdateResponse {
    pub token: String,
    pub updated: bool,
    pub analysis_id: Option<Uuid>,
    pub new_status: String,
    pub timestamp: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Serialize)]
pub struct BacktestResponse {
    pub analysis_id: Uuid,
    pub backtest_id: Uuid,
    pub result: BacktestResult,
    pub timestamp: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Serialize)]
pub struct BacktestResultsResponse {
    pub analysis_id: Uuid,
    pub results: Vec<BacktestResult>,
    pub summary: BacktestSummary,
}

#[derive(Debug, Serialize)]
pub struct BacktestSummary {
    pub total_backtests: u32,
    pub accuracy_score: f64,
    pub consistency_score: f64,
    pub improvement_trend: String,
}

#[derive(Debug, Serialize)]
pub struct HistoryResponse {
    pub user_id: String,
    pub entries: Vec<crate::models::HistoryEntry>,
    pub total_count: u32,
}

#[derive(Debug, Serialize)]
pub struct TokenHistoryResponse {
    pub token: String,
    pub entries: Vec<crate::models::HistoryEntry>,
    pub trend_analysis: TrendAnalysis,
}

#[derive(Debug, Serialize)]
pub struct TrendAnalysis {
    pub direction: String,
    pub confidence_trend: f64,
    pub ruling_changes: usize,
    pub latest_change: Option<chrono::DateTime<chrono::Utc>>,
}

#[derive(Debug, Serialize)]
pub struct HealthResponse {
    pub status: String,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub version: String,
}

#[derive(Debug, Serialize)]
pub struct SystemStatus {
    pub actors_running: bool,
    pub database_connected: bool,
    pub ai_service_available: bool,
    pub vector_db_connected: bool,
    pub last_analysis: chrono::DateTime<chrono::Utc>,
    pub total_analyses: u64,
    pub uptime_seconds: u64,
}

#[derive(Debug, Serialize)]
pub struct ErrorResponse {
    pub error: String,
    pub message: String,
    pub details: Option<serde_json::Value>,
}
