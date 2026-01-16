package com.test.app.data


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface AnalyticsLogDao {

    @Insert
    suspend fun insertLog(log: AnalyticsLogEntity)

    @Query("SELECT * FROM analytics_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllLogs(): Flow<List<AnalyticsLogEntity>>

    @Query("DELETE FROM analytics_logs")
    suspend fun clearAllLogs()
}