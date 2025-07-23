use chrono::DateTime;
use chrono::Utc;
use serde::Deserialize;
use serde::Serialize;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Enum)]
pub enum FatwaSource {
    MUI,    // Majelis Ulama Indonesia
    AAOIFI, // Accounting and Auditing Organization for Islamic Financial Institutions
    OIC,    // Organization of Islamic Cooperation
    Darul,  // Darul Ifta institutions
    Scholar {
        name: String,
    }, // Individual scholar name
    Custom {
        name: String,
    }, // User-provided source
}

/// Islamic ruling principles for token analysis
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash, uniffi::Enum)]
pub enum IslamicPrinciple {
    Halal,    // Permissible
    Haram,    // Forbidden
    Makruh,   // Discouraged
    Mustahab, // Recommended
    Mubah,    // Neutral/Permissible
    Riba,     // Interest/Usury (Haram category)
    Gharar,   // Excessive uncertainty (Haram category)
    Maysir,   // Gambling (Haram category)
    Syubhat,  // Doubtful/Suspicious
}

/// Maqashid Syariah principle assessment
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct MaqashidPrinciple {
    pub name: String,
    pub category: String, // e.g., "Hifz al-Mal" (Preservation of Wealth)
    pub description: String,
    pub relevance_score: f64, // 0-1
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Fatwa {
    pub id: Uuid,
    pub title: String,
    pub content: String,
    pub source: FatwaSource,
    pub principles_addressed: Vec<IslamicPrinciple>,
    pub maqashid_relevance: Vec<MaqashidPrinciple>,
    pub keywords: Vec<String>,
    pub language: String,
    pub issued_date: Option<DateTime<Utc>>,
    pub created_at: DateTime<Utc>,
    pub vector_embedding: Option<Vec<f32>>, // For vector search
    pub confidence_score: Option<f64>,      // Relevance to query
}

/// Reference to supporting fatwa
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct FatwaReference {
    pub fatwa_id: String, // UUID as String for UniFFI
    pub relevance_score: f64,
    pub excerpt: String,
    pub reasoning: String, // Why this fatwa is relevant
}

/// Islamic analysis result
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct IslamicAnalysis {
    pub ruling: IslamicPrinciple,
    pub confidence: f64, // 0-1
    pub reasoning: String,
    pub supporting_fatwas: Vec<FatwaReference>,
    pub risk_factors: Vec<String>,
    pub recommendations: Vec<String>,
    pub maqashid_assessment: Vec<MaqashidPrinciple>,
}

impl Fatwa {
    pub fn new(
        title: String,
        content: String,
        source: FatwaSource,
        principles: Vec<IslamicPrinciple>,
        keywords: Vec<String>,
        language: String,
    ) -> Self {
        Self {
            id: Uuid::new_v4(),
            title,
            content,
            source,
            principles_addressed: principles,
            maqashid_relevance: Vec::new(),
            keywords,
            language,
            issued_date: None,
            created_at: Utc::now(),
            vector_embedding: None,
            confidence_score: None,
        }
    }

    pub fn calculate_relevance(
        &self,
        query_keywords: &[String],
    ) -> f64 {
        let matching_keywords = self
            .keywords
            .iter()
            .filter(|k| query_keywords.iter().any(|q| q.to_lowercase().contains(&k.to_lowercase())))
            .count();

        if self.keywords.is_empty() {
            0.0
        } else {
            matching_keywords as f64 / self.keywords.len() as f64
        }
    }
}

impl Default for IslamicAnalysis {
    fn default() -> Self {
        Self {
            ruling: IslamicPrinciple::Mubah,
            confidence: 0.0,
            reasoning: String::new(),
            supporting_fatwas: Vec::new(),
            risk_factors: Vec::new(),
            recommendations: Vec::new(),
            maqashid_assessment: Vec::new(),
        }
    }
}

// Helper implementations for UniFFI compatibility
impl FatwaReference {
    pub fn from_fatwa_ref_with_uuid(
        fatwa_id: Uuid,
        relevance_score: f64,
        excerpt: String,
        reasoning: String,
    ) -> Self {
        Self {
            fatwa_id: fatwa_id.to_string(),
            relevance_score,
            excerpt,
            reasoning,
        }
    }
}
