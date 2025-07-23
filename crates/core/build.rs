fn main() {
    // Configure OpenSSL for Android cross-compilation
    if let Ok(target) = std::env::var("TARGET") {
        if target.contains("android") {
            // Force vendored OpenSSL for Android targets
            println!("cargo:rustc-env=OPENSSL_STATIC=1");
            println!("cargo:rustc-env=OPENSSL_VENDORED=1");

            // Disable system OpenSSL detection
            println!("cargo:rustc-env={}_OPENSSL_LIB_DIR=", target.to_uppercase().replace('-', "_"));
            println!("cargo:rustc-env={}_OPENSSL_INCLUDE_DIR=", target.to_uppercase().replace('-', "_"));
        }
    }

    // UniFFI will automatically generate bindings from Rust annotations
    // No UDL file needed with modern UniFFI
    println!("cargo:rerun-if-changed=src/lib.rs");
}
