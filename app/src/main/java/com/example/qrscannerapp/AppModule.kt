// Path: app/src/main/java/com/example/qrscannerapp/AppModule.kt

package com.example.qrscannerapp

import android.content.Context
import com.example.qrscannerapp.features.electrician.data.local.dao.RepairLogDao
import com.example.qrscannerapp.features.electrician.data.repository.RepairLogRepository
// --- V НАЧАЛО ИЗМЕНЕНИЙ V ---
import com.example.qrscannerapp.features.inventory.data.local.dao.StorageCellDao
import com.example.qrscannerapp.features.inventory.data.local.dao.StoragePalletDao
// --- ^ КОНЕЦ ИЗМЕНЕНИЙ ^ ---
import com.example.qrscannerapp.features.scanner.data.local.dao.ScanSessionDao
import com.example.qrscannerapp.features.scanner.data.repository.ScanSessionRepository
import com.example.qrscannerapp.features.tasks.data.local.dao.TaskDao
import com.example.qrscannerapp.features.vehicle_report.data.local.dao.VehicleReportHistoryDao
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.qrscannerapp.performance.DevicePerformanceManager

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthManager(@ApplicationContext context: Context): AuthManager {
        return AuthManager(context)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideRepairLogDao(database: AppDatabase): RepairLogDao {
        return database.repairLogDao()
    }

    @Provides
    @Singleton
    fun provideScanSessionDao(database: AppDatabase): ScanSessionDao {
        return database.scanSessionDao()
    }

    // --- V НАЧАЛО ИЗМЕНЕНИЙ V ---
    @Provides
    @Singleton
    fun provideStorageCellDao(database: AppDatabase): StorageCellDao {
        return database.storageCellDao()
    }

    @Provides
    @Singleton
    fun provideStoragePalletDao(database: AppDatabase): StoragePalletDao {
        return database.storagePalletDao()
    }
    // --- ^ КОНЕЦ ИЗМЕНЕНИЙ ^ ---

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideVehicleReportHistoryDao(database: AppDatabase): VehicleReportHistoryDao {
        return database.vehicleReportHistoryDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideRepairLogRepository(repairLogDao: RepairLogDao): RepairLogRepository {
        return RepairLogRepository(repairLogDao)
    }

    @Provides
    @Singleton
    fun provideScanSessionRepository(scanSessionDao: ScanSessionDao): ScanSessionRepository {
        return ScanSessionRepository(scanSessionDao)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        repairLogRepository: RepairLogRepository,
        scanSessionRepository: ScanSessionRepository
    ): SyncManager {
        return SyncManager(context, repairLogRepository, scanSessionRepository)
    }

    @Provides
    @Singleton
    fun provideTelemetryManager(@ApplicationContext context: Context): TelemetryManager {
        return TelemetryManager(context)
    }

    @Provides
    @Singleton
    fun provideDevicePerformanceManager(telemetryManager: TelemetryManager): DevicePerformanceManager {
        return DevicePerformanceManager(telemetryManager)
    }
}