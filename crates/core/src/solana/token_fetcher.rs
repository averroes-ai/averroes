use reqwest::Client;
use serde_json::Value;

pub struct TokenFetcher {
    client: Client,
}

impl TokenFetcher {
    pub fn new() -> Self {
        Self {
            client: Client::new(),
        }
    }

    pub async fn get_token_info(
        &self,
        symbol: &str,
    ) -> Result<TokenInfo, Box<dyn std::error::Error>> {
        // Use CoinGecko free API (no key required)
        let url = format!("https://api.coingecko.com/api/v3/coins/{}", symbol.to_lowercase());

        let response: Value = self.client.get(&url).send().await?.json().await?;

        Ok(TokenInfo {
            symbol: symbol.to_uppercase(),
            name: response["name"].as_str().unwrap_or("Unknown").to_string(),
            current_price: response["market_data"]["current_price"]["usd"].as_f64().unwrap_or(0.0),
            description: response["description"]["en"].as_str().unwrap_or("").to_string(),
            total_supply: response["market_data"]["total_supply"].as_f64().unwrap_or(0.0),
            market_cap: response["market_data"]["market_cap"]["usd"].as_f64().unwrap_or(0.0),
        })
    }
}
