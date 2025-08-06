pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

rootProject.name = "Averroes"

// Existing Android modules
include(":app")
include(":core")  // Re-enabled for running the app

// Mobile Wallet Adapter modules (using published versions instead of local projects)
// include(":mobile-wallet-adapter:android:common")
// include(":mobile-wallet-adapter:android:clientlib")
// include(":mobile-wallet-adapter:android:clientlib-ktx")

// Set project directories for mobile wallet adapter
// project(":mobile-wallet-adapter:android:common").projectDir = file("../../uniffi-collection/mobile-wallet-adapter/android/common")
// project(":mobile-wallet-adapter:android:clientlib").projectDir = file("../../uniffi-collection/mobile-wallet-adapter/android/clientlib")
// project(":mobile-wallet-adapter:android:clientlib-ktx").projectDir = file("../../uniffi-collection/mobile-wallet-adapter/android/clientlib-ktx")