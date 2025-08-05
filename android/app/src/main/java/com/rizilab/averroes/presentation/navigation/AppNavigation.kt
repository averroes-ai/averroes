package com.rizilab.averroes.presentation.navigation

/**
 * Navigation destinations for the app
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Main : Screen("main")
    object Chat : Screen("chat")
    object CryptoList : Screen("crypto_list")
    object CryptoDetail : Screen("crypto_detail/{cryptoId}") {
        fun createRoute(cryptoId: String) = "crypto_detail/$cryptoId"
    }
    object Wallet : Screen("wallet")
    object SeedVault : Screen("seed_vault")
    object Settings : Screen("settings")
}

/**
 * Bottom navigation tabs
 */
enum class MainTab(
    val route: String,
    val title: String,
    val iconName: String
) {
    AI_CHAT("chat", "AI Chat", "chat"),
    CRYPTO_LIST("crypto_list", "Halal Crypto", "currency_bitcoin"),
    WALLET("wallet", "Wallet", "account_balance_wallet"),
    SETTINGS("settings", "Settings", "settings")
}
