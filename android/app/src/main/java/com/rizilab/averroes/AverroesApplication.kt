package com.rizilab.averroes

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AverroesApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for dependency injection (re-enabled)
        startKoin {
            androidLogger()
            androidContext(this@AverroesApplication)
            modules(appModule)
        }
    }
}

// Koin module for dependency injection
val appModule = module {
    // Wallet service for Solana integration
    single { com.rizilab.averroes.data.wallet.WalletService() }

    // Wallet repository for data storage
    single { com.rizilab.averroes.data.repository.WalletRepository(androidContext()) }

    // AI Manager for fiqh_core integration
    single { com.rizilab.averroes.core.AverroesManager() }

    // ViewModels
    factory { com.rizilab.averroes.presentation.auth.AuthViewModel() }
    factory { com.rizilab.averroes.presentation.chat.ChatViewModel() }
    factory { com.rizilab.averroes.presentation.crypto.CryptoViewModel() }
    factory { com.rizilab.averroes.presentation.wallet.WalletViewModel(get(), get()) }
}
