# FiqhAdvisor AI System Diagnostic Guide

This guide provides comprehensive instructions for testing and diagnosing the FiqhAdvisor AI system integration.

## 📋 Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Built-in Diagnostic Features](#built-in-diagnostic-features)
4. [Using the Test Script](#using-the-test-script)
5. [Manual Testing Steps](#manual-testing-steps)
6. [Understanding Diagnostic Output](#understanding-diagnostic-output)
7. [Troubleshooting Guide](#troubleshooting-guide)
8. [Advanced Debugging](#advanced-debugging)

## 🎯 Overview

The FiqhAdvisor app includes comprehensive diagnostic tools to test and monitor the AI system integration. These tools help verify:

- ✅ API key configuration (Grok and Groq)
- ✅ System initialization and UniFFI bindings
- ✅ AI service connectivity
- ✅ End-to-end query processing
- ✅ Real AI responses vs fallback messages

## 📱 Prerequisites

### Required Tools
```bash
# Android SDK with ADB
adb --version

# Connected Android device or emulator
adb devices
```

### Project Setup
```bash
# Ensure project is built
cd /path/to/fiqhadvisor
cargo build

# Ensure Android project is ready
cd android
./gradlew build
```

## 🔧 Built-in Diagnostic Features

### 1. Automatic Startup Diagnostics

The app automatically runs diagnostics during startup in `FiqhAdvisorApplication.onCreate()`:

```kotlin
AISystemDiagnostics.runComprehensiveDiagnostics()
```

**What it tests:**
- API key configuration validation
- FiqhAI system initialization
- AI service integration
- Query processing readiness

### 2. Manual Diagnostic Testing

You can trigger diagnostics manually from the ViewModel:

```kotlin
// Test AI response generation
viewModel.testAIResponse("Is Bitcoin halal?")
```

### 3. Diagnostic Test Coverage

| Test | Purpose | Success Indicator |
|------|---------|------------------|
| **API Key Configuration** | Validates Grok/Groq keys | `✓ API configuration created successfully` |
| **System Initialization** | Tests FiqhAI system creation | `✓ FiqhAI system created successfully` |
| **AI Service Integration** | Verifies service connectivity | `✓ AI Service integration test completed` |
| **Query Processing** | Tests end-to-end flow | `✓ Query processing test setup completed` |

## 🖥️ Using the Test Script

### Interactive Mode

```bash
# Run the test script
./test_ai_system.sh
```

**Menu Options:**
1. **Monitor all FiqhAdvisor logs** - Real-time log monitoring
2. **Monitor AI diagnostics only** - Focused diagnostic output
3. **Save logs to file (60 seconds)** - Capture logs for analysis
4. **Install and launch app** - Build, install, and start the app
5. **Run comprehensive test** - Full automated test sequence
6. **Clear logs** - Clean log buffer
7. **Exit** - Close the script

### Quick Commands

```bash
# Monitor all logs with colors
./test_ai_system.sh monitor

# Run complete diagnostic test
./test_ai_system.sh test

# Monitor only diagnostic messages
./test_ai_system.sh diagnostics

# Install and launch app
./test_ai_system.sh install
```

## 🧪 Manual Testing Steps

### Step 1: Prepare Environment

```bash
# 1. Connect Android device/emulator
adb devices

# Expected output:
# List of devices attached
# [device_id]    device

# 2. Clear existing logs
adb logcat -c

# 3. Verify project compilation
cd /path/to/fiqhadvisor
cargo build
```

### Step 2: Start Log Monitoring

Open a terminal window and start monitoring:

```bash
# Monitor all FiqhAdvisor related logs
adb logcat -v time | grep -E "(FiqhAdvisor|AISystemDiagnostics|FiqhAIViewModel|uniffi|fiqh)" --color=always
```

**Alternative monitoring options:**
```bash
# Monitor only diagnostics
adb logcat -v time | grep "AISystemDiagnostics" --color=always

# Monitor with timestamps and save to file
adb logcat -v time | grep -i fiqh | tee fiqhadvisor_logs.txt

# Monitor errors only
adb logcat *:E | grep -i fiqh
```

### Step 3: Build and Install App

In a second terminal window:

```bash
# Navigate to Android project
cd /path/to/fiqhadvisor/android

# Build and install debug version
./gradlew installDebug

# Expected output should end with:
# BUILD SUCCESSFUL
```

### Step 4: Launch App and Monitor

```bash
# Launch the app
adb shell am start -n com.rizilab.fiqhadvisor/.MainActivity

# Watch the first terminal for diagnostic output
```

### Step 5: Test AI Functionality

1. **Open the chat interface** in the app
2. **Type a test query** like "Is Bitcoin halal?"
3. **Watch logs** for AI processing messages
4. **Verify response** is from AI (not fallback)

## 📊 Understanding Diagnostic Output

### Success Indicators

```log
AISystemDiagnostics: === Starting AI System Diagnostics ===
AISystemDiagnostics: --- Testing API Key Configuration ---
AISystemDiagnostics: Grok API Key length: 78
AISystemDiagnostics: Groq API Key length: 56
AISystemDiagnostics: Grok API Key prefix: xai-udKDRv...
AISystemDiagnostics: Groq API Key prefix: gsk_KMqz35...
AISystemDiagnostics: ✓ API configuration created successfully
AISystemDiagnostics: Preferred model: grok

AISystemDiagnostics: --- Testing System Initialization ---
AISystemDiagnostics: Creating FiqhAI system...
AISystemDiagnostics: ✓ FiqhAI system created successfully
AISystemDiagnostics: System object: FiqhAiSystem

AISystemDiagnostics: --- Testing AI Service Integration ---
AISystemDiagnostics: Testing system methods...
AISystemDiagnostics: ✓ AI Service integration test completed

AISystemDiagnostics: --- Testing Query Processing ---
AISystemDiagnostics: Testing query: 'What is the ruling on prayer times?'
AISystemDiagnostics: ✓ Query processing test setup completed

AISystemDiagnostics: === AI System Diagnostics Complete ===
```

### Error Indicators

```log
AISystemDiagnostics: ✗ API Key configuration failed: [error details]
AISystemDiagnostics: ✗ System initialization failed: [error details]
AISystemDiagnostics: ✗ AI Service integration failed: [error details]
AISystemDiagnostics: ✗ Query processing test failed: [error details]
```

### AI Response Testing

```log
FiqhAIViewModel: Testing AI response for: 'Is Bitcoin halal?'
AISystemDiagnostics: AI response generated successfully
FiqhAIViewModel: AI response received: AI System Diagnostic: System initialized...
```

## 🚨 Troubleshooting Guide

### Issue: No Diagnostic Logs Appear

**Symptoms:**
- App launches but no diagnostic messages in logs
- Empty or minimal log output

**Solutions:**
```bash
# 1. Check if app is actually starting
adb logcat | grep "FiqhAdvisor"

# 2. Verify log filtering
adb logcat | grep -i diagnostic

# 3. Check for app crashes
adb logcat | grep -E "(FATAL|AndroidRuntime)"

# 4. Monitor all app logs
adb logcat | grep "com.rizilab.fiqhadvisor"
```

### Issue: System Initialization Fails

**Symptoms:**
```log
AISystemDiagnostics: ✗ System initialization failed: [error]
```

**Solutions:**
```bash
# 1. Check UniFFI library loading
adb logcat | grep "uniffi"

# 2. Verify native library loading
adb logcat | grep -E "(JNI|native|library)"

# 3. Check Rust compilation
cd /path/to/fiqhadvisor
cargo build

# 4. Verify Android targets built
cd android
./gradlew :core:buildCargoNdkDebug
```

### Issue: AI Responses Are Fallback Messages

**Symptoms:**
- App shows generic responses instead of AI analysis
- Logs show fallback response generation
- ViewModel logs show "⚠️ System not initialized"

**Debugging Steps:**
```bash
# 1. Monitor ViewModel initialization logs
adb logcat | grep "FiqhAIViewModel"

# 2. Look for specific initialization messages
adb logcat | grep -E "(🔍|✅|❌|⚠️|💥)"

# 3. Check FiqhAIManager logs
adb logcat | grep "FiqhAIManager"

# 4. Monitor UniFFI library loading
adb logcat | grep -E "(uniffi|native|library|JNI)"
```

**Solutions:**
```bash
# 1. Check if native library loads properly
adb logcat | grep "Native library loaded successfully"

# 2. Verify API key configuration
adb logcat | grep -E "(API|key|config)"

# 3. Check for specific error messages
adb logcat | grep -E "(System not initialized|initialization failed)"

# 4. Test with a simple query and monitor logs
# In app: Type "test" and watch logs for initialization flow
```

**Common Causes:**
- Native library (libfiqh_core.so) not loading properly
- FiqhAIManager.initialize() throwing exceptions
- createMobileConfig() parameter mismatch
- UniFFI binding issues between Rust and Android

**Fix Steps:**
1. **Verify native library**: Check if `libfiqh_core.so` is in APK
2. **Check initialization**: Look for "✅ AI system initialized successfully" in logs
3. **API configuration**: Ensure createMobileConfig parameters are correct
4. **Test manually**: Use diagnostic tools to test initialization

### Issue: App Crashes on Startup

**Symptoms:**
- App closes immediately after launch
- FATAL errors in logs

**Solutions:**
```bash
# 1. Check crash logs
adb logcat | grep -A 10 "FATAL EXCEPTION"

# 2. Verify permissions
adb logcat | grep "permission"

# 3. Check native library issues
adb logcat | grep -E "(UnsatisfiedLinkError|library)"

# 4. Clear app data and retry
adb shell pm clear com.rizilab.fiqhadvisor
```

## 🔍 Advanced Debugging

### Detailed Log Analysis

```bash
# Save comprehensive logs
adb logcat -v threadtime > fiqhadvisor_debug_$(date +%Y%m%d_%H%M%S).log

# Filter by specific components
adb logcat | grep -E "(FiqhAI|uniffi|rust)" > rust_integration.log

# Monitor memory usage
adb shell dumpsys meminfo com.rizilab.fiqhadvisor
```

### Performance Monitoring

```bash
# Monitor CPU usage
adb shell top | grep fiqhadvisor

# Check network activity
adb shell netstat | grep fiqhadvisor

# Monitor battery usage
adb shell dumpsys batterystats | grep fiqhadvisor
```

### Testing Different Scenarios

```bash
# Test with airplane mode
adb shell svc wifi disable
adb shell svc data disable

# Test with limited connectivity
adb shell svc wifi enable
adb shell svc data disable

# Test with full connectivity
adb shell svc wifi enable
adb shell svc data enable
```

## 📋 Quick Reference Commands

### Essential Monitoring Commands

```bash
# Start comprehensive monitoring
adb logcat -v time | grep -E "(FiqhAdvisor|AISystemDiagnostics|FiqhAIViewModel)" --color=always

# Monitor diagnostics only
adb logcat | grep "AISystemDiagnostics"

# Monitor with error focus
adb logcat *:W | grep -i fiqh

# Save logs to file
adb logcat | grep -i fiqh > logs_$(date +%Y%m%d_%H%M%S).txt
```

### App Management Commands

```bash
# Install debug version
./gradlew installDebug

# Launch app
adb shell am start -n com.rizilab.fiqhadvisor/.MainActivity

# Force stop app
adb shell am force-stop com.rizilab.fiqhadvisor

# Clear app data
adb shell pm clear com.rizilab.fiqhadvisor

# Uninstall app
adb uninstall com.rizilab.fiqhadvisor
```

### System Information Commands

```bash
# Check device info
adb shell getprop ro.build.version.release

# Check available space
adb shell df /data

# Check running processes
adb shell ps | grep fiqhadvisor

# Check device connectivity
adb shell ping -c 3 8.8.8.8
```

---

## 📞 Support

If you encounter issues not covered in this guide:

1. **Check logs** for specific error messages
2. **Save diagnostic output** to a file for analysis
3. **Verify API keys** are correctly configured
4. **Test network connectivity** from the device
5. **Ensure latest code** is built and deployed

For additional support, include the diagnostic log output when reporting issues.
## Database Permission Issues

### Problem: "Permission denied" when creating database
**Error**: `Failed to spawn history actor: DatabaseError("Permission denied (os error 13)")`

**Root Cause**: The History Actor was trying to create a Sled database file in a location where the Android app doesn't have write permissions.

**Solution**: Modified the History Actor to use in-memory database for Android compatibility:

1. **Updated `HistoryActor::new()`** - Now uses in-memory database when no path is provided
2. **Updated `FiqhAISystem::new()`** - Passes `None` for database path instead of file path
3. **Added fallback logic** - If file creation fails, automatically falls back to in-memory database

**Code Changes**:
```rust
// Before: Always tried to create file database
let db = sled::open(db_path).map_err(|e| HistoryError::DatabaseError(e.to_string()))?;

// After: Uses in-memory database for Android
let db = if let Some(path) = db_path {
    sled::open(&path).or_else(|e| {
        warn!("Failed to create database at {}: {}. Using in-memory database.", path, e);
        sled::Config::new().temporary(true).open()
    }).map_err(|e| HistoryError::DatabaseError(e.to_string()))?
} else {
    info!("Using in-memory database for history storage");
    sled::Config::new().temporary(true).open()
        .map_err(|e| HistoryError::DatabaseError(e.to_string()))?
};
```

**Impact**: 
- ✅ Eliminates permission denied errors on Android
- ✅ History is stored in memory (survives app session)
- ✅ Automatic fallback if file system access fails
- ✅ All tests updated to use in-memory database

---