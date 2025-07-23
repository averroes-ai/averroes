#![allow(clippy::self_named_module_files)]

use std::hint::black_box;

use criterion::Criterion;

// Mock function for benchmarking
fn parse_market_data(data: &str) -> String {
    // Simulate some processing
    let processed = data
        .lines()
        .filter(|line| !line.is_empty())
        .map(|line| line.trim())
        .collect::<Vec<&str>>()
        .join(",");

    format!("processed: {processed}")
}

pub fn benchmark_market_data(c: &mut Criterion) {
    let data = "BTC,50000\nETH,3000\nSOL,100\n";

    c.bench_function("parse_market_data", |b| b.iter(|| parse_market_data(black_box(data))));
}

criterion::criterion_group!(benches, benchmark_market_data);
criterion::criterion_main!(benches);
