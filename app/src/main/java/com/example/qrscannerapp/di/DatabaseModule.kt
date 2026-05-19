package com.example.qrscannerapp.di

import com.example.qrscannerapp.AppDatabase
import com.example.qrscannerapp.data.local.dao.TelemetryDao
import com.example.qrscannerapp.features.settings.ui.SpyderAnimationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTelemetryDao(database: AppDatabase): TelemetryDao {
        return database.telemetryDao()
    }

    @Provides
    @Singleton
    fun provideSpyderAnimationDao(database: AppDatabase): SpyderAnimationDao {
        return database.spyderAnimationDao()
    }
}