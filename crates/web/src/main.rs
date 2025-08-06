use axum::Router;
use axum::response::Html;
use axum::routing::get;
use tokio::net::TcpListener;
use tracing::info;
use tracing_subscriber::fmt::init;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Initialize tracing
    init();

    info!("🌐 Starting Averroes Web Server");

    // Create router with basic UI
    let app = Router::new().route("/", get(home_page)).route("/health", get(health_check));

    // Bind to localhost:8080
    let listener = TcpListener::bind("127.0.0.1:8080").await?;
    info!("🌐 Web server listening on: http://127.0.0.1:8080");

    // Start server
    axum::serve(listener, app).await?;

    Ok(())
}

async fn health_check() -> &'static str {
    "OK"
}

async fn home_page() -> Html<&'static str> {
    Html(
        r#"
    <!DOCTYPE html>
    <html>
    <head>
        <title>Averroes - Islamic Token Advisor</title>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                   margin: 0; padding: 2rem; background: #f5f5f5; }
            .container { max-width: 800px; margin: 0 auto; background: white; 
                        padding: 2rem; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
            h1 { color: #2563eb; text-align: center; margin-bottom: 2rem; }
            .status { background: #10b981; color: white; padding: 1rem; border-radius: 4px; 
                     text-align: center; margin-bottom: 2rem; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🕌 Averroes - Islamic Token Advisor</h1>
            <div class="status">✅ Web Server Running</div>
            <p>Welcome to Averroes, your Islamic finance advisor for cryptocurrency analysis according to Sharia principles.</p>
            <p><strong>Features:</strong></p>
            <ul>
                <li>🔍 Token Analysis according to Maqashid Syariah</li>
                <li>📊 Riba, Gharar, and Maysir Assessment</li>
                <li>🤖 AI-Powered Fatwa Analysis</li>
                <li>📱 Cross-Platform (Android, iOS, Web, Desktop)</li>
            </ul>
            <p><em>Server is running and ready for development.</em></p>
        </div>
    </body>
    </html>
    "#,
    )
}
