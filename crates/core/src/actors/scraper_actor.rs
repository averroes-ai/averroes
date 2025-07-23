use std::sync::Arc;
use std::sync::Mutex;
use std::time::Duration;
use std::time::Instant;

use chrono::Utc;
use reqwest::Client;
use scraper::Html;
use scraper::Selector;
use tokio::sync::Semaphore;
use tokio::sync::mpsc;
use tracing::debug;
use tracing::error;
use tracing::info;
use tracing::warn;

use crate::models::ScrapedData;
use crate::models::ScrapedDataType;
use crate::models::ScraperError;
use crate::models::ScraperMessage;
use crate::models::ScrapingStats;

pub struct ScraperActor {
    receiver: mpsc::Receiver<ScraperMessage>,
    client: Client,
    stats: Arc<Mutex<ScrapingStats>>,
    rate_limiter: Arc<Semaphore>, // Max concurrent requests
}

impl ScraperActor {
    pub fn new(receiver: mpsc::Receiver<ScraperMessage>) -> Self {
        let client = Client::builder()
            .user_agent("FiqhAI/1.0 Islamic Token Analyzer")
            .timeout(Duration::from_secs(30))
            .build()
            .expect("Failed to create HTTP client");

        let stats = Arc::new(Mutex::new(ScrapingStats {
            total_requests: 0,
            successful_scrapes: 0,
            failed_scrapes: 0,
            average_response_time_ms: 0,
            rate_limit_hits: 0,
            last_scrape_time: None,
        }));

        // Allow up to 5 concurrent scraping operations
        let rate_limiter = Arc::new(Semaphore::new(5));

        Self {
            receiver,
            client,
            stats,
            rate_limiter,
        }
    }

    pub async fn run(&mut self) {
        info!("ScraperActor started");

        while let Some(msg) = self.receiver.recv().await {
            debug!("ScraperActor received message");

            match msg {
                ScraperMessage::ScrapeUrl {
                    url,
                    keywords,
                    respond_to,
                } => {
                    let result = self.scrape_single_url(url, keywords).await;
                    if let Err(e) = respond_to.send(result) {
                        error!("Failed to send scraping result: {:?}", e);
                    }
                },
                ScraperMessage::BatchScrape {
                    urls,
                    keywords,
                    respond_to,
                } => {
                    let results = self.batch_scrape_urls(urls, keywords).await;
                    if let Err(e) = respond_to.send(results) {
                        error!("Failed to send batch scraping results: {:?}", e);
                    }
                },
                ScraperMessage::ScrapeCryptoHalal {
                    token_symbol,
                    respond_to,
                } => {
                    let result = self.scrape_crypto_halal(token_symbol).await;
                    if let Err(e) = respond_to.send(result) {
                        error!("Failed to send CryptoHalal scraping result: {:?}", e);
                    }
                },
                ScraperMessage::GetScrapingStats {
                    respond_to,
                } => {
                    let stats = self.get_stats();
                    if let Err(e) = respond_to.send(stats) {
                        error!("Failed to send scraping stats: {:?}", e);
                    }
                },
            }
        }

        warn!("ScraperActor shutting down");
    }

    async fn scrape_single_url(
        &self,
        url: String,
        keywords: Vec<String>,
    ) -> Result<ScrapedData, ScraperError> {
        debug!("Scraping URL: {}", url);
        let start_time = Instant::now();

        // Acquire semaphore permit for rate limiting
        let _permit = self.rate_limiter.acquire().await.map_err(|_| ScraperError::RateLimited)?;

        self.increment_total_requests();

        // Validate URL
        if !self.is_valid_url(&url) {
            self.increment_failed_scrapes();
            return Err(ScraperError::InvalidUrl(url));
        }

        // Add small delay to be respectful
        tokio::time::sleep(Duration::from_millis(100)).await;

        let response = match self.client.get(&url).send().await {
            Ok(resp) => {
                if resp.status().is_success() {
                    resp
                } else {
                    self.increment_failed_scrapes();
                    return Err(ScraperError::NetworkError(format!("HTTP {} for {}", resp.status(), url)));
                }
            },
            Err(e) => {
                self.increment_failed_scrapes();
                return Err(ScraperError::NetworkError(e.to_string()));
            },
        };

        let content = match response.text().await {
            Ok(text) => {
                if text.len() > 1_000_000 {
                    // 1MB limit
                    self.increment_failed_scrapes();
                    return Err(ScraperError::ContentTooLarge);
                }
                text
            },
            Err(e) => {
                self.increment_failed_scrapes();
                return Err(ScraperError::NetworkError(e.to_string()));
            },
        };

        // Parse and extract relevant content
        let parsed_content = self.extract_relevant_content(&content, &keywords, &url);

        let processing_time = start_time.elapsed();
        self.update_stats(processing_time);
        self.increment_successful_scrapes();

        let data_type = self.classify_data_type(&url);
        let scraped_data = ScrapedData::new(url, parsed_content, data_type, Some("Mock Title".to_owned()));
        // Mock implementation of calculate_relevance
        // In real implementation this would be in the ScrapedData impl block
        // scraped_data.calculate_relevance(&keywords);

        Ok(scraped_data)
    }

    async fn batch_scrape_urls(
        &self,
        urls: Vec<String>,
        keywords: Vec<String>,
    ) -> Vec<Result<ScrapedData, ScraperError>> {
        info!("Batch scraping {} URLs", urls.len());

        // Use join_all for concurrent scraping with rate limiting handled by semaphore
        let futures: Vec<_> = urls
            .into_iter()
            .map(|url| {
                let keywords_clone = keywords.clone();
                async move { self.scrape_single_url(url, keywords_clone).await }
            })
            .collect();

        futures::future::join_all(futures).await
    }

    async fn scrape_crypto_halal(
        &self,
        token_symbol: String,
    ) -> Result<Vec<ScrapedData>, ScraperError> {
        info!("Scraping CryptoHalal for token: {}", token_symbol);

        let urls = vec![
            format!("https://cryptohalal.cc/search?q={}", token_symbol),
            format!("https://cryptohalal.cc/token/{}", token_symbol.to_lowercase()),
            format!("https://cryptohalal.cc/analysis/{}", token_symbol.to_lowercase()),
        ];

        let keywords = vec![
            token_symbol.clone(),
            "halal".to_owned(),
            "haram".to_owned(),
            "islamic".to_owned(),
            "syariah".to_owned(),
            "fatwa".to_owned(),
        ];

        let results = self.batch_scrape_urls(urls, keywords).await;

        // Filter successful results
        let successful_results: Vec<ScrapedData> = results
            .into_iter()
            .filter_map(|r| r.ok())
            .filter(|data| !data.content.is_empty())
            .collect();

        if successful_results.is_empty() {
            Err(ScraperError::NetworkError("No data found for token".to_owned()))
        } else {
            Ok(successful_results)
        }
    }

    fn extract_relevant_content(
        &self,
        html: &str,
        keywords: &[String],
        url: &str,
    ) -> String {
        // Parse HTML
        let document = Html::parse_document(html);

        // Try different strategies based on URL type
        if url.contains("cryptohalal") {
            self.extract_crypto_halal_content(&document)
        } else if url.contains("coingecko") || url.contains("coinmarketcap") {
            self.extract_crypto_market_content(&document)
        } else {
            self.extract_general_content(&document, keywords)
        }
    }

    fn extract_crypto_halal_content(
        &self,
        document: &Html,
    ) -> String {
        let mut content = Vec::new();

        // Extract main content areas
        let selectors = vec![
            ".analysis-result",
            ".token-analysis",
            ".fatwa-content",
            ".ruling-section",
            "article",
            ".content",
            "main",
        ];

        for selector_str in &selectors {
            if let Ok(selector) = Selector::parse(selector_str) {
                for element in document.select(&selector) {
                    let text = element.text().collect::<Vec<_>>().join(" ");
                    if !text.trim().is_empty() && text.len() > 20 {
                        content.push(text);
                    }
                }
            }
        }

        // If no specific content found, extract from paragraphs
        if content.is_empty() {
            if let Ok(p_selector) = Selector::parse("p") {
                for element in document.select(&p_selector) {
                    let text = element.text().collect::<Vec<_>>().join(" ");
                    if text.len() > 50
                        && (text.to_lowercase().contains("halal")
                            || text.to_lowercase().contains("haram")
                            || text.to_lowercase().contains("islamic"))
                    {
                        content.push(text);
                    }
                }
            }
        }

        content.join("\n\n").chars().take(5000).collect()
    }

    fn extract_crypto_market_content(
        &self,
        document: &Html,
    ) -> String {
        let mut content = Vec::new();

        // Extract key information from crypto market sites
        let selectors = vec![
            ".description",
            ".project-description",
            ".about",
            ".overview",
            ".token-description",
            "meta[name='description']",
        ];

        for selector_str in &selectors {
            if let Ok(selector) = Selector::parse(selector_str) {
                for element in document.select(&selector) {
                    let text = if selector_str.contains("meta") {
                        element.value().attr("content").unwrap_or("").to_owned()
                    } else {
                        element.text().collect::<Vec<_>>().join(" ")
                    };

                    if !text.trim().is_empty() && text.len() > 20 {
                        content.push(text);
                    }
                }
            }
        }

        content.join("\n\n").chars().take(3000).collect()
    }

    fn extract_general_content(
        &self,
        document: &Html,
        keywords: &[String],
    ) -> String {
        let mut content = Vec::new();

        // Extract paragraphs that contain keywords
        if let Ok(selector) = Selector::parse("p") {
            for element in document.select(&selector) {
                let text = element.text().collect::<Vec<_>>().join(" ");
                if text.len() > 30 {
                    let text_lower = text.to_lowercase();
                    let has_keyword = keywords.iter().any(|keyword| text_lower.contains(&keyword.to_lowercase()));

                    if has_keyword {
                        content.push(text);
                    }
                }
            }
        }

        // Limit content length
        content.join("\n\n").chars().take(4000).collect()
    }

    fn classify_data_type(
        &self,
        url: &str,
    ) -> ScrapedDataType {
        let url_lower = url.to_lowercase();

        if url_lower.contains("cryptohalal") {
            ScrapedDataType::Official
        } else if url_lower.contains("news") || url_lower.contains("article") {
            ScrapedDataType::News
        } else if url_lower.contains("docs") || url_lower.contains("documentation") {
            ScrapedDataType::Documentation
        } else if url_lower.contains("forum") || url_lower.contains("reddit") || url_lower.contains("discord") {
            ScrapedDataType::Forum
        } else if url_lower.contains("twitter") || url_lower.contains("telegram") || url_lower.contains("social") {
            ScrapedDataType::Social
        } else {
            ScrapedDataType::UserProvided
        }
    }

    fn is_valid_url(
        &self,
        url: &str,
    ) -> bool {
        if let Ok(parsed) = url::Url::parse(url) {
            matches!(parsed.scheme(), "http" | "https")
        } else {
            false
        }
    }

    // Statistics management
    fn increment_total_requests(&self) {
        if let Ok(mut stats) = self.stats.lock() {
            stats.total_requests += 1;
        }
    }

    fn increment_successful_scrapes(&self) {
        if let Ok(mut stats) = self.stats.lock() {
            stats.successful_scrapes += 1;
            stats.last_scrape_time = Some(Utc::now());
        }
    }

    fn increment_failed_scrapes(&self) {
        if let Ok(mut stats) = self.stats.lock() {
            stats.failed_scrapes += 1;
        }
    }

    fn update_stats(
        &self,
        processing_time: Duration,
    ) {
        if let Ok(mut stats) = self.stats.lock() {
            let new_time_ms = processing_time.as_millis() as u64;
            if stats.total_requests > 1 {
                stats.average_response_time_ms = (stats.average_response_time_ms + new_time_ms) / 2;
            } else {
                stats.average_response_time_ms = new_time_ms;
            }
        }
    }

    fn get_stats(&self) -> ScrapingStats {
        self.stats.lock().map(|stats| stats.clone()).unwrap_or_else(|_| ScrapingStats {
            total_requests: 0,
            successful_scrapes: 0,
            failed_scrapes: 0,
            average_response_time_ms: 0,
            rate_limit_hits: 0,
            last_scrape_time: None,
        })
    }
}

// Actor spawner function
pub async fn spawn_scraper_actor() -> crate::models::ScraperActorHandle {
    let (sender, receiver) = mpsc::channel(100);

    let mut actor = ScraperActor::new(receiver);

    tokio::spawn(async move {
        actor.run().await;
    });

    crate::models::ScraperActorHandle {
        sender,
    }
}

#[cfg(test)]
mod tests {
    // use tokio_test;

    use super::*;

    #[tokio::test]
    async fn test_scraper_actor_creation() {
        let handle = spawn_scraper_actor().await;
        assert!(handle.sender.capacity() > 0);
    }

    #[tokio::test]
    async fn test_url_validation() {
        let (_sender, receiver) = mpsc::channel(10);
        let actor = ScraperActor::new(receiver);

        assert!(actor.is_valid_url("https://example.com"));
        assert!(actor.is_valid_url("http://example.com"));
        assert!(!actor.is_valid_url("ftp://example.com"));
        assert!(!actor.is_valid_url("invalid-url"));
    }

    #[tokio::test]
    async fn test_data_type_classification() {
        let (_sender, receiver) = mpsc::channel(10);
        let actor = ScraperActor::new(receiver);

        assert!(matches!(actor.classify_data_type("https://cryptohalal.cc/token/btc"), ScrapedDataType::Official));

        assert!(matches!(actor.classify_data_type("https://news.com/article"), ScrapedDataType::News));

        assert!(matches!(actor.classify_data_type("https://docs.example.com"), ScrapedDataType::Documentation));
    }
}
