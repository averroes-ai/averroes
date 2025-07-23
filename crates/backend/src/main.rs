use axum::Router;
use axum::routing::get;
use tokio::net::TcpListener;
use tracing::info;
use tracing_subscriber::fmt::init;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Initialize tracing
    init();

    info!("🚀 Starting FiqhAI Backend Server");

    // Create router with basic health check
    let app = Router::new()
        .route("/health", get(health_check))
        .route("/", get(|| async { "FiqhAI Backend Server" }));

    // Bind to localhost:3000
    let listener = TcpListener::bind("127.0.0.1:3000").await?;
    info!("📡 Backend server listening on: http://127.0.0.1:3000");

    // Start server
    axum::serve(listener, app).await?;

    Ok(())
}

async fn health_check() -> &'static str {
    "OK"
}
