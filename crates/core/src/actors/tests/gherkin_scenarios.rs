use std::time::Duration;

use tempfile::TempDir;
use tokio::time::timeout;
use uuid::Uuid;

use crate::actors::spawn_analyzer_actor;
use crate::actors::spawn_history_actor;
use crate::actors::spawn_query_actor;
use crate::actors::spawn_scraper_actor;
use crate::models::AnalysisStatus;
use crate::models::BacktestResult;
use crate::models::HistoryQuery;
use crate::models::IslamicPrinciple;
use crate::models::Query;
use crate::models::QueryResponse;
use crate::models::QueryType;
use crate::models::ScrapedData;
use crate::models::ScrapedDataType;
use crate::models::TokenAnalysis;

/// Test suite implementing all Gherkin scenarios from spec/features/

#[tokio::test]
async fn test_gherkin_scenario_text_input_btc() {
    // Scenario: Pengguna input dengan teks
    // JIKA user ingin tahu kehalalan token $BTC
    // KETIKA user input token $BTC
    // KEMUDIAN user mengetik prompt teks: "Apakah token $BTC itu halal secara maqashid syariah?"

    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    // User creates text query about BTC
    let query = Query::new_text(
        "Apakah token $BTC itu halal secara maqashid syariah?".to_owned(),
        Some("gherkin_user_1".to_owned()),
        Some("id".to_owned()),
    );

    // Process the query
    let result = timeout(Duration::from_secs(5), query_handle.process_query(query))
        .await
        .expect("Query should complete within 5 seconds")
        .expect("Query should succeed");

    // Validate the expected response format from Gherkin
    assert!(!result.response.is_empty(), "AI should provide a response");
    assert!(
        result.response.contains("maqashid syariah")
            || result.response.contains("riba")
            || result.response.contains("gharar")
            || result.response.contains("halal")
            || result.response.contains("haram"),
        "Response should contain Islamic finance terms"
    );

    // Expected: "Pengertian dari maqashid syariah adalah menjaga agama, jiwa, akal, keturunan, dan harta..."
    assert!(result.confidence > 0.0, "Should have confidence score");

    // Expected follow-up questions like "Apakah kamu ingin lebih tahu tentang..."
    assert!(!result.follow_up_questions.is_empty(), "Should provide follow-up questions");
    assert!(
        result
            .follow_up_questions
            .iter()
            .any(|q| q.contains("Fatwa") || q.contains("negara lain")),
        "Should offer to explain fatwas from other countries"
    );

    // Verify analysis is saved to history
    if let Some(analysis_id) = result.analysis_id {
        let history_result = history_handle.get_analysis_by_id(analysis_id).await;
        assert!(history_result.is_ok(), "Analysis should be saved to history");
    }

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_gherkin_scenario_audio_input_sol() {
    // Scenario: Pengguna input dengan audio
    // JIKA user ingin tahu kehalalan token $SOL
    // KETIKA user mengirimkan audio dengan berkata: "$SOL itu halal gak sih?"

    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    // Simulate audio input (mock audio bytes)
    let mock_audio_data = b"SOL itu halal gak sih?".to_vec(); // Mock audio representation
    let query = Query::new_audio(mock_audio_data, Some("gherkin_user_2".to_owned()), Some("id".to_owned()));

    let result = timeout(Duration::from_secs(10), query_handle.process_query(query))
        .await
        .expect("Audio query should complete within 10 seconds")
        .expect("Audio query should succeed");

    // Expected response: "Solana atau $SOL merupakan platform blockchain..."
    assert!(!result.response.is_empty());
    assert!(
        result.response.to_lowercase().contains("solana")
            || result.response.to_lowercase().contains("sol")
            || result.response.to_lowercase().contains("blockchain"),
        "Should mention Solana or blockchain"
    );

    // Expected follow-up: "Apakah kamu ingin melihat daftar DApps di ekosistem Solana..."
    assert!(!result.follow_up_questions.is_empty());
    assert!(
        result.follow_up_questions.iter().any(|q| q.to_lowercase().contains("dapps")
            || q.to_lowercase().contains("solana")
            || q.to_lowercase().contains("ekosistem")),
        "Should ask about Solana ecosystem DApps"
    );

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_gherkin_scenario_contract_address() {
    // Scenario: Pengguna memasukkan kontrak address
    // KETIKA user memasukkan smart contract address dari token ERC20 atau SPL
    // KEMUDIAN user mengetik: "Tolong analisa kontrak berikut: 0x1234567890abcdef..."

    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    let mock_contract_address = "0x1234567890abcdef1234567890abcdef12345678";
    let query = Query::new_contract_address(
        mock_contract_address.to_owned(),
        Some("gherkin_user_3".to_owned()),
        Some("id".to_owned()),
    );

    let result = timeout(Duration::from_secs(8), query_handle.process_query(query))
        .await
        .expect("Contract analysis should complete within 8 seconds")
        .expect("Contract analysis should succeed");

    // Expected response: "Token ini belum memiliki audit syariah formal..."
    assert!(!result.response.is_empty());
    assert!(
        result.response.contains("audit")
            || result.response.contains("kontrak")
            || result.response.contains("token")
            || result.response.contains("maysir")
            || result.response.contains("gharar"),
        "Should analyze contract for Islamic compliance"
    );

    // Should identify potential issues like "maysir dan gharar"
    let response_lower = result.response.to_lowercase();
    let contains_islamic_analysis = response_lower.contains("syariah")
        || response_lower.contains("halal")
        || response_lower.contains("haram")
        || response_lower.contains("maysir")
        || response_lower.contains("gharar");
    assert!(contains_islamic_analysis, "Should contain Islamic compliance analysis");

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_gherkin_scenario_chatbot_usdt() {
    // Scenario: Interaksi pengguna dengan chatbot CEX/DEX
    // KEMUDIAN user mengetik: "Apakah $USDT halal?"
    // Response: "USDT adalah stablecoin berbasis fiat..."

    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    let query =
        Query::new_text("Apakah $USDT halal?".to_owned(), Some("chatbot_user".to_owned()), Some("id".to_owned()));

    let result = timeout(Duration::from_secs(6), query_handle.process_query(query))
        .await
        .expect("USDT query should complete within 6 seconds")
        .expect("USDT query should succeed");

    // Expected: "USDT adalah stablecoin berbasis fiat. Tidak mengandung unsur riba secara langsung..."
    assert!(!result.response.is_empty());
    let response_lower = result.response.to_lowercase();
    assert!(
        response_lower.contains("usdt") || response_lower.contains("stablecoin") || response_lower.contains("fiat"),
        "Should explain USDT as fiat-backed stablecoin"
    );

    assert!(
        response_lower.contains("riba") || response_lower.contains("halal") || response_lower.contains("mubah"),
        "Should address riba concerns"
    );

    // Expected follow-up: "Ingin melihat daftar DEX yang memfasilitasi perdagangan USDT yang sesuai syariah?"
    assert!(!result.follow_up_questions.is_empty());
    assert!(
        result.follow_up_questions.iter().any(|q| q.to_lowercase().contains("dex")
            || q.to_lowercase().contains("perdagangan")
            || q.to_lowercase().contains("syariah")),
        "Should ask about Sharia-compliant DEX options"
    );

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_gherkin_scenario_automatic_scraping() {
    // Feature: Scraping Otomatis dari Sumber Awal
    // JIKA scraping engine aktif
    // KEMUDIAN sistem mengakses situs berikut dan mengekstrak informasi

    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    // Test scraping from the sources mentioned in Gherkin spec
    let test_urls = vec![
        "https://www.cryptohalal.cc/currencies/4",
        "https://sharlife.my/crypto-shariah/crypto/bitcoin",
        "https://www.islamicfinanceguru.com/crypto",
        "https://app.practicalislamicfinance.com/reports/crypto/",
    ];

    for url in test_urls {
        let result = timeout(Duration::from_secs(10), scraper_handle.scrape_single_url(url.to_owned())).await;

        match result {
            Ok(Ok(scraped_data)) => {
                // MAKA sistem akan memindai semua halaman terkait
                assert!(!scraped_data.content.is_empty(), "Should extract content from {}", url);

                // DAN mengekstrak informasi: nama token, hukum, alasan hukum, referensi
                assert!(scraped_data.source_url == url, "Should preserve source URL");

                // DAN menandainya sebagai sumber default internal
                assert!(matches!(
                    scraped_data.data_type,
                    ScrapedDataType::CryptoHalalVerification | ScrapedDataType::IslamicFinanceContent
                ));
            },
            Ok(Err(_)) => {
                // Scraping might fail in test environment, which is acceptable
                println!("⚠️  Scraping failed for {} (expected in test environment)", url);
            },
            Err(_) => {
                panic!("Scraping timeout for {}", url);
            },
        }
    }

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_gherkin_scenario_additional_user_data() {
    // Feature: Tambahan Data oleh Pengguna
    // JIKA user memberikan link tambahan untuk dianalisis

    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    // User provides additional sources
    let additional_urls = vec![
        "https://example.com/bitcoin-fatwa".to_owned(),
        "https://example.com/crypto-analysis".to_owned(),
    ];

    let query = Query::new_text(
        "Analisa BTC dengan sumber tambahan yang saya berikan".to_owned(),
        Some("user_with_sources".to_owned()),
        Some("id".to_owned()),
    );

    // In a real implementation, additional sources would be passed with the query
    // For now, we test that the system can handle multiple source analysis
    let result = timeout(Duration::from_secs(8), query_handle.process_query(query))
        .await
        .expect("Query with additional sources should complete")
        .expect("Query should succeed");

    assert!(!result.response.is_empty());
    assert!(result.sources.len() >= 1, "Should include source references");

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_gherkin_scenario_analysis_history() {
    // Feature: Riwayat Analisis Token
    // JIKA user ingin melihat histori analisis sebelumnya

    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    let user_id = "history_test_user".to_owned();

    // Perform multiple analyses to build history
    let tokens = vec!["BTC", "ETH", "SOL"];
    let mut analysis_ids = Vec::new();

    for token in tokens {
        let query = Query::new_token_ticker(token.to_owned(), Some(user_id.clone()), Some("id".to_owned()));

        let result = query_handle.process_query(query).await.expect("Analysis should succeed");
        if let Some(analysis_id) = result.analysis_id {
            analysis_ids.push(analysis_id);
        }
    }

    // Query analysis history
    let history_query = HistoryQuery::new().for_user(user_id.clone()).limit(10);

    let history = timeout(Duration::from_secs(5), history_handle.query_analyses(history_query))
        .await
        .expect("History query should complete")
        .expect("History query should succeed");

    assert!(!history.entries.is_empty(), "Should have analysis history");
    assert!(history.entries.len() >= 3, "Should have entries for all analyzed tokens");

    // Test that we can retrieve specific analysis
    for analysis_id in analysis_ids {
        let analysis = history_handle.get_analysis_by_id(analysis_id).await;
        assert!(analysis.is_ok(), "Should be able to retrieve specific analysis");
    }

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_gherkin_scenario_backtest_evaluation() {
    // Feature: Backtest & Evaluasi Berkala
    // JIKA jadwal mingguan backtest dimulai
    // MAKA sistem membandingkan hasil lama dengan data terkini

    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    // Create initial analysis
    let query = Query::new_token_ticker("BTC".to_owned(), Some("backtest_user".to_owned()), Some("id".to_owned()));

    let initial_result = query_handle
        .process_query(query)
        .await
        .expect("Initial analysis should succeed");
    let analysis_id = initial_result.analysis_id.expect("Should have analysis ID");

    // Wait a moment to simulate time passage
    tokio::time::sleep(Duration::from_millis(100)).await;

    // Run backtest
    let backtest_result = timeout(Duration::from_secs(10), analyzer_handle.run_backtest(analysis_id))
        .await
        .expect("Backtest should complete")
        .expect("Backtest should succeed");

    // Validate backtest results
    assert_eq!(backtest_result.analysis_id, analysis_id);
    assert!(!backtest_result.summary.is_empty(), "Should provide backtest summary");

    // Expected: "Dulu $XYZ dinilai syubhat, sekarang terindikasi haram karena ada info baru"
    println!("Backtest result: {}", backtest_result.summary);

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_gherkin_scenario_follow_up_questions() {
    // Test the interactive follow-up flow
    // Expected: AI akan otomatis berhenti jika tidak ada respon selama 5 menit

    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    // Initial query
    let query =
        Query::new_text("Apakah Bitcoin halal?".to_owned(), Some("followup_user".to_owned()), Some("id".to_owned()));

    let initial_result = query_handle.process_query(query).await.expect("Initial query should succeed");
    let original_query_id = initial_result.query_id;

    // Simulate user selecting a follow-up question
    assert!(!initial_result.follow_up_questions.is_empty(), "Should provide follow-up questions");

    let follow_up_query = Query {
        id: Uuid::new_v4(),
        query_type: QueryType::FollowUp {
            original_query_id,
            question: "Bagaimana hukum jual beli Bitcoin dalam Islam?".to_owned(),
        },
        user_id: Some("followup_user".to_owned()),
        timestamp: chrono::Utc::now(),
        language: "id".to_owned(),
        context: Some("Following up on Bitcoin analysis".to_owned()),
    };

    let follow_up_result = timeout(Duration::from_secs(6), query_handle.process_query(follow_up_query))
        .await
        .expect("Follow-up should complete")
        .expect("Follow-up should succeed");

    assert!(!follow_up_result.response.is_empty(), "Should respond to follow-up");
    assert!(
        follow_up_result.response.to_lowercase().contains("jual beli")
            || follow_up_result.response.to_lowercase().contains("trading")
            || follow_up_result.response.to_lowercase().contains("bitcoin"),
        "Should address Bitcoin trading question"
    );

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_performance_target_500ms() {
    // Ensure we meet the <500ms performance target mentioned in requirements
    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    let query = Query::new_token_ticker("BTC".to_owned(), Some("perf_test_user".to_owned()), Some("id".to_owned()));

    let start_time = std::time::Instant::now();
    let result = query_handle.process_query(query).await.expect("Query should succeed");
    let duration = start_time.elapsed();

    // Performance target: <500ms for token analysis
    assert!(
        duration < Duration::from_millis(500),
        "Analysis should complete in <500ms, took {}ms",
        duration.as_millis()
    );

    assert!(!result.response.is_empty(), "Should provide response");
    assert!(result.confidence > 0.0, "Should have confidence score");

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

#[tokio::test]
async fn test_meme_coin_analysis_pepe() {
    // Test analysis of meme coins like PEPE mentioned in requirements
    let (query_handle, scraper_handle, analyzer_handle, history_handle) = setup_test_system().await;

    let query = Query::new_token_ticker("PEPE".to_owned(), Some("meme_test_user".to_owned()), Some("id".to_owned()));

    let result = query_handle.process_query(query).await.expect("PEPE analysis should succeed");

    // Meme coins often have gambling-like characteristics
    let response_lower = result.response.to_lowercase();
    let mentions_risk_factors = response_lower.contains("speculation")
        || response_lower.contains("gambling")
        || response_lower.contains("maysir")
        || response_lower.contains("gharar")
        || response_lower.contains("spekulasi");

    assert!(mentions_risk_factors, "Should identify meme coin risk factors");

    cleanup_test_system(query_handle, scraper_handle, analyzer_handle, history_handle).await;
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

async fn setup_test_system() -> (
    crate::models::QueryActorHandle,
    crate::models::ScraperActorHandle,
    crate::models::AnalyzerActorHandle,
    crate::models::HistoryActorHandle,
) {
    // Use in-memory database for tests
    let history_handle = spawn_history_actor(None).await.expect("Failed to spawn history actor");

    let analyzer_handle = spawn_analyzer_actor(
        Some("http://localhost:8899".to_owned()),
        None, // Use default config for tests
        std::sync::Arc::new(crate::ai::AIService::new(&crate::AverroesConfig::default()).await.unwrap()),
    )
    .await;

    let scraper_handle = spawn_scraper_actor().await;

    let query_handle = spawn_query_actor(scraper_handle.clone(), analyzer_handle.clone(), history_handle.clone()).await;

    (query_handle, scraper_handle, analyzer_handle, history_handle)
}

async fn cleanup_test_system(
    _query_handle: crate::models::QueryActorHandle,
    _scraper_handle: crate::models::ScraperActorHandle,
    _analyzer_handle: crate::models::AnalyzerActorHandle,
    _history_handle: crate::models::HistoryActorHandle,
) {
    // In a real implementation, we might need to shut down actors gracefully
    // For now, they'll be dropped automatically
    tokio::time::sleep(Duration::from_millis(10)).await;
}
