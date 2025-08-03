plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.github.willir.rust.cargo-ndk-android")
}

android {
    namespace = "com.rizilab.averroes.core"
    compileSdk = 34
    ndkVersion = "29.0.13599879"

    defaultConfig {
        minSdk = 24
        // targetSdk removed as it's deprecated for library modules

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    
    // Essential! UniFFI requires JNA for native bindings
    implementation("net.java.dev.jna:jna:5.15.0@aar")
    
    // Coroutines for async Rust calls - match app versions
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

cargoNdk {
    module = "../crates/core"
    librariesNames = arrayListOf("libfiqh_core.so")
    targets = arrayListOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
    targetDirectory = "../../target"
    extraCargoBuildArguments = arrayListOf("--features", "ai,mobile")
}

// Generate UniFFI bindings - simplified approach to avoid Kotlin DSL issues
afterEvaluate {
    android.libraryVariants.forEach { variant ->
        val bDir = layout.buildDirectory.dir("generated/source/uniffi/${variant.name}/java").get()
        val generateBindings = tasks.register("generate${variant.name.replaceFirstChar { it.uppercaseChar() }}UniFFIBindings", Exec::class.java) {
            workingDir = file("../../crates/core")
            commandLine(
                "cargo", "run", "--bin", "uniffi-bindgen", "generate",
                "--library", "../../target/aarch64-linux-android/${if (variant.name.contains("release")) "release" else "debug"}/libfiqh_core.so",
                "--language", "kotlin",
                "--out-dir", bDir.asFile.absolutePath
            )
            
            dependsOn("buildCargoNdk${variant.name.replaceFirstChar { it.uppercaseChar() }}")
        }

        // Add task dependencies
        tasks.findByName("compile${variant.name.replaceFirstChar { it.uppercaseChar() }}Kotlin")?.dependsOn(generateBindings)
        variant.javaCompileProvider.get().dependsOn(generateBindings)
        
        // Add source directory
        android.sourceSets.getByName(variant.name).java.srcDirs(bDir.asFile)
    }
} 