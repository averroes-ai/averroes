# FiqhAI Development Commands
# Run `just --list` to see all available commands

# Default command - show help
default:
    @just --list

# ============================================================================
# BENCHMARKS
# ============================================================================

# Run all performance benchmarks
bench:
    @echo "🚀 Running all FiqhAI performance benchmarks..."
    cargo bench --all

# Run only Islamic analysis benchmarks (targeting <500ms)
bench-analysis:
    @echo "🕌 Running Islamic analysis benchmarks..."
    cargo bench fiqh_analysis_benches

# Run specific benchmark by name
bench-specific name:
    @echo "🎯 Running benchmark: {{name}}"
    cargo bench {{name}}

# Run benchmarks with detailed output and save results
bench-detailed:
    @echo "📊 Running detailed benchmarks with saved results..."
    mkdir -p target/criterion
    cargo bench --all -- --output-format html --plotting-backend plotters

# Benchmark performance regression test (500ms target)
bench-perf-target:
    @echo "⏱️ Running 500ms performance target benchmark..."
    cargo bench benchmark_performance_target

# Run concurrent analysis benchmark
bench-concurrent:
    @echo "🔄 Running concurrent analysis benchmark..."
    cargo bench benchmark_concurrent_analysis

# ============================================================================
# TESTING
# ============================================================================

# Run all tests
test:
    @echo "🧪 Running all tests..."
    cargo test --all

# Run tests with output
test-verbose:
    @echo "🧪 Running all tests with verbose output..."
    cargo test --all -- --nocapture

# Run only Gherkin scenario tests
test-gherkin:
    @echo "🥒 Running Gherkin scenario tests..."
    cargo test --lib gherkin_scenarios

# Run AI chain tests
test-ai:
    @echo "🤖 Running AI chain tests..."
    cargo test --lib ai::

# Run actor tests
test-actors:
    @echo "🎭 Running actor system tests..."
    cargo test --lib actors::

# Run API endpoint tests
test-api:
    @echo "🌐 Running API endpoint tests..."
    cargo test --lib api::

# Run tests with coverage
test-coverage:
    @echo "📋 Running tests with coverage..."
    cargo tarpaulin --all --out html --output-dir target/tarpaulin

# ============================================================================
# BUILD COMMANDS
# ============================================================================

# Build all crates in development mode
build:
    @echo "🔨 Building FiqhAI (development)..."
    cargo build --all

# Build all crates in release mode
build-release:
    @echo "🚀 Building FiqhAI (release)..."
    cargo build --all --release

# Build core crate only
build-core:
    @echo "🔨 Building core crate..."
    cargo build -p core

# Build desktop app
build-desktop:
    @echo "🖥️ Building desktop application..."
    cargo build -p dekstop --release

# Build web service
build-web:
    @echo "🌐 Building web service..."
    cargo build -p web --release

# Build with all features enabled
build-full:
    @echo "🔨 Building with all features..."
    cargo build --all --all-features --release

# ============================================================================
# MOBILE BUILDS - UNIFFI INTEGRATION (Ferrostar Approach)
# ============================================================================

# Test Android build (Gradle-integrated approach)
test-android-build:
    @echo "🧪 Testing Android build (Gradle-integrated)..."
    @just install-cargo-ndk
    #!/usr/bin/env sh
    if [ "{{os()}}" = "windows" ]; then \
        cd android && ./gradlew.bat build; \
    else \
        cd android && ./gradlew build; \
    fi
    @echo "✅ Android build test passed!"

# Test Android Rust compilation only (manual approach for debugging)
test-android-rust:
    @echo "🦀 Testing Rust compilation for Android (manual)..."
    cd crates/core && cargo check --target aarch64-linux-android --features "vendored,mobile"
    @echo "✅ Android Rust compilation test passed!"

# Build Rust core for Android first (sequential)
build-rust-android:
    @echo "🦀 Building Rust core for Android targets..."
    cd crates/core && cargo build --target aarch64-linux-android --features "vendored,mobile"
    cd crates/core && cargo build --target armv7-linux-androideabi --features "vendored,mobile"
    cd crates/core && cargo build --target i686-linux-android --features "vendored,mobile"
    cd crates/core && cargo build --target x86_64-linux-android --features "vendored,mobile"
    @echo "✅ Rust core built for all Android targets!"

# Generate UniFFI bindings for Android
generate-uniffi-android:
    @echo "🔗 Generating UniFFI bindings for Android..."
    # Create the output directory for generated Kotlin bindings
    #!/usr/bin/env sh
    if [ "{{os()}}" = "windows" ]; then \
        mkdir android\\core\\src\\main\\java\\uniffi\\fiqh_core 2>nul || echo "Directory already exists"; \
    else \
        mkdir -p android/core/src/main/java/uniffi/fiqh_core/; \
    fi
    # Generate Kotlin bindings from the built library
    cd crates/core && cargo run --bin uniffi-bindgen generate --library ../../target/aarch64-linux-android/debug/libfiqh_core.so --language kotlin --out-dir ../../android/core/src/main/java/uniffi/fiqh_core/
    @echo "✅ UniFFI Kotlin bindings generated!"

# Build Android app with Rust core (Gradle handles everything - Ferrostar approach)
build-android:
    @echo "📱 Building Android app with Rust core (Gradle-integrated)..."
    #!/usr/bin/env sh
    if [ "{{os()}}" = "windows" ]; then \
        cd android && ./gradlew.bat assembleDebug; \
    else \
        cd android && ./gradlew assembleDebug; \
    fi

# Build Rust core for Android release
build-rust-android-release:
    @echo "🦀 Building Rust core for Android targets (release)..."
    cd crates/core && cargo build --target aarch64-linux-android --features "vendored,mobile" --release
    cd crates/core && cargo build --target armv7-linux-androideabi --features "vendored,mobile" --release
    cd crates/core && cargo build --target i686-linux-android --features "vendored,mobile" --release
    cd crates/core && cargo build --target x86_64-linux-android --features "vendored,mobile" --release
    @echo "✅ Rust core built for all Android targets (release)!"

# Generate UniFFI bindings for Android release
generate-uniffi-android-release:
    @echo "🔗 Generating UniFFI bindings for Android (release)..."
    #!/usr/bin/env sh
    if [ "{{os()}}" = "windows" ]; then \
        mkdir android\\core\\src\\main\\java\\uniffi\\fiqh_core 2>nul || echo "Directory already exists"; \
    else \
        mkdir -p android/core/src/main/java/uniffi/fiqh_core/; \
    fi
    cd crates/core && cargo run --bin uniffi-bindgen generate --library ../../target/aarch64-linux-android/release/libfiqh_core.so --language kotlin --out-dir ../../android/core/src/main/java/uniffi/fiqh_core/
    @echo "✅ UniFFI Kotlin bindings generated (release)!"

# Build Android release (Gradle handles everything - clean build)
build-android-release:
    @echo "🚀 Building Android release (Gradle-integrated)..."
    #!/usr/bin/env sh
    if [ "{{os()}}" = "windows" ]; then \
        cd android && ./gradlew.bat clean assembleRelease; \
    else \
        cd android && ./gradlew clean assembleRelease; \
    fi

# Build iOS app (requires macOS)
build-ios:
    @echo "🍎 Building iOS app with Rust core..."
    cd ios && xcodebuild -workspace FiqhAI.xcworkspace -scheme FiqhAI -destination "generic/platform=iOS" build

# Generate UniFFI bindings for Android (simpler approach - no separate crate needed)
uniffi-android:
    @echo "🔗 Generating UniFFI bindings for Android (core crate approach)..."
    cd crates/core && cargo run --bin uniffi-bindgen generate --library ../../android/core/src/main/jniLibs/arm64-v8a/libfiqhai_core.so --language kotlin --out-dir ../../android/core/src/main/java/generated/

# Generate UniFFI bindings for iOS (simpler approach - no separate crate needed)
uniffi-ios:
    @echo "🔗 Generating UniFFI bindings for iOS (core crate approach)..."
    cd crates/core && cargo run --bin uniffi-bindgen generate --library target/release/libfiqhai_core.dylib --language swift --out-dir ../../ios/Generated/

# Note: Rust compilation for Android is handled automatically by Gradle plugin
# No manual build-rust-android needed - the cargoNdk plugin does this when building Android

# Build Rust core for iOS targets
build-rust-ios:
    @echo "🦀 Building Rust core for iOS..."
    cd crates/core && cargo lipo --release

# Run Android app
run-android:
    @echo "📱 Running Android app..."
    cd android && ./gradlew installDebug
    adb shell am start -n com.rizilab.fiqhadvisor/.MainActivity

# Run Android instrumentation tests
test-android:
    @echo "🧪 Running Android instrumentation tests..."
    cd android && ./gradlew connectedDebugAndroidTest

# Clean mobile builds
cleanup-mobile:
    @echo "🧹 Cleaning mobile builds..."
    #!/usr/bin/env sh
    if [ "{{os()}}" = "windows" ]; then \
        cd android && ./gradlew.bat clean; \
        rmdir /s /q android\\core\\src\\main\\java\\uniffi 2>nul || echo "UniFFI bindings folder not found"; \
    else \
        cd android && ./gradlew clean; \
        rm -rf android/core/src/main/java/uniffi/; \
    fi
    #@-cd ios && xcodebuild clean
    #rm -rf ios/Generated/

# Clean only UniFFI generated bindings (for development)
clean-uniffi:
    @echo "🧹 Cleaning UniFFI generated bindings..."
    #!/usr/bin/env sh
    if [ "{{os()}}" = "windows" ]; then \
        rmdir /s /q android\\core\\src\\main\\java\\uniffi 2>nul || echo "UniFFI bindings folder not found"; \
    else \
        rm -rf android/core/src/main/java/uniffi/; \
    fi

# Full mobile build (Android + iOS) - debug builds
build-mobile:
    @echo "📱 Building for all mobile platforms (debug)..."
    @just build-android  # Gradle plugin handles Rust automatically
    #@-just build-rust-ios
    #@-just uniffi-ios
    #@-just build-ios

# Full mobile release build (Android + iOS) - release builds
build-mobile-release:
    @echo "📱 Building for all mobile platforms (release)..."
    @just build-android-release  # Clean release build
    #@-just build-rust-ios
    #@-just uniffi-ios
    #@-just build-ios

# Mobile development workflow (fast incremental builds - Ferrostar approach)
mobile-dev:
    @echo "🔄 Mobile development workflow (Gradle-integrated)..."
    @just fmt
    @just clippy
    @just test
    @just build-android      # Gradle handles Rust + UniFFI + Android
    @echo "✅ Mobile development cycle completed!"

# Mobile release workflow (clean build for reliability - Ferrostar approach)
mobile-release:
    @echo "🚀 Mobile release workflow (Gradle-integrated)..."
    @just fmt
    @just clippy
    @just test
    @just build-android-release  # Gradle handles everything
    @echo "✅ Mobile release cycle completed!"

# ============================================================================
# MOBILE-SPECIFIC TESTING
# ============================================================================

# Test mobile bindings
test-mobile-bindings:
    @echo "🧪 Testing mobile bindings..."
    cd crates/core && cargo test --features mobile

# Test UniFFI binding generation only (manual approach for debugging)
test-uniffi-generation:
    @echo "🔗 Testing UniFFI binding generation (manual)..."
    @just build-rust-android
    @just generate-uniffi-android
    @echo "✅ UniFFI binding generation test completed!"

# Benchmark mobile performance
bench-mobile:
    @echo "📊 Benchmarking mobile performance..."
    cargo bench benchmark_performance_target

# Validate mobile integration (modern approach)
validate-mobile:
    @echo "✅ Validating mobile integration (modern approach)..."
    @just build-android  # Gradle plugin handles everything
    @just test-android
    @echo "✅ Mobile integration validation completed!"

# ============================================================================
# DEVELOPMENT TOOLS
# ============================================================================

# Format all code
fmt:
    @echo "🎨 Formatting code..."
    cargo fmt --all

# Check code formatting
fmt-check:
    @echo "🎨 Checking code formatting..."
    cargo fmt --all -- --check

# Run clippy lints
clippy:
    @echo "📎 Running clippy lints..."
    cargo clippy --all --all-targets -- -D warnings

# Fix clippy issues automatically
clippy-fix:
    @echo "📎 Fixing clippy issues..."
    cargo clippy --all --all-targets --fix -- -D warnings

# Check all code (fmt + clippy + test)
check:
    @echo "✅ Running full code check..."
    @just fmt-check
    @just clippy
    @just test

# Clean build artifacts
clean:
    @echo "🧹 Cleaning build artifacts..."
    cargo clean

# Update dependencies
update:
    @echo "📦 Updating dependencies..."
    cargo update

# ============================================================================
# ANALYSIS AND METRICS
# ============================================================================

# Generate documentation
docs:
    @echo "📚 Generating documentation..."
    cargo doc --all --no-deps --open

# Analyze code metrics
metrics:
    @echo "📊 Analyzing code metrics..."
    cargo bloat --release --crates
    echo "\n=== Binary sizes ==="
    ls -lh target/release/ | grep -v ".d$"

# Check binary size
size:
    @echo "📏 Checking binary sizes..."
    cargo build --release
    ls -lh target/release/ | grep -E "(core|desktop|web)$"

# Security audit
audit:
    @echo "🔒 Running security audit..."
    cargo audit

# ============================================================================
# DATABASE AND SERVICES
# ============================================================================

# Start Qdrant vector database (Docker)
start-qdrant:
    @echo "🔍 Starting Qdrant vector database..."
    docker run -d --name qdrant-fiqh \
        -p 6333:6333 \
        -v qdrant_storage:/qdrant/storage \
        qdrant/qdrant

# Stop Qdrant
stop-qdrant:
    @echo "🛑 Stopping Qdrant..."
    docker stop qdrant-fiqh && docker rm qdrant-fiqh

# Start all development services
dev-services:
    @echo "🚀 Starting development services..."
    @just start-qdrant

# Stop all development services  
stop-services:
    @echo "🛑 Stopping development services..."
    @just stop-qdrant

# ============================================================================
# RUNNING APPLICATIONS
# ============================================================================

# Run web API server
run-web:
    @echo "🌐 Starting web API server..."
    cargo run -p web

# Run desktop application
run-desktop:
    @echo "🖥️ Starting desktop application..."
    cargo run -p dekstop

# Run with environment variables
run-env:
    @echo "🌐 Starting with development environment..."
    RUST_LOG=debug OPENAI_API_KEY=${OPENAI_API_KEY:-"test_key"} cargo run -p web

# ============================================================================
# DEPLOYMENT
# ============================================================================

# Build for deployment
deploy-build:
    @echo "🚀 Building for deployment..."
    cargo build --all --release
    strip target/release/web target/release/dekstop 2>/dev/null || true

# Run performance validation before deploy
deploy-check:
    @echo "✅ Running deployment validation..."
    @just build-release
    @just test
    @just bench-perf-target
    @echo "✅ Deployment validation passed!"

# ============================================================================
# QUICK DEVELOPMENT WORKFLOWS
# ============================================================================

# Quick development cycle: format, check, test
quick:
    @echo "⚡ Quick development cycle..."
    @just fmt
    @just clippy
    @just test

# Full development cycle: everything
full:
    @echo "🔄 Full development cycle..."
    @just clean
    @just build
    @just check
    @just bench-analysis
    @echo "✅ Full cycle completed!"

# Setup development environment
setup:
    @echo "🎯 Setting up development environment..."
    rustup component add clippy rustfmt
    cargo install cargo-tarpaulin cargo-bloat cargo-audit
    @just dev-services
    @echo "✅ Development environment ready!"

# Reset everything (clean + stop services)
reset:
    @echo "🔄 Resetting development environment..."
    @just clean
    @just stop-services

# Show project status
status:
    @echo "📊 FiqhAI Project Status"
    @echo "======================="
    @echo "📦 Workspace packages:"
    cargo metadata --no-deps --format-version 1 | jq -r '.workspace_members[] | split(" ") | .[0]'
    @echo "\n🔧 Build status:"
    cargo check --all --message-format short 2>/dev/null && echo "✅ All crates compile" || echo "❌ Compilation issues"
    @echo "\n🧪 Test status:"
    cargo test --all --no-run 2>/dev/null && echo "✅ Tests compile" || echo "❌ Test compilation issues"
    @echo "\n📏 Binary sizes:"
    @just size 2>/dev/null || echo "❌ No release binaries found"
