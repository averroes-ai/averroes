package com.rizilab.fiqhadvisor.di

import android.content.Context
import com.rizilab.fiqhadvisor.core.FiqhAIManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFiqhAIManager(@ApplicationContext context: Context): FiqhAIManager {
        return FiqhAIManager()
    }
}