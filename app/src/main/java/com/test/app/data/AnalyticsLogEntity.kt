package com.test.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "analytics_logs")
data class AnalyticsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val logMessage: String
)