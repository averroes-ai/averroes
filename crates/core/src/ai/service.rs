use async_trait::async_trait;
use tracing::debug;
use tracing::error;
use tracing::info;

use crate::FiqhAIConfig;
use crate::ai::grok_client::GrokClient;
use crate::ai::groq_client::GroqClient;
use crate::ai::models::LanguageModel;
use crate::ai::openai_client::OpenAIClient;
use crate::models::token::BlockchainNetwork;
use crate::models::token::TokenMetadata;
use crate::models::token::TokenPriceData;
use crate::models::token::TokenStandard;
use crate::models::token::UniversalTokenInfo;

/// Unified AI service that can use different models
pub struct AIService {
    groq_client: Option<GroqClient>,
    grok_client: Option<GrokClient>,
    openai_client: Option<OpenAIClient>,
    preferred_model: String,
}

impl AIService {
    pub async fn new(config: &FiqhAIConfig) -> Result<Self, Box<dyn std::error::Error + Send + Sync>> {
        info!("Initializing AI service with preferred model: {}", config.preferred_model);

        let groq_client = if !config.groq_api_key.is_empty() {
            Some(GroqClient::new(config.groq_api_key.clone()))
        } else {
            None
        };

        let grok_client = if !config.grok_api_key.is_empty() {
            Some(GrokClient::new(config.grok_api_key.clone()))
        } else {
            None
        };

        let openai_client = if !config.openai_api_key.is_empty() {
            Some(OpenAIClient::new(config.openai_api_key.clone()))
        } else {
            None
        };

        Ok(Self {
            groq_client,
            grok_client,
            openai_client,
            preferred_model: config.preferred_model.clone(),
        })
    }

    /// Analyze Islamic compliance using the preferred model
    pub async fn analyze_islamic_compliance(
        &self,
        prompt: &str,
    ) -> Result<String, String> {
        debug!("Analyzing Islamic compliance with preferred model: {}", self.preferred_model);

        match self.preferred_model.as_str() {
            "groq" => {
                if let Some(client) = &self.groq_client {
                    client
                        .analyze_islamic_compliance(prompt)
                        .await
                        .map_err(|e| format!("Groq analysis failed: {e}"))
                } else {
                    self.fallback_analysis(prompt).await
                }
            },
            "grok" => {
                if let Some(client) = &self.grok_client {
                    client
                        .analyze_islamic_compliance(prompt)
                        .await
                        .map_err(|e| format!("Grok analysis failed: {e}"))
                } else {
                    self.fallback_analysis(prompt).await
                }
            },
            "openai" => {
                if let Some(client) = &self.openai_client {
                    // Create a dummy token info for analysis
                    let token_info = UniversalTokenInfo {
                        address: "QUERY".to_owned(),
                        metadata: TokenMetadata {
                            name: "General Query".to_owned(),
                            symbol: "QUERY".to_owned(),
                            contract_address: "QUERY".to_owned(),
                            decimals: 0,
                            description: Some(prompt.to_owned()),
                            image_url: None,
                            creator: None,
                            verified: false,
                            token_standard: TokenStandard::Other {
                                name: "General".to_owned(),
                            },
                            blockchain: BlockchainNetwork::Other {
                                name: "Unknown".to_owned(),
                            },
                        },
                        price_data: Some(TokenPriceData {
                            price_usd: 0.0,
                            price_change_24h: 0.0,
                            volume_24h: 0,
                            market_cap: 0,
                            total_supply: Some(0.0),
                            last_updated: 0,
                        }),
                        holders: None,
                        liquidity_pools: vec![],
                        is_verified: false,
                        risk_score: None,
                        blockchain: BlockchainNetwork::Other {
                            name: "Unknown".to_owned(),
                        },
                    };
                    client
                        .analyze_islamic_compliance(&token_info)
                        .await
                        .map_err(|e| format!("OpenAI analysis failed: {e}"))
                } else {
                    self.fallback_analysis(prompt).await
                }
            },
            _ => {
                error!("Unknown preferred model: {}", self.preferred_model);
                self.fallback_analysis(prompt).await
            },
        }
    }

    /// Generate follow-up questions based on analysis
    pub async fn generate_follow_up_questions(
        &self,
        analysis: &str,
    ) -> Result<Vec<String>, String> {
        match self.preferred_model.as_str() {
            "groq" => {
                if let Some(client) = &self.groq_client {
                    client
                        .generate_follow_up_questions(analysis)
                        .await
                        .map_err(|e| format!("Groq follow-up generation failed: {e}"))
                } else {
                    Ok(self.default_follow_up_questions())
                }
            },
            "grok" => {
                if let Some(client) = &self.grok_client {
                    client
                        .generate_follow_up_questions(analysis)
                        .await
                        .map_err(|e| format!("Grok follow-up generation failed: {e}"))
                } else {
                    Ok(self.default_follow_up_questions())
                }
            },
            "openai" => {
                if let Some(_client) = &self.openai_client {
                    // OpenAI client doesn't have follow-up questions method, use default
                    Ok(self.default_follow_up_questions())
                } else {
                    Ok(self.default_follow_up_questions())
                }
            },
            _ => Ok(self.default_follow_up_questions()),
        }
    }

    /// Test connection to the preferred model
    pub async fn test_connection(&self) -> Result<bool, String> {
        match self.preferred_model.as_str() {
            "groq" => {
                if let Some(client) = &self.groq_client {
                    client
                        .test_connection()
                        .await
                        .map_err(|e| format!("Groq connection test failed: {e}"))
                } else {
                    Ok(false)
                }
            },
            "grok" => {
                if let Some(client) = &self.grok_client {
                    client
                        .test_connection()
                        .await
                        .map_err(|e| format!("Grok connection test failed: {e}"))
                } else {
                    Ok(false)
                }
            },
            "openai" => {
                // OpenAI client doesn't have a test method, assume it works if initialized
                Ok(self.openai_client.is_some())
            },
            _ => Ok(false),
        }
    }

    /// Get available models
    pub fn get_available_models(&self) -> Vec<String> {
        let mut models = Vec::new();

        if self.groq_client.is_some() {
            models.push("groq".to_owned());
        }
        if self.grok_client.is_some() {
            models.push("grok".to_owned());
        }
        if self.openai_client.is_some() {
            models.push("openai".to_owned());
        }

        models
    }

    /// Fallback analysis when preferred model is not available
    async fn fallback_analysis(
        &self,
        prompt: &str,
    ) -> Result<String, String> {
        info!("Attempting fallback analysis");

        // Try Groq first
        if let Some(client) = &self.groq_client {
            match client.analyze_islamic_compliance(prompt).await {
                Ok(result) => return Ok(result),
                Err(e) => error!("Groq fallback failed: {}", e),
            }
        }

        // Try Grok second
        if let Some(client) = &self.grok_client {
            match client.analyze_islamic_compliance(prompt).await {
                Ok(result) => return Ok(result),
                Err(e) => error!("Grok fallback failed: {}", e),
            }
        }

        // Try OpenAI last
        if let Some(client) = &self.openai_client {
            let token_info = UniversalTokenInfo {
                address: "QUERY".to_owned(),
                metadata: TokenMetadata {
                    name: "General Query".to_owned(),
                    symbol: "QUERY".to_owned(),
                    contract_address: "QUERY".to_owned(),
                    decimals: 0,
                    description: Some(prompt.to_owned()),
                    image_url: None,
                    creator: None,
                    verified: false,
                    token_standard: TokenStandard::Other {
                        name: "General".to_owned(),
                    },
                    blockchain: BlockchainNetwork::Other {
                        name: "Unknown".to_owned(),
                    },
                },
                price_data: Some(TokenPriceData {
                    price_usd: 0.0,
                    price_change_24h: 0.0,
                    volume_24h: 0,
                    market_cap: 0,
                    total_supply: Some(0.0),
                    last_updated: 0,
                }),
                holders: None,
                liquidity_pools: vec![],
                is_verified: false,
                risk_score: None,
                blockchain: BlockchainNetwork::Other {
                    name: "Unknown".to_owned(),
                },
            };
            match client.analyze_islamic_compliance(&token_info).await {
                Ok(result) => return Ok(result),
                Err(e) => error!("OpenAI fallback failed: {}", e),
            }
        }

        // If all fail, return a default response
        Ok("I apologize, but I'm currently unable to analyze this request due to service limitations. Please try \
            again later or contact support."
            .to_owned())
    }

    /// Default follow-up questions when AI generation fails
    fn default_follow_up_questions(&self) -> Vec<String> {
        vec![
            "Would you like to know more about the Islamic principles involved?".to_owned(),
            "Are there any specific aspects of this analysis you'd like clarified?".to_owned(),
            "Would you like to explore alternative Sharia-compliant options?".to_owned(),
        ]
    }
}

#[async_trait]
impl LanguageModel for AIService {
    async fn complete(
        &self,
        prompt: &str,
    ) -> Result<String, String> {
        self.analyze_islamic_compliance(prompt).await
    }

    async fn generate_follow_up_questions(
        &self,
        response: &str,
    ) -> Result<Vec<String>, String> {
        self.generate_follow_up_questions(response).await
    }
}
