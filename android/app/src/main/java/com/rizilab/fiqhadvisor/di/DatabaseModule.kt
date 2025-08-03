package com.rizilab.averroes.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.rizilab.averroes.data.database.AppDatabase
import com.rizilab.averroes.data.database.AnalysisDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "fiqh_advisor_db").build()
    }

    @Provides
    fun provideAnalysisDao(database: AppDatabase): AnalysisDao {
        return database.analysisDao()
    }
}
