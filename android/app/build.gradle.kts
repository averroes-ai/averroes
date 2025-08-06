plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.0.0-1.0.23"
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.rizilab.averroes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rizilab.averroes"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Ensure all architectures are included
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            // Use debug keystore for easier installation
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "SOLANA_NETWORK", "\"devnet\"")
            buildConfigField("String", "RPC_URL", "\"https://api.devnet.solana.com\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "SOLANA_NETWORK", "\"mainnet\"")
            buildConfigField("String", "RPC_URL", "\"https://api.mainnet-beta.solana.com\"")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    ndkVersion = rootProject.extra["ndkVersion"] as String
}

dependencies {
    // Core module with UniFFI bindings (re-enabled for fiqh_core integration)
    implementation(project(":core"))

    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose BOM - use stable version compatible with Kotlin 2.0.0
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    
    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    
    // Material Design 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // ViewModel & Navigation - stable versions
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Koin for dependency injection (re-enabled for AI integration)
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")

    // Kotlinx Serialization for wallet data storage
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // HTTP client for Solana RPC calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Mobile Wallet Adapter for Solana wallet connection (re-enabled for wallet integration)
    implementation("com.solanamobile:mobile-wallet-adapter-clientlib:2.0.0")
    implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.0")
    implementation("com.neovisionaries:nv-websocket-client:2.14")

    // Networking for crypto API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Solana Web3 for blockchain interactions
    implementation("com.solanamobile:web3-solana:0.3.0-beta4")

    // Charts for crypto price visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")

    // BIP39 mnemonic generation (temporarily commented due to repository issues)
    // implementation("io.github.novacrypto:BIP39:2019.01.27")
    // implementation("io.github.novacrypto:SecureString:2019.01.27")

    // Security and encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
} 