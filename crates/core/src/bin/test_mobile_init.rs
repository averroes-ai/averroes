use fiqh_core::FiqhAISystem;
use fiqh_core::create_mobile_config;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("🔧 Testing Mobile AI System Initialization...");

    // Test mobile config creation
    let config = create_mobile_config(
        Some("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".to_owned()),
        Some("xai-udKDRvvyrDuAYyrfye8thCLUgiKCNveOyYQrHjHnHCJ5pWUWH9TtfEQ73OI7Poh5b0UJZJvAPhYCKFEE".to_owned()),
        None,
        false,
    );

    println!("✓ Mobile config created successfully");

    // Test AI system initialization
    match FiqhAISystem::new(config).await {
        Ok(_system) => {
            println!("✅ SUCCESS: AI system initialized without database!");
            println!("✅ No more 'Permission denied' errors!");
            println!("✅ Ready for real AI chat functionality!");
        },
        Err(e) => {
            println!("❌ AI system initialization failed: {e}");
            return Err(e.into());
        },
    }

    Ok(())
}
