fn main() {
    // Tell cargo to rerun this build script if lib.rs changes
    println!("cargo:rerun-if-changed=src/lib.rs");
}
