# Fiqh Advisor - Your Sharia Trading Assistance

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/rizilab/fiqhadvisor)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Rust](https://img.shields.io/badge/rust-1.80+-orange.svg)](https://www.rust-lang.org)
[![Android](https://img.shields.io/badge/android-api%2024+-green.svg)](https://developer.android.com)

A cross-platform Islamic finance and cryptocurrency analysis tool that helps Muslims make Sharia-compliant trading decisions. Built with Rust core and UniFFI bindings for Android, with plans for iOS and desktop support.

## 🌟 Features

### Core Capabilities

- **🤖 AI-Powered Analysis**: Advanced Islamic jurisprudence (Fiqh) analysis for cryptocurrency tokens
- **🔗 Solana Integration**: Direct blockchain analysis and token information retrieval
- **🎙️ Audio Processing**: Voice input for queries and analysis requests
- **💬 Chatbot Interface**: Interactive Islamic finance guidance
- **📊 Historical Tracking**: Analysis history and user statistics
- **🔄 Real-time Updates**: Automated scraping and analysis updates
- **📱 Cross-Platform**: Android app with planned iOS and desktop support

### Islamic Finance Features

- Token Halal/Haram analysis based on Islamic principles
- Sharia-compliant trading recommendations
- Islamic principle explanations and references
- Backtesting against Islamic finance criteria
- Community-driven analysis validation

## 🏗️ Architecture

### System Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Android App   │    │   iOS App (TBD)  │    │ Desktop (TBD)   │
│  (Jetpack       │    │     (Swift)      │    │     (Iced)      │
│   Compose)      │    │                  │    │                 │
└─────────┬───────┘    └────────┬─────────┘    └─────────┬───────┘
          │                     │                        │
          └─────────────────────┼────────────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │     UniFFI Layer      │
                    │   (Cross-platform     │
                    │     bindings)         │
                    └───────────┬───────────┘
                                │
                    ┌───────────▼───────────┐
                    │      Rust Core        │
                    │                       │
                    │  ┌─────────────────┐  │
                    │  │  Actor System   │  │
                    │  │                 │  │
                    │  │ • Query Actor   │  │
                    │  │ • History Actor │  │
                    │  │ • Analyzer      │  │
                    │  │ • Scraper       │  │
                    │  └─────────────────┘  │
                    │                       │
                    │  ┌─────────────────┐  │
                    │  │   AI Modules    │  │
                    │  │                 │  │
                    │  │ • LLM Chains    │  │
                    │  │ • Embeddings    │  │
                    │  │ • Fiqh Analysis │  │
                    │  └─────────────────┘  │
                    └───────────┬───────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
┌─────────▼────────┐  ┌─────────▼────────┐  ┌─────────▼────────┐
│   Solana RPC     │  │  Vector Database │  │   External APIs  │
│   (Blockchain)   │  │   (Embeddings)   │  │  (Price feeds,   │
│                  │  │                  │  │   News, etc.)    │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

### Actor-Based Architecture

The Rust core uses an actor-based architecture for concurrent operations:

- **Query Actor**: Handles user queries and coordinates responses
- **History Actor**: Manages analysis history and user statistics
- **Analyzer Actor**: Performs Solana token analysis and Fiqh evaluation
- **Scraper Actor**: Automated data collection from various sources

### Technology Stack

**Core (Rust)**

- `tokio` - Async runtime
- `uniffi` - Cross-platform bindings generation
- `serde` - Serialization/deserialization
- `sqlx` - Database operations
- `reqwest` - HTTP client
- `solana-sdk` - Solana blockchain integration

**Android**

- Kotlin with Coroutines
- Jetpack Compose UI
- Material3 Design
- Android NDK for native library integration

**Build System**

- `cargo-ndk` - Rust to Android cross-compilation
- Gradle with Kotlin DSL
- `just` - Command runner for development workflows

## 🚀 Quick Start

### Prerequisites

- **Rust**: 1.80+ with `cargo` and `rustup`
- **Android Studio**: Latest stable version
- **Android SDK**: API level 24+ (Android 7.0+)
- **Android NDK**: 26.1.10909125
- **Java**: JDK 17+ for Gradle
- **Just**: Command runner (`cargo install just`)
- **ktlint**: Kotlin linter (`brew install ktlint` or download binary)

### Development Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/rizilab/fiqhadvisor.git
   cd fiqhadvisor
   ```

2. **Set up Rust toolchain**

   ```bash
   # Install Rust targets for Android
   rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android

   # Install cargo-ndk for cross-compilation
   cargo install cargo-ndk
   ```

3. **Configure environment**

   ```bash
   # Copy and configure environment
   cp .env.example .env
   # Edit .env with your configuration
   ```

4. **Build and run**

   ```bash
   # Development build and install
   just mobile-dev

   # Or step by step:
   just build-android-debug
   just install-debug
   ```

### Available Commands

```bash
# Development
just mobile-dev          # Build and install debug version
just mobile-release      # Build and install release version

# Building
just build-android-debug    # Build Android debug APK
just build-android-release  # Build Android release APK
just build-rust-debug      # Build Rust library only
just build-rust-release    # Build Rust library (optimized)

# Testing
just test                  # Run Rust tests
just test-android         # Run Android instrumentation tests
just lint                 # Run all linters
just lint-kotlin          # Run ktlint on Kotlin code
just fix-kotlin           # Auto-fix Kotlin formatting

# Utilities
just clean                # Clean all build artifacts
just generate-bindings    # Generate UniFFI bindings only
```

## 📱 Project Structure

```
fiqhadvisor/
├── android/                    # Android application
│   ├── app/                   # Main Android app module
│   │   └── src/main/java/com/rizilab/fiqhadvisor/
│   │       ├── MainActivity.kt
│   │       └── ui/            # Compose UI components
│   ├── core/                  # Core Android module (UniFFI bindings)
│   │   ├── build.gradle.kts   # Gradle build with UniFFI integration
│   │   └── src/main/java/com/rizilab/fiqhadvisor/core/
│   │       └── FiqhAIManager.kt  # Kotlin wrapper for Rust core
│   └── build.gradle.kts       # Root Android build configuration
├── crates/                    # Rust workspace
│   ├── core/                  # Main Rust library
│   │   ├── src/
│   │   │   ├── lib.rs        # UniFFI exports and main API
│   │   │   ├── actors/       # Actor system implementation
│   │   │   ├── ai/           # AI and ML modules
│   │   │   └── api/          # HTTP API handlers
│   │   └── Cargo.toml
│   ├── desktop/              # Desktop application (Tauri)
│   └── web/                  # Web interface
├── docs/                     # Documentation
│   └── features/            # Feature specifications (Gherkin)
├── docker/                  # Docker configurations
├── justfile                 # Development command runner
├── Cargo.toml              # Rust workspace configuration
└── README.md               # This file
```

## 🔧 Development Workflow

### Making Changes

1. **Rust Core Changes**

   ```bash
   # Make changes to crates/core/src/
   just test                    # Run tests
   just build-rust-debug       # Build Rust library
   just generate-bindings      # Regenerate UniFFI bindings
   just mobile-dev            # Test on Android
   ```

2. **Android UI Changes**

   ```bash
   # Make changes to android/app/src/
   just mobile-dev           # Build and install
   ```

3. **Adding New UniFFI Functions**
   ```bash
   # 1. Add function to crates/core/src/lib.rs with #[uniffi::export]
   # 2. Regenerate bindings
   just generate-bindings
   # 3. Update Kotlin wrapper in android/core/src/.../FiqhAIManager.kt
   # 4. Test the integration
   just mobile-dev
   ```

### Code Quality

- **Rust**: `cargo fmt`, `cargo clippy`
- **Kotlin**: `ktlint` (enforced in CI)
- **Git hooks**: Run linters before commit
- **Documentation**: Required for public APIs

### Testing Strategy

- **Unit Tests**: Rust core functionality
- **Integration Tests**: Actor system communication
- **Android Tests**: UI and UniFFI integration
- **Manual Testing**: Real device testing for blockchain operations

## 🌐 API Reference

### Core Rust API

```rust
// Initialize the system
let config = FiqhAIConfig {
    database_path: "path/to/db".to_string(),
    solana_rpc_url: "https://api.mainnet-beta.solana.com".to_string(),
    ai_model_config: Default::default(),
};

let system = FiqhAISystem::new(config).await?;

// Analyze a token
let query = Query::TokenAnalysis {
    contract_address: "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v".to_string(),
    analysis_type: AnalysisType::IslamicCompliance,
};

let response = system.process_query(query).await?;
```

### Android Kotlin API

```kotlin
// Initialize through FiqhAIManager
class MainActivity : ComponentActivity() {
    private val fiqhManager = FiqhAIManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            fiqhManager.initialize(this@MainActivity)

            // Use the system
            val response = fiqhManager.analyzeToken(
                contractAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                analysisType = AnalysisType.ISLAMIC_COMPLIANCE
            )
        }
    }
}
```

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

### Quick Contributing Guide

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes following our coding standards
4. Run tests and linters (`just test && just lint`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Islamic scholars who provided Fiqh guidance
- Solana developer community
- UniFFI project for cross-platform bindings
- Rust and Android development communities

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/rizilab/fiqhadvisor/issues)
- **Discussions**: [GitHub Discussions](https://github.com/rizilab/fiqhadvisor/discussions)
- **Email**: support@rizilab.com

---

Built with ❤️ for the Muslim tech community
