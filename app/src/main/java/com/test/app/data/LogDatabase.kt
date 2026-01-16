package com.test.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AnalyticsLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LogDatabase : RoomDatabase() {
    abstract fun analyticsLogDao(): AnalyticsLogDao
}