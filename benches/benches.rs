use criterion::{black_box, criterion_group, criterion_main, Criterion};

fn parse_market_data(data: &[u8]) -> u64 {
    // Mock low-latency parser (zero-copy style)
    let mut sum = 0;
    for &byte in data {
        sum += byte as u64;
    }
    sum
}

fn benchmark_parser(c: &mut Criterion) {
    let data = vec![42u8; 1024];  // Simulate 1KB market packet
    c.bench_function("parse_market_data", |b| b.iter(|| parse_market_data(black_box(&data))));
}

criterion_group!(benches, benchmark_parser);
criterion_main!(benches);