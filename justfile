# Averroes Development Commands
# Run `just --list` to see all available commands

# Default command - show help
default:
    @just --list

# ============================================================================
# BENCHMARKS
# ============================================================================

# Run all performance benchmarks
bench:
    @echo "🚀 Running all Averroes performance benchmarks..."
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
    @echo "🔨 Building Averroes (development)..."
    cargo build --all

# Build all crates in release mode
build-release:
    @echo "🚀 Building Averroes (release)..."
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
    cd ios && xcodebuild -workspace Averroes.xcworkspace -scheme Averroes -destination "generic/platform=iOS" build

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

# List connected Android devices
list-devices:
    @echo "📱 Connected Android devices:"
    adb devices

# Run Android app (handles multiple devices)
run-android:
    @echo "📱 Running Android app..."
    cd android && ./gradlew installDebug
    #!/usr/bin/env bash
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | cut -f1)
    DEVICE_COUNT=$(echo "$DEVICES" | wc -l)
    if [ $DEVICE_COUNT -eq 1 ]; then
    echo "📱 Installing and running on device: $DEVICES"
    adb -s $DEVICES shell am start -n com.rizilab.averroes/.MainActivity
    elif [ $DEVICE_COUNT -gt 1 ]; then
    echo "⚠️ Multiple devices found. Please specify device:"
    echo "$DEVICES"
    echo "Use: just run-android-device <device_id>"
    else
    echo "❌ No devices found. Please connect a device or start an emulator."
    fi

# Run Android app on specific device
run-android-device DEVICE:
    @echo "📱 Running Android app on device: {{DEVICE}}..."
    cd android && ./gradlew installDebug
    adb -s {{DEVICE}} shell am start -n com.rizilab.averroes/.MainActivity

# Install APK on all connected devices
install-android-all:
    @echo "📱 Installing on all connected devices..."
    cd android && ./gradlew installDebug
    #!/usr/bin/env bash
    adb devices | grep "device$" | cut -f1 | while read device; do
    echo "📱 Installing on $device"
    adb -s $device install -r android/app/build/outputs/apk/debug/app-debug.apk
    done

# Run Android instrumentation tests
test-android:
    @echo "🧪 Running Android instrumentation tests..."
    cd android && ./gradlew connectedDebugAndroidTest

# Fix ADB connection issues
fix-adb:
    @echo "🔧 Fixing ADB connection issues..."
    #!/usr/bin/env bash
    echo "🛑 Killing ADB server..."
    adb kill-server || true
    sleep 2
    echo "🔄 Starting ADB server..."
    adb start-server
    sleep 2
    echo "📱 Connected devices:"
    adb devices
    echo "✅ ADB fix completed!"

# Clean up offline/problematic devices
clean-adb-devices:
    @echo "🧹 Cleaning up ADB devices..."
    #!/usr/bin/env bash
    echo "📱 Current devices:"
    adb devices
    echo "🛑 Killing ADB server..."
    adb kill-server
    sleep 2
    echo "🔄 Restarting ADB server..."
    adb start-server
    sleep 2
    echo "📱 Cleaned devices:"
    adb devices

# Connect to BlueStacks emulator
connect-bluestacks:
    @echo "🔵 Connecting to BlueStacks emulator..."
    adb connect 127.0.0.1:5555
    @echo "📱 Connected devices:"
    adb devices

# Disconnect from BlueStacks
disconnect-bluestacks:
    @echo "🔵 Disconnecting from BlueStacks..."
    adb disconnect 127.0.0.1:5555
    @echo "📱 Remaining devices:"
    adb devices

# Uninstall app from BlueStacks (to fix signature conflicts)
uninstall-android-bluestacks:
    @echo "🔵 Uninstalling app from BlueStacks..."
    adb connect 127.0.0.1:5555
    adb -s 127.0.0.1:5555 uninstall com.rizilab.averroes || echo "App not installed or already uninstalled"

# Install only on BlueStacks (excludes other devices)
install-bluestacks-only:
    @echo "🔵 Installing app only on BlueStacks..."
    adb connect 127.0.0.1:5555
    cd android && ./gradlew assembleDebug
    adb -s 127.0.0.1:5555 install -r android/app/build/outputs/apk/debug/app-debug.apk

# Run Android app on BlueStacks specifically
run-android-bluestacks:
    @echo "🔵 Running Android app on BlueStacks..."
    adb connect 127.0.0.1:5555
    @just install-bluestacks-only
    adb -s 127.0.0.1:5555 shell am start -n com.rizilab.averroes/.MainActivity

# Clean install on BlueStacks (uninstall + install)
clean-install-bluestacks:
    @echo "🔵 Clean installing app on BlueStacks..."
    @just uninstall-android-bluestacks
    adb connect 127.0.0.1:5555
    cd android && ./gradlew installDebug -Pandroid.injected.testOnly=false
    adb -s 127.0.0.1:5555 shell am start -n com.rizilab.averroes/.MainActivity

# Force install on BlueStacks (bypasses signature checks)
force-install-bluestacks:
    @echo "🔵 Force installing app on BlueStacks..."
    adb connect 127.0.0.1:5555
    @just uninstall-android-bluestacks
    cd android && ./gradlew assembleDebug
    adb -s 127.0.0.1:5555 install -r android/app/build/outputs/apk/debug/app-debug.apk
    adb -s 127.0.0.1:5555 shell am start -n com.rizilab.averroes/.MainActivity

# Start Android emulator (if available)
start-emulator:
    @echo "🚀 Starting Android emulator..."
    #!/usr/bin/env bash
    EMULATOR_NAME=$(emulator -list-avds | head -1)
    if [ -n "$EMULATOR_NAME" ]; then
    echo "🚀 Starting emulator: $EMULATOR_NAME"
    emulator -avd $EMULATOR_NAME -no-snapshot-load &
    echo "⏳ Waiting for emulator to boot..."
    adb wait-for-device
    echo "✅ Emulator ready!"
    else
    echo "❌ No emulators found. Create one in Android Studio first."
    fi

# Setup BlueStacks for development
setup-bluestacks:
    @echo "🔵 Setting up BlueStacks for development..."
    @echo "1. Make sure BlueStacks is running"
    @echo "2. Enable ADB debugging in BlueStacks settings"
    @echo "3. Connecting to BlueStacks..."
    adb connect 127.0.0.1:5555
    @echo "📱 Connected devices:"
    adb devices
    @echo "✅ BlueStacks setup completed!"

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
    @just check-wsl-filesystem  # Check WSL health before build
    @just build-android      # Gradle handles Rust + UniFFI + Android
    @echo "✅ Mobile development cycle completed!"

# Quick Android development cycle (build + install + run)
android-dev:
    @echo "📱 Quick Android development cycle..."
    @just fix-adb           # Fix any ADB issues first
    @just build-android     # Build the app
    @just run-android       # Install and run
    @echo "✅ Android development cycle completed!"

# Quick Android development cycle with BlueStacks
android-dev-bluestacks:
    @echo "🔵 Quick Android development cycle (BlueStacks)..."
    @just connect-bluestacks  # Connect to BlueStacks
    @just build-android       # Build the app
    @just run-android-bluestacks  # Install and run on BlueStacks
    @echo "✅ Android BlueStacks development cycle completed!"

# Clean Android development cycle with BlueStacks (fixes signature issues)
android-dev-bluestacks-clean:
    @echo "🔵 Clean Android development cycle (BlueStacks)..."
    @just connect-bluestacks  # Connect to BlueStacks
    @just build-android       # Build the app
    @just clean-install-bluestacks  # Clean install and run on BlueStacks
    @echo "✅ Clean Android BlueStacks development cycle completed!"

# Android development with device check
android-dev-safe:
    @echo "📱 Safe Android development cycle..."
    @just list-devices      # Show available devices
    @just fix-adb          # Fix any ADB issues
    @just build-android    # Build the app
    @just install-android-all  # Install on all devices
    @echo "✅ Safe Android development cycle completed!"

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
    @echo "📊 Averroes Project Status"
    @echo "======================="
    @echo "📦 Workspace packages:"
    cargo metadata --no-deps --format-version 1 | jq -r '.workspace_members[] | split(" ") | .[0]'
    @echo "\n🔧 Build status:"
    cargo check --all --message-format short 2>/dev/null && echo "✅ All crates compile" || echo "❌ Compilation issues"
    @echo "\n🧪 Test status:"
    cargo test --all --no-run 2>/dev/null && echo "✅ Tests compile" || echo "❌ Test compilation issues"
    @echo "\n📏 Binary sizes:"
    @just size 2>/dev/null || echo "❌ No release binaries found"

# ============================================================================
# GRADLE DAEMON & WSL FIX COMMANDS
# ============================================================================

# Fix WSL I/O errors and Gradle daemon corruption
fix-gradle-wsl:
    #!/usr/bin/env bash
    echo "🔧 Fixing WSL I/O errors and Gradle daemon issues..."
    cd android
    
    # Stop all Gradle daemons
    echo "⏹️ Stopping Gradle daemons..."
    ./gradlew --stop || true
    
    # Kill any remaining Gradle processes
    echo "🔥 Killing remaining Gradle processes..."
    pkill -f gradle || true
    pkill -f "Gradle Daemon" || true
    
    # Clear Gradle caches and temporary files
    echo "🗑️ Clearing Gradle caches..."
    rm -rf ~/.gradle/caches/ || true
    rm -rf ~/.gradle/daemon/ || true
    rm -rf .gradle/ || true
    rm -rf build/ || true
    rm -rf app/build/ || true
    rm -rf core/build/ || true
    
    # Clear Android build outputs
    echo "🗑️ Clearing Android build outputs..."
    find . -name "*.tmp" -delete || true
    find . -name "*.lock" -delete || true
    
    # Restart Gradle daemon with fresh settings
    echo "🔄 Restarting Gradle daemon..."
    ./gradlew --daemon --refresh-dependencies
    
    echo "✅ Gradle WSL fix completed!"

# Clean build for when gradle is corrupted
clean-build:
    #!/usr/bin/env bash
    echo "🧹 Performing complete clean build..."
    
    # Fix WSL/Gradle issues first
    just fix-gradle-wsl
    
    # Clean Rust target directory
    echo "🗑️ Cleaning Rust targets..."
    cargo clean
    
    # Rebuild everything
    echo "🔨 Starting fresh build..."
    cd android
    ./gradlew clean
    ./gradlew :core:assembleDebug
    
    echo "✅ Clean build completed!"

# Quick WSL file system check
check-wsl-filesystem:
    #!/usr/bin/env bash
    echo "🔍 Checking WSL file system health..."
    
    # Check disk space
    echo "💾 Disk space:"
    df -h /mnt/d
    
    # Check file permissions
    echo "📂 File permissions:"
    ls -la android/
    
    # Check for locked files
    echo "🔒 Checking for locked files:"
    lsof +D android/ 2>/dev/null | head -10 || echo "No locked files found"
    
    # Test file creation
    echo "📝 Testing file creation..."
    touch android/test_file.tmp && rm android/test_file.tmp && echo "✅ File I/O works" || echo "❌ File I/O failed"
