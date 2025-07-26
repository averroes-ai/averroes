use serde_json::Value;
use solana_client::rpc_client::RpcClient;
use solana_program::pubkey::Pubkey;

pub struct SolanaTokenAnalyzer {
    rpc_client: RpcClient,
    coingecko_client: reqwest::Client,
}

impl SolanaTokenAnalyzer {
    pub fn new() -> Self {
        Self {
            rpc_client: RpcClient::new("https://api.mainnet-beta.solana.com"),
            coingecko_client: reqwest::Client::new(),
        }
    }

    pub async fn get_token_info(
        &self,
        mint_address: &str,
    ) -> Result<TokenInfo, SolanaError> {
        let mint_pubkey = mint_address.parse::<Pubkey>()?;

        // Get token supply
        let supply = self.rpc_client.get_token_supply(&mint_pubkey)?;

        // Get metadata account
        let metadata = self.get_token_metadata(&mint_pubkey).await?;

        // Get price from CoinGecko
        let price = self.get_token_price(&metadata.symbol).await.unwrap_or(0.0);

        Ok(TokenInfo {
            mint_address: mint_address.to_string(),
            name: metadata.name,
            symbol: metadata.symbol,
            decimals: metadata.decimals,
            total_supply: supply.ui_amount.unwrap_or(0.0),
            current_price: price,
            description: metadata.description,
        })
    }

    async fn get_token_price(
        &self,
        symbol: &str,
    ) -> Result<f64, reqwest::Error> {
        let url =
            format!("https://api.coingecko.com/api/v3/simple/price?ids={}&vs_currencies=usd", symbol.to_lowercase());
        let response: Value = self.coingecko_client.get(&url).send().await?.json().await?;

        Ok(response[symbol.to_lowercase()]["usd"].as_f64().unwrap_or(0.0))
    }
}
