# Add any ProGuard configurations needed by the consuming app here.
# These rules are used by all consumers of your library.

# Keep native methods and UniFFI generated classes
-keep class com.rizilab.fiqhadvisor.fiqhcore.** { *; }
-keep class uniffi.** { *; }
-keepclassmembers class * {
    native <methods>;
}

# Keep JNI-related classes and methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep UniFFI callback interfaces
-keep interface com.rizilab.fiqhadvisor.fiqhcore.UniffiRustFutureContinuationCallback { *; }
-keep class com.rizilab.fiqhadvisor.fiqhcore.UniffiLib { *; }

# Prevent obfuscation of Rust-generated classes
-keep class com.rizilab.fiqhadvisor.fiqhcore.FiqhAi* { *; }
-keep class com.rizilab.fiqhadvisor.fiqhcore.*Exception* { *; } 