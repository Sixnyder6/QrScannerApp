// Полное содержимое для ИСПРАВЛЕННОГО файла AppModule.kt

package com.example.qrscannerapp

import android.content.Context
import coil.ImageLoader // <-- НОВЫЙ ИМПОРТ
import com.example.qrscannerapp.features.electrician.data.local.dao.RepairLogDao
import com.example.qrscannerapp.features.electrician.data.repository.RepairLogRepository
import com.example.qrscannerapp.features.inventory.data.local.dao.StorageCellDao
import com.example.qrscannerapp.features.inventory.data.local.dao.StoragePalletDao
import com.example.qrscannerapp.features.profile.data.repository.EmployeeProfileRepository
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

    // --- DAO PROVIDERS ---

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

    // --- FIREBASE & MANAGERS ---

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideDevicePerformanceManager(telemetryManager: TelemetryManager): DevicePerformanceManager {
        return DevicePerformanceManager(telemetryManager)
    }

    // --- REPOSITORIES ---

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
    fun provideEmployeeProfileRepository(
        firestore: FirebaseFirestore,
        telemetryManager: TelemetryManager
    ): EmployeeProfileRepository {
        return EmployeeProfileRepository(firestore, telemetryManager)
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

    // V-- НАЧАЛО НОВЫХ ПРОВАЙДЕРОВ ДЛЯ ПРЕДЗАГРУЗКИ --V

    @Singleton
    @Provides
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Singleton
    @Provides
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            // ИСПРАВЛЕНИЕ: Убран лишний параметр "enable ="
            .respectCacheHeaders(false) // Важно для корректной работы кэша с GitHub
            .build()
    }
    // ^-- КОНЕЦ НОВЫХ ПРОВАЙДЕРОВ --^
}