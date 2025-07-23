use std::hint::black_box;
use std::time::Duration;

use criterion::BenchmarkId;
use criterion::Criterion;
use criterion::Throughput;
use criterion::criterion_group;
use criterion::criterion_main;
use fiqh_core::ai::chains::IslamicAnalysisChain;
use fiqh_core::ai::chains::IslamicChainConfig;
use fiqh_core::models::Query;
use fiqh_core::models::ScrapedData;
use fiqh_core::models::ScrapedDataType;

/// Benchmark token analysis performance
pub fn bench_token_analysis_simple(c: &mut Criterion) {
    let rt = tokio::runtime::Runtime::new().unwrap();

    let mut group = c.benchmark_group("token_analysis");
    group.throughput(Throughput::Elements(1));

    // Create mock query
    let query = Query::new_token_ticker("SOL".to_owned(), Some("test_user".to_owned()), None);

    // Create test scraped data
    let scraped_data = vec![ScrapedData::new(
        "example.com".to_owned(),
        "SOL is the native token of Solana blockchain".to_owned(),
        ScrapedDataType::CryptoHalalVerification,
        Some("SOL Token Info".to_owned()),
    )];

    group.bench_function("analyze_token_simple", |b| {
        b.iter(|| {
            rt.block_on(async {
                let config = IslamicChainConfig {
                    model_name: "gpt-3.5-turbo".to_owned(),
                    temperature: 0.7,
                    max_tokens: 500,
                    system_prompt: "You are an Islamic finance expert.".to_owned(),
                    embedding_model: "text-embedding-ada-002".to_owned(),
                };

                let chain = IslamicAnalysisChain::new(config).await.unwrap();

                let _result = chain
                    .analyze_token(
                        black_box(&query),
                        black_box(None), // No token info for benchmark
                        black_box(&scraped_data),
                    )
                    .await;
            });
        });
    });

    group.finish();
}

/// Benchmark with complex analysis
pub fn bench_token_analysis_complex(c: &mut Criterion) {
    let rt = tokio::runtime::Runtime::new().unwrap();

    let mut group = c.benchmark_group("token_analysis_complex");
    group.throughput(Throughput::Elements(1));

    // Create mock query
    let query = Query::new_contract_address("0x1234567890abcdef".to_owned(), Some("test_user".to_owned()), None);

    // Create more complex scraped data
    let scraped_data = create_complex_scraped_data();

    for &num_sources in [1, 5, 10].iter() {
        group.bench_with_input(BenchmarkId::new("complex_analysis", num_sources), &num_sources, |b, &num_sources| {
            b.iter(|| {
                rt.block_on(async {
                    let config = IslamicChainConfig {
                        model_name: "gpt-4".to_owned(),
                        temperature: 0.3,
                        max_tokens: 1000,
                        system_prompt: "You are an Islamic finance scholar with expertise in cryptocurrency analysis."
                            .to_owned(),
                        embedding_model: "text-embedding-ada-002".to_owned(),
                    };

                    let chain = IslamicAnalysisChain::new(config).await.unwrap();
                    let limited_data: Vec<_> = scraped_data.iter().take(num_sources).cloned().collect();

                    let _result = chain
                        .analyze_token(black_box(&query), black_box(None), black_box(&limited_data))
                        .await;
                });
            });
        });
    }

    group.finish();
}

/// Benchmark concurrent analysis requests
pub fn bench_concurrent_analysis(c: &mut Criterion) {
    let rt = tokio::runtime::Runtime::new().unwrap();

    let mut group = c.benchmark_group("concurrent_analysis");

    let query = Query::new_token_ticker("ETH".to_owned(), Some("test_user".to_owned()), None);
    let scraped_data = create_complex_scraped_data();

    for &concurrency in [1, 2, 4].iter() {
        group.bench_with_input(BenchmarkId::new("concurrent", concurrency), &concurrency, |b, &concurrency| {
            b.iter(|| {
                rt.block_on(async {
                    let config = IslamicChainConfig {
                        model_name: "gpt-3.5-turbo".to_owned(),
                        temperature: 0.5,
                        max_tokens: 750,
                        system_prompt: "You are an expert in Islamic finance.".to_owned(),
                        embedding_model: "text-embedding-ada-002".to_owned(),
                    };

                    let futures: Vec<_> = (0..concurrency)
                        .map(|_| async {
                            let chain = IslamicAnalysisChain::new(config.clone()).await.unwrap();
                            chain.analyze_token(&query, None, &scraped_data).await
                        })
                        .collect();

                    let _results = futures::future::join_all(black_box(futures)).await;
                });
            });
        });
    }

    group.finish();
}

/// Benchmark analysis performance under time pressure
pub fn bench_time_constrained_analysis(c: &mut Criterion) {
    let rt = tokio::runtime::Runtime::new().unwrap();

    let query = Query::new_token_ticker("BTC".to_owned(), Some("test_user".to_owned()), None);
    let scraped_data = create_complex_scraped_data();

    c.bench_function("time_constrained_analysis", |b| {
        b.iter(|| {
            rt.block_on(async {
                let config = IslamicChainConfig {
                    model_name: "gpt-3.5-turbo".to_owned(),
                    temperature: 0.2,
                    max_tokens: 300,
                    system_prompt: "Provide quick Islamic finance analysis.".to_owned(),
                    embedding_model: "text-embedding-ada-002".to_owned(),
                };

                let chain = IslamicAnalysisChain::new(config).await.unwrap();

                // Use tokio::time::timeout for time constraint simulation
                let _result = tokio::time::timeout(
                    Duration::from_secs(5),
                    chain.analyze_token(black_box(&query), black_box(None), black_box(&scraped_data)),
                )
                .await;
            });
        });
    });
}

/// Create complex scraped data for benchmarking
fn create_complex_scraped_data() -> Vec<ScrapedData> {
    vec![
        ScrapedData::new(
            "tokeninfo.com".to_owned(),
            "Detailed token information about blockchain project".to_owned(),
            ScrapedDataType::CryptoHalalVerification,
            Some("Token Info".to_owned()),
        ),
        ScrapedData::new(
            "islamicfinance.org".to_owned(),
            "Islamic ruling on blockchain technology and cryptocurrencies".to_owned(),
            ScrapedDataType::IslamicFinanceContent,
            Some("Blockchain Fatwa".to_owned()),
        ),
        ScrapedData::new(
            "scholar.edu".to_owned(),
            "Scholar opinion on the permissibility of this token".to_owned(),
            ScrapedDataType::IslamicFinanceContent,
            Some("Scholar Opinion".to_owned()),
        ),
        ScrapedData::new(
            "project.io".to_owned(),
            "Technical whitepaper describing the project goals".to_owned(),
            ScrapedDataType::Documentation,
            Some("Whitepaper".to_owned()),
        ),
    ]
}

// Criterion group and main setup
criterion_group!(
    benches,
    bench_token_analysis_simple,
    bench_token_analysis_complex,
    bench_concurrent_analysis,
    bench_time_constrained_analysis
);
criterion_main!(benches);
