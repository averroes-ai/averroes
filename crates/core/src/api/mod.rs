use crate::models::AnalyzerActorHandle;
use crate::models::HistoryActorHandle;
use crate::models::QueryActorHandle;

pub mod handlers;
pub mod middleware;
pub mod routes;

/// Application state shared across all handlers
#[derive(Clone)]
pub struct AppState {
    pub query_actor: QueryActorHandle,
    pub history_actor: HistoryActorHandle,
    pub analyzer_actor: AnalyzerActorHandle,
}

impl AppState {
    pub fn new(
        query_actor: QueryActorHandle,
        history_actor: HistoryActorHandle,
        analyzer_actor: AnalyzerActorHandle,
    ) -> Self {
        Self {
            query_actor,
            history_actor,
            analyzer_actor,
        }
    }
}
