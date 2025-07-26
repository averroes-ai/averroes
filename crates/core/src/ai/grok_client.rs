use reqwest::Client;
use serde_json::json;
use tracing::debug;
use tracing::error;

/// Grok client for xAI API integration
pub struct GrokClient {
    client: Client,
    api_key: String,
}

impl GrokClient {
    pub fn new(api_key: String) -> Self {
        Self {
            client: Client::new(),
            api_key,
        }
    }

    /// Analyze Islamic compliance using Grok
    pub async fn analyze_islamic_compliance(
        &self,
        prompt: &str,
    ) -> Result<String, reqwest::Error> {
        debug!("Analyzing Islamic compliance with Grok: {}", &prompt[..50.min(prompt.len())]);

        let request_body = json!({
            "messages": [
                {
                    "role": "system",
                    "content": "You are an expert in Islamic finance and Sharia compliance. Analyze cryptocurrency and financial instruments based on Islamic principles: no riba (interest), no gharar (excessive uncertainty), no maysir (gambling), and adherence to maqashid shariah (objectives of Islamic law). Provide clear, scholarly analysis with references to Islamic sources when possible."
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "model": "grok-beta",
            "temperature": 0.3,
            "max_tokens": 1000,
            "stream": false
        });

        let response = self
            .client
            .post("https://api.x.ai/v1/chat/completions")
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&request_body)
            .send()
            .await?;

        if !response.status().is_success() {
            let status = response.status();
            error!("Grok API error: {}", status);
            let error_text = response.text().await?;
            error!("Error response: {}", error_text);
            return Ok(format!("Error: Unable to analyze with Grok. Status: {status}"));
        }

        let response_json: serde_json::Value = response.json().await?;

        let content = response_json["choices"][0]["message"]["content"]
            .as_str()
            .unwrap_or("Analysis unavailable")
            .to_owned();

        debug!("Grok analysis completed successfully");
        Ok(content)
    }

    /// Generate follow-up questions based on analysis
    pub async fn generate_follow_up_questions(
        &self,
        analysis: &str,
    ) -> Result<Vec<String>, reqwest::Error> {
        let prompt = format!(
            "Based on this Islamic finance analysis, generate 3 relevant follow-up questions that would help users \
             understand the topic better:\n\n{analysis}\n\nProvide only the questions, one per line, without \
             numbering."
        );

        let request_body = json!({
            "messages": [
                {
                    "role": "system",
                    "content": "You are an Islamic finance expert. Generate relevant follow-up questions that help users understand Islamic finance concepts better."
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "model": "grok-beta",
            "temperature": 0.5,
            "max_tokens": 200,
            "stream": false
        });

        let response = self
            .client
            .post("https://api.x.ai/v1/chat/completions")
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&request_body)
            .send()
            .await?;

        if !response.status().is_success() {
            return Ok(vec![
                "Would you like to know more about the Islamic principles involved?".to_owned(),
                "Are there any specific aspects of this analysis you'd like clarified?".to_owned(),
                "Would you like to explore alternative Sharia-compliant options?".to_owned(),
            ]);
        }

        let response_json: serde_json::Value = response.json().await?;
        let content = response_json["choices"][0]["message"]["content"]
            .as_str()
            .unwrap_or("")
            .to_owned();

        let questions: Vec<String> = content
            .lines()
            .map(|line| line.trim().to_owned())
            .filter(|line| !line.is_empty())
            .take(3)
            .collect();

        if questions.is_empty() {
            Ok(vec![
                "Would you like to know more about the Islamic principles involved?".to_owned(),
                "Are there any specific aspects of this analysis you'd like clarified?".to_owned(),
                "Would you like to explore alternative Sharia-compliant options?".to_owned(),
            ])
        } else {
            Ok(questions)
        }
    }

    /// Test the API connection
    pub async fn test_connection(&self) -> Result<bool, reqwest::Error> {
        let request_body = json!({
            "messages": [
                {
                    "role": "user",
                    "content": "Test connection"
                }
            ],
            "model": "grok-beta",
            "temperature": 0.1,
            "max_tokens": 10,
            "stream": false
        });

        let response = self
            .client
            .post("https://api.x.ai/v1/chat/completions")
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&request_body)
            .send()
            .await?;

        Ok(response.status().is_success())
    }
}
