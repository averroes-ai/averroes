use chrono::DateTime;
use chrono::Duration;
use chrono::Utc;
use serde::Deserialize;
use serde::Serialize;
use uuid::Uuid;

use crate::models::IslamicPrinciple;
use crate::models::Query;
use crate::models::SolanaTokenInfo;
use crate::models::TokenAnalysis;

/// Analysis history with pagination
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct AnalysisHistory {
    pub entries: Vec<HistoryEntry>,
    pub total_count: u32,
    pub next_cursor: Option<String>,
}

/// Single history entry  
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct HistoryEntry {
    pub analysis_id: String, // UUID as String for UniFFI
    pub token_symbol: String,
    pub ruling: IslamicPrinciple,
    pub confidence: f64,
    pub summary: String,
    pub analyzed_at: u64, // Unix timestamp for UniFFI
    pub token_info: Option<SolanaTokenInfo>,
}

/// User analysis statistics
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct UserAnalysisStats {
    pub user_id: String,
    pub total_analyses: u32,
    pub halal_count: u32,
    pub haram_count: u32,
    pub mubah_count: u32,
    pub average_confidence: f64,
    pub first_analysis: u64,        // Unix timestamp for UniFFI
    pub last_analysis: Option<u64>, // Unix timestamp for UniFFI
    pub top_tokens: Vec<String>,
}

/// Chat message for chatbot sessions
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct ChatMessage {
    pub id: String,
    pub content: String,
    pub is_user_message: bool,
    pub timestamp: u64, // Unix timestamp for UniFFI
    pub analysis_id: Option<String>,
    pub follow_up_options: Option<Vec<String>>,
}

// Internal types (not exposed to UniFFI) for backward compatibility
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AnalysisHistoryInternal {
    pub id: Uuid,
    pub user_id: Option<String>,
    pub token_identifier: String, // ticker or contract address
    pub analyses: Vec<HistoryEntryInternal>,
    pub created_at: DateTime<Utc>,
    pub last_updated: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HistoryEntryInternal {
    pub analysis_id: Uuid,
    pub query_id: Uuid,
    pub timestamp: DateTime<Utc>,
    pub ruling: IslamicPrinciple,
    pub confidence: f64,
    pub brief_reasoning: String,
    pub major_factors: Vec<String>,
    pub price_at_time: Option<f64>,
    pub is_backtest: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HistoryQuery {
    pub user_id: Option<String>,
    pub token_identifier: Option<String>,
    pub date_range: Option<DateRange>,
    pub ruling_filter: Option<Vec<IslamicPrinciple>>,
    pub min_confidence: Option<f64>,
    pub limit: Option<usize>,
    pub include_backtests: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DateRange {
    pub start: DateTime<Utc>,
    pub end: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AnalysisTrend {
    pub token_identifier: String,
    pub trend_period: Duration,
    pub ruling_consistency: f64, // 0-1, higher means more consistent rulings
    pub confidence_trend: TrendDirection,
    pub ruling_changes: Vec<RulingChange>,
    pub key_factors_evolution: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum TrendDirection {
    Increasing,
    Decreasing,
    Stable,
    Volatile,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RulingChange {
    pub from_ruling: IslamicPrinciple,
    pub to_ruling: IslamicPrinciple,
    pub change_date: DateTime<Utc>,
    pub reason: String,
    pub confidence_impact: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserAnalysisStatsInternal {
    pub user_id: String,
    pub total_queries: usize,
    pub unique_tokens_analyzed: usize,
    pub avg_confidence: f64,
    pub most_common_ruling: IslamicPrinciple,
    pub analysis_frequency: AnalysisFrequency,
    pub last_activity: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AnalysisFrequency {
    pub daily_avg: f64,
    pub weekly_avg: f64,
    pub monthly_avg: f64,
    pub peak_hours: Vec<u8>,  // Hours of day (0-23)
    pub active_days: Vec<u8>, // Days of week (0-6, Sunday=0)
}

impl AnalysisHistoryInternal {
    pub fn new(
        token_identifier: String,
        user_id: Option<String>,
    ) -> Self {
        let now = Utc::now();
        Self {
            id: Uuid::new_v4(),
            user_id,
            token_identifier,
            analyses: Vec::new(),
            created_at: now,
            last_updated: now,
        }
    }

    pub fn add_analysis(
        &mut self,
        analysis: &TokenAnalysis,
        query: &Query,
    ) {
        // Convert query UUID from string
        let query_uuid = query.get_uuid().unwrap_or_else(|_| Uuid::new_v4());
        let analysis_uuid = Uuid::parse_str(&analysis.id).unwrap_or_else(|_| Uuid::new_v4());

        let entry = HistoryEntryInternal {
            analysis_id: analysis_uuid,
            query_id: query_uuid,
            timestamp: DateTime::from_timestamp_millis(analysis.created_at as i64).unwrap_or_else(Utc::now),
            ruling: analysis.islamic_analysis.ruling.clone(),
            confidence: analysis.islamic_analysis.confidence,
            brief_reasoning: analysis
                .islamic_analysis
                .reasoning
                .clone()
                .chars()
                .take(200)
                .collect::<String>(),
            major_factors: analysis.islamic_analysis.risk_factors.clone().into_iter().take(3).collect(),
            price_at_time: analysis
                .token_info
                .as_ref()
                .and_then(|ti| ti.price_data.as_ref())
                .map(|pd| pd.price_usd),
            is_backtest: !analysis.backtest_results.is_empty(),
        };

        self.analyses.push(entry);
        self.last_updated = Utc::now();

        // Keep only last 100 analyses to prevent unlimited growth
        if self.analyses.len() > 100 {
            self.analyses.remove(0);
        }
    }

    pub fn get_latest_analysis(&self) -> Option<&HistoryEntryInternal> {
        self.analyses.last()
    }

    pub fn get_analyses_in_range(
        &self,
        date_range: &DateRange,
    ) -> Vec<&HistoryEntryInternal> {
        self.analyses
            .iter()
            .filter(|entry| entry.timestamp >= date_range.start && entry.timestamp <= date_range.end)
            .collect()
    }

    pub fn calculate_ruling_consistency(
        &self,
        period: Duration,
    ) -> f64 {
        let cutoff_date = Utc::now() - period;
        let recent_analyses: Vec<_> = self.analyses.iter().filter(|entry| entry.timestamp >= cutoff_date).collect();

        if recent_analyses.len() < 2 {
            return 1.0; // Perfect consistency if less than 2 analyses
        }

        let mut consistent = 0;
        for window in recent_analyses.windows(2) {
            if std::mem::discriminant(&window[0].ruling) == std::mem::discriminant(&window[1].ruling) {
                consistent += 1;
            }
        }

        consistent as f64 / (recent_analyses.len() - 1) as f64
    }
}

impl Default for HistoryQuery {
    fn default() -> Self {
        Self::new()
    }
}

impl HistoryQuery {
    pub fn new() -> Self {
        Self {
            user_id: None,
            token_identifier: None,
            date_range: None,
            ruling_filter: None,
            min_confidence: None,
            limit: None,
            include_backtests: true,
        }
    }

    pub fn for_user(
        mut self,
        user_id: String,
    ) -> Self {
        self.user_id = Some(user_id);
        self
    }

    pub fn for_token(
        mut self,
        token_identifier: String,
    ) -> Self {
        self.token_identifier = Some(token_identifier);
        self
    }

    pub fn in_range(
        mut self,
        start: DateTime<Utc>,
        end: DateTime<Utc>,
    ) -> Self {
        self.date_range = Some(DateRange {
            start,
            end,
        });
        self
    }

    pub fn with_min_confidence(
        mut self,
        min_confidence: f64,
    ) -> Self {
        self.min_confidence = Some(min_confidence);
        self
    }

    pub fn limit(
        mut self,
        limit: usize,
    ) -> Self {
        self.limit = Some(limit);
        self
    }
}

// Conversion helpers for mobile interface
impl AnalysisHistory {
    pub fn new(
        _token_identifier: String,
        _user_id: Option<String>,
    ) -> Self {
        Self {
            entries: Vec::new(),
            total_count: 0,
            next_cursor: None,
        }
    }

    pub fn add_analysis(
        &mut self,
        analysis: &TokenAnalysis,
        query: &Query,
    ) {
        // Extract token symbol from query
        let token_symbol = match &query.query_type {
            crate::QueryType::TokenTicker {
                ticker,
            } => ticker.clone(),
            crate::QueryType::ContractAddress {
                address,
            } => address.clone(),
            _ => "UNKNOWN".to_owned(),
        };

        let entry = HistoryEntry {
            analysis_id: analysis.id.clone(),
            token_symbol,
            ruling: analysis.islamic_analysis.ruling.clone(),
            confidence: analysis.islamic_analysis.confidence,
            summary: analysis.islamic_analysis.reasoning.clone(),
            analyzed_at: analysis.created_at,
            token_info: analysis.token_info.clone(),
        };

        self.entries.push(entry);
        self.total_count += 1;
    }

    pub fn from_internal(internal: Vec<HistoryEntryInternal>) -> Self {
        let entries = internal
            .into_iter()
            .map(|entry| {
                HistoryEntry {
                    analysis_id: entry.analysis_id.to_string(),
                    token_symbol: "UNKNOWN".to_owned(), // Would need to be populated from actual data
                    ruling: entry.ruling,
                    confidence: entry.confidence,
                    summary: entry.brief_reasoning,
                    analyzed_at: entry.timestamp.timestamp_millis() as u64,
                    token_info: None, // Would need to be populated from actual data
                }
            })
            .collect();

        Self {
            entries,
            total_count: 0, // Would be set by the calling code
            next_cursor: None,
        }
    }
}
