pub struct EnhancedAnalyzer {
    islamic_analyzer: IslamicAnalyzer,
    openai_client: Option<OpenAIClient>,
    solana_analyzer: SolanaTokenAnalyzer,
}

impl EnhancedAnalyzer {
    pub async fn comprehensive_analysis(&self, token_identifier: &str) -> Result<ComprehensiveAnalysis, AnalysisError> {
        // 1. Get token info from Solana/CoinGecko
        let token_info = self.solana_analyzer.get_token_info(token_identifier).await?;

        // 2. Rule-based Islamic analysis
        let islamic_analysis = self.islamic_analyzer.analyze_token(&token_info).await;

        // 3. AI enhancement (if available)
        let ai_reasoning = if let Some(ai_client) = &self.openai_client {
            ai_client.analyze_islamic_compliance(&token_info).await.ok()
        } else {
            None
        };

        // 4. Combine results
        Ok(ComprehensiveAnalysis {
            token_info,
            islamic_analysis,
            ai_reasoning,
            timestamp: chrono::Utc::now(),
            analysis_id: uuid::Uuid::new_v4(),
        })
    }
}