use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;
use tokio::sync::RwLock;
use tracing::debug;
use tracing::info;

use crate::models::analysis::ScrapedData;
use crate::models::fatwa::IslamicAnalysis;
use crate::models::fatwa::IslamicPrinciple;
use crate::models::fatwa::MaqashidPrinciple;

/// Trait for language models used in Islamic finance analysis
#[async_trait]
pub trait LanguageModel: Send + Sync {
    async fn complete(
        &self,
        prompt: &str,
    ) -> Result<String, String>;
    async fn generate_follow_up_questions(
        &self,
        response: &str,
    ) -> Result<Vec<String>, String>;
}

/// Configuration for AI models
#[derive(Debug, Clone)]
pub struct ModelConfig {
    pub system_prompt: String,
    pub temperature: f64,
    pub max_tokens: u32,
    pub model_name: String,
}

impl Default for ModelConfig {
    fn default() -> Self {
        Self {
            system_prompt: "You are an expert in Islamic finance and cryptocurrency analysis.".to_owned(),
            temperature: 0.3,
            max_tokens: 1500,
            model_name: "gpt-4".to_owned(),
        }
    }
}

/// Mock `OpenAI` model implementation
pub struct OpenAIModel {
    #[allow(dead_code)]
    context_store: Arc<RwLock<HashMap<String, String>>>,
    #[allow(dead_code)]
    system_prompt: String,
}

impl OpenAIModel {
    pub async fn new(
        config: ModelConfig,
        _model_name: String,
        _api_key: String,
    ) -> Result<Self, Box<dyn std::error::Error + Send + Sync>> {
        info!("Initializing mock OpenAI model");

        Ok(Self {
            context_store: Arc::new(RwLock::new(HashMap::new())),
            system_prompt: config.system_prompt,
        })
    }

    pub async fn complete_with_context(
        &self,
        prompt: &str,
        _context: &str,
    ) -> Result<String, String> {
        debug!("Completing prompt with context: {}", &prompt[..50.min(prompt.len())].to_string());

        // Mock implementation
        Ok(format!("Mock response for: {prompt}"))
    }
}

#[async_trait]
impl LanguageModel for OpenAIModel {
    async fn complete(
        &self,
        prompt: &str,
    ) -> Result<String, String> {
        debug!("Completing prompt with OpenAI model: {}", &prompt[..50.min(prompt.len())].to_string());

        // Mock implementation
        Ok(format!("Mock completion: {prompt}"))
    }

    async fn generate_follow_up_questions(
        &self,
        _response: &str,
    ) -> Result<Vec<String>, String> {
        // Mock implementation
        Ok(vec![
            "Apakah Anda ingin mengetahui lebih lanjut tentang aspek syariah dari token ini?".to_owned(),
            "Ingin melihat fatwa dari negara lain tentang topik ini?".to_owned(),
            "Apakah ada pertanyaan spesifik lain tentang kehalalan investasi ini?".to_owned(),
        ])
    }
}

/// Parse AI model response into structured Islamic analysis
pub async fn parse_islamic_analysis(
    response: &str,
    _scraped_data: &[ScrapedData],
    _model: &dyn LanguageModel,
) -> Result<IslamicAnalysis, String> {
    // Extract ruling
    let ruling = if response.to_lowercase().contains("halal") && !response.to_lowercase().contains("haram") {
        IslamicPrinciple::Halal
    } else if response.to_lowercase().contains("haram") {
        IslamicPrinciple::Haram
    } else if response.to_lowercase().contains("makruh") {
        IslamicPrinciple::Makruh
    } else if response.to_lowercase().contains("riba") {
        IslamicPrinciple::Riba
    } else if response.to_lowercase().contains("gharar") {
        IslamicPrinciple::Gharar
    } else if response.to_lowercase().contains("maysir") {
        IslamicPrinciple::Maysir
    } else {
        IslamicPrinciple::Mubah
    };

    // Extract confidence (look for decimal numbers)
    let confidence: f64 = {
        use regex::Regex;
        lazy_static::lazy_static! {
            static ref CONFIDENCE_REGEX: Regex = Regex::new(r"([0-9]*\.?[0-9]+)").unwrap();
        }

        CONFIDENCE_REGEX
            .find(response)
            .and_then(|m| m.as_str().parse().ok())
            .unwrap_or(0.7)
    };

    // Extract reasoning (everything before "Risk Factors" or similar)
    let reasoning = if let Some(reasoning_start) = response.find("reasoning") {
        let reasoning_section = &response[reasoning_start..];
        if let Some(end) = reasoning_section.find("Risk") {
            reasoning_section[..end].trim().to_owned()
        } else {
            reasoning_section.trim().to_owned()
        }
    } else {
        response.to_owned()
    };

    let risk_factors = vec!["Analysis requires further review".to_owned()];
    let recommendations = vec!["Consult with Islamic scholars for detailed guidance".to_owned()];

    Ok(IslamicAnalysis {
        ruling: ruling.clone(),
        confidence: confidence.min(1.0),
        reasoning: if reasoning.is_empty() {
            "Analisis berdasarkan prinsip-prinsip Islam dan Maqashid Syariah".to_owned()
        } else {
            reasoning
        },
        supporting_fatwas: Vec::new(),
        risk_factors,
        recommendations,
        maqashid_assessment: vec![MaqashidPrinciple {
            name: "Hifz al-Mal (Preservation of Wealth)".to_owned(),
            category: "Financial Security".to_owned(),
            description: "Protecting wealth from harmful activities".to_owned(),
            relevance_score: match ruling {
                IslamicPrinciple::Halal => 0.9,
                IslamicPrinciple::Haram => 0.1,
                _ => 0.5,
            },
        }],
    })
}

/// Generate summary from analysis response
pub fn generate_summary(
    response: &str,
    _token_name: &str,
) -> String {
    // Extract first sentence or first 100 characters as summary
    if let Some(first_sentence_end) = response.find('.') {
        response[..first_sentence_end + 1].trim().to_owned()
    } else {
        {
            let mut truncated = response.chars().take(100).collect::<String>();
            truncated.push_str("...");
            truncated
        }
    }
}

/// Extract risk factors from response text
pub fn extract_risk_factors(response: &str) -> Vec<String> {
    if response.to_lowercase().contains("riba") || response.to_lowercase().contains("interest") {
        vec!["Contains interest-based elements".to_owned()]
    } else if response.to_lowercase().contains("haram") {
        vec!["Contains prohibited activities".to_owned()]
    } else {
        vec!["Requires further analysis".to_owned()]
    }
}

/// Extract recommendations from response text
pub fn extract_recommendations(response: &str) -> Vec<String> {
    if response.to_lowercase().contains("halal") {
        vec!["Consider for Islamic investment portfolio".to_owned()]
    } else {
        vec!["Consult with Islamic finance scholars".to_owned()]
    }
}
