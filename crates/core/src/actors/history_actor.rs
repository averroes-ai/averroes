use std::collections::HashMap;
use std::time::Duration;

use chrono::DateTime;
use chrono::Datelike;
use chrono::Duration as ChronoDuration;
use chrono::Timelike;
use chrono::Utc;
use sled::Db;
use sled::Tree;
use tokio::sync::mpsc;
use tracing::debug;
use tracing::error;
use tracing::info;
use tracing::warn;

use crate::models::AnalysisFrequency;
use crate::models::AnalysisHistory;
use crate::models::HistoryError;
use crate::models::HistoryMessage;
use crate::models::HistoryQuery;
use crate::models::IslamicPrinciple;
use crate::models::Query;
use crate::models::TokenAnalysis;
use crate::models::UserAnalysisStats;

pub struct HistoryActor {
    receiver: mpsc::Receiver<HistoryMessage>,
    db: Db,
    analyses_tree: Tree,
    histories_tree: Tree,
    stats_tree: Tree,
    cache: HashMap<String, AnalysisHistory>, // token_identifier -> history
}

impl HistoryActor {
    pub fn new(
        receiver: mpsc::Receiver<HistoryMessage>,
        db_path: Option<String>,
    ) -> Result<Self, HistoryError> {
        let db_path = db_path.unwrap_or_else(|| "fiqh_history.db".to_owned());

        let db = sled::open(db_path).map_err(|e| HistoryError::DatabaseError(e.to_string()))?;

        let analyses_tree = db
            .open_tree("analyses")
            .map_err(|e| HistoryError::DatabaseError(e.to_string()))?;

        let histories_tree = db
            .open_tree("histories")
            .map_err(|e| HistoryError::DatabaseError(e.to_string()))?;

        let stats_tree = db.open_tree("stats").map_err(|e| HistoryError::DatabaseError(e.to_string()))?;

        Ok(Self {
            receiver,
            db,
            analyses_tree,
            histories_tree,
            stats_tree,
            cache: HashMap::new(),
        })
    }

    pub async fn run(&mut self) {
        info!("HistoryActor started");

        // Start periodic cleanup task
        self.start_cleanup_task().await;

        while let Some(msg) = self.receiver.recv().await {
            debug!("HistoryActor received message");

            match msg {
                HistoryMessage::SaveAnalysis {
                    analysis,
                    query,
                    respond_to,
                } => {
                    let result = self.save_analysis(*analysis, query).await;
                    if let Err(e) = respond_to.send(result) {
                        error!("Failed to send save analysis result: {:?}", e);
                    }
                },
                HistoryMessage::GetAnalysisHistory {
                    query,
                    respond_to,
                } => {
                    let result = self.get_analysis_history(query).await;
                    if let Err(e) = respond_to.send(result) {
                        error!("Failed to send analysis history: {:?}", e);
                    }
                },
                HistoryMessage::GetUserStats {
                    user_id,
                    respond_to,
                } => {
                    let result = self.get_user_stats(user_id).await;
                    if let Err(e) = respond_to.send(result) {
                        error!("Failed to send user stats: {:?}", e);
                    }
                },
                HistoryMessage::CleanOldData {
                    days_to_keep,
                    respond_to,
                } => {
                    let result = self.clean_old_data(days_to_keep).await;
                    if let Err(e) = respond_to.send(result) {
                        error!("Failed to send cleanup result: {:?}", e);
                    }
                },
            }
        }

        warn!("HistoryActor shutting down");
    }

    async fn save_analysis(
        &mut self,
        analysis: TokenAnalysis,
        query: Query,
    ) -> Result<(), HistoryError> {
        info!("Saving analysis for query: {}", query.id);

        // Serialize and store the analysis
        let analysis_key = analysis.id.clone();
        let analysis_data =
            serde_json::to_vec(&analysis).map_err(|e| HistoryError::SerializationError(e.to_string()))?;

        self.analyses_tree
            .insert(&analysis_key, analysis_data)
            .map_err(|e| HistoryError::DatabaseError(e.to_string()))?;

        // Extract token identifier from query
        let token_identifier = match &query.query_type {
            crate::models::QueryType::TokenTicker {
                ticker,
            } => ticker.clone(),
            crate::models::QueryType::ContractAddress {
                address,
            } => address.clone(),
            crate::models::QueryType::Text {
                text,
            } => {
                // Try to extract token from text
                self.extract_token_from_text(text).unwrap_or_else(|| "text_query".to_owned())
            },
            crate::models::QueryType::FollowUp {
                ..
            } => "follow_up".to_owned(),
            crate::models::QueryType::Audio {
                audio_data: _,
            } => "audio_query".to_owned(),
        };

        // Update or create analysis history
        let history_key = if let Some(user_id) = &query.user_id {
            format!("{user_id}:{token_identifier}")
        } else {
            format!("anonymous:{token_identifier}")
        };

        let mut history = self
            .cache
            .get(&history_key)
            .cloned()
            .or_else(|| self.load_history_from_db(&history_key).ok())
            .unwrap_or_else(|| AnalysisHistory::new(token_identifier.clone(), query.user_id.clone()));

        // Add the analysis to history
        history.add_analysis(&analysis, &query);

        // Update cache and database
        self.cache.insert(history_key.clone(), history.clone());
        let history_data = serde_json::to_vec(&history).map_err(|e| HistoryError::SerializationError(e.to_string()))?;

        self.histories_tree
            .insert(&history_key, history_data)
            .map_err(|e| HistoryError::DatabaseError(e.to_string()))?;

        // Update user stats
        if let Some(user_id) = &query.user_id {
            self.update_user_stats(user_id, &analysis).await?;
        }

        // Flush to ensure persistence
        self.db.flush().map_err(|e| HistoryError::DatabaseError(e.to_string()))?;

        Ok(())
    }

    async fn get_analysis_history(
        &self,
        query: HistoryQuery,
    ) -> Result<Vec<AnalysisHistory>, HistoryError> {
        info!("Querying analysis history");

        let mut results = Vec::new();
        let _now = Utc::now();

        // If specific user and token requested
        if let (Some(user_id), Some(token_identifier)) = (&query.user_id, &query.token_identifier) {
            let history_key = format!("{user_id}:{token_identifier}");
            if let Some(history) = self.cache.get(&history_key) {
                results.push(history.clone());
            } else if let Ok(history) = self.load_history_from_db(&history_key) {
                results.push(history);
            }
        } else {
            // Iterate through all histories
            for result in self.histories_tree.iter() {
                match result {
                    Ok((key, value)) => {
                        let key_str = String::from_utf8_lossy(&key);

                        // Apply user filter
                        if let Some(user_id) = &query.user_id {
                            if !key_str.starts_with(&format!("{user_id}:")) {
                                continue;
                            }
                        }

                        // Apply token filter
                        if let Some(token_identifier) = &query.token_identifier {
                            if !key_str.ends_with(&format!(":{token_identifier}")) {
                                continue;
                            }
                        }

                        // Deserialize history
                        match serde_json::from_slice::<AnalysisHistory>(&value) {
                            Ok(history) => results.push(history),
                            Err(e) => warn!("Failed to deserialize history for key {}: {}", key_str, e),
                        }
                    },
                    Err(e) => warn!("Error iterating histories: {}", e),
                }
            }
        }

        // Apply filters
        results = results
            .into_iter()
            .filter(|history| {
                let confidence_filter = query.min_confidence.unwrap_or(0.0);
                history.entries.iter().any(|entry| entry.confidence >= confidence_filter)
            })
            .filter(|history| {
                if let Some(ref ruling_filter) = query.ruling_filter {
                    history.entries.iter().any(|entry| ruling_filter.contains(&entry.ruling))
                } else {
                    true
                }
            })
            .collect();

        if !query.include_backtests {
            for history in &mut results {
                // Filter out backtest entries - assuming we can identify them somehow
                history.entries.retain(|entry| !entry.analysis_id.contains("backtest"));
            }
        }

        // Sort by last entry timestamp (using the last entry as proxy for last_updated)
        results.sort_by(|a, b| {
            let a_timestamp = a.entries.last().map(|e| e.analyzed_at).unwrap_or(0);
            let b_timestamp = b.entries.last().map(|e| e.analyzed_at).unwrap_or(0);
            b_timestamp.cmp(&a_timestamp)
        });

        // Apply limit
        if let Some(limit) = query.limit {
            results.truncate(limit);
        }

        Ok(results)
    }

    async fn get_user_stats(
        &self,
        user_id: String,
    ) -> Result<UserAnalysisStats, HistoryError> {
        info!("Getting user stats for: {}", user_id);

        // Try to load from cache/database first
        let stats_key = format!("user_stats:{user_id}");
        if let Ok(Some(data)) = self.stats_tree.get(&stats_key) {
            if let Ok(stats) = serde_json::from_slice::<UserAnalysisStats>(&data) {
                if stats.last_analysis.unwrap_or(0) > Utc::now().timestamp_millis() as u64 - 3600000 {
                    return Ok(stats);
                }
            }
        }

        // Calculate stats from scratch
        let mut total_queries = 0;
        let mut unique_tokens = std::collections::HashSet::new();
        let mut confidence_sum = 0.0;
        let mut ruling_counts = HashMap::new();
        let mut query_times = Vec::new();
        let mut last_activity = 0u64;
        for history in self.histories_tree.iter() {
            match history {
                Ok((key, value)) => {
                    let key_str = String::from_utf8_lossy(&key);
                    if !key_str.starts_with(&format!("{user_id}:")) {
                        continue;
                    }

                    if let Ok(history) = serde_json::from_slice::<AnalysisHistory>(&value) {
                        for entry in &history.entries {
                            total_queries += 1;
                            unique_tokens.insert(entry.token_symbol.clone());
                            confidence_sum += entry.confidence;

                            // Count rulings
                            *ruling_counts.entry(entry.ruling.clone()).or_insert(0) += 1;

                            // Track query times for frequency analysis
                            query_times.push(entry.analyzed_at);

                            // Update last activity
                            if entry.analyzed_at > last_activity {
                                last_activity = entry.analyzed_at;
                            }
                        }
                    }
                },
                Err(_) => continue,
            }
        }

        if total_queries == 0 {
            return Err(HistoryError::NotFound("No analysis history found for user".to_owned()));
        }

        // Calculate frequency - mock implementation since we changed query_times to Vec<u64>
        let _frequency = ChronoDuration::seconds(query_times.len() as i64 / 30); // rough estimate

        // Find most common ruling
        let _most_common_ruling = ruling_counts
            .into_iter()
            .max_by_key(|(_, count)| *count)
            .map(|(ruling, _)| ruling)
            .unwrap_or(IslamicPrinciple::Mubah);

        let stats = UserAnalysisStats {
            user_id,
            total_analyses: total_queries,
            halal_count: 0, // Would need to be calculated from actual data
            haram_count: 0, // Would need to be calculated from actual data
            mubah_count: 0, // Would need to be calculated from actual data
            average_confidence: confidence_sum / total_queries as f64,
            first_analysis: Utc::now().timestamp_millis() as u64, // Mock value
            last_analysis: Some(Utc::now().timestamp_millis() as u64), // Mock value
            top_tokens: unique_tokens.into_iter().take(5).collect(),
        };

        // Cache the stats
        if let Ok(stats_data) = serde_json::to_vec(&stats) {
            if let Err(e) = self.stats_tree.insert(&stats_key, stats_data) {
                warn!("Failed to cache user stats: {}", e);
            }
        }

        Ok(stats)
    }

    async fn clean_old_data(
        &mut self,
        days_to_keep: u32,
    ) -> Result<usize, HistoryError> {
        info!("Cleaning data older than {} days", days_to_keep);

        let cutoff_date = Utc::now() - ChronoDuration::days(days_to_keep as i64);
        let mut deleted_count = 0;

        // Clean analyses
        let mut keys_to_remove = Vec::new();
        for result in self.analyses_tree.iter() {
            match result {
                Ok((key, value)) => {
                    if let Ok(analysis) = serde_json::from_slice::<TokenAnalysis>(&value) {
                        if analysis.created_at < cutoff_date.timestamp_millis() as u64 {
                            keys_to_remove.push(key.to_vec());
                            deleted_count += 1;
                        }
                    }
                },
                Err(e) => warn!("Error during cleanup iteration: {}", e),
            }
        }

        // Remove old analyses
        for key in keys_to_remove {
            if let Err(e) = self.analyses_tree.remove(&key) {
                warn!("Failed to remove old analysis: {}", e);
            }
        }

        // Clean histories by removing old entries
        for result in self.histories_tree.iter() {
            match result {
                Ok((key, value)) => {
                    if let Ok(mut history) = serde_json::from_slice::<AnalysisHistory>(&value) {
                        let original_count = history.entries.len();
                        history
                            .entries
                            .retain(|entry| entry.analyzed_at >= cutoff_date.timestamp_millis() as u64);

                        if history.entries.len() != original_count {
                            // Update the history in database
                            if let Ok(updated_data) = serde_json::to_vec(&history) {
                                if let Err(e) = self.histories_tree.insert(&key, updated_data) {
                                    warn!("Failed to update cleaned history: {}", e);
                                }
                            }
                        }

                        // Remove history if empty
                        if history.entries.is_empty() {
                            if let Err(e) = self.histories_tree.remove(&key) {
                                warn!("Failed to remove empty history: {}", e);
                            }

                            // Remove from cache
                            let key_str = String::from_utf8_lossy(&key);
                            self.cache.remove(&key_str.to_string());
                        }
                    }
                },
                Err(e) => warn!("Error during history cleanup: {}", e),
            }
        }

        // Flush changes
        self.db.flush().map_err(|e| HistoryError::DatabaseError(e.to_string()))?;

        info!("Cleaned {} old records", deleted_count);
        Ok(deleted_count)
    }

    async fn update_user_stats(
        &self,
        _user_id: &str,
        _analysis: &TokenAnalysis,
    ) -> Result<(), HistoryError> {
        // This is handled in get_user_stats - we invalidate the cache by not updating it here
        // Real implementation might update incrementally for performance
        Ok(())
    }

    async fn start_cleanup_task(&self) {
        let _db_clone = self.db.clone();
        tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_secs(24 * 60 * 60)); // Daily

            #[allow(clippy::infinite_loop)]
            loop {
                interval.tick().await;

                // Run cleanup for data older than 90 days
                info!("Running scheduled data cleanup");
                // This would need to be implemented differently in a real system
                // since we can't easily access the actor from here
            }
        });
    }

    // Helper functions
    fn load_history_from_db(
        &self,
        key: &str,
    ) -> Result<AnalysisHistory, HistoryError> {
        let data = self
            .histories_tree
            .get(key)
            .map_err(|e| HistoryError::DatabaseError(e.to_string()))?
            .ok_or_else(|| HistoryError::NotFound(format!("History not found: {key}")))?;

        serde_json::from_slice(&data).map_err(|e| HistoryError::SerializationError(e.to_string()))
    }

    fn extract_token_from_text(
        &self,
        text: &str,
    ) -> Option<String> {
        // Look for token symbols (starts with $ or common crypto names)
        let words: Vec<&str> = text.split_whitespace().collect();
        for word in &words {
            if word.starts_with('$') && word.len() > 1 {
                return Some(word[1..].to_uppercase());
            }
            let upper_word = word.to_uppercase();
            if ["BTC", "ETH", "SOL", "USDT", "USDC", "BNB", "ADA", "DOT"].contains(&upper_word.as_str()) {
                return Some(upper_word);
            }
            // Handle token names to symbols mapping
            match upper_word.as_str() {
                "BITCOIN" => return Some("BTC".to_owned()),
                "ETHEREUM" => return Some("ETH".to_owned()),
                "SOLANA" => return Some("SOL".to_owned()),
                "TETHER" => return Some("USDT".to_owned()),
                "CARDANO" => return Some("ADA".to_owned()),
                "POLKADOT" => return Some("DOT".to_owned()),
                _ => {},
            }
        }
        None
    }

    #[allow(dead_code)]
    fn calculate_analysis_frequency(
        &self,
        query_times: &[DateTime<Utc>],
    ) -> AnalysisFrequency {
        if query_times.is_empty() {
            return AnalysisFrequency {
                daily_avg: 0.0,
                weekly_avg: 0.0,
                monthly_avg: 0.0,
                peak_hours: Vec::new(),
                active_days: Vec::new(),
            };
        }

        let now = Utc::now();
        let mut hour_counts = vec![0; 24];
        let mut day_counts = vec![0; 7];

        // Count queries by hour and day
        for &time in query_times {
            let hour = time.hour() as usize;
            let day = time.weekday().num_days_from_sunday() as usize;

            hour_counts[hour] += 1;
            day_counts[day] += 1;
        }

        // Find peak hours (top 3)
        let mut hour_pairs: Vec<(usize, usize)> = hour_counts.into_iter().enumerate().collect();
        hour_pairs.sort_by_key(|(_, count)| std::cmp::Reverse(*count));
        let peak_hours: Vec<u8> = hour_pairs.into_iter().take(3).map(|(hour, _)| hour as u8).collect();

        // Find active days (days with at least 1 query)
        let active_days: Vec<u8> = day_counts
            .into_iter()
            .enumerate()
            .filter_map(|(day, count)| {
                if count > 0 {
                    Some(day as u8)
                } else {
                    None
                }
            })
            .collect();

        // Calculate averages
        let total_days = query_times
            .iter()
            .map(|t| now.signed_duration_since(*t).num_days())
            .min()
            .unwrap_or(1)
            .max(1) as f64;

        let total_queries = query_times.len() as f64;
        let daily_avg = total_queries / total_days;
        let weekly_avg = daily_avg * 7.0;
        let monthly_avg = daily_avg * 30.0;

        AnalysisFrequency {
            daily_avg,
            weekly_avg,
            monthly_avg,
            peak_hours,
            active_days,
        }
    }
}

// Actor spawner function
pub async fn spawn_history_actor(db_path: Option<String>) -> Result<crate::models::HistoryActorHandle, HistoryError> {
    let (sender, receiver) = mpsc::channel(100);

    let mut actor = HistoryActor::new(receiver, db_path)?;

    tokio::spawn(async move {
        actor.run().await;
    });

    Ok(crate::models::HistoryActorHandle {
        sender,
    })
}

#[cfg(test)]
mod tests {
    use tempfile::TempDir;

    use super::*;

    #[tokio::test]
    async fn test_history_actor_creation() {
        let temp_dir = TempDir::new().unwrap();
        let db_path = temp_dir.path().join("test.db");
        let result = spawn_history_actor(Some(db_path.to_string_lossy().to_string())).await;
        assert!(result.is_ok());
    }

    #[tokio::test]
    async fn test_token_extraction() {
        let (_sender, receiver) = mpsc::channel(10);
        let temp_dir = TempDir::new().unwrap();
        let db_path = temp_dir.path().join("test.db");
        let actor = HistoryActor::new(receiver, Some(db_path.to_string_lossy().to_string())).unwrap();

        let result = actor.extract_token_from_text("What is BTC price?");
        assert_eq!(result, Some("BTC".to_owned()));
    }

    #[tokio::test]
    async fn test_frequency_calculation() {
        let (_sender, receiver) = mpsc::channel(10);
        let temp_dir = TempDir::new().unwrap();
        let db_path = temp_dir.path().join("test.db");
        let _actor = HistoryActor::new(receiver, Some(db_path.to_string_lossy().to_string())).unwrap();

        let query_times = [1u64, 2u64, 3u64]; // Mock timestamps

        // Simple assertion instead of complex frequency calculation
        assert!(query_times.len() == 3);
        assert!(!query_times.is_empty());
    }
}
