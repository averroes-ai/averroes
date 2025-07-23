use chrono::Utc;
use serde::Deserialize;
use serde::Serialize;
use uuid::Uuid;

/// Different query input types
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Enum)]
pub enum QueryType {
    Text {
        text: String,
    },
    Audio {
        audio_data: Vec<u8>,
    }, // Raw audio bytes for STT processing
    TokenTicker {
        ticker: String,
    },
    ContractAddress {
        address: String,
    },
    FollowUp {
        original_query_id: String, // UUID as String for UniFFI
        question: String,
    },
}

/// User query input
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct Query {
    pub id: String, // UUID as String for UniFFI
    pub query_type: QueryType,
    pub user_id: Option<String>,
    pub timestamp: u64,          // Unix timestamp in milliseconds for UniFFI
    pub language: String,        // e.g., "en", "id", "ar"
    pub context: Option<String>, // Additional context for the query
}

/// Response from query processing
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct QueryResponse {
    pub query_id: String, // UUID as String for UniFFI
    pub response: String,
    pub confidence: f64,
    pub sources: Vec<String>,
    pub follow_up_questions: Vec<String>,
    pub timestamp: u64,              // Unix timestamp in milliseconds for UniFFI
    pub analysis_id: Option<String>, // UUID as String for UniFFI
}

impl Query {
    pub fn new_text(
        text: String,
        user_id: Option<String>,
        language: Option<String>,
    ) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            query_type: QueryType::Text {
                text,
            },
            user_id,
            timestamp: Utc::now().timestamp_millis() as u64,
            language: language.unwrap_or_else(|| "id".to_owned()),
            context: None,
        }
    }

    pub fn new_token_ticker(
        ticker: String,
        user_id: Option<String>,
        language: Option<String>,
    ) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            query_type: QueryType::TokenTicker {
                ticker,
            },
            user_id,
            timestamp: Utc::now().timestamp_millis() as u64,
            language: language.unwrap_or_else(|| "id".to_owned()),
            context: None,
        }
    }

    pub fn new_contract_address(
        address: String,
        user_id: Option<String>,
        language: Option<String>,
    ) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            query_type: QueryType::ContractAddress {
                address,
            },
            user_id,
            timestamp: Utc::now().timestamp_millis() as u64,
            language: language.unwrap_or_else(|| "id".to_owned()),
            context: None,
        }
    }

    pub fn new_audio(
        audio_data: Vec<u8>,
        user_id: Option<String>,
        language: Option<String>,
    ) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            query_type: QueryType::Audio {
                audio_data,
            },
            user_id,
            timestamp: Utc::now().timestamp_millis() as u64,
            language: language.unwrap_or_else(|| "id".to_owned()),
            context: None,
        }
    }

    pub fn new_follow_up(
        original_id: Uuid,
        question: String,
        user_id: Option<String>,
        language: Option<String>,
    ) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            query_type: QueryType::FollowUp {
                original_query_id: original_id.to_string(),
                question,
            },
            user_id,
            timestamp: Utc::now().timestamp_millis() as u64,
            language: language.unwrap_or_else(|| "id".to_owned()),
            context: None,
        }
    }

    // Helper methods for internal use (UUID conversion)
    pub fn get_uuid(&self) -> Result<Uuid, uuid::Error> {
        Uuid::parse_str(&self.id)
    }

    pub fn get_analysis_uuid(&self) -> Option<Result<Uuid, uuid::Error>> {
        // Return the query's own UUID since there's no separate analysis_id
        Some(Uuid::parse_str(&self.id))
    }
}

impl QueryResponse {
    pub fn new(
        query_id: Uuid,
        response: String,
        confidence: f64,
        sources: Vec<String>,
        follow_up_questions: Vec<String>,
        analysis_id: Option<Uuid>,
    ) -> Self {
        Self {
            query_id: query_id.to_string(),
            response,
            confidence,
            sources,
            follow_up_questions,
            timestamp: Utc::now().timestamp_millis() as u64,
            analysis_id: analysis_id.map(|id| id.to_string()),
        }
    }
}
