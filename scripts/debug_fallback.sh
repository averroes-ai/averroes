#!/bin/bash

# averroes Fallback Response Debugging Script
# This script helps debug why the AI system is returning fallback responses

echo "🔍 averroes Fallback Response Debugger"
echo "========================================"

# Function to check if adb is available
check_adb() {
    if ! command -v adb &> /dev/null; then
        echo "❌ ADB not found. Please install Android SDK platform-tools."
        exit 1
    fi
}

# Function to check if device is connected
check_device() {
    local devices=$(adb devices | grep -v "List of devices" | grep -v "^$" | wc -l)
    if [ $devices -eq 0 ]; then
        echo "❌ No Android device/emulator connected."
        exit 1
    fi
    echo "✅ Android device connected"
}

# Function to clear logs and prepare monitoring
prepare_monitoring() {
    echo "🧹 Clearing logs and preparing monitoring..."
    adb logcat -c
    echo "✅ Logs cleared"
}

# Function to monitor initialization flow
monitor_initialization() {
    echo "👀 Monitoring AI system initialization..."
    echo "Launch the app now and perform a query"
    echo "Press Ctrl+C to stop monitoring"
    echo "----------------------------------------"
    
    # Monitor with emojis and specific patterns
    adb logcat -v time | grep -E "(AverroesViewModel|AverroesManager|🔍|✅|❌|⚠️|💥|System not initialized|initialization failed|Native library)" --color=always
}

# Function to check native library loading
check_native_library() {
    echo "📚 Checking native library loading..."
    echo "Launch the app and watch for library loading messages..."
    echo "Press Ctrl+C to stop"
    echo "----------------------------------------"
    
    adb logcat -v time | grep -E "(library|JNI|uniffi|libfiqh_core|UnsatisfiedLinkError)" --color=always
}

# Function to monitor specific ViewModel flow
monitor_viewmodel_flow() {
    echo "🎯 Monitoring ViewModel analysis flow..."
    echo "Perform a query in the app (e.g., type 'Bitcoin')"
    echo "Press Ctrl+C to stop"
    echo "----------------------------------------"
    
    adb logcat -v time | grep "AverroesViewModel" --color=always
}

# Function to check AverroesManager status
check_manager_status() {
    echo "⚙️ Checking AverroesManager initialization status..."
    echo "Launch the app and watch for manager messages..."
    echo "Press Ctrl+C to stop"
    echo "----------------------------------------"
    
    adb logcat -v time | grep "AverroesManager" --color=always
}

# Function to run comprehensive diagnostic test
run_diagnostic_test() {
    echo "🧪 Running comprehensive diagnostic test..."
    
    # Clear logs
    prepare_monitoring
    
    echo "📱 Installing and launching app..."
    cd /mnt/d/Engineer/web3/rizilab/averroes/android
    
    # Build and install
    if ./gradlew installDebug --quiet; then
        echo "✅ App installed successfully"
        
        # Launch app
        adb shell am start -n com.rizilab.averroes/.MainActivity
        echo "🚀 App launched"
        
        echo "⏱️  Monitoring for 30 seconds..."
        echo "Please perform a query in the app (e.g., type 'Bitcoin')"
        
        # Monitor for 30 seconds
        timeout 30s adb logcat -v time | grep -E "(AverroesViewModel|AverroesManager|🔍|✅|❌|⚠️|💥)" --color=always
        
        echo "✅ Diagnostic test completed"
    else
        echo "❌ App installation failed"
        exit 1
    fi
}

# Function to save diagnostic logs
save_diagnostic_logs() {
    local filename="averroes_fallback_debug_$(date +%Y%m%d_%H%M%S).log"
    echo "💾 Saving diagnostic logs to $filename..."
    echo "Launch the app and perform a query, then press Ctrl+C"
    
    # Save logs for analysis
    adb logcat -v threadtime | grep -E "(AverroesViewModel|AverroesManager|uniffi|library|initialization|fallback)" > "$filename" &
    local log_pid=$!
    
    echo "📝 Logging to $filename (PID: $log_pid)"
    echo "Press Ctrl+C to stop logging"
    
    # Wait for user interrupt
    trap "kill $log_pid 2>/dev/null; echo; echo '✅ Logs saved to $filename'" INT
    wait $log_pid
}

# Function to analyze common issues
analyze_common_issues() {
    echo "🔍 Analyzing common initialization issues..."
    
    echo "1. Checking for native library loading errors..."
    adb logcat -d | grep -E "(UnsatisfiedLinkError|library.*not found|JNI.*error)" | tail -5
    
    echo "2. Checking for initialization failures..."
    adb logcat -d | grep -E "(initialization failed|System not initialized)" | tail -5
    
    echo "3. Checking for UniFFI errors..."
    adb logcat -d | grep -E "(uniffi.*error|uniffi.*exception)" | tail -5
    
    echo "4. Checking for recent AverroesManager messages..."
    adb logcat -d | grep "AverroesManager" | tail -10
    
    echo "5. Checking for recent ViewModel messages..."
    adb logcat -d | grep "AverroesViewModel" | tail -10
}

# Main menu
show_menu() {
    echo ""
    echo "📋 Choose debugging option:"
    echo "1. Monitor initialization flow (recommended)"
    echo "2. Check native library loading"
    echo "3. Monitor ViewModel flow only"
    echo "4. Check AverroesManager status"
    echo "5. Run comprehensive diagnostic test"
    echo "6. Save diagnostic logs to file"
    echo "7. Analyze recent logs for common issues"
    echo "8. Clear logs"
    echo "9. Exit"
    echo ""
}

# Main script execution
main() {
    check_adb
    check_device
    
    while true; do
        show_menu
        read -p "Enter your choice (1-9): " choice
        
        case $choice in
            1)
                prepare_monitoring
                monitor_initialization
                ;;
            2)
                prepare_monitoring
                check_native_library
                ;;
            3)
                prepare_monitoring
                monitor_viewmodel_flow
                ;;
            4)
                prepare_monitoring
                check_manager_status
                ;;
            5)
                run_diagnostic_test
                ;;
            6)
                save_diagnostic_logs
                ;;
            7)
                analyze_common_issues
                ;;
            8)
                prepare_monitoring
                echo "✅ Logs cleared"
                ;;
            9)
                echo "👋 Goodbye!"
                exit 0
                ;;
            *)
                echo "❌ Invalid choice. Please try again."
                ;;
        esac
        
        echo ""
        read -p "Press Enter to continue..."
    done
}

# Quick commands for direct execution
case "${1:-}" in
    "init")
        check_adb
        check_device
        prepare_monitoring
        monitor_initialization
        ;;
    "test")
        check_adb
        check_device
        run_diagnostic_test
        ;;
    "analyze")
        check_adb
        check_device
        analyze_common_issues
        ;;
    *)
        main
        ;;
esac