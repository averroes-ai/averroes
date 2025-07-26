use std::collections::HashMap;

use lazy_static::lazy_static;
use crate::models::token::UniversalTokenInfo as TokenInfo;
use crate::models::analysis::IslamicAnalysis;

lazy_static! {
    pub static ref FALLBACK_TOKENS: HashMap<&'static str, TokenInfo> = {
        let mut m = HashMap::new();

        m.insert("SOL", TokenInfo {
            address: "11111111111111111111111111111112".to_string(),
            metadata: crate::models::token::TokenMetadata {
                name: "Solana".to_string(),
                symbol: "SOL".to_string(),
                contract_address: "11111111111111111111111111111112".to_string(),
                decimals: 9,
                description: Some("Solana is a high-performance blockchain supporting builders around the world creating crypto apps that scale today.".to_string()),
                image_url: None,
                creator: None,
                verified: true,
                token_standard: crate::models::token::TokenStandard::SPL,
                blockchain: crate::models::token::BlockchainNetwork::Solana,
            },
            price_data: Some(crate::models::token::TokenPriceData {
                price_usd: 23.45,
                price_change_24h: 2.5,
                volume_24h: 1_000_000_000,
                market_cap: 13_600_000_000,
                total_supply: Some(582_000_000.0),
                last_updated: 0,
            }),
            holders: Some(1_000_000),
            liquidity_pools: vec![],
            is_verified: true,
            risk_score: Some(0.2),
            blockchain: crate::models::token::BlockchainNetwork::Solana,
        });

        m.insert("BTC", TokenInfo {
            address: "bitcoin".to_string(),
            metadata: crate::models::token::TokenMetadata {
                name: "Bitcoin".to_string(),
                symbol: "BTC".to_string(),
                contract_address: "bitcoin".to_string(),
                decimals: 8,
                description: Some("Bitcoin is a decentralized digital currency, without a central bank or single administrator.".to_string()),
                image_url: None,
                creator: None,
                verified: true,
                token_standard: crate::models::token::TokenStandard::Other { name: "Bitcoin".to_string() },
                blockchain: crate::models::token::BlockchainNetwork::Other { name: "Bitcoin".to_string() },
            },
            price_data: Some(crate::models::token::TokenPriceData {
                price_usd: 43_200.0,
                price_change_24h: -1.2,
                volume_24h: 20_000_000_000,
                market_cap: 900_000_000_000,
                total_supply: Some(21_000_000.0),
                last_updated: 0,
            }),
            holders: Some(50_000_000),
            liquidity_pools: vec![],
            is_verified: true,
            risk_score: Some(0.8),
            blockchain: crate::models::token::BlockchainNetwork::Other { name: "Bitcoin".to_string() },
        });

        m.insert("USDC", TokenInfo {
            address: "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v".to_string(),
            metadata: crate::models::token::TokenMetadata {
                name: "USD Coin".to_string(),
                symbol: "USDC".to_string(),
                contract_address: "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v".to_string(),
                decimals: 6,
                description: Some("USDC is a fully collateralized US dollar stablecoin.".to_string()),
                image_url: None,
                creator: None,
                verified: true,
                token_standard: crate::models::token::TokenStandard::SPL,
                blockchain: crate::models::token::BlockchainNetwork::Solana,
            },
            price_data: Some(crate::models::token::TokenPriceData {
                price_usd: 1.0,
                price_change_24h: 0.01,
                volume_24h: 5_000_000_000,
                market_cap: 25_000_000_000,
                total_supply: Some(25_000_000_000.0),
                last_updated: 0,
            }),
            holders: Some(10_000_000),
            liquidity_pools: vec![],
            is_verified: true,
            risk_score: Some(0.1),
            blockchain: crate::models::token::BlockchainNetwork::Solana,
        });

        m
    };
    pub static ref FALLBACK_ANALYSES: HashMap<&'static str, IslamicAnalysis> = {
        let mut m = HashMap::new();

        m.insert("SOL", IslamicAnalysis {
            is_halal: true,
            compliance_score: 0.85,
            confidence: 0.8,
            reasoning: vec![
                "Utility token for blockchain infrastructure".to_string(),
                "No direct involvement in riba-based activities".to_string(),
                "Supports halal applications and services".to_string(),
            ],
            scholar_references: vec![
                "AAOIFI Sharia Standard No. 17".to_string(),
                "Islamic Finance Council guidance on utility tokens".to_string(),
            ],
            ai_reasoning: Some(
                "HALAL - Solana is primarily a utility token for blockchain infrastructure. It doesn't   
involve riba (interest), excessive gharar (uncertainty), or maysir (gambling). Confidence: 85%"
                    .to_string(),
            ),
        });

        m.insert("BTC", IslamicAnalysis {
            is_halal: false,
            compliance_score: 0.3,
            confidence: 0.7,
            reasoning: vec![
                "Highly speculative nature (gharar)".to_string(),
                "Often used for gambling and speculation".to_string(),
                "No intrinsic utility or backing".to_string(),
            ],
            scholar_references: vec![
                "Fatwa by Grand Mufti of Egypt".to_string(),
                "Islamic Finance Council concerns on speculation".to_string(),
            ],
            ai_reasoning: Some(
                "HARAM - Bitcoin's extreme volatility and speculative nature constitute excessive gharar 
(uncertainty). Widely used for gambling and speculation. Confidence: 70%"
                    .to_string(),
            ),
        });

        m
    };
}

pub fn get_fallback_analysis(symbol: &str) -> Option<(TokenInfo, IslamicAnalysis)> {
    let token = FALLBACK_TOKENS.get(symbol)?;
    let analysis = FALLBACK_ANALYSES.get(symbol)?;
    Some((token.clone(), analysis.clone()))
}
