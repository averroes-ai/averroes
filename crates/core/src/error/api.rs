use axum::http::StatusCode;
use axum::response::IntoResponse;
use axum::response::Json;
use axum::response::Response;
use serde::Deserialize;
use serde::Serialize;
use thiserror::Error;

/// API-specific errors for `Averroes` service
#[derive(Error, Debug)]
pub enum ApiError {
    #[error("Invalid token identifier: {0}")]
    InvalidToken(String),
    #[error("Token analysis failed: {0}")]
    TokenAnalysisFailed(String),
    #[error("Query processing failed: {0}")]
    QueryProcessingFailed(String),
    #[error("History query failed: {0}")]
    HistoryQueryFailed(String),
    #[error("Audio processing failed: {0}")]
    AudioProcessingFailed(String),
    #[error("Contract analysis failed: {0}")]
    ContractAnalysisFailed(String),
    #[error("Backtest execution failed: {0}")]
    BacktestFailed(String),
    #[error("Rate limit exceeded: {0}")]
    RateLimitExceeded(String),
    #[error("Authentication failed: {0}")]
    AuthenticationFailed(String),
    #[error("Internal server error: {0}")]
    InternalError(String),
    #[error("Bad request: {0}")]
    BadRequest(String),
    #[error("Not found: {0}")]
    NotFound(String),
    #[error("Validation error: {0}")]
    ValidationError(String),
}

impl ApiError {
    /// Get the appropriate HTTP status code for this error
    pub fn status_code(&self) -> StatusCode {
        match self {
            ApiError::InvalidToken(_) => StatusCode::BAD_REQUEST,
            ApiError::BadRequest(_) => StatusCode::BAD_REQUEST,
            ApiError::ValidationError(_) => StatusCode::BAD_REQUEST,
            ApiError::NotFound(_) => StatusCode::NOT_FOUND,
            ApiError::AuthenticationFailed(_) => StatusCode::UNAUTHORIZED,
            ApiError::RateLimitExceeded(_) => StatusCode::TOO_MANY_REQUESTS,
            ApiError::TokenAnalysisFailed(_) => StatusCode::INTERNAL_SERVER_ERROR,
            ApiError::QueryProcessingFailed(_) => StatusCode::INTERNAL_SERVER_ERROR,
            ApiError::HistoryQueryFailed(_) => StatusCode::INTERNAL_SERVER_ERROR,
            ApiError::AudioProcessingFailed(_) => StatusCode::INTERNAL_SERVER_ERROR,
            ApiError::ContractAnalysisFailed(_) => StatusCode::INTERNAL_SERVER_ERROR,
            ApiError::BacktestFailed(_) => StatusCode::INTERNAL_SERVER_ERROR,
            ApiError::InternalError(_) => StatusCode::INTERNAL_SERVER_ERROR,
        }
    }

    /// Get the error code as a string
    pub fn error_code(&self) -> &'static str {
        match self {
            ApiError::InvalidToken(_) => "invalid_token",
            ApiError::TokenAnalysisFailed(_) => "token_analysis_failed",
            ApiError::QueryProcessingFailed(_) => "query_processing_failed",
            ApiError::HistoryQueryFailed(_) => "history_query_failed",
            ApiError::AudioProcessingFailed(_) => "audio_processing_failed",
            ApiError::ContractAnalysisFailed(_) => "contract_analysis_failed",
            ApiError::BacktestFailed(_) => "backtest_failed",
            ApiError::RateLimitExceeded(_) => "rate_limit_exceeded",
            ApiError::AuthenticationFailed(_) => "authentication_failed",
            ApiError::InternalError(_) => "internal_error",
            ApiError::BadRequest(_) => "bad_request",
            ApiError::NotFound(_) => "not_found",
            ApiError::ValidationError(_) => "validation_error",
        }
    }
}

/// Standard JSON error response for API endpoints
#[derive(Debug, Serialize, Deserialize)]
pub struct ErrorResponse {
    pub error: String,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub details: Option<serde_json::Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timestamp: Option<String>,
}

impl ErrorResponse {
    pub fn new(
        error: impl Into<String>,
        message: impl Into<String>,
    ) -> Self {
        Self {
            error: error.into(),
            message: message.into(),
            details: None,
            timestamp: Some(chrono::Utc::now().to_rfc3339()),
        }
    }

    pub fn from_api_error(api_error: &ApiError) -> Self {
        Self {
            error: api_error.error_code().to_owned(),
            message: api_error.to_string(),
            details: None,
            timestamp: Some(chrono::Utc::now().to_rfc3339()),
        }
    }

    pub fn with_details(
        error: impl Into<String>,
        message: impl Into<String>,
        details: serde_json::Value,
    ) -> Self {
        Self {
            error: error.into(),
            message: message.into(),
            details: Some(details),
            timestamp: Some(chrono::Utc::now().to_rfc3339()),
        }
    }
}

/// Implement `IntoResponse` for `ApiError` to automatically convert to HTTP responses
impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        let status = self.status_code();
        let error_response = ErrorResponse::from_api_error(&self);
        (status, Json(error_response)).into_response()
    }
}

/// Convenience type alias for API results
pub type ApiResult<T> = Result<T, ApiError>;
