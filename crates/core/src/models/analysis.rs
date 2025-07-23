use chrono::DateTime;
use chrono::Utc;
use serde::Deserialize;
use serde::Serialize;
use uuid::Uuid;

use crate::models::IslamicPrinciple;
use crate::models::Query;
use crate::models::SolanaTokenInfo;
use crate::models::fatwa::IslamicAnalysis;

/// Analysis processing status
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Enum)]
pub enum AnalysisStatus {
    Pending,
    InProgress,
    Completed,
    Failed,
    Cancelled,
}

/// Types of scraped data sources
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Enum)]
pub enum ScrapedDataType {
    CryptoHalalVerification,
    IslamicFinanceContent,
    UserProvidedContent,
    GeneralCryptoContent,
    Documentation,
    Official,
    News,
    Forum,
    Social,
    UserProvided,
}

/// Scraped data from external sources
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct ScrapedData {
    pub source_url: String,
    pub content: String,
    pub data_type: ScrapedDataType,
    pub title: Option<String>,
    pub relevance_score: f64,
    pub scraped_at: u64, // Unix timestamp for UniFFI
}

/// Backtest comparison result
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct BacktestResult {
    pub analysis_id: String,                  // UUID as String for UniFFI
    pub previous_analysis_id: Option<String>, // UUID as String for UniFFI
    pub comparison_date: u64,                 // Unix timestamp for UniFFI
    pub ruling_changed: bool,
    pub confidence_change: f64,
    pub new_factors: Vec<String>,
    pub removed_factors: Vec<String>,
    pub summary: String,
}

/// Confidence breakdown for analysis
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct ConfidenceBreakdown {
    pub token_data_quality: f64,
    pub fatwa_relevance: f64,
    pub scraping_completeness: f64,
    pub consensus_level: f64,
    pub data_freshness: f64,
    pub overall_confidence: f64,
}

/// User feedback on analysis
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct UserFeedback {
    pub user_id: String,
    pub is_helpful: bool,
    pub comment: Option<String>,
    pub rating: Option<u8>,
    pub created_at: u64, // Unix timestamp for UniFFI
}

/// Complete token analysis result
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct TokenAnalysis {
    pub id: String,       // UUID as String for UniFFI
    pub query_id: String, // UUID as String for UniFFI
    pub token_info: Option<SolanaTokenInfo>,
    pub islamic_analysis: IslamicAnalysis,
    pub scraped_data: Vec<ScrapedData>,
    pub status: AnalysisStatus,
    pub created_at: u64,           // Unix timestamp for UniFFI
    pub completed_at: Option<u64>, // Unix timestamp for UniFFI
    pub processing_time_ms: Option<u64>,
    pub confidence_breakdown: ConfidenceBreakdown,
    pub backtest_results: Vec<BacktestResult>,
    pub user_feedback: Option<UserFeedback>,
}

impl TokenAnalysis {
    pub fn new(query_id: Uuid) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            query_id: query_id.to_string(),
            token_info: None,
            islamic_analysis: IslamicAnalysis {
                ruling: IslamicPrinciple::Mubah,
                confidence: 0.5,
                reasoning: String::new(),
                supporting_fatwas: Vec::new(),
                risk_factors: Vec::new(),
                recommendations: Vec::new(),
                maqashid_assessment: Vec::new(),
            },
            scraped_data: Vec::new(),
            status: AnalysisStatus::Pending,
            created_at: Utc::now().timestamp_millis() as u64,
            completed_at: None,
            processing_time_ms: None,
            confidence_breakdown: ConfidenceBreakdown {
                token_data_quality: 0.0,
                fatwa_relevance: 0.0,
                scraping_completeness: 0.0,
                consensus_level: 0.0,
                data_freshness: 0.0,
                overall_confidence: 0.0,
            },
            backtest_results: Vec::new(),
            user_feedback: None,
        }
    }

    pub fn calculate_overall_confidence(&mut self) {
        let breakdown = &self.confidence_breakdown;
        self.confidence_breakdown.overall_confidence = (breakdown.token_data_quality
            + breakdown.fatwa_relevance
            + breakdown.scraping_completeness
            + breakdown.consensus_level
            + breakdown.data_freshness)
            / 5.0;
    }
}

// Internal types (not exposed to UniFFI) for backward compatibility
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ScrapedDataTypeInternal {
    News,
    Documentation,
    Forum,
    Social,
    Official,
    UserProvided,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ScrapedDataInternal {
    pub source_url: String,
    pub content: String,
    pub title: Option<String>,
    pub scraped_at: DateTime<Utc>,
    pub relevance_score: f64,
    pub data_type: ScrapedDataTypeInternal,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BacktestResultInternal {
    pub analysis_id: Uuid,
    pub previous_analysis_id: Option<Uuid>,
    pub comparison_date: DateTime<Utc>,
    pub ruling_changed: bool,
    pub confidence_change: f64,
    pub new_factors: Vec<String>,
    pub removed_factors: Vec<String>,
    pub summary: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ConfidenceBreakdownInternal {
    pub token_data_quality: f64,    // 0-1
    pub fatwa_relevance: f64,       // 0-1
    pub scraping_completeness: f64, // 0-1
    pub consensus_level: f64,       // 0-1
    pub data_freshness: f64,        // 0-1
    pub overall_confidence: f64,    // 0-1
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserFeedbackInternal {
    pub rating: i8, // 1-5 stars
    pub comment: Option<String>,
    pub is_helpful: Option<bool>,
    pub suggested_correction: Option<String>,
    pub submitted_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenAnalysisInternal {
    pub id: Uuid,
    pub query_id: Uuid,
    pub token_info: Option<SolanaTokenInfo>,
    pub islamic_analysis: IslamicAnalysis,
    pub scraped_data: Vec<ScrapedDataInternal>,
    pub status: AnalysisStatus,
    pub created_at: DateTime<Utc>,
    pub completed_at: Option<DateTime<Utc>>,
    pub processing_time_ms: Option<u64>,
    pub confidence_breakdown: ConfidenceBreakdownInternal,
    pub backtest_results: Vec<BacktestResultInternal>,
    pub user_feedback: Option<UserFeedbackInternal>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AnalysisRequest {
    pub query: Query,
    pub priority: AnalysisPriority,
    pub include_backtest: bool,
    pub custom_sources: Vec<String>, // Additional URLs to scrape
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AnalysisPriority {
    Low,
    Normal,
    High,
    Urgent,
}

impl TokenAnalysisInternal {
    pub fn new(query_id: Uuid) -> Self {
        Self {
            id: Uuid::new_v4(),
            query_id,
            token_info: None,
            islamic_analysis: IslamicAnalysis::default(),
            scraped_data: Vec::new(),
            status: AnalysisStatus::Pending,
            created_at: Utc::now(),
            completed_at: None,
            processing_time_ms: None,
            confidence_breakdown: ConfidenceBreakdownInternal::default(),
            backtest_results: Vec::new(),
            user_feedback: None,
        }
    }

    pub fn mark_completed(
        &mut self,
        processing_time_ms: u64,
    ) {
        self.status = AnalysisStatus::Completed;
        self.completed_at = Some(Utc::now());
        self.processing_time_ms = Some(processing_time_ms);
    }

    pub fn calculate_overall_confidence(&mut self) {
        let breakdown = &self.confidence_breakdown;
        self.confidence_breakdown.overall_confidence = (breakdown.token_data_quality
            + breakdown.fatwa_relevance
            + breakdown.scraping_completeness
            + breakdown.consensus_level
            + breakdown.data_freshness)
            / 5.0;
    }
}

impl Default for ConfidenceBreakdownInternal {
    fn default() -> Self {
        Self {
            token_data_quality: 0.0,
            fatwa_relevance: 0.0,
            scraping_completeness: 0.0,
            consensus_level: 0.0,
            data_freshness: 0.0,
            overall_confidence: 0.0,
        }
    }
}

impl Default for ConfidenceBreakdown {
    fn default() -> Self {
        Self {
            token_data_quality: 0.0,
            fatwa_relevance: 0.0,
            scraping_completeness: 0.0,
            consensus_level: 0.0,
            data_freshness: 0.0,
            overall_confidence: 0.0,
        }
    }
}

impl ScrapedData {
    pub fn new(
        source_url: String,
        content: String,
        data_type: ScrapedDataType,
        title: Option<String>,
    ) -> Self {
        Self {
            source_url,
            content,
            data_type,
            title,
            relevance_score: 0.0,
            scraped_at: Utc::now().timestamp_millis() as u64,
        }
    }

    pub fn calculate_relevance(
        &mut self,
        keywords: &[String],
    ) -> f64 {
        let content_lower = self.content.to_lowercase();
        let title_lower = self.title.as_ref().map(|t| t.to_lowercase()).unwrap_or_default();

        let mut score = 0.0;
        let mut matches = 0;

        for keyword in keywords {
            let keyword_lower = keyword.to_lowercase();
            if content_lower.contains(&keyword_lower) {
                score += 1.0;
                matches += 1;
            }
            if title_lower.contains(&keyword_lower) {
                score += 2.0; // Title matches are weighted higher
                matches += 1;
            }
        }

        if matches > 0 {
            score /= keywords.len() as f64;
        }

        self.relevance_score = score.min(1.0);
        self.relevance_score
    }
}

impl ScrapedDataInternal {
    pub fn new(
        source_url: String,
        content: String,
        data_type: ScrapedDataTypeInternal,
    ) -> Self {
        Self {
            source_url,
            content,
            title: None,
            scraped_at: Utc::now(),
            relevance_score: 0.0,
            data_type,
        }
    }

    pub fn calculate_relevance(
        &mut self,
        keywords: &[String],
    ) -> f64 {
        let content_lower = self.content.to_lowercase();
        let title_lower = self.title.as_ref().map(|t| t.to_lowercase()).unwrap_or_default();

        let mut score = 0.0;
        let mut matches = 0;

        for keyword in keywords {
            let keyword_lower = keyword.to_lowercase();
            if content_lower.contains(&keyword_lower) {
                score += 1.0;
                matches += 1;
            }
            if title_lower.contains(&keyword_lower) {
                score += 2.0; // Title matches are weighted higher
                matches += 1;
            }
        }

        if matches > 0 {
            score /= keywords.len() as f64;
        }

        self.relevance_score = score.min(1.0);
        self.relevance_score
    }
}
