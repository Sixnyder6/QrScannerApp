package com.example.qrscannerapp.features.delivery.data

import android.content.Context
import androidx.room.Room
import com.example.qrscannerapp.features.delivery.data.local.DeliveryDatabase
import com.example.qrscannerapp.features.delivery.data.local.dao.DeliveryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DeliveryModule {

    @Provides
    @Singleton
    fun provideDeliveryDatabase(@ApplicationContext context: Context): DeliveryDatabase {
        return Room.databaseBuilder(
            context,
            DeliveryDatabase::class.java,
            "delivery_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideDeliveryDao(database: DeliveryDatabase): DeliveryDao {
        return database.deliveryDao()
    }

    @Provides
    @Singleton
    fun provideDeliveryPdfExporter(@ApplicationContext context: Context): DeliveryPdfExporter {
        return DeliveryPdfExporter(context)
    }
}