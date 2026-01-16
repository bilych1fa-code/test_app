package com.test.app.di

import android.content.Context
import androidx.room.Room
import com.test.app.data.AnalyticsLogDao
import com.test.app.data.LogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): LogDatabase {
        return Room.databaseBuilder(
            context,
            LogDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideAnalyticsLogDao(database: LogDatabase): AnalyticsLogDao {
        return database.analyticsLogDao()
    }
}