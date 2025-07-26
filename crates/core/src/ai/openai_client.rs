use reqwest::Client;
use serde_json::json;

use crate::models::token::UniversalTokenInfo as TokenInfo;

pub struct OpenAIClient {
    client: Client,
    api_key: String,
}

impl OpenAIClient {
    pub fn new(api_key: String) -> Self {
        Self {
            client: Client::new(),
            api_key,
        }
    }

    pub async fn analyze_islamic_compliance(
        &self,
        token_info: &TokenInfo,
    ) -> Result<String, reqwest::Error> {
        let prompt = format!(
            "As an Islamic finance expert, analyze this cryptocurrency token for Sharia compliance:

Token: {} ({})
Description: {}
Category: Utility Token
Current Price: ${:.4}

Please provide:
1. Halal/Haram determination
2. Islamic reasoning based on Quran and Hadith
3. Risk factors from Islamic perspective
4. Confidence level (0-100%)

Be concise but thorough. Focus on Islamic principles like avoiding riba, gharar, and maysir.",
            token_info.metadata.name,
            token_info.metadata.symbol,
            token_info
                .metadata
                .description
                .as_ref()
                .unwrap_or(&"No description available".to_owned()),
            token_info.price_data.as_ref().map(|p| p.price_usd).unwrap_or(0.0)
        );

        let request_body = json!({
            "model": "gpt-3.5-turbo",
            "messages": [
                {
                    "role": "system",
                    "content": "You are an expert in Islamic finance and cryptocurrency analysis. Provide clear,
        accurate guidance based on established Islamic principles."
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "max_tokens": 500,
            "temperature": 0.3
        });

        let response = self
            .client
            .post("https://api.openai.com/v1/chat/completions")
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&request_body)
            .send()
            .await?;

        let response_json: serde_json::Value = response.json().await?;
        let content = response_json["choices"][0]["message"]["content"]
            .as_str()
            .unwrap_or("Analysis unavailable")
            .to_owned();

        Ok(content)
    }
}
