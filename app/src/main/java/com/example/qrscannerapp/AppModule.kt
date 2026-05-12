package com.example.qrscannerapp

import android.content.Context
import coil.ImageLoader
import com.example.qrscannerapp.features.electrician.data.local.dao.RepairLogDao
import com.example.qrscannerapp.features.electrician.data.repository.RepairLogRepository
import com.example.qrscannerapp.features.interaction.data.local.dao.InteractionDao
import com.example.qrscannerapp.features.interaction.data.repository.InteractionRepository
import com.example.qrscannerapp.features.inventory.data.local.dao.StorageCellDao
import com.example.qrscannerapp.features.inventory.data.local.dao.StoragePalletDao
import com.example.qrscannerapp.features.profile.data.repository.EmployeeProfileRepository
import com.example.qrscannerapp.features.scanner.data.local.dao.ScanSessionDao
import com.example.qrscannerapp.features.scanner.data.repository.ScanSessionRepository
import com.example.qrscannerapp.features.tasks.data.local.dao.TaskDao
import com.example.qrscannerapp.features.vehicle_report.data.local.dao.VehicleReportHistoryDao
import com.example.qrscannerapp.performance.DevicePerformanceManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePresenceManager(firestore: FirebaseFirestore): PresenceManager {
        return PresenceManager(firestore)
    }

    @Provides
    @Singleton
    fun provideAuthManager(
        @ApplicationContext context: Context,
        telemetryManager: TelemetryManager,
        presenceManager: PresenceManager
    ): AuthManager {
        return AuthManager(context, presenceManager) { telemetryManager }
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

    @Provides
    @Singleton
    fun provideInteractionDao(database: AppDatabase): InteractionDao {
        return database.interactionDao()
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
    fun provideInteractionRepository(
        interactionDao: InteractionDao,
        firestore: FirebaseFirestore
    ): InteractionRepository {
        return InteractionRepository(interactionDao, firestore)
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

    // V-- ПРОВАЙДЕРЫ ДЛЯ ПРЕДЗАГРУЗКИ --V

    @Singleton
    @Provides
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Singleton
    @Provides
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .respectCacheHeaders(false)
            .build()
    }
    // ^-- КОНЕЦ ПРОВАЙДЕРОВ --^
}