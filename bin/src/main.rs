use package_core::process_tick;

fn main() {
    let mut sum = 0u64;
    for i in 0..1_000_000 {
        sum += process_tick(i);
    }
    println!("Sum: {sum}");
}
