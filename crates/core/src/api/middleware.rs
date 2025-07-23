use std::sync::Arc;
use std::time::Duration;
use std::time::Instant;

use axum::extract::Request;
use axum::extract::State;
use axum::http::HeaderValue;
use axum::http::Method;
use axum::http::StatusCode;
use axum::middleware::Next;
use axum::response::Json;
use axum::response::Response;
use dashmap::DashMap;
use tower_http::cors::Any;
use tower_http::cors::CorsLayer;
use tracing::Span;
use tracing::debug;
use tracing::error;
use tracing::info;
use tracing::instrument;
use tracing::warn;
use uuid::Uuid;

use crate::error::ErrorResponse;

/// Rate limiting configuration
#[derive(Debug, Clone)]
pub struct RateLimitConfig {
    pub requests_per_minute: u32,
    pub requests_per_hour: u32,
    pub burst_limit: u32,
}

impl Default for RateLimitConfig {
    fn default() -> Self {
        Self {
            requests_per_minute: 30,
            requests_per_hour: 500,
            burst_limit: 10,
        }
    }
}

/// Rate limiter state
#[derive(Debug)]
pub struct RateLimiter {
    config: RateLimitConfig,
    minute_counters: Arc<DashMap<String, (Instant, u32)>>,
    hour_counters: Arc<DashMap<String, (Instant, u32)>>,
    burst_counters: Arc<DashMap<String, (Instant, u32)>>,
}

impl RateLimiter {
    pub fn new(config: RateLimitConfig) -> Self {
        Self {
            config,
            minute_counters: Arc::new(DashMap::new()),
            hour_counters: Arc::new(DashMap::new()),
            burst_counters: Arc::new(DashMap::new()),
        }
    }

    pub async fn check_rate_limit(
        &self,
        client_id: &str,
    ) -> Result<(), RateLimitError> {
        let now = Instant::now();

        // Check burst limit (requests per 10 seconds)
        let burst_key = format!("{client_id}:burst");
        if let Some(mut counter) = self.burst_counters.get_mut(&burst_key) {
            if now.duration_since(counter.0) < Duration::from_secs(10) {
                if counter.1 >= self.config.burst_limit {
                    return Err(RateLimitError::BurstLimit {
                        limit: self.config.burst_limit,
                        reset_in: Duration::from_secs(10) - now.duration_since(counter.0),
                    });
                }
                counter.1 += 1;
            } else {
                *counter = (now, 1);
            }
        } else {
            self.burst_counters.insert(burst_key, (now, 1));
        }

        // Check per-minute limit
        let minute_key = format!("{client_id}:minute");
        if let Some(mut counter) = self.minute_counters.get_mut(&minute_key) {
            if now.duration_since(counter.0) < Duration::from_secs(60) {
                if counter.1 >= self.config.requests_per_minute {
                    return Err(RateLimitError::MinuteLimit {
                        limit: self.config.requests_per_minute,
                        reset_in: Duration::from_secs(60) - now.duration_since(counter.0),
                    });
                }
                counter.1 += 1;
            } else {
                *counter = (now, 1);
            }
        } else {
            self.minute_counters.insert(minute_key, (now, 1));
        }

        // Check per-hour limit
        let hour_key = format!("{client_id}:hour");
        if let Some(mut counter) = self.hour_counters.get_mut(&hour_key) {
            if now.duration_since(counter.0) < Duration::from_secs(3600) {
                if counter.1 >= self.config.requests_per_hour {
                    return Err(RateLimitError::HourLimit {
                        limit: self.config.requests_per_hour,
                        reset_in: Duration::from_secs(3600) - now.duration_since(counter.0),
                    });
                }
                counter.1 += 1;
            } else {
                *counter = (now, 1);
            }
        } else {
            self.hour_counters.insert(hour_key, (now, 1));
        }

        Ok(())
    }

    /// Clean up expired counters
    pub async fn cleanup(&self) {
        let now = Instant::now();

        // Clean burst counters
        self.burst_counters
            .retain(|_, (timestamp, _)| now.duration_since(*timestamp) < Duration::from_secs(10));

        // Clean minute counters
        self.minute_counters
            .retain(|_, (timestamp, _)| now.duration_since(*timestamp) < Duration::from_secs(60));

        // Clean hour counters
        self.hour_counters
            .retain(|_, (timestamp, _)| now.duration_since(*timestamp) < Duration::from_secs(3600));
    }
}

#[derive(Debug, thiserror::Error)]
pub enum RateLimitError {
    #[error("Burst limit exceeded: {limit} requests per 10 seconds")]
    BurstLimit {
        limit: u32,
        reset_in: Duration,
    },

    #[error("Minute limit exceeded: {limit} requests per minute")]
    MinuteLimit {
        limit: u32,
        reset_in: Duration,
    },

    #[error("Hour limit exceeded: {limit} requests per hour")]
    HourLimit {
        limit: u32,
        reset_in: Duration,
    },
}

/// Middleware state
#[derive(Clone)]
pub struct MiddlewareState {
    pub rate_limiter: Arc<RateLimiter>,
}

/// Create CORS middleware layer
pub fn create_cors_layer() -> CorsLayer {
    CorsLayer::new()
        .allow_origin(Any)
        .allow_methods([Method::GET, Method::POST, Method::PUT, Method::DELETE, Method::OPTIONS])
        .allow_headers([
            axum::http::header::CONTENT_TYPE,
            axum::http::header::AUTHORIZATION,
            axum::http::header::ACCEPT,
            axum::http::header::USER_AGENT,
        ])
        .allow_credentials(false)
        .max_age(Duration::from_secs(3600))
}

/// Rate limiting middleware
#[instrument(skip(state, request, next), fields(client_id, path = %request.uri().path()))]
pub async fn rate_limit_middleware(
    State(state): State<MiddlewareState>,
    mut request: Request,
    next: Next,
) -> Result<Response, (StatusCode, Json<ErrorResponse>)> {
    // Extract client identifier (IP address or API key)
    let client_id = extract_client_id(&request);
    Span::current().record("client_id", &client_id);

    debug!("Rate limit check for client: {}", client_id);

    // Check rate limits
    match state.rate_limiter.check_rate_limit(&client_id).await {
        Ok(()) => {
            // Add rate limit info to request extensions
            request.extensions_mut().insert(RateLimitInfo {
                client_id: client_id.clone(),
                remaining_minute: state.rate_limiter.config.requests_per_minute, // Would calculate actual remaining
                remaining_hour: state.rate_limiter.config.requests_per_hour,
            });

            let response = next.run(request).await;
            Ok(add_rate_limit_headers(response, &client_id, &state.rate_limiter.config))
        },
        Err(e) => {
            warn!("Rate limit exceeded for client {}: {}", client_id, e);

            let (status, error_response) = match e {
                RateLimitError::BurstLimit {
                    limit,
                    reset_in,
                } => (StatusCode::TOO_MANY_REQUESTS, ErrorResponse {
                    error: "rate_limit_exceeded".to_owned(),
                    message: format!("Burst limit exceeded: {limit} requests per 10 seconds"),
                    details: Some(serde_json::json!({
                        "limit_type": "burst",
                        "limit": limit,
                        "reset_in_seconds": reset_in.as_secs(),
                    })),
                    timestamp: Some(chrono::Utc::now().to_rfc3339()),
                }),
                RateLimitError::MinuteLimit {
                    limit,
                    reset_in,
                } => (StatusCode::TOO_MANY_REQUESTS, ErrorResponse {
                    error: "rate_limit_exceeded".to_owned(),
                    message: format!("Rate limit exceeded: {limit} requests per minute"),
                    details: Some(serde_json::json!({
                        "limit_type": "minute",
                        "limit": limit,
                        "reset_in_seconds": reset_in.as_secs(),
                    })),
                    timestamp: Some(chrono::Utc::now().to_rfc3339()),
                }),
                RateLimitError::HourLimit {
                    limit,
                    reset_in,
                } => (StatusCode::TOO_MANY_REQUESTS, ErrorResponse {
                    error: "rate_limit_exceeded".to_owned(),
                    message: format!("Hourly limit exceeded: {limit} requests per hour"),
                    details: Some(serde_json::json!({
                        "limit_type": "hour",
                        "limit": limit,
                        "reset_in_seconds": reset_in.as_secs(),
                    })),
                    timestamp: Some(chrono::Utc::now().to_rfc3339()),
                }),
            };

            Err((status, Json(error_response)))
        },
    }
}

/// Authentication middleware
#[instrument(skip(request, next), fields(user_id, authenticated = false))]
pub async fn auth_middleware(
    mut request: Request,
    next: Next,
) -> Result<Response, (StatusCode, Json<ErrorResponse>)> {
    // Extract auth info from headers
    let auth_info = extract_auth_info(&request);

    match auth_info {
        Some(info) => {
            Span::current().record("user_id", &info.user_id);
            Span::current().record("authenticated", true);

            // Add auth info to request extensions
            request.extensions_mut().insert(info);

            debug!("Authenticated request");
            let response = next.run(request).await;
            Ok(response)
        },
        None => {
            // For now, allow unauthenticated requests but log them
            debug!("Unauthenticated request - proceeding");

            // Add default auth info
            request.extensions_mut().insert(AuthInfo {
                user_id: "anonymous".to_owned(),
                authenticated: false,
                permissions: vec!["read".to_owned()],
            });

            let response = next.run(request).await;
            Ok(response)
        },
    }
}

/// Request tracing middleware
#[instrument(skip(request, next), fields(request_id, method = %request.method(), uri = %request.uri()))]
pub async fn tracing_middleware(
    mut request: Request,
    next: Next,
) -> Response {
    let request_id = Uuid::new_v4();
    let start_time = Instant::now();

    Span::current().record("request_id", request_id.to_string());

    // Add request ID to headers
    request.headers_mut().insert(
        "x-request-id",
        HeaderValue::from_str(&request_id.to_string()).unwrap_or_else(|_| HeaderValue::from_static("invalid")),
    );

    info!("Request started");

    let response = next.run(request).await;

    let processing_time = start_time.elapsed();
    info!("Request completed in {:?}", processing_time);

    // Add response headers
    let (mut parts, body) = response.into_parts();
    parts.headers.insert(
        "x-request-id",
        HeaderValue::from_str(&request_id.to_string()).unwrap_or_else(|_| HeaderValue::from_static("invalid")),
    );
    parts.headers.insert(
        "x-processing-time-ms",
        HeaderValue::from_str(&processing_time.as_millis().to_string())
            .unwrap_or_else(|_| HeaderValue::from_static("0")),
    );

    Response::from_parts(parts, body)
}

/// Request validation middleware
pub async fn validation_middleware(
    request: Request,
    next: Next,
) -> Result<Response, (StatusCode, Json<ErrorResponse>)> {
    // Validate content type for POST/PUT requests
    if matches!(request.method(), &Method::POST | &Method::PUT) {
        if let Some(content_type) = request.headers().get(axum::http::header::CONTENT_TYPE) {
            let content_type_str = content_type.to_str().unwrap_or("");
            if !content_type_str.starts_with("application/json") && !content_type_str.starts_with("multipart/form-data")
            {
                return Err((
                    StatusCode::UNSUPPORTED_MEDIA_TYPE,
                    Json(ErrorResponse {
                        error: "unsupported_content_type".to_owned(),
                        message: "Content-Type must be application/json or multipart/form-data".to_owned(),
                        details: Some(serde_json::json!({
                            "received_content_type": content_type_str,
                            "supported_types": ["application/json", "multipart/form-data"]
                        })),
                        timestamp: Some(chrono::Utc::now().to_rfc3339()),
                    }),
                ));
            }
        }
    }

    let response = next.run(request).await;
    Ok(response)
}

/// Security headers middleware
pub async fn security_headers_middleware(
    request: Request,
    next: Next,
) -> Response {
    let response = next.run(request).await;

    let (mut parts, body) = response.into_parts();

    // Add security headers
    parts
        .headers
        .insert("x-content-type-options", HeaderValue::from_static("nosniff"));
    parts.headers.insert("x-frame-options", HeaderValue::from_static("DENY"));
    parts
        .headers
        .insert("x-xss-protection", HeaderValue::from_static("1; mode=block"));
    parts
        .headers
        .insert("strict-transport-security", HeaderValue::from_static("max-age=31536000; includeSubDomains"));
    parts
        .headers
        .insert("referrer-policy", HeaderValue::from_static("strict-origin-when-cross-origin"));

    Response::from_parts(parts, body)
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

fn extract_client_id(request: &Request) -> String {
    // Try to get API key first
    if let Some(auth_header) = request.headers().get(axum::http::header::AUTHORIZATION) {
        if let Ok(auth_str) = auth_header.to_str() {
            if let Some(stripped) = auth_str.strip_prefix("Bearer ") {
                return stripped.to_owned();
            }
        }
    }

    // Fall back to IP address
    if let Some(forwarded_for) = request.headers().get("x-forwarded-for") {
        if let Ok(ip_str) = forwarded_for.to_str() {
            return ip_str.split(',').next().unwrap_or("unknown").trim().to_owned();
        }
    }

    if let Some(real_ip) = request.headers().get("x-real-ip") {
        if let Ok(ip_str) = real_ip.to_str() {
            return ip_str.to_owned();
        }
    }

    "unknown".to_owned()
}

fn extract_auth_info(request: &Request) -> Option<AuthInfo> {
    if let Some(auth_header) = request.headers().get(axum::http::header::AUTHORIZATION) {
        if let Ok(auth_str) = auth_header.to_str() {
            if let Some(token) = auth_str.strip_prefix("Bearer ") {
                // In a real implementation, you would validate the token
                return Some(AuthInfo {
                    user_id: format!("user_{}", &token[..8.min(token.len())]),
                    authenticated: true,
                    permissions: vec!["read".to_owned(), "write".to_owned()],
                });
            }
        }
    }
    None
}

fn add_rate_limit_headers(
    mut response: Response,
    client_id: &str,
    config: &RateLimitConfig,
) -> Response {
    let headers = response.headers_mut();

    headers.insert(
        "x-ratelimit-limit-minute",
        HeaderValue::from_str(&config.requests_per_minute.to_string())
            .unwrap_or_else(|_| HeaderValue::from_static("30")),
    );

    headers.insert(
        "x-ratelimit-limit-hour",
        HeaderValue::from_str(&config.requests_per_hour.to_string())
            .unwrap_or_else(|_| HeaderValue::from_static("500")),
    );

    headers.insert(
        "x-ratelimit-client-id",
        HeaderValue::from_str(&format!("client_{}", &client_id[..8.min(client_id.len())]))
            .unwrap_or_else(|_| HeaderValue::from_static("unknown")),
    );

    response
}

// ============================================================================
// DATA TYPES
// ============================================================================

#[derive(Debug, Clone)]
pub struct AuthInfo {
    pub user_id: String,
    pub authenticated: bool,
    pub permissions: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct RateLimitInfo {
    pub client_id: String,
    pub remaining_minute: u32,
    pub remaining_hour: u32,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_rate_limiter_creation() {
        let config = RateLimitConfig::default();
        let limiter = RateLimiter::new(config);

        // Test that we can check rate limits
        let result = limiter.check_rate_limit("test_client").await;
        assert!(result.is_ok());
    }

    #[tokio::test]
    async fn test_rate_limiter_burst_limit() {
        let config = RateLimitConfig {
            requests_per_minute: 30,
            requests_per_hour: 500,
            burst_limit: 2,
        };
        let limiter = RateLimiter::new(config);

        // First two requests should succeed
        assert!(limiter.check_rate_limit("test_client").await.is_ok());
        assert!(limiter.check_rate_limit("test_client").await.is_ok());

        // Third request should fail due to burst limit
        let result = limiter.check_rate_limit("test_client").await;
        assert!(result.is_err());
        assert!(matches!(result.unwrap_err(), RateLimitError::BurstLimit { .. }));
    }

    #[tokio::test]
    async fn test_client_id_extraction() {
        // This would test the client ID extraction logic
        // For now, just test that the function doesn't panic
        let request = Request::builder()
            .header("x-forwarded-for", "192.168.1.1")
            .body(axum::body::Body::empty())
            .unwrap();

        let client_id = extract_client_id(&request);
        assert_eq!(client_id, "192.168.1.1");
    }
}
