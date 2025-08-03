package com.rizilab.averroes.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "analyses")
data class AnalysisEntity(
        @PrimaryKey val id: String,
        val tokenSymbol: String,
        val tokenName: String,
        val isHalal: Boolean,
        val complianceScore: Double,
        val confidence: Double,
        val reasoning: String, // JSON string
        val aiReasoning: String?,
        val timestamp: Long
)

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analyses ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE tokenSymbol = :symbol ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestAnalysis(symbol: String): AnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity)
}

@Database(entities = [AnalysisEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao
}
