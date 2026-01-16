package com.test.app.repositories

import android.util.Log
import com.test.app.data.AnalyticsLogDao
import com.test.app.data.AnalyticsLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsTracker @Inject constructor(
    private val analyticsLogDao: AnalyticsLogDao
) {
    companion object
    {
        private const val TAG = "APP_ANALYTICS"
    }

    fun getAllLogs(): Flow<List<AnalyticsLogEntity>> {
        return analyticsLogDao.getAllLogs()
    }

    suspend fun log(message: String) {
        Log.i(TAG, message)
        analyticsLogDao.insertLog(
            AnalyticsLogEntity(logMessage = message)
        )
    }

    suspend fun clearAllLogs() {
        analyticsLogDao.clearAllLogs()
    }
}