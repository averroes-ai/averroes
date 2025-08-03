package com.rizilab.averroes.di

/**
 * Simplified DI - using singleton pattern instead of Hilt for now
 * FiqhAIManager uses getInstance() pattern
 */
object AppModule {
    // No complex DI needed for core testing
    // FiqhAIManager.getInstance() provides the singleton
}