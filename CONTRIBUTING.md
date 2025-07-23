# Contributing to Fiqh Advisor

Thank you for your interest in contributing to Fiqh Advisor! This document provides comprehensive guidelines for contributors, from first-time setup to advanced development workflows.

## 📋 Table of Contents

- [Getting Started](#getting-started)
- [Development Environment Setup](#development-environment-setup)
- [Project Architecture](#project-architecture)
- [Development Workflow](#development-workflow)
- [Code Standards](#code-standards)
- [Testing Guidelines](#testing-guidelines)
- [Submitting Changes](#submitting-changes)
- [Troubleshooting](#troubleshooting)

## 🚀 Getting Started

### Prerequisites Checklist

Before you begin, ensure you have all required tools installed:

#### Required Tools

- [ ] **Rust 1.80+** - Install via [rustup.rs](https://rustup.rs/)
- [ ] **Android Studio** - Latest stable version
- [ ] **Android SDK** - API level 24+ (Android 7.0+)
- [ ] **Android NDK** - Version 26.1.10909125
- [ ] **Java JDK** - Version 17+ for Gradle
- [ ] **Git** - Latest version
- [ ] **Just** - Command runner (`cargo install just`)

#### Platform-Specific Tools

**Windows**

```powershell
# Install Rust
winget install Rustlang.Rustup

# Install Just
cargo install just

# Install ktlint
scoop install ktlint
```

**macOS**

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Install Just and ktlint
brew install just ktlint
```

**Linux (Ubuntu/Debian)**

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Install Just
cargo install just

# Install ktlint
curl -sSLO https://github.com/pinterest/ktlint/releases/latest/download/ktlint && chmod a+x ktlint && sudo mv ktlint /usr/local/bin/
```

### Initial Setup

1. **Fork and Clone**

   ```bash
   # Fork the repository on GitHub first, then:
   git clone https://github.com/YOUR_USERNAME/fiqhadvisor.git
   cd fiqhadvisor

   # Add upstream remote
   git remote add upstream https://github.com/rizilab/fiqhadvisor.git
   ```

2. **Configure Rust**

   ```bash
   # Install Android targets
   rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android

   # Install cargo-ndk
   cargo install cargo-ndk

   # Verify installation
   rustc --version
   cargo --version
   ```

3. **Setup Android Environment**

   ```bash
   # Set environment variables (add to your shell profile)
   export ANDROID_HOME=$HOME/Android/Sdk  # or /c/Users/%USERNAME%/AppData/Local/Android/Sdk on Windows
   export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/26.1.10909125
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```

4. **Configure Environment Variables**

   ```bash
   # Create .env file
   cp .env.example .env

   # Edit .env with your configuration
   # SOLANA_RPC_URL=https://api.mainnet-beta.solana.com
   # DATABASE_PATH=./fiqh_advisor.db
   # AI_MODEL_ENDPOINT=your_ai_endpoint
   ```

5. **Verify Setup**

   ```bash
   # Test Rust compilation
   just build-rust-debug

   # Test Android build (requires Android device/emulator)
   just mobile-dev
   ```

## 🏗 Development Environment Setup

### IDE Configuration

#### Visual Studio Code (Recommended)

Install these extensions:

- `rust-analyzer` - Rust language support
- `Kotlin Language` - Kotlin support
- `Android iOS Emulator` - Device emulation
- `GitLens` - Git integration
- `Error Lens` - Inline error display

```json
// .vscode/settings.json
{
  "rust-analyzer.cargo.features": "all",
  "rust-analyzer.checkOnSave.command": "clippy",
  "kotlin.languageServer.enabled": true,
  "files.associations": {
    "*.udl": "rust"
  }
}
```

#### Android Studio

- Install Rust plugin
- Configure Kotlin formatting to use ktlint
- Setup Android SDK and NDK paths

### Environment Variables

Create a comprehensive `.env` file:

```bash
# Solana Configuration
SOLANA_RPC_URL=https://api.mainnet-beta.solana.com
SOLANA_DEVNET_RPC_URL=https://api.devnet.solana.com

# Database
DATABASE_PATH=./fiqh_advisor.db
DATABASE_URL=sqlite:./fiqh_advisor.db

# AI Configuration
AI_MODEL_ENDPOINT=https://api.openai.com/v1
OPENAI_API_KEY=your_api_key_here
EMBEDDING_MODEL=text-embedding-ada-002

# Development
RUST_LOG=debug
RUST_BACKTRACE=1
```

## 🏛 Project Architecture

### Codebase Overview

```
fiqhadvisor/
├── android/                    # Android application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/rizilab/fiqhadvisor/
│   │   │   │   ├── MainActivity.kt        # Main entry point
│   │   │   │   ├── ui/                    # Compose UI components
│   │   │   │   │   ├── components/        # Reusable UI components
│   │   │   │   │   ├── screens/           # Screen composables
│   │   │   │   │   └── theme/             # Material3 theming
│   │   │   │   └── viewmodel/             # ViewModels
│   │   │   └── res/                       # Android resources
│   │   └── build.gradle.kts
│   ├── core/                              # UniFFI bindings module
│   │   ├── src/main/java/com/rizilab/fiqhadvisor/core/
│   │   │   └── FiqhAIManager.kt          # Kotlin wrapper for Rust
│   │   └── build.gradle.kts              # UniFFI build configuration
│   └── build.gradle.kts                  # Root Android build
├── crates/                               # Rust workspace
│   ├── core/                            # Main Rust library
│   │   ├── src/
│   │   │   ├── lib.rs                   # UniFFI exports
│   │   │   ├── actors/                  # Actor system
│   │   │   │   ├── query_actor.rs
│   │   │   │   ├── history_actor.rs
│   │   │   │   ├── analyzer_actor.rs
│   │   │   │   └── scraper_actor.rs
│   │   │   ├── ai/                      # AI modules
│   │   │   │   ├── models.rs
│   │   │   │   ├── embeddings.rs
│   │   │   │   └── chains.rs
│   │   │   └── api/                     # API handlers
│   │   └── Cargo.toml
│   ├── desktop/                         # Future: Tauri desktop app
│   └── web/                            # Future: Web interface
├── docs/                               # Documentation
├── docker/                             # Container configurations
├── scripts/                            # Build and utility scripts
├── justfile                            # Development commands
├── Cargo.toml                          # Rust workspace
└── README.md
```

### Key Components

#### Rust Core (`crates/core/`)

- **Actor System**: Concurrent message-passing architecture
- **UniFFI Integration**: Cross-platform bindings generation
- **AI Modules**: LLM chains and embeddings for Islamic analysis
- **Solana Integration**: Blockchain interaction and token analysis

#### Android App (`android/`)

- **Jetpack Compose UI**: Modern declarative UI framework
- **Material3 Design**: Google's latest design system
- **UniFFI Bindings**: Native Rust integration via generated Kotlin code
- **Coroutines**: Async/await for Rust interop

#### Build System

- **Cargo**: Rust package manager and build tool
- **Gradle**: Android build system with Kotlin DSL
- **cargo-ndk**: Rust to Android cross-compilation
- **UniFFI**: Automatic binding generation

## 🔄 Development Workflow

### Daily Development

1. **Start Your Day**

   ```bash
   # Sync with upstream
   git fetch upstream
   git checkout main
   git merge upstream/main

   # Create feature branch
   git checkout -b feature/your-feature-name
   ```

2. **Make Changes**

   ```bash
   # Work on your feature...

   # Frequent testing
   just test                    # Run Rust tests
   just lint                   # Check code quality
   just mobile-dev             # Test on Android
   ```

3. **Pre-commit Checks**

   ```bash
   # Full verification before committing
   just test && just lint && just build-android-debug

   # Commit your changes
   git add .
   git commit -m "feat: add new feature"
   ```

### Common Development Tasks

#### Adding New Rust Functions

1. **Add to Rust Core**

   ```rust
   // crates/core/src/lib.rs
   #[uniffi::export]
   impl FiqhAISystem {
       pub async fn new_function(&self, param: String) -> Result<String, FiqhAIError> {
           // Implementation
           Ok("result".to_string())
       }
   }
   ```

2. **Regenerate Bindings**

   ```bash
   just generate-bindings
   ```

3. **Update Kotlin Wrapper**

   ```kotlin
   // android/core/src/main/java/com/rizilab/fiqhadvisor/core/FiqhAIManager.kt
   suspend fun newFunction(param: String): String {
       return fiqhSystem?.newFunction(param) ?: throw IllegalStateException("System not initialized")
   }
   ```

4. **Test Integration**
   ```bash
   just mobile-dev
   ```

#### Adding Android UI Components

1. **Create Composable**

   ```kotlin
   // android/app/src/main/java/com/rizilab/fiqhadvisor/ui/components/NewComponent.kt
   @Composable
   fun NewComponent(
       modifier: Modifier = Modifier
   ) {
       // Implementation
   }
   ```

2. **Follow Material3 Guidelines**

   - Use theme colors and typography
   - Follow accessibility guidelines
   - Support dark/light themes

3. **Test UI**
   ```bash
   just mobile-dev
   # Test on different screen sizes and orientations
   ```

#### Working with Actors

The actor system is central to our architecture. Here's how to work with it:

1. **Understanding Actors**

   ```rust
   // Each actor has:
   // - Message types (enum)
   // - Actor struct with state
   // - Message handler
   // - Spawn function returning handle
   ```

2. **Adding New Messages**

   ```rust
   // crates/core/src/actors/query_actor.rs
   #[derive(Debug)]
   pub enum QueryMessage {
       ExistingMessage(String),
       NewMessage { param: String, response: oneshot::Sender<Result<String, ActorError>> },
   }
   ```

3. **Handle Messages**
   ```rust
   async fn handle_message(&mut self, msg: QueryMessage) {
       match msg {
           QueryMessage::NewMessage { param, response } => {
               let result = self.process_new_message(param).await;
               let _ = response.send(result);
           }
           // ... other cases
       }
   }
   ```

## 📏 Code Standards

### Rust Code Style

#### General Guidelines

- Follow the [Rust API Guidelines](https://rust-lang.github.io/api-guidelines/)
- Use `cargo fmt` for formatting (enforced in CI)
- Pass `cargo clippy` without warnings
- Write documentation for public APIs

#### Example Rust Code

````rust
/// Analyzes a Solana token for Islamic compliance
///
/// # Arguments
/// * `contract_address` - The token's contract address
/// * `analysis_type` - Type of analysis to perform
///
/// # Returns
/// * `Result<AnalysisResult, FiqhAIError>` - Analysis result or error
///
/// # Example
/// ```rust
/// let result = system.analyze_token(
///     "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v".to_string(),
///     AnalysisType::IslamicCompliance
/// ).await?;
/// ```
#[uniffi::export]
impl FiqhAISystem {
    pub async fn analyze_token(
        &self,
        contract_address: String,
        analysis_type: AnalysisType,
    ) -> Result<AnalysisResult, FiqhAIError> {
        // Implementation...
        todo!()
    }
}
````

#### Error Handling

```rust
// Use custom error types
#[derive(uniffi::Error, thiserror::Error, Debug)]
pub enum FiqhAIError {
    #[error("Network error: {message}")]
    NetworkError { message: String },

    #[error("Analysis failed: {reason}")]
    AnalysisError { reason: String },
}

// Proper error propagation
pub async fn risky_operation() -> Result<String, FiqhAIError> {
    let data = fetch_data().await
        .map_err(|e| FiqhAIError::NetworkError {
            message: e.to_string()
        })?;

    Ok(data)
}
```

### Kotlin Code Style

#### ktlint Configuration

We use ktlint for Kotlin formatting. Configuration in `.editorconfig`:

```ini
# .editorconfig
[*.{kt,kts}]
indent_size = 4
continuation_indent_size = 4
max_line_length = 120
insert_final_newline = true
```

#### Example Kotlin Code

```kotlin
/**
 * Manages the FiqhAI system integration for Android
 */
class FiqhAIManager {
    private var fiqhSystem: FiqhAiSystem? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Initializes the FiqhAI system
     * @param context Android context for initialization
     */
    suspend fun initialize(context: Context) {
        try {
            val config = createMobileConfig(
                databasePath = context.getDatabasePath("fiqh.db").absolutePath,
                solanaRpcUrl = "https://api.mainnet-beta.solana.com"
            )

            fiqhSystem = FiqhAiSystem(config)
        } catch (e: Exception) {
            Log.e("FiqhAIManager", "Initialization failed", e)
            throw e
        }
    }
}
```

#### Compose Guidelines

```kotlin
@Composable
fun TokenAnalysisScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TokenAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Component implementation
    }
}
```

### Git Commit Guidelines

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

#### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

#### Examples

```
feat(android): add token analysis screen
fix(rust): resolve memory leak in history actor
docs: update contribution guidelines
style(kotlin): apply ktlint formatting
refactor(rust): simplify actor message handling
```

## 🧪 Testing Guidelines

### Rust Testing

#### Unit Tests

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use tokio::test;

    #[tokio::test]
    async fn test_token_analysis() {
        let config = FiqhAIConfig::default();
        let system = FiqhAISystem::new(config).await.unwrap();

        let result = system.analyze_token(
            "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v".to_string(),
            AnalysisType::IslamicCompliance
        ).await;

        assert!(result.is_ok());
    }
}
```

#### Integration Tests

```rust
// tests/integration_test.rs
use fiqh_core::*;

#[tokio::test]
async fn test_full_analysis_workflow() {
    // Test complete workflow from query to response
}
```

### Android Testing

#### Unit Tests

```kotlin
@RunWith(JUnit4::class)
class FiqhAIManagerTest {
    @Test
    fun testInitialization() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = FiqhAIManager()

        manager.initialize(context)
        assertTrue(manager.isInitialized())
    }
}
```

#### UI Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class TokenAnalysisScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTokenAnalysisDisplay() {
        composeTestRule.setContent {
            TokenAnalysisScreen(onNavigateUp = {})
        }

        composeTestRule
            .onNodeWithText("Token Analysis")
            .assertIsDisplayed()
    }
}
```

### Running Tests

```bash
# Run all Rust tests
just test

# Run specific test
cargo test test_token_analysis

# Run Android unit tests
just test-android

# Run with coverage
cargo tarpaulin --out html
```

## 📤 Submitting Changes

### Pull Request Process

1. **Prepare Your Branch**

   ```bash
   # Ensure you're up to date
   git fetch upstream
   git rebase upstream/main

   # Run full test suite
   just test && just lint
   ```

2. **Create Pull Request**

   - Use clear, descriptive title
   - Reference any related issues
   - Include screenshots for UI changes
   - Add tests for new functionality

3. **PR Template**

   ```markdown
   ## Description

   Brief description of changes

   ## Type of Change

   - [ ] Bug fix
   - [ ] New feature
   - [ ] Breaking change
   - [ ] Documentation update

   ## Testing

   - [ ] Unit tests pass
   - [ ] Integration tests pass
   - [ ] Manual testing completed

   ## Screenshots (if applicable)

   ## Checklist

   - [ ] Code follows style guidelines
   - [ ] Self-review completed
   - [ ] Documentation updated
   ```

### Code Review Process

Your PR will be reviewed for:

- **Functionality**: Does it work as intended?
- **Code Quality**: Follows standards and best practices?
- **Testing**: Adequate test coverage?
- **Documentation**: Clear and up-to-date?
- **Security**: No security vulnerabilities?

## 🛠 Troubleshooting

### Common Issues

#### Build Issues

**"cargo-ndk not found"**

```bash
# Install cargo-ndk
cargo install cargo-ndk

# Verify installation
which cargo-ndk
```

**"Android NDK not found"**

```bash
# Set NDK path
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/26.1.10909125

# Verify
ls $ANDROID_NDK_HOME
```

**"Rust target not found"**

```bash
# Install missing targets
rustup target add aarch64-linux-android armv7-linux-androideabi
```

#### UniFFI Issues

**"Binding generation failed"**

```bash
# Clean and regenerate
just clean
just generate-bindings
```

**"Unresolved reference in Kotlin"**

- Check if Rust function is marked with `#[uniffi::export]`
- Verify function signature matches generated bindings
- Regenerate bindings after Rust changes

#### Runtime Issues

**"Actor spawn failed"**

- Check tokio runtime is available
- Verify database permissions
- Check network connectivity for RPC calls

**"Kotlin coroutine errors"**

- Ensure proper scope management
- Use appropriate dispatchers
- Handle cancellation correctly

### Getting Help

1. **Check Existing Issues**: [GitHub Issues](https://github.com/rizilab/fiqhadvisor/issues)
2. **Search Documentation**: README.md and inline docs
3. **Ask Questions**: [GitHub Discussions](https://github.com/rizilab/fiqhadvisor/discussions)
4. **Join Community**: Discord/Telegram links in main README

### Debug Tips

#### Rust Debugging

```bash
# Enable debug logging
export RUST_log=debug
export RUST_BACKTRACE=1

# Run with debugging
just mobile-dev
```

#### Android Debugging

```bash
# View logs
adb logcat | grep FiqhAdvisor

# Debug specific component
adb logcat | grep FiqhAIManager
```

---

Thank you for contributing to Fiqh Advisor! Your efforts help build better tools for the Muslim tech community. 🚀
