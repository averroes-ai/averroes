pub struct IslamicAnalyzer {
    prohibited_keywords: Vec<String>,
    halal_categories: Vec<String>,
    haram_categories: Vec<String>,
}

impl IslamicAnalyzer {
    pub fn new() -> Self {
        Self {
            prohibited_keywords: vec![
                "gambling".to_string(), "casino".to_string(), "lottery".to_string(),
                "alcohol".to_string(), "pork".to_string(), "interest".to_string(),
                "usury".to_string(), "riba".to_string(), "lending".to_string(),
                "borrowing".to_string(), "leverage".to_string(), "margin".to_string(),
            ],
            halal_categories: vec![
                "utility".to_string(), "infrastructure".to_string(), "gaming".to_string(),
                "social".to_string(), "education".to_string(), "healthcare".to_string(),
            ],
            haram_categories: vec![
                "defi".to_string(), "lending".to_string(), "gambling".to_string(),
                "adult".to_string(), "alcohol".to_string(),
            ],
        }
    }

    pub async fn analyze_token(&self, token_info: &TokenInfo) -> IslamicAnalysis {
        let mut compliance_score = 0.5; // Start neutral
        let mut reasoning = Vec::new();
        let mut confidence = 0.7;

        // Check description for prohibited content
        let description_lower = token_info.description.to_lowercase();

        for keyword in &self.prohibited_keywords {
            if description_lower.contains(keyword) {
                compliance_score -= 0.2;
                reasoning.push(format!("Contains prohibited keyword: {}", keyword));
            }
        }

        // Check for DeFi/lending protocols (high risk)
        if description_lower.contains("defi") || description_lower.contains("yield") {
            compliance_score -= 0.3;
            reasoning.push("DeFi protocols may involve riba (interest)".to_string());
        }

        // Check for utility tokens (generally acceptable)
        if description_lower.contains("utility") || description_lower.contains("infrastructure") {
            compliance_score += 0.2;
            reasoning.push("Utility tokens are generally permissible".to_string());
        }

        // Ensure score is between 0 and 1
        compliance_score = compliance_score.max(0.0).min(1.0);

        let is_halal = compliance_score >= 0.6;

        IslamicAnalysis {
            is_halal,
            compliance_score,
            confidence,
            reasoning,
            scholar_references: vec![
                "Quran 2:275 - Allah has permitted trade and forbidden riba".to_string(),
                "AAOIFI Sharia Standard No. 17".to_string(),
            ],
            risk_factors: self.identify_risk_factors(&token_info),
        }
    }

    fn identify_risk_factors(&self, token_info: &TokenInfo) -> Vec<String> {
        let mut risks = Vec::new();

        if token_info.total_supply > 1_000_000_000.0 {
            risks.push("High token supply may indicate inflationary pressure".to_string());
        }

        if token_info.current_price < 0.01 {
            risks.push("Very low price may indicate speculative nature".to_string());
        }

        risks
    }
}