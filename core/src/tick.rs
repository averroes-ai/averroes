pub fn process_tick(tick: u64) -> u64 {
    // Mock low-latency computation
    tick * 2 + 1
}

pub fn parse_market_data(data: &[u8]) -> u64 {
    data.iter().map(|&b| b as u64).sum()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_market_data() {
        let data = [1u8, 2, 3];
        assert_eq!(parse_market_data(&data), 6);
    }
}